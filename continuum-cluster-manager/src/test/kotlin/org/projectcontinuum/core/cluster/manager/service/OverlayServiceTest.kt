package org.projectcontinuum.core.cluster.manager.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.projectcontinuum.core.cluster.manager.config.OverlayProperties
import java.nio.file.Files
import java.nio.file.Path

class OverlayServiceTest {

  @TempDir
  lateinit var overlayDir: Path

  private lateinit var service: OverlayService

  private val baseDeploymentYaml = """
    apiVersion: apps/v1
    kind: Deployment
    metadata:
      name: wb-test-123-deployment
      namespace: default
      labels:
        app: continuum-workbench
        instance-id: "test-123"
        managed-by: continuum-cluster-manager
    spec:
      replicas: 1
      selector:
        matchLabels:
          app: continuum-workbench
          instance-id: "test-123"
      template:
        metadata:
          labels:
            app: continuum-workbench
            instance-id: "test-123"
            managed-by: continuum-cluster-manager
        spec:
          securityContext:
            runAsUser: 1000
            runAsGroup: 1000
            fsGroup: 1000
          containers:
            - name: theia
              image: projectcontinuum/continuum-workbench:latest
              ports:
                - containerPort: 8080
              resources:
                requests:
                  cpu: "500m"
                  memory: "512Mi"
                limits:
                  cpu: "2"
                  memory: "1Gi"
              volumeMounts:
                - name: workspace-storage
                  mountPath: /workspace
          volumes:
            - name: workspace-storage
              persistentVolumeClaim:
                claimName: wb-test-123-pvc
  """.trimIndent()

  private val baseServiceYaml = """
    apiVersion: v1
    kind: Service
    metadata:
      name: wb-test-123-svc
      namespace: default
      labels:
        app: continuum-workbench
        instance-id: "test-123"
        managed-by: continuum-cluster-manager
    spec:
      type: ClusterIP
      selector:
        app: continuum-workbench
        instance-id: "test-123"
      ports:
        - protocol: TCP
          port: 8080
          targetPort: 8080
  """.trimIndent()

  private val basePvcYaml = """
    apiVersion: v1
    kind: PersistentVolumeClaim
    metadata:
      name: wb-test-123-pvc
      namespace: default
      labels:
        app: continuum-workbench
        instance-id: "test-123"
        managed-by: continuum-cluster-manager
    spec:
      accessModes:
        - ReadWriteOnce
      resources:
        requests:
          storage: 5Gi
  """.trimIndent()

  @BeforeEach
  fun setUp() {
    val properties = OverlayProperties(enabled = true, path = overlayDir.toString())
    service = OverlayService(properties)
  }

  // ── disabled / no-op scenarios ─────────────────────────────────────────

  @Test
  fun `returns base unchanged when overlays disabled`() {
    val disabledService = OverlayService(OverlayProperties(enabled = false))
    val result = disabledService.applyOverlay(baseDeploymentYaml, ResourceType.DEPLOYMENT)
    assertEquals(baseDeploymentYaml, result)
  }

  @Test
  fun `returns base unchanged when overlay file does not exist`() {
    val result = service.applyOverlay(baseDeploymentYaml, ResourceType.DEPLOYMENT)
    assertEquals(baseDeploymentYaml, result)
  }

  @Test
  fun `returns base unchanged when overlay file is empty`() {
    Files.writeString(overlayDir.resolve("deployment.yaml"), "")
    val result = service.applyOverlay(baseDeploymentYaml, ResourceType.DEPLOYMENT)
    assertEquals(baseDeploymentYaml, result)
  }

  @Test
  fun `returns base unchanged when overlay file is whitespace only`() {
    Files.writeString(overlayDir.resolve("deployment.yaml"), "   \n  \n  ")
    val result = service.applyOverlay(baseDeploymentYaml, ResourceType.DEPLOYMENT)
    assertEquals(baseDeploymentYaml, result)
  }

