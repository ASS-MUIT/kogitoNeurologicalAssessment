# Kogito Task API Usage Guide

## 📋 Problem Summary and Solution

### Identified Problem
Users with the `practitioner` role (such as `doctorWho`) could not see their assigned tasks due to:

1. **Mismatch between role and group**: Spring Security added the `ROLE_` prefix to roles, but Kogito looked for groups without the prefix
2. **Missing Task Management addon**: The required dependency for managing tasks was not included

### Applied Solution
1. ✅ Changed `.roles()` to `.authorities()` in the security configuration
2. ✅ Added the `kogito-addons-springboot-task-management` addon to `pom.xml`
3. ✅ Configured `kogito.service.url` in `application.properties`

## 🔌 Task API Endpoints

### 1. Get all tasks for the authenticated user

```bash
GET http://localhost:8080/assessment/tasks
```

**Authentication required**: Yes (Basic Auth)

**Example with curl:**
```bash
curl -u doctorWho:doctorWho http://localhost:8080/assessment/tasks
```

**Example with PowerShell:**
```powershell
$credentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("doctorWho:doctorWho"))
Invoke-RestMethod -Uri "http://localhost:8080/assessment/tasks" -Headers @{Authorization="Basic $credentials"}
```

### 2. Get tasks for a specific process instance

```bash
GET http://localhost:8080/assessment/{processInstanceId}/tasks
```

**Example:**
```bash
curl -u doctorWho:doctorWho http://localhost:8080/assessment/12345-67890-abcdef/tasks
```

### 3. Get details of a specific task

```bash
GET http://localhost:8080/assessment/{processInstanceId}/painAssessment/{taskId}
```

**Example:**
```bash
curl -u doctorWho:doctorWho http://localhost:8080/assessment/12345-67890-abcdef/painAssessment/task-001
```

### 4. Complete a task

```bash
POST http://localhost:8080/assessment/{processInstanceId}/painAssessment/{taskId}
Content-Type: application/json

{
  "dn4": {
    "burning": true,
    "painfulCold": false,
    "electricShocks": true,
    "tingling": true,
    "pinsPricks": false,
    "numbness": true,
    "itching": false,
    "hypoesthesiaTouch": true,
    "hypoesthesiaPinprick": false,
    "brushingAllodynia": true
  }
}
```

**Example with curl:**
```bash
curl -u doctorWho:doctorWho \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"dn4":{"burning":true,"painfulCold":false,"electricShocks":true,"tingling":true,"pinsPricks":false,"numbness":true,"itching":false,"hypoesthesiaTouch":true,"hypoesthesiaPinprick":false,"brushingAllodynia":true}}' \
  http://localhost:8080/assessment/12345-67890-abcdef/painAssessment/task-001
```

**Example with PowerShell:**
```powershell
$credentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("doctorWho:doctorWho"))
$body = @{
    dn4 = @{
        burning = $true
        painfulCold = $false
        electricShocks = $true
        tingling = $true
        pinsPricks = $false
        numbness = $true
        itching = $false
        hypoesthesiaTouch = $true
        hypoesthesiaPinprick = $false
        brushingAllodynia = $true
    }
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/assessment/12345-67890-abcdef/painAssessment/task-001" `
  -Method POST `
  -Headers @{
      Authorization="Basic $credentials"
      "Content-Type"="application/json"
  } `
  -Body $body
```

## 👥 Configured Users

| User      | Password  | Group/Authority | Can view tasks for |
|-----------|-----------|-----------------|---------------------|
| doctorWho | doctorWho | practitioner    | painAssessment      |
| paul      | paul      | practitioner    | painAssessment      |
| mary      | mary      | patient         | (none)              |

## 🔍 Functionality Verification

### Step 1: Compile the project
```bash
mvn clean install
```

### Step 2: Start the application
```bash
mvn spring-boot:run
```

Or in PowerShell:
```powershell
.\run.ps1
```

### Step 3: Create a process instance

Use the existing script `send-test-message.ps1` or send a message to Kafka with an appointmentId.

### Step 4: Verify available tasks

```bash
curl -u doctorWho:doctorWho http://localhost:8080/assessment/tasks
```

You should see a response similar to:
```json
[
  {
    "id": "task-id-123",
    "name": "painAssessment",
    "description": "Pain Assessment with DN4 Scale",
    "priority": "0",
    "processInstanceId": "12345-67890-abcdef",
    "processId": "neurologicalassessment.assessment",
    "state": "Ready",
    "actualOwner": null,
    "potentialUsers": [],
    "potentialGroups": ["practitioner"],
    "inputs": {
      "dn4": {
        "burning": null,
        "painfulCold": null,
        ...
      }
    }
  }
]
```

## 🐛 Troubleshooting

### I don't see any tasks
1. Verify that the process was started correctly
2. Check the logs: set `logging.level.org.kie.kogito=DEBUG` in `application.properties`
3. Make sure you are authenticated with the correct user (`doctorWho` or `paul`)

### Error 401 Unauthorized
- Verify Basic Auth credentials
- Make sure the user exists in `DefaultWebSecurityConfig`

### Error 403 Forbidden
- Verify that the user has the correct group (`practitioner`)
- Check that the `GroupId` in the BPMN matches the user's authority

### Tasks appear but I cannot complete them
- Verify that the JSON body has the correct structure
- Make sure to use `Content-Type: application/json`
- Check that all fields of the `dn4` object are present

## 📚 References

- [Kogito Documentation - Human Tasks](https://docs.jboss.org/kogito/release/latest/html_single/#con-human-tasks_kogito-developing-process-services)
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/in-memory.html)
