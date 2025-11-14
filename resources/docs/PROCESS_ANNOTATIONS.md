# Process Annotations - Neurological Assessment

This document provides detailed annotations for each element in the BPMN process `assessment.bpmn`. These annotations explain the purpose and functionality of each step in the neurological assessment workflow.

## Process Overview

The neurological assessment process is triggered by receiving an appointment event from a Kafka topic. It retrieves appointment details, performs a pain assessment using the DN4 scale, calculates the score, and determines if the patient has neuropathic pain based on the results.

---

## Process Elements

### 1. Start Event: `appointment`
**Element ID:** `_AC30C2C0-10A2-4EA1-B723-3D30D35CC77C`  
**Type:** Message Start Event

**Annotation:** The reception of an event from appointments topic starts the service

The process begins when a message is received from the Kafka topic `appointments`. This message contains the appointment ID which is extracted and stored in the `appointmentId` process variable.

---

### 2. Service Task: `Solving Appointment Data`
**Element ID:** `_8C18F2A9-65C9-46A3-B445-0F44021BDD0D`  
**Type:** Service Task  
**Service Interface:** `AppointmentDAOService.getAppointmentAttributes`

**Annotation:** Service Task querying Appointment Data

This task calls the `AppointmentDAOService` to retrieve detailed appointment information from the FHIR server. It receives:
- **Input:** `appointmentId` (String)
- **Output:** `appointmentDTO` (AppointmentDTO object containing patient and practitioner information)

The service queries the FHIR server and populates the `appointmentDTO` with all relevant appointment attributes.

---

### 3. Script Task: `Logging Start`
**Element ID:** `_842E6A29-594C-439E-A6B7-64FE7A4AD7CF`  
**Type:** Script Task (Java)

**Annotation:** Setting and logging process variables

This script task performs the following actions:
- Logs the initial values of process variables
- Logs `appointmentId` and `appointmentDTO`
- Extracts and sets `patientId` from `appointmentDTO.getPatient()`
- Extracts and sets `practitionerId` from `appointmentDTO.getPractitioner()`
- Logs the extracted patient and practitioner IDs

**Code:**
```java
System.out.println("Initial values of process variables");
System.out.println("AppointmentId is " + appointmentId);
System.out.println("AppointmentDTO is " + appointmentDTO);
kcontext.setVariable("patientId", appointmentDTO.getPatient());
kcontext.setVariable("practitionerId", appointmentDTO.getPractitioner());
System.out.println("Patient is " + kcontext.getVariable("patientId"));
System.out.println("Practitioner is " + kcontext.getVariable("practitionerId"));
```

---

### 4. User Task: `Pain Assessment with DN4 Scale`
**Element ID:** `_5DC23A05-EA2B-42D9-A75F-A293C02B5775`  
**Type:** User Task  
**Assigned to:** `practitioner` group

**Annotation:** Human task for pain assessment. A practitioner has to ask and explore the patient

This is an interactive task where a healthcare practitioner must:
- Interview the patient about their pain
- Perform a physical examination
- Fill out the DN4 (Douleur Neuropathique 4) questionnaire

The task receives a DN4 object as input and returns the completed DN4 object with all questions answered. The task cannot be skipped (`Skippable: false`).

**Task Inputs:**
- `dn4` - DN4 object to be filled
- `TaskName` - "painAssessment"
- `GroupId` - "practitioner"

**Task Outputs:**
- `dn4` - Completed DN4 object with answers

---

### 5. Script Task: `Calculate Score`
**Element ID:** `_A779CA67-267B-488F-876C-68292C63DB1B`  
**Type:** Script Task (Java)

**Annotation:** Calculate the Score in the returned DN4 object

This script task calculates the total DN4 score based on the answers provided in the previous user task. The DN4 scale has a maximum score of 10 points.

**Code:**
```java
dn4.calculateScore();
```

The score is stored in the DN4 object and will be used in the next step to determine if the patient has neuropathic pain.

---

### 6. Exclusive Gateway (Decision Point)
**Element ID:** `_76837526-5627-472D-9695-2717AD0CD7BF`  
**Type:** Exclusive Gateway (XOR)

**Annotation:** Conditional path based on DN4 score

This gateway evaluates the DN4 score to determine the appropriate path:

- **If score >= 4:** Patient likely has neuropathic pain → Route to "Logging Neuropathic pain"
- **If score < 4:** Patient does not have neuropathic pain → Route to "Logging NOT Neuropathic pain"