  @Test
  fun `returns base unchanged when overlay directory does not exist`() {
    val missingDir = OverlayService(OverlayProperties(enabled = true, path = "/nonexistent/path"))
    val result = missingDir.applyOverlay(baseDeploymentYaml, ResourceType.DEPLOYMENT)
    assertEquals(baseDeploymentYaml, result)
  }

  // ── deployment overlays ────────────────────────────────────────────────

  @Test
  fun `deployment overlay adds tolerations`() {
    val overlay = """
      spec:
        template:
          spec:
            tolerations:
              - key: "workbench"
                operator: "Equal"
                value: "true"
                effect: "NoSchedule"
    """.trimIndent()
    Files.writeString(overlayDir.resolve("deployment.yaml"), overlay)

    val result = service.applyOverlay(baseDeploymentYaml, ResourceType.DEPLOYMENT)

    assertTrue(result.contains("tolerations"))
    assertTrue(result.contains("workbench"))
    assertTrue(result.contains("NoSchedule"))
  }

  @Test
  fun `deployment overlay adds nodeSelector`() {
    val overlay = """
      spec:
        template:
          spec:
            nodeSelector:
              workload-type: workbench
              accelerator: nvidia-tesla-v100
    """.trimIndent()
    Files.writeString(overlayDir.resolve("deployment.yaml"), overlay)

    val result = service.applyOverlay(baseDeploymentYaml, ResourceType.DEPLOYMENT)

    assertTrue(result.contains("nodeSelector"))
    assertTrue(result.contains("workload-type"))
    assertTrue(result.contains("nvidia-tesla-v100"))
  }

  @Test
  fun `deployment overlay adds annotations to metadata`() {
    val overlay = """
      metadata:
        annotations:
          custom.io/team: platform
          custom.io/env: production
    """.trimIndent()
    Files.writeString(overlayDir.resolve("deployment.yaml"), overlay)

    val result = service.applyOverlay(baseDeploymentYaml, ResourceType.DEPLOYMENT)

    assertTrue(result.contains("annotations"))
    assertTrue(result.contains("custom.io/team"))
    assertTrue(result.contains("platform"))
  }

  @Test
  fun `deployment overlay adds initContainers`() {
    val overlay = """
      spec:
        template:
          spec:
            initContainers:
              - name: setup
                image: busybox:1.36
                command: ["sh", "-c", "echo setup complete"]
    """.trimIndent()
    Files.writeString(overlayDir.resolve("deployment.yaml"), overlay)

    val result = service.applyOverlay(baseDeploymentYaml, ResourceType.DEPLOYMENT)

    assertTrue(result.contains("initContainers"))
    assertTrue(result.contains("setup"))
    assertTrue(result.contains("busybox"))
  }

  @Test
  fun `deployment overlay adds extra labels to metadata`() {
    val overlay = """
      metadata:
        labels:
          team: data-engineering
          environment: staging
    """.trimIndent()
    Files.writeString(overlayDir.resolve("deployment.yaml"), overlay)

    val result = service.applyOverlay(baseDeploymentYaml, ResourceType.DEPLOYMENT)

    assertTrue(result.contains("team"))
    assertTrue(result.contains("data-engineering"))
    // Protected labels must still be present
    assertTrue(result.contains("continuum-workbench"))
    assertTrue(result.contains("test-123"))
    assertTrue(result.contains("continuum-cluster-manager"))
  }

  @Test
  fun `deployment overlay preserves existing securityContext when merging pod spec`() {
    val overlay = """
      spec:
        template:
          spec:
            tolerations:
              - key: "gpu"
                operator: "Exists"
    """.trimIndent()
    Files.writeString(overlayDir.resolve("deployment.yaml"), overlay)

    val result = service.applyOverlay(baseDeploymentYaml, ResourceType.DEPLOYMENT)

    // The securityContext should still be present (it was in base, not overridden by overlay)
    assertTrue(result.contains("securityContext"))
    assertTrue(result.contains("runAsUser"))
    // And the overlay is also applied
    assertTrue(result.contains("tolerations"))
  }

