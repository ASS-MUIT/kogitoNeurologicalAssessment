# Solución al Problema de Visualización de Tareas

## 📋 Problema Identificado

El usuario `doctorWho` con el grupo `practitioner` no podía ver las tareas asignadas a través de la API REST, incluso especificando parámetros `?user=doctorWho&group=practitioner`.

## 🔍 Análisis del Problema

Después de investigar, se identificaron varios problemas:

### 1. **Incompatibilidad de configuración de seguridad**
- Spring Security con `.roles()` añade automáticamente el prefijo `ROLE_`
- El BPMN busca el grupo `practitioner` sin prefijo
- **Solución**: Cambiar a `.authorities()` en lugar de `.roles()`

### 2. **Falta de addon de Task Management**
- No estaba incluida la dependencia `kogito-addons-springboot-task-management`
- **Solución**: Agregada al `pom.xml`

### 3. **Endpoints de tareas no disponibles por defecto**
- Kogito 1.44.1 con Spring Boot no expone automáticamente endpoints de tareas de forma consistente
- **Solución**: Creado un controlador REST personalizado (`TasksController.java`)

## ✅ Cambios Realizados

### 1. **DefaultWebSecurityConfig.java**
```java
// ANTES
auth.inMemoryAuthentication().withUser("doctorWho").password("doctorWho").roles("practitioner");

// DESPUÉS
auth.inMemoryAuthentication()
    .withUser("doctorWho").password("doctorWho")
    .authorities("practitioner");
```

### 2. **pom.xml**
```xml
<!-- AGREGADO -->
<dependency>
    <groupId>org.kie.kogito</groupId>
    <artifactId>kogito-addons-springboot-task-management</artifactId>
    <version>${kogito.version}</version>
</dependency>
```

### 3. **IdentityProviderConfig.java**
- Agregados logs detallados para debugging
- Mantiene la lógica de eliminación del prefijo `ROLE_` si existiera

### 4. **TasksController.java** (NUEVO)
Creado un controlador REST personalizado que:
- Expone el endpoint `GET /assessment/tasks` para obtener todas las tareas del usuario autenticado
- Expone el endpoint `GET /assessment/{processInstanceId}/tasks` para tareas de un proceso específico
- Filtra las tareas basándose en el `IdentityProvider` de Kogito
- Proporciona logs detallados para debugging
- Verifica tanto `ActorId` como `GroupId` en los parámetros de la tarea

### 5. **application.properties**
- Habilitados logs de DEBUG para Kogito, Spring Security y el proyecto
- Agregado `kogito.service.url=http://localhost:8080`

## 🚀 Instrucciones de Uso

### Paso 1: Recompilar el Proyecto
```powershell
mvn clean install
```

### Paso 2: Iniciar la Aplicación
```powershell
mvn spring-boot:run
```

O usando el script:
```powershell
.\run.ps1
```

### Paso 3: Enviar un Mensaje de Prueba (si no hay procesos activos)
```powershell
.\send-test-message.ps1
```

### Paso 4: Probar los Nuevos Endpoints

#### Opción A: Usando el script de prueba
```powershell
.\test-tasks.ps1
```

#### Opción B: Manualmente con PowerShell

**Obtener todas las tareas del usuario autenticado:**
```powershell
$credentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("doctorWho:doctorWho"))
$headers = @{Authorization="Basic $credentials"}

Invoke-RestMethod -Uri "http://localhost:8080/assessment/tasks" -Headers $headers
```

**Obtener tareas de un proceso específico:**
```powershell
$processId = "82f142fc-81ac-4f13-b40f-bc0acaccb48b"
$credentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("doctorWho:doctorWho"))
$headers = @{Authorization="Basic $credentials"}

Invoke-RestMethod -Uri "http://localhost:8080/assessment/$processId/tasks" -Headers $headers
```

