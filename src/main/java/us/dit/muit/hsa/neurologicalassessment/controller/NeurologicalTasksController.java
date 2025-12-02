package us.dit.muit.hsa.neurologicalassessment.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.kie.kogito.auth.IdentityProvider;
import org.kie.kogito.process.Process;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.WorkItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import us.dit.muit.hsa.neurologicalassessment.entities.DN4;

/**
 * Custom REST controller for user task management in Kogito processes.
 * 
 * <p>
 * This controller provides task query endpoints that automatically filter tasks
 * based on
 * the authenticated user's identity and roles using Spring Security's
 * IdentityProvider.
 * 
 * <p>
 * <b>Purpose:</b> Kogito's auto-generated task endpoints require explicit
 * 'group' and/or 'user'
 * query parameters to filter tasks. This controller overrides that behavior to
 * provide a more
 * convenient REST API that automatically retrieves tasks assigned to the
 * authenticated user
 * without requiring manual parameter passing.
 * 
 * <p>
 * <b>How it works:</b>
 * <ul>
 * <li>Uses {@link IdentityProvider} to automatically extract the authenticated
 * user's name
 * and roles from the Spring Security context</li>
 * <li>Filters tasks by matching the task's ActorId (assigned user) or GroupId
 * (assigned group)
 * against the authenticated user's identity and roles</li>
 * <li>Returns only active tasks (phaseStatus="active") that the user has
 * permission to see</li>
 * <li>Provides detailed logging for debugging task assignment and filtering
 * logic</li>
 * </ul>
 * 
 * <p>
 * <b>Endpoints:</b>
 * <ul>
 * <li>GET /assessment/tasks - Returns all tasks across all process instances
 * for the current user</li>
 * <li>GET /assessment/{processInstanceId}/tasks - Returns tasks for a specific
 * process instance
 * filtered by the current user</li>
 * </ul>
 * 
 * <p>
 * <b>Authentication:</b> All endpoints require HTTP Basic Authentication. The
 * controller uses
 * Spring Security's authentication context to determine user identity and
 * roles.
 * 
 * <p>
 * <b>Note:</b> This controller uses @Order(0) to ensure it takes precedence
 * over Kogito's
 * auto-generated task endpoints for the same URL patterns.
 * 
 * @see IdentityProvider
 * @see org.kie.kogito.process.WorkItem
 * @see org.springframework.security.core.Authentication
 */
@RestController
@RequestMapping("/assessment")
@Order(0)
public class NeurologicalTasksController {

    private static final Logger logger = LoggerFactory.getLogger(NeurologicalTasksController.class);

    @Autowired
    private IdentityProvider identityProvider;

    @Autowired(required = false)
    @Qualifier("neurologicalassessment.assessment")
    private Process<?> assessmentProcess;