The threshold of 4 points is the clinical cutoff for diagnosing neuropathic pain using the DN4 scale.

---

### 7a. Script Task: `Logging Neuropathic pain` (High Score Path)
**Element ID:** `_097E5DF3-E21D-4428-AE1A-84535E642B9F`  
**Type:** Script Task (Java)

**Annotation:** Logging process variables

This script executes when the DN4 score is 4 or higher, indicating neuropathic pain:

**Code:**
```java
System.out.println("Neuropathic pain");
System.out.println("Scale Result is " + dn4.getScore());
```

This logs the diagnosis of neuropathic pain along with the specific score for clinical records.

---

### 7b. Script Task: `Logging NOT Neuropathic pain` (Low Score Path)
**Element ID:** `_0BBAD683-A612-4653-9EB3-22D2568180AD`  
**Type:** Script Task (Java)

**Annotation:** Logging process variables

This script executes when the DN4 score is less than 4, indicating the pain is likely not neuropathic:

**Code:**
```java
System.out.println("NOT Neuropathic pain");
System.out.println("Scale Result is " + dn4.getScore());
```

This logs that the pain does not meet the threshold for neuropathic pain diagnosis.

---

### 8. End Events
**Element IDs:** 
- `_526D6F63-642A-4518-98AD-020B5B9915F2` (Neuropathic pain path)
- `_0CD7F494-80C6-4DA7-914A-385660F144DF` (Non-neuropathic pain path)

**Type:** Terminate End Events

Both paths end with terminate end events, which immediately end the process instance and all its activities.

---

## Process Variables

| Variable Name | Type | Description |
|--------------|------|-------------|
| `appointmentId` | String | The ID of the appointment received from Kafka |
| `appointmentDTO` | AppointmentDTO | Complete appointment information retrieved from FHIR server |
| `patientId` | String | Patient identifier extracted from appointment |
| `practitionerId` | String | Practitioner identifier extracted from appointment |
| `dn4` | DN4 | The DN4 questionnaire object with questions, answers, and calculated score |

---

## DN4 Scale Information

The **DN4 (Douleur Neuropathique 4 Questions)** is a validated screening tool for neuropathic pain:

- **Total Score Range:** 0-10 points
- **Diagnostic Threshold:** ≥ 4 points suggests neuropathic pain
- **Components:**
  - 7 items related to pain descriptors and sensory symptoms
  - 3 items related to physical examination findings

### Scoring Interpretation:
- **0-3 points:** Pain is unlikely to be neuropathic
- **4-10 points:** Pain is likely to be neuropathic (sensitivity: 80%, specificity: 92%)

---

## Process Flow Diagram Reference

For a visual representation of this process, see the BPMN diagram in:
- **BPMN XML:** `src/main/resources/assessment.bpmn`
- **Visual Diagram:** `resources/img/bpmn.png`

You can also view the process diagram through the web interface at:
- http://localhost:8080/process-diagram.html

Or view specific process instances at:
- http://localhost:8080/process-instances-viewer.html

---

## Testing the Process

### 1. Send a Test Message to Kafka

Use the provided PowerShell script:
```powershell
.\src\test\resources\send-test-message.ps1
```

### 2. Monitor Process Instance

Check the logs to see the process execution:
```
Initial values of process variables
AppointmentId is <appointment-id>
Patient is <patient-id>
Practitioner is <practitioner-id>
```

### 3. Complete the User Task

Navigate to the task list and complete the DN4 assessment:
- http://localhost:8080/task-list.html

### 4. View Results

Check the console for the final result:
```
Neuropathic pain (or NOT Neuropathic pain)
Scale Result is <score>
```

---

## Integration Points

### Input
- **Kafka Topic:** `appointments`
- **Message Format:** String (appointment ID)
- **Trigger:** Message arrival on topic

### Services Used
- **AppointmentDAOService:** FHIR server integration for retrieving appointment details
- **HAPI FHIR Server:** http://localhost:8083

### Output
- **Console Logs:** Process execution details and diagnosis results
- **Process Variables:** Stored in Kogito runtime for process instance

---

## Additional Resources

- **BPMN 2.0 Specification:** https://www.omg.org/spec/BPMN/2.0/
- **Kogito Documentation:** https://docs.jboss.org/kogito/release/latest/html_single/
- **DN4 Questionnaire:** https://www.neuropathicpain.eu/en/dn4-questionnaire

---

*This document corresponds to the assessment.bpmn process in the Neurological Assessment application (version 1.0.0-SNAPSHOT)*