#### Opción C: Con cURL
```bash
curl -u doctorWho:doctorWho http://localhost:8080/assessment/tasks

curl -u doctorWho:doctorWho http://localhost:8080/assessment/82f142fc-81ac-4f13-b40f-bc0acaccb48b/tasks
```

## 📊 Respuesta Esperada

Si todo funciona correctamente, deberías ver una respuesta como esta:

```json
{
  "tasks": [
    {
      "id": "uuid-task-id",
      "name": "painAssessment",
      "processInstanceId": "82f142fc-81ac-4f13-b40f-bc0acaccb48b",
      "phase": "active",
      "phaseStatus": "active",
      "parameters": {
        "TaskName": "painAssessment",
        "GroupId": "practitioner",
        "Skippable": "false",
        "dn4": { ... }
      }
    }
  ],
  "userName": "doctorWho",
  "userRoles": ["practitioner"],
  "totalTasks": 1
}
```

## 🔍 Debugging

### Verificar los Logs

Los logs ahora mostrarán información detallada:

```
=== Getting tasks for process: 82f142fc-... and user: doctorWho ===
User roles: [practitioner]
Process instance status: 1
Work item: id=..., name=painAssessment, phase=active, phaseStatus=active, params={...}
=== Checking assignment for task: painAssessment ===
User: doctorWho, Roles: [practitioner]
Task parameters: {TaskName=painAssessment, GroupId=practitioner, ...}
Task GroupId (raw): practitioner (type: java.lang.String)
✓ Task painAssessment assigned to user doctorWho by GroupId: practitioner
Found 1 tasks for process 82f142fc-... and user doctorWho
```

### Si No Ves Tareas

1. **Verificar que el proceso existe y está activo:**
   ```powershell
   curl -u doctorWho:doctorWho http://localhost:8080/assessment
   ```

2. **Verificar los logs del servidor:**
   - Busca mensajes como `"Assessment process not initialized"`
   - Verifica que el `IdentityProvider` esté retornando los roles correctos
   - Confirma que el `GroupId` en los logs coincida con `"practitioner"`

3. **Verificar la autenticación:**
   ```powershell
   # Deberías ver información del usuario
   curl -u doctorWho:doctorWho http://localhost:8080/assessment/tasks
   ```

## 📝 Notas Importantes

1. **Los parámetros de URL `?user=...&group=...` NO se usan** en este endpoint personalizado. El filtrado se hace automáticamente basándose en el usuario autenticado.

2. **El `IdentityProvider` es la clave**: Asegúrate de que retorna correctamente:
   - `getName()` → `"doctorWho"`
   - `getRoles()` → `["practitioner"]`

3. **La tarea debe tener `GroupId=practitioner`** en el BPMN, lo cual ya está configurado correctamente.

4. **El controlador personalizado** proporciona mejor debugging y más control sobre el filtrado de tareas que los endpoints automáticos de Kogito.

## 🎯 Próximos Pasos

Una vez que veas las tareas correctamente:

1. **Obtener el ID de la tarea** de la respuesta
2. **Completar la tarea** usando el endpoint POST:
   ```powershell
   $taskId = "uuid-de-la-tarea"
   $processId = "82f142fc-81ac-4f13-b40f-bc0acaccb48b"
   
   $body = @{
       dn4 = @{
           burning = $true
           painfulCold = $false
           # ... resto de campos
       }
   } | ConvertTo-Json
   
   Invoke-RestMethod -Uri "http://localhost:8080/assessment/$processId/painAssessment/$taskId" `
       -Method POST `
       -Headers @{Authorization="Basic $credentials"; "Content-Type"="application/json"} `
       -Body $body
   ```

## 🐛 Troubleshooting

Si sigues sin ver las tareas:

1. Reinicia completamente la aplicación
2. Verifica que Maven haya descargado el addon `kogito-addons-springboot-task-management`
3. Revisa los logs en busca de excepciones durante el inicio
4. Confirma que el proceso se creó correctamente después de los cambios
5. Prueba con el usuario `paul` que también tiene el grupo `practitioner`
