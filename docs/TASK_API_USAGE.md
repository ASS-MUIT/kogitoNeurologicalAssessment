# Guía de Uso de la API de Tareas de Kogito

## 📋 Resumen del Problema y Solución

### Problema Identificado
Los usuarios con rol `practitioner` (como `doctorWho`) no podían ver sus tareas asignadas debido a:

1. **Discrepancia entre rol y grupo**: Spring Security agregaba el prefijo `ROLE_` a los roles, pero Kogito buscaba grupos sin prefijo
2. **Falta del addon de Task Management**: No estaba incluida la dependencia necesaria para gestionar tareas

### Solución Aplicada
1. ✅ Cambiado `.roles()` por `.authorities()` en la configuración de seguridad
2. ✅ Agregado el addon `kogito-addons-springboot-task-management` al `pom.xml`
3. ✅ Configurado `kogito.service.url` en `application.properties`

## 🔌 Endpoints de la API de Tareas

### 1. Obtener todas las tareas del usuario autenticado

```bash
GET http://localhost:8080/assessment/tasks
```

**Autenticación requerida**: Sí (Basic Auth)

**Ejemplo con curl:**
```bash
curl -u doctorWho:doctorWho http://localhost:8080/assessment/tasks
```

**Ejemplo con PowerShell:**
```powershell
$credentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("doctorWho:doctorWho"))
Invoke-RestMethod -Uri "http://localhost:8080/assessment/tasks" -Headers @{Authorization="Basic $credentials"}
```

### 2. Obtener tareas de una instancia de proceso específica

```bash
GET http://localhost:8080/assessment/{processInstanceId}/tasks
```

**Ejemplo:**
```bash
curl -u doctorWho:doctorWho http://localhost:8080/assessment/12345-67890-abcdef/tasks
```

### 3. Obtener detalles de una tarea específica

```bash
GET http://localhost:8080/assessment/{processInstanceId}/painAssessment/{taskId}
```

**Ejemplo:**
```bash
curl -u doctorWho:doctorWho http://localhost:8080/assessment/12345-67890-abcdef/painAssessment/task-001
```

### 4. Completar una tarea

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

**Ejemplo con curl:**
```bash
curl -u doctorWho:doctorWho \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"dn4":{"burning":true,"painfulCold":false,"electricShocks":true,"tingling":true,"pinsPricks":false,"numbness":true,"itching":false,"hypoesthesiaTouch":true,"hypoesthesiaPinprick":false,"brushingAllodynia":true}}' \
  http://localhost:8080/assessment/12345-67890-abcdef/painAssessment/task-001
```

**Ejemplo con PowerShell:**
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

## 👥 Usuarios Configurados

| Usuario   | Password  | Grupo/Autoridad | Puede ver tareas de |
|-----------|-----------|-----------------|---------------------|
| doctorWho | doctorWho | practitioner    | painAssessment      |
| paul      | paul      | practitioner    | painAssessment      |
| mary      | mary      | patient         | (ninguna)           |

## 🔍 Verificación del Funcionamiento

### Paso 1: Compilar el proyecto
```bash
mvn clean install
```

### Paso 2: Iniciar la aplicación
```bash
mvn spring-boot:run
```

O en PowerShell:
```powershell
.\run.ps1
```

### Paso 3: Crear una instancia de proceso

Usa el script existente `send-test-message.ps1` o envía un mensaje a Kafka con un appointmentId.

### Paso 4: Verificar tareas disponibles

```bash
curl -u doctorWho:doctorWho http://localhost:8080/assessment/tasks
```

Deberías ver una respuesta similar a:
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

### No veo ninguna tarea
1. Verifica que el proceso se haya iniciado correctamente
2. Comprueba los logs: `logging.level.org.kie.kogito=DEBUG` en `application.properties`
3. Asegúrate de estar autenticado con el usuario correcto (`doctorWho` o `paul`)

### Error 401 Unauthorized
- Verifica las credenciales de Basic Auth
- Asegúrate de que el usuario existe en `DefaultWebSecurityConfig`

### Error 403 Forbidden
- Verifica que el usuario tiene el grupo correcto (`practitioner`)
- Comprueba que el `GroupId` en el BPMN coincida con la autoridad del usuario

### Las tareas aparecen pero no puedo completarlas
- Verifica que el body JSON tenga la estructura correcta
- Asegúrate de usar `Content-Type: application/json`
- Comprueba que todos los campos del objeto `dn4` estén presentes

## 📚 Referencias

- [Kogito Documentation - Human Tasks](https://docs.jboss.org/kogito/release/latest/html_single/#con-human-tasks_kogito-developing-process-services)
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/in-memory.html)