  // ── service overlays ───────────────────────────────────────────────────

  @Test
  fun `service overlay adds annotations`() {
    val overlay = """
      metadata:
        annotations:
          service.beta.kubernetes.io/aws-load-balancer-internal: "true"
    """.trimIndent()
    Files.writeString(overlayDir.resolve("service.yaml"), overlay)

    val result = service.applyOverlay(baseServiceYaml, ResourceType.SERVICE)

    assertTrue(result.contains("annotations"))
    assertTrue(result.contains("aws-load-balancer-internal"))
  }

  // ── PVC overlays ───────────────────────────────────────────────────────

  @Test
  fun `pvc overlay adds storageClassName`() {
    val overlay = """
      spec:
        storageClassName: fast-ssd
    """.trimIndent()
    Files.writeString(overlayDir.resolve("pvc.yaml"), overlay)

    val result = service.applyOverlay(basePvcYaml, ResourceType.PVC)

    assertTrue(result.contains("storageClassName"))
    assertTrue(result.contains("fast-ssd"))
  }

  @Test
  fun `pvc overlay adds annotations`() {
    val overlay = """
      metadata:
        annotations:
          volume.beta.kubernetes.io/storage-provisioner: ebs.csi.aws.com
    """.trimIndent()
    Files.writeString(overlayDir.resolve("pvc.yaml"), overlay)

    val result = service.applyOverlay(basePvcYaml, ResourceType.PVC)

    assertTrue(result.contains("storage-provisioner"))
    assertTrue(result.contains("ebs.csi.aws.com"))
  }

  // ── protected field enforcement ────────────────────────────────────────

  @Test
  fun `overlay cannot change metadata name`() {
    val overlay = """
      metadata:
        name: hacked-name
    """.trimIndent()
    Files.writeString(overlayDir.resolve("deployment.yaml"), overlay)

    val result = service.applyOverlay(baseDeploymentYaml, ResourceType.DEPLOYMENT)

    assertTrue(result.contains("wb-test-123-deployment"))
    assertFalse(result.contains("hacked-name"))
  }

  @Test
  fun `overlay cannot change metadata namespace`() {
    val overlay = """
      metadata:
        namespace: hacked-namespace
    """.trimIndent()
    Files.writeString(overlayDir.resolve("deployment.yaml"), overlay)

    val result = service.applyOverlay(baseDeploymentYaml, ResourceType.DEPLOYMENT)

    assertTrue(result.contains("namespace: \"default\"") || result.contains("namespace: default"))
    assertFalse(result.contains("hacked-namespace"))
  }

  @Test
  fun `overlay cannot change instance-id label`() {
    val overlay = """
      metadata:
        labels:
          instance-id: "hacked-id"
    """.trimIndent()
    Files.writeString(overlayDir.resolve("deployment.yaml"), overlay)

    val result = service.applyOverlay(baseDeploymentYaml, ResourceType.DEPLOYMENT)

    assertTrue(result.contains("test-123"))
    assertFalse(result.contains("hacked-id"))
  }

  @Test
  fun `overlay cannot change app label`() {
    val overlay = """
      metadata:
        labels:
          app: hacked-app
    """.trimIndent()
    Files.writeString(overlayDir.resolve("deployment.yaml"), overlay)

    val result = service.applyOverlay(baseDeploymentYaml, ResourceType.DEPLOYMENT)

    assertTrue(result.contains("continuum-workbench"))
    assertFalse(result.contains("hacked-app"))
  }

