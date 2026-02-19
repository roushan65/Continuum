package com.continuum.base.node

import com.continuum.core.commons.exception.NodeRuntimeException
import com.continuum.core.commons.model.ContinuumWorkflowModel
import com.continuum.core.commons.node.ProcessNodeModel
import com.continuum.core.commons.utils.NodeInputReader
import com.continuum.core.commons.utils.NodeOutputWriter
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.stereotype.Component
import javax.script.ScriptEngineManager

@Component
class ScriptPerRowNodeModel : ProcessNodeModel() {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ScriptPerRowNodeModel::class.java)
        private val objectMapper = ObjectMapper()
    }

    final override val inputPorts = mapOf(
        "data" to ContinuumWorkflowModel.NodePort(
            name = "input table",
            contentType = APPLICATION_JSON_VALUE
        ),
        "script" to ContinuumWorkflowModel.NodePort(
            name = "kotlin script",
            contentType = "text/plain"
        )
    )

    final override val outputPorts = mapOf(
        "data" to ContinuumWorkflowModel.NodePort(
            name = "enriched table",
            contentType = APPLICATION_JSON_VALUE
        )
    )

    override val categories = listOf("Transform")

    val propertiesSchema: Map<String, Any> = objectMapper.readValue(
        """
        {
          "type": "object",
          "properties": {},
          "required": []
        }
        """.trimIndent(),
        object : TypeReference<Map<String, Any>>() {}
    )

    override val metadata = ContinuumWorkflowModel.NodeData(
        id = this.javaClass.name,
        description = "Run a Kotlin script for each row, adding script_result column",
        title = "Script Per Row",
        subTitle = "Evaluate Kotlin script per row",
        nodeModel = this.javaClass.name,
        icon = """
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" stroke="currentColor" strokeWidth={1.5} viewBox="0 0 24 24">
                <path d="M3 7v10a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V7M3 7a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2M3 7h18M8 11l2 2 4-4"/>
            </svg>
        """.trimIndent(),
        inputs = inputPorts,
        outputs = outputPorts,
        properties = emptyMap(),
        propertiesSchema = propertiesSchema
    )

    override fun execute(
        properties: Map<String, Any>?,
        inputs: Map<String, NodeInputReader>,
        nodeOutputWriter: NodeOutputWriter
    ) {
        val scriptReader = inputs["script"] ?: throw NodeRuntimeException(
            workflowId = "",
            nodeId = "",
            message = "Script port required"
        )
        val dataReader = inputs["data"] ?: throw NodeRuntimeException(
            workflowId = "",
            nodeId = "",
            message = "Data port required"
        )

        // Read script (single value)
        val scriptRow = scriptReader.read()
            ?: throw NodeRuntimeException(
                workflowId = "",
                nodeId = "",
                message = "Script port is empty"
            )
        val scriptText = (scriptRow.values.firstOrNull() as? String)
            ?: throw NodeRuntimeException(
                workflowId = "",
                nodeId = "",
                message = "Script must be a string"
            )

        // Create Kotlin script engine
        val engine = ScriptEngineManager().getEngineByName("kotlin")
            ?: throw NodeRuntimeException(
                workflowId = "",
                nodeId = "",
                message = "Kotlin script engine not found"
            )

        nodeOutputWriter.createOutputPortWriter("data").use { writer ->
            dataReader.use { reader ->
                var rowNumber = 0L
                var row = reader.read()
                while (row != null) {
                    try {
                        val bindings = engine.createBindings().apply {
                            put("row", row)
                        }
                        val scriptResult = engine.eval(scriptText, bindings)
                        val enrichedRow = row.toMutableMap().apply {
                            put("script_result", scriptResult)
                        }
                        writer.write(rowNumber, enrichedRow)
                        rowNumber++
                    } catch (e: Exception) {
                        throw NodeRuntimeException(
                            workflowId = "",
                            nodeId = "",
                            message = "Script execution error at row $rowNumber: ${e.message}"
                        )
                    }
                    row = reader.read()
                }
            }
        }
    }
}