    @GetMapping("/tasks")
    public ResponseEntity<?> getAllTasksForUser() {
        String userName = identityProvider.getName();
        List<String> userRoles = new ArrayList<>(identityProvider.getRoles());

        logger.info("=== Getting all tasks for user: {} ===", userName);
        logger.info("User roles: {}", userRoles);

        if (assessmentProcess == null) {
            logger.error("Assessment process not found! Check if the process is properly initialized.");
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Assessment process not initialized");
            error.put("tasks", new ArrayList<>());
            error.put("userName", userName);
            error.put("userRoles", userRoles);
            return ResponseEntity.ok(error);
        }

        try {
            List<Map<String, Object>> allTasks = new ArrayList<>();
            String baseUrl = "http://localhost:8080";

            assessmentProcess.instances().stream()
                    .filter(pi -> pi.status() == ProcessInstance.STATE_ACTIVE)
                    .forEach(pi -> {
                        logger.debug("Checking process instance: {}", pi.id());
                        logger.debug("Process instance status: {}", pi.status());
                        logger.debug("Process instance variables: {}", pi.variables());
                        logger.debug("WorkItems size: {}", pi.workItems().size());

                        // Llamada HTTP al endpoint de Kogito
                        String url = String.format("%s/assessment/%s/tasks?user=%s", baseUrl, pi.id(), userName);

                        try {
                            RestTemplate restTemplate = new RestTemplate();

                            HttpHeaders headers = new HttpHeaders();
                            headers.setBasicAuth("wbadmin", "wbadmin");

                            HttpEntity<String> entity = new HttpEntity<>(headers);

                            @SuppressWarnings("unchecked")
                            ResponseEntity<List<Map<String, Object>>> taskResponse = (ResponseEntity<List<Map<String, Object>>>) (ResponseEntity<?>) restTemplate
                                    .exchange(
                                            url,
                                            HttpMethod.GET,
                                            entity,
                                            List.class);

                            if (taskResponse.getStatusCode() == HttpStatus.OK && taskResponse.getBody() != null) {

                                logger.debug("Found {} tasks via HTTP", taskResponse.getBody());
                                taskResponse.getBody().forEach(task -> {
                                    logger.debug("Task via HTTP: {}", task);
                                    Map<String, Object> map = new HashMap<String, Object>();

                                    map.put("id", task.get("id"));
                                    map.put("name", task.get("name"));
                                    map.put("processInstanceId", pi.id());
                                    map.put("phase", task.get("phase"));
                                    map.put("phaseStatus", task.get("phaseStatus"));
                                    map.put("parameters", task.get("parameters"));
                                    allTasks.add(map);
                                });
                            }

                        } catch (Exception e) {
                            logger.error("Error calling Kogito endpoint: {}", e.getMessage(), e);
                        }

                        // Procesamiento directo de workItems
                        pi.workItems().stream()
                                .peek(wi -> logger.debug("Work item found: id={}, name={}, phase={}, phaseStatus={}",
                                        wi.getId(), wi.getName(), wi.getPhase(), wi.getPhaseStatus()))
                                .filter(wi -> "active".equalsIgnoreCase(wi.getPhaseStatus()) ||
                                        wi.getPhase() == null ||
                                        wi.getPhase().equals("active"))
                                .filter(wi -> isTaskAssignedToUser(wi, userName, userRoles))
                                .forEach(wi -> {
                                    Map<String, Object> taskMap = workItemToMap(wi, pi.id());
                                    allTasks.add(taskMap);
                                    logger.info("Added task: {}", taskMap);
                                });
                    });

            logger.info("Total potential tasks found for user {}: {}", userName, allTasks.size());

            Map<String, Object> response = new HashMap<>();
            response.put("tasks", allTasks);
            response.put("userName", userName);
            response.put("userRoles", userRoles);
            response.put("totalTasks", allTasks.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting tasks", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("tasks", new ArrayList<>());
            error.put("userName", userName);
            error.put("userRoles", userRoles);
            return ResponseEntity.ok(error);
        }
    }

    @GetMapping("/{processInstanceId}/tasks")
    public ResponseEntity<?> getTasksForProcess(@PathVariable String processInstanceId) {
        String userName = identityProvider.getName();
        List<String> userRoles = new ArrayList<>(identityProvider.getRoles());

        logger.info("=== Getting tasks for process: {} and user: {} ===", processInstanceId, userName);
        logger.info("User roles: {}", userRoles);

        if (assessmentProcess == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Assessment process not initialized");
            error.put("tasks", new ArrayList<>());
            return ResponseEntity.ok(error);
        }

        try {
            ProcessInstance<?> instance = assessmentProcess.instances()
                    .findById(processInstanceId)
                    .orElse(null);

            if (instance == null) {
                logger.warn("Process instance {} not found", processInstanceId);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Process instance not found");
                error.put("processInstanceId", processInstanceId);
                error.put("tasks", new ArrayList<>());
                return ResponseEntity.ok(error);
            }

            logger.info("Process instance status: {}", instance.status());

            List<Map<String, Object>> tasks = instance.workItems().stream()
                    .peek(wi -> logger.debug("Work item: id={}, name={}, phase={}, phaseStatus={}, params={}",
                            wi.getId(), wi.getName(), wi.getPhase(), wi.getPhaseStatus(), wi.getParameters()))
                    .filter(wi -> "active".equalsIgnoreCase(wi.getPhaseStatus()) ||
                            wi.getPhase() == null ||
                            wi.getPhase().equals("active"))
                    .peek(wi -> logger.debug("Filtering task {} for user {} with roles {}",
                            wi.getName(), userName, userRoles))
                    .filter(wi -> isTaskAssignedToUser(wi, userName, userRoles))
                    .map(wi -> workItemToMap(wi, processInstanceId))
                    .collect(Collectors.toList());

            logger.info("Found {} tasks for process {} and user {}", tasks.size(), processInstanceId, userName);

            Map<String, Object> response = new HashMap<>();
            response.put("tasks", tasks);
            response.put("processInstanceId", processInstanceId);
            response.put("userName", userName);
            response.put("userRoles", userRoles);
            response.put("totalTasks", tasks.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting tasks for process " + processInstanceId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("tasks", new ArrayList<>());
            return ResponseEntity.ok(error);
        }
    }

    private boolean isTaskAssignedToUser(WorkItem workItem, String userName, List<String> userRoles) {
        logger.debug("=== Checking assignment for task: {} ===", workItem.getName());
        logger.debug("User: {}, Roles: {}", userName, userRoles);
        logger.debug("Task parameters: {}", workItem.getParameters());

        // Verificar si estÃ¡ asignado al usuario especÃ­fico
        if (workItem.getParameters().containsKey("ActorId")) {
            String actorId = (String) workItem.getParameters().get("ActorId");
            logger.debug("Task ActorId: {}", actorId);
            if (userName.equals(actorId)) {
                logger.info("âœ“ Task {} assigned to user {} by ActorId", workItem.getName(), userName);
                return true;
            }
        }

        // Verificar si estÃ¡ asignado a un grupo del usuario
        if (workItem.getParameters().containsKey("GroupId")) {
            Object groupIdObj = workItem.getParameters().get("GroupId");
            logger.debug("Task GroupId (raw): {} (type: {})", groupIdObj,
                    groupIdObj != null ? groupIdObj.getClass().getName() : "null");

            String groupId = groupIdObj != null ? groupIdObj.toString() : null;

            if (groupId != null && userRoles.contains(groupId)) {
                logger.info("âœ“ Task {} assigned to user {} by GroupId: {}",
                        workItem.getName(), userName, groupId);
                return true;
            } else {
                logger.debug("âœ— GroupId '{}' not in user roles: {}", groupId, userRoles);
            }
        }

        logger.debug("âœ— Task {} NOT assigned to user {}", workItem.getName(), userName);
        return false;
    }

    private Map<String, Object> workItemToMap(WorkItem wi, String processInstanceId) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", wi.getId());
        map.put("name", wi.getName());
        map.put("processInstanceId", processInstanceId);
        map.put("phase", wi.getPhase());
        map.put("phaseStatus", wi.getPhaseStatus());
        map.put("parameters", wi.getParameters());
        return map;
    }

    /**
     * Completes a pain assessment task by submitting the DN4 questionnaire data.
     * 
     * <p>
     * This endpoint allows authenticated users to complete their assigned pain
     * assessment task
     * by providing the DN4 (Douleur Neuropathique 4 Questions) questionnaire
     * responses.
     * 
     * @param processInstanceId The ID of the process instance containing the task
     * @param taskId            The ID of the task to complete
     * @param dn4               The DN4 questionnaire data with all pain assessment
     *                          responses
     * @return ResponseEntity containing success message and task completion
     *         details, or error if task not found/not authorized
     */
    @PostMapping("/{processInstanceId}/tasks/{taskId}")
    public ResponseEntity<?> completeTask(
            @PathVariable String processInstanceId,
            @PathVariable String taskId,
            @RequestBody DN4 dn4) {

        String userName = identityProvider.getName();

        logger.info("=== Completing task {} for process {} by user {} ===", taskId, processInstanceId, userName);
        logger.debug("DN4 data received: {}", dn4);

        try {
            ProcessInstance<?> instance = assessmentProcess.instances()
                    .findById(processInstanceId)
                    .orElse(null);

            if (instance == null) {
                logger.warn("Process instance {} not found", processInstanceId);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Process instance not found");
                error.put("processInstanceId", processInstanceId);
                return ResponseEntity.status(404).body(error);
            }

            /*
             * Find the task
             * WorkItem task = instance.workItems().stream()
             * .filter(wi -> wi.getId().equals(taskId))
             * .findFirst()
             * .orElse(null);
             */
            // Buscar la tarea usando REST API
            String baseUrl = "http://localhost:8080";
            String url = String.format("%s/assessment/%s/tasks?user=%s", baseUrl, processInstanceId, userName);

            boolean taskFound = false;
            boolean taskBelongsToUser = false;

            try {
                RestTemplate restTemplate = new RestTemplate();

                HttpHeaders headers = new HttpHeaders();
                headers.setBasicAuth("wbadmin", "wbadmin");

                HttpEntity<String> entity = new HttpEntity<>(headers);

                @SuppressWarnings("unchecked")
                ResponseEntity<List<Map<String, Object>>> taskResponse = (ResponseEntity<List<Map<String, Object>>>) (ResponseEntity<?>) restTemplate
                        .exchange(
                                url,
                                HttpMethod.GET,
                                entity,
                                List.class);
                if (taskResponse.getStatusCode() == HttpStatus.OK && taskResponse.getBody() != null) {
                    // Buscar el taskId específico en la lista
                    for (Map<String, Object> task : taskResponse.getBody()) {
                        if (taskId.equals(task.get("id"))) {
                            taskFound = true;
                            taskBelongsToUser = true;
                            logger.info("Task {} found and belongs to user {}", taskId, userName);
                            break;
                        }
                    }
                }

            } catch (Exception e) {
                logger.error("Error verifying task via REST: {}", e.getMessage(), e);
                // Si falla REST, intentar con workItems como fallback
                WorkItem task = instance.workItems().stream()
                        .filter(wi -> wi.getId().equals(taskId))
                        .findFirst()
                        .orElse(null);

                if (task != null) {
                    taskFound = true;
                    taskBelongsToUser = true;

                    // taskBelongsToUser = isTaskAssignedToUser(task, userName, userRoles);
                }
            }
            if (!taskFound) {
                logger.warn("Task {} not found in process instance {}", taskId, processInstanceId);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Task not found");
                error.put("taskId", taskId);
                error.put("processInstanceId", processInstanceId);
                return ResponseEntity.status(404).body(error);
            }

            // Verify user has permission to complete this task
            if (!taskBelongsToUser) {
                logger.warn("User {} not authorized to complete task {}", userName, taskId);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Not authorized to complete this task");
                error.put("taskId", taskId);
                error.put("userName", userName);
                return ResponseEntity.status(403).body(error);
            }

            // Complete the task with DN4 data using REST API
            /// assessment/{id}/painAssessment/{taskId}
            String completeUrl = String.format("%s/assessment/%s/painAssessment/%s?user=%s&group=practitioners",
                    baseUrl, processInstanceId, taskId, userName);

            Map<String, Object> taskData = new HashMap<>();
            taskData.put("dn4", dn4);

            HttpHeaders completeHeaders = new HttpHeaders();
            completeHeaders.setBasicAuth("wbadmin", "wbadmin");
            completeHeaders.set("Content-Type", "application/json");

            HttpEntity<Map<String, Object>> completeEntity = new HttpEntity<>(taskData, completeHeaders);

            logger.info("Completing task {} with DN4 data via REST API: {}", taskId, completeUrl);

            RestTemplate completeRestTemplate = new RestTemplate();
            completeRestTemplate.exchange(
                    completeUrl,
                    HttpMethod.POST,
                    completeEntity,
                    Map.class);

            logger.info("✓ Task {} completed successfully by user {}", taskId, userName);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Task completed successfully");
            response.put("taskId", taskId);
            response.put("processInstanceId", processInstanceId);
            response.put("completedBy", userName);
            response.put("dn4", dn4);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error completing task {} in process {}", taskId, processInstanceId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error completing task: " + e.getMessage());
            error.put("taskId", taskId);
            error.put("processInstanceId", processInstanceId);
            return ResponseEntity.status(500).body(error);
        }
    }
}