  @Test
  fun `overlay cannot change managed-by label`() {
    val overlay = """
      metadata:
        labels:
          managed-by: hacked-manager
    """.trimIndent()
    Files.writeString(overlayDir.resolve("deployment.yaml"), overlay)

    val result = service.applyOverlay(baseDeploymentYaml, ResourceType.DEPLOYMENT)

    assertTrue(result.contains("continuum-cluster-manager"))
    assertFalse(result.contains("hacked-manager"))
  }

  @Test
  fun `overlay cannot change deployment selector matchLabels`() {
    val overlay = """
      spec:
        selector:
          matchLabels:
            instance-id: "hacked-selector"
            app: "hacked-selector-app"
    """.trimIndent()
    Files.writeString(overlayDir.resolve("deployment.yaml"), overlay)

    val result = service.applyOverlay(baseDeploymentYaml, ResourceType.DEPLOYMENT)

    assertFalse(result.contains("hacked-selector"))
  }

  @Test
  fun `overlay cannot change service selector labels`() {
    val overlay = """
      spec:
        selector:
          instance-id: "hacked-selector"
          app: "hacked-selector-app"
    """.trimIndent()
    Files.writeString(overlayDir.resolve("service.yaml"), overlay)

    val result = service.applyOverlay(baseServiceYaml, ResourceType.SERVICE)

    assertFalse(result.contains("hacked-selector"))
    assertFalse(result.contains("hacked-selector-app"))
  }

  // ── error handling ─────────────────────────────────────────────────────

  @Test
  fun `malformed overlay YAML returns base unchanged`() {
    Files.writeString(overlayDir.resolve("deployment.yaml"), "this: is: not: valid: yaml: [[[")

    val result = service.applyOverlay(baseDeploymentYaml, ResourceType.DEPLOYMENT)

    // Should fall back to base YAML
    assertTrue(result.contains("wb-test-123-deployment"))
  }

  @Test
  fun `overlay with non-object root returns base unchanged`() {
    Files.writeString(overlayDir.resolve("deployment.yaml"), "just a string")

    val result = service.applyOverlay(baseDeploymentYaml, ResourceType.DEPLOYMENT)

    assertTrue(result.contains("wb-test-123-deployment"))
  }

  // ── resource type routing ──────────────────────────────────────────────

  @Test
  fun `each resource type reads its own overlay file`() {
    Files.writeString(overlayDir.resolve("deployment.yaml"), """
      metadata:
        annotations:
          overlay: deployment
    """.trimIndent())
    Files.writeString(overlayDir.resolve("service.yaml"), """
      metadata:
        annotations:
          overlay: service
    """.trimIndent())
    Files.writeString(overlayDir.resolve("pvc.yaml"), """
      metadata:
        annotations:
          overlay: pvc
    """.trimIndent())

    val deploymentResult = service.applyOverlay(baseDeploymentYaml, ResourceType.DEPLOYMENT)
    val serviceResult = service.applyOverlay(baseServiceYaml, ResourceType.SERVICE)
    val pvcResult = service.applyOverlay(basePvcYaml, ResourceType.PVC)

    assertTrue(deploymentResult.contains("overlay: deployment") || deploymentResult.contains("overlay: \"deployment\""))
    assertTrue(serviceResult.contains("overlay: service") || serviceResult.contains("overlay: \"service\""))
    assertTrue(pvcResult.contains("overlay: pvc") || pvcResult.contains("overlay: \"pvc\""))
  }

  @Test
  fun `missing overlay for one type does not affect another`() {
    // Only deployment overlay exists
    Files.writeString(overlayDir.resolve("deployment.yaml"), """
      metadata:
        annotations:
          custom: value
    """.trimIndent())

    val deploymentResult = service.applyOverlay(baseDeploymentYaml, ResourceType.DEPLOYMENT)
    val serviceResult = service.applyOverlay(baseServiceYaml, ResourceType.SERVICE)

    assertTrue(deploymentResult.contains("custom"))
    assertEquals(baseServiceYaml, serviceResult)
  }
}
