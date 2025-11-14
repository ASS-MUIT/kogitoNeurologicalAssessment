package us.dit.muit.hsa.neurologicalassessment.controller;

import java.util.Optional;

import org.kie.kogito.process.Process;
import org.kie.kogito.process.Processes;
import org.kie.kogito.svg.ProcessSvgService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/svg")
public class NeurologicalSvgController {

    private static final Logger logger = LoggerFactory.getLogger(NeurologicalSvgController.class);

    @Autowired
    private Processes processes;

    @Autowired(required = false)
    private ProcessSvgService processSvgService;

    /**
     * Obtiene el SVG de una instancia específica del proceso, mostrando su estado
     * actual
     * 
     * @param processId  El nombre del proceso (ej: "assessment")
     * @param instanceId El ID de la instancia del proceso
     * @return SVG con el estado actual de la instancia
     */
    @GetMapping(value = "/{processId}/{instanceId}", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getProcessInstanceSvg(
            @PathVariable("processId") String processId,
            @PathVariable("instanceId") String instanceId) {

        logger.info("Request for SVG of process '{}' instance '{}'", processId, instanceId);

        if (processSvgService == null) {
            logger.error("ProcessSvgService is not available");
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body("<!-- SVG service not available. Ensure kogito-addons-springboot-process-svg is configured correctly -->");
        }

        Process<?> process = processes.processById(processId);
        if (process == null) {
            logger.error("Process with id '{}' not found", processId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("<!-- Process with id '" + processId + "' not found -->");
        }

        // Verificar que la instancia existe
        if (!process.instances().findById(instanceId).isPresent()) {
            logger.error("Process instance '{}' not found", instanceId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("<!-- Process instance with id '" + instanceId + "' not found -->");
        }

        try {
            // Intentar primero con el processId tal cual llega
            logger.debug("Calling processSvgService.getProcessInstanceSvg('{}', '{}', '')", processId, instanceId);
            Optional<String> svgOptional = processSvgService.getProcessInstanceSvg(processId, instanceId, "");

            if (!svgOptional.isPresent() || svgOptional.get().isEmpty()) {
                logger.warn("SVG generation returned empty result for process '{}' instance '{}'", processId,
                        instanceId);

                // Si el processId parece ser corto (sin punto), intentar con el prefijo
                // completo
                if (!processId.contains(".")) {
                    String fullProcessId = "neurologicalassessment." + processId;
                    logger.debug("Trying with full processId: '{}'", fullProcessId);
                    svgOptional = processSvgService.getProcessInstanceSvg(fullProcessId, instanceId, "");
                }

                if (!svgOptional.isPresent() || svgOptional.get().isEmpty()) {
                    logger.error("SVG generation failed with both full and short processId");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("<!-- Failed to generate SVG for process instance. The BPMN file may not contain diagram information (BPMNDiagram section), or the process-svg addon is not working correctly. -->");
                }
            }

            String svg = svgOptional.get();
            logger.info("Successfully generated SVG ({} chars) for process '{}' instance '{}'", svg.length(), processId,
                    instanceId);

            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("image/svg+xml"))
                    .body(svg);

        } catch (Exception e) {
            logger.error("Error generating SVG for process '{}' instance '{}': {}", processId, instanceId,
                    e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<!-- Error generating SVG: " + e.getMessage() + " -->");
        }
    }

    /**
     * Obtiene el SVG del proceso completo (sin estado de instancia específica)
     * 
     * @param processId El nombre del proceso (ej: "assessment")
     * @return SVG del proceso
     */
    @GetMapping(value = "/processes/{processId}", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getProcessSvg(@PathVariable("processId") String processId) {

        logger.info("Request for SVG of process definition '{}'", processId);

        if (processSvgService == null) {
            logger.error("ProcessSvgService is not available");
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body("<!-- SVG service not available -->");
        }

        Process<?> process = processes.processById(processId);
        if (process == null) {
            logger.error("Process with id '{}' not found", processId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("<!-- Process with id '" + processId + "' not found -->");
        }

        try {
            // Intentar primero con el processId tal cual llega
            logger.debug("Trying to get process SVG with processId: '{}'", processId);
            Optional<String> svgOptional = processSvgService.getProcessSvg(processId);

            if (!svgOptional.isPresent() || svgOptional.get().isEmpty()) {
                // Si el processId parece ser corto (sin punto), intentar con el prefijo
                // completo
                if (!processId.contains(".")) {
                    String fullProcessId = "neurologicalassessment." + processId;
                    logger.debug("Failed with short processId, trying with full: '{}'", fullProcessId);
                    svgOptional = processSvgService.getProcessSvg(fullProcessId);
                }
            }

            if (!svgOptional.isPresent() || svgOptional.get().isEmpty()) {
                logger.error("Failed to generate SVG for process '{}'", processId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("<!-- Failed to generate SVG for process -->");
            }

            String svg = svgOptional.get();
            logger.info("Successfully generated process SVG ({} chars) for '{}'", svg.length(), processId);

            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("image/svg+xml"))
                    .body(svg);

        } catch (Exception e) {
            logger.error("Error generating process SVG for '{}': {}", processId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<!-- Error generating SVG: " + e.getMessage() + " -->");
        }
    }

    /**
     * Endpoint de diagnóstico para verificar el estado del servicio SVG
     * 
     * @return Información sobre la disponibilidad del servicio SVG
     */
    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getSvgServiceStatus() {
        StringBuilder status = new StringBuilder();
        status.append("{\n");
        status.append("  \"processSvgServiceAvailable\": ").append(processSvgService != null).append(",\n");

        if (processSvgService != null) {
            status.append("  \"processSvgServiceClass\": \"").append(processSvgService.getClass().getName())
                    .append("\",\n");
        }

        status.append("  \"availableProcesses\": [\n");
        processes.processIds().forEach(id -> {
            status.append("    \"").append(id).append("\",\n");
        });
        // Remover última coma
        if (status.toString().endsWith(",\n")) {
            status.setLength(status.length() - 2);
            status.append("\n");
        }
        status.append("  ]\n");
        status.append("}");

        logger.info("SVG Service Status: {}", status);
        return ResponseEntity.ok(status.toString());
    }
}