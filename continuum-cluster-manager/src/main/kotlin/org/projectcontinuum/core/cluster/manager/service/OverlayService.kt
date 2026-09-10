package org.projectcontinuum.core.cluster.manager.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import jakarta.annotation.PostConstruct
import org.projectcontinuum.core.cluster.manager.config.OverlayProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Resource types that can be customized via overlays.
 * Each maps to an expected filename in the overlay directory.
 */
enum class ResourceType(val filename: String) {
  DEPLOYMENT("deployment.yaml"),
  SERVICE("service.yaml"),
  PVC("pvc.yaml")
}

/**
 * Loads YAML overlay files from a mounted ConfigMap directory and deep-merges
 * them onto rendered K8s resource YAML before it is applied to the cluster.
 *
 * Merge semantics:
 * - Object fields merge recursively (maps are merged key-by-key)
 * - Arrays and scalars replace entirely
 * - Critical identity fields (name, namespace, lifecycle labels) are protected
 *   and restored from the base after the merge
 *
 * Error handling is fail-open: overlay failures degrade to the un-overlaid
 * base YAML rather than blocking workbench operations.
 */
@Service
class OverlayService(
  private val overlayProperties: OverlayProperties
) {

  private val logger = LoggerFactory.getLogger(OverlayService::class.java)

  private val yamlMapper: ObjectMapper = ObjectMapper(
    YAMLFactory()
      .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
  )

  /** Labels that must never be changed by an overlay */
  private val protectedLabels = listOf("instance-id", "app", "managed-by")

  @PostConstruct
  fun init() {
    if (!overlayProperties.enabled) {
      logger.info("K8s resource overlays are disabled")
      return
    }

    val overlayDir = Paths.get(overlayProperties.path)
    if (!Files.isDirectory(overlayDir)) {
      logger.warn("Overlay directory does not exist: {}", overlayProperties.path)
      return
    }

    val presentFiles = ResourceType.entries
      .map { it.filename }
      .filter { Files.exists(overlayDir.resolve(it)) }

    logger.info(
      "K8s resource overlays enabled — directory: {}, files present: {}",
      overlayProperties.path,
      if (presentFiles.isEmpty()) "none" else presentFiles.joinToString(", ")
    )
  }

  /**
   * Applies the overlay for [resourceType] onto [baseYaml].
   * Returns the merged YAML string, or [baseYaml] unchanged if overlays are
   * disabled, the overlay file is missing, or an error occurs.
   */
  fun applyOverlay(baseYaml: String, resourceType: ResourceType): String {
    if (!overlayProperties.enabled) {
      return baseYaml
    }

    val overlayFile = Paths.get(overlayProperties.path, resourceType.filename)
    if (!Files.exists(overlayFile)) {
      return baseYaml
    }

    return try {
      val overlayContent = Files.readString(overlayFile).trim()
      if (overlayContent.isEmpty()) {
        return baseYaml
      }

      val baseTree = yamlMapper.readTree(baseYaml) as? ObjectNode
        ?: return baseYaml
      val overlayTree = yamlMapper.readTree(overlayContent) as? ObjectNode
        ?: return baseYaml

      // Snapshot protected fields before merge
      val protectedSnapshot = snapshotProtectedFields(baseTree, resourceType)

      // Deep merge overlay onto base
      deepMerge(baseTree, overlayTree)

      // Restore protected fields
      restoreProtectedFields(baseTree, protectedSnapshot, resourceType)

      val mergedYaml = yamlMapper.writeValueAsString(baseTree)
      logger.info("Applied {} overlay from {}", resourceType.name, overlayFile)
      mergedYaml
    } catch (ex: Exception) {
      logger.error("Failed to apply {} overlay from {}: {}", resourceType.name, overlayFile, ex.message, ex)
      baseYaml
    }
  }

  /**
   * Recursively deep-merges [overlay] into [base].
   * - Object nodes: merge recursively
   * - Array nodes and scalars: overlay replaces base
   */
  private fun deepMerge(base: ObjectNode, overlay: ObjectNode): ObjectNode {
    val fields = overlay.fieldNames()
    while (fields.hasNext()) {
      val fieldName = fields.next()
      val baseVal = base.get(fieldName)
      val overlayVal = overlay.get(fieldName)
      if (baseVal != null && baseVal.isObject && overlayVal != null && overlayVal.isObject) {
        deepMerge(baseVal as ObjectNode, overlayVal as ObjectNode)
      } else {
        base.set<JsonNode>(fieldName, overlayVal)
      }
    }
    return base
  }

  /**
   * Snapshots the fields that must be preserved through the merge.
   */
  private fun snapshotProtectedFields(
    base: ObjectNode,
    resourceType: ResourceType
  ): ProtectedFieldSnapshot {
    val metadata = base.path("metadata")
    val name = metadata.path("name").takeUnless { it.isMissingNode }
    val namespace = metadata.path("namespace").takeUnless { it.isMissingNode }
    val metadataLabels = snapshotLabels(metadata.path("labels"))

    val selectorMatchLabels = when (resourceType) {
      ResourceType.DEPLOYMENT -> snapshotLabels(base.at("/spec/selector/matchLabels"))
      ResourceType.SERVICE -> snapshotLabels(base.at("/spec/selector"))
      ResourceType.PVC -> emptyMap()
    }

    val templateLabels = when (resourceType) {
      ResourceType.DEPLOYMENT -> snapshotLabels(base.at("/spec/template/metadata/labels"))
      else -> emptyMap()
    }

    return ProtectedFieldSnapshot(
      name = name,
      namespace = namespace,
      metadataLabels = metadataLabels,
      selectorMatchLabels = selectorMatchLabels,
      templateLabels = templateLabels
    )
  }

  private fun snapshotLabels(labelsNode: JsonNode): Map<String, JsonNode> {
    if (labelsNode.isMissingNode || !labelsNode.isObject) return emptyMap()
    val result = mutableMapOf<String, JsonNode>()
    for (label in protectedLabels) {
      val value = labelsNode.get(label)
      if (value != null) {
        result[label] = value.deepCopy<JsonNode>()
      }
    }
    return result
  }

  /**
   * Restores protected fields from the snapshot onto the (already merged) tree.
   */
  private fun restoreProtectedFields(
    merged: ObjectNode,
    snapshot: ProtectedFieldSnapshot,
    resourceType: ResourceType
  ) {
    val mergedMeta = merged.path("metadata")
    if (mergedMeta is ObjectNode) {
      snapshot.name?.let { mergedMeta.set<JsonNode>("name", it) }
      snapshot.namespace?.let { mergedMeta.set<JsonNode>("namespace", it) }
      restoreLabelsOnNode(mergedMeta.path("labels"), snapshot.metadataLabels)
    }

    when (resourceType) {
      ResourceType.DEPLOYMENT -> {
        restoreLabelsOnNode(merged.at("/spec/selector/matchLabels"), snapshot.selectorMatchLabels)
        restoreLabelsOnNode(merged.at("/spec/template/metadata/labels"), snapshot.templateLabels)
      }
      ResourceType.SERVICE -> {
        restoreLabelsOnNode(merged.at("/spec/selector"), snapshot.selectorMatchLabels)
      }
      ResourceType.PVC -> { /* no additional protected fields */ }
    }
  }

  private fun restoreLabelsOnNode(node: JsonNode, labels: Map<String, JsonNode>) {
    if (node !is ObjectNode || labels.isEmpty()) return
    labels.forEach { (key, value) ->
      node.set<JsonNode>(key, value)
    }
  }

  private data class ProtectedFieldSnapshot(
    val name: JsonNode?,
    val namespace: JsonNode?,
    val metadataLabels: Map<String, JsonNode>,
    val selectorMatchLabels: Map<String, JsonNode>,
    val templateLabels: Map<String, JsonNode>
  )
}
