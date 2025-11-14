# ✅ SOLUCIÓN FINAL - Problema de Visibilidad de Tareas

## 🔴 EL VERDADERO PROBLEMA

Después de investigar exhaustivamente los logs, descubrí que el problema **NO era** la configuración de Spring Security ni el `IdentityProvider`.

### El problema real estaba en el BPMN:

En los logs encontré esta línea clave:
```json
"potentialGroups":["Practitioner/3"]
```

El `GroupId` de la tarea era **`"Practitioner/3"`** (la referencia FHIR del practitioner), cuando debería ser simplemente **`"practitioner"`** (el rol/grupo).

## 🔍 CAUSA RAÍZ

En el archivo `assessment.bpmn`, el `GroupId` estaba definido así:

```xml
<bpmn2:from xsi:type="bpmn2:tFormalExpression"><![CDATA[practitioner]]></bpmn2:from>
```

**Sin comillas**, Kogito lo interpretaba como una **variable** en lugar de un **string literal**.

Como el proceso tenía una variable llamada `practitioner` con el valor `"Practitioner/3"` (establecida en el script "Logging Start"), Kogito usaba ese valor como `GroupId`.

## ✅ LA SOLUCIÓN

Cambiar el BPMN para usar un string literal con comillas:

```xml
<!-- ANTES (INCORRECTO) -->
<bpmn2:from xsi:type="bpmn2:tFormalExpression"><![CDATA[practitioner]]></bpmn2:from>

<!-- DESPUÉS (CORRECTO) -->
<bpmn2:from xsi:type="bpmn2:tFormalExpression"><![CDATA["practitioner"]]></bpmn2:from>
```

## 📝 CAMBIOS REALIZADOS

### 1. **assessment.bpmn** ✅ CRÍTICO
```xml
Línea ~188:
<bpmn2:from xsi:type="bpmn2:tFormalExpression"><![CDATA["practitioner"]]></bpmn2:from>
```

### 2. **DefaultWebSecurityConfig.java** ✅ IMPORTANTE
```java
// Cambiado de .roles() a .authorities() para evitar el prefijo ROLE_
auth.inMemoryAuthentication()
    .withUser("doctorWho").password("doctorWho")
    .authorities("practitioner");
```

### 3. **pom.xml** ✅ IMPORTANTE
```xml
<!-- Agregado addon de task management -->
<dependency>
    <groupId>org.kie.kogito</groupId>
    <artifactId>kogito-addons-springboot-task-management</artifactId>
    <version>${kogito.version}</version>
</dependency>
```

### 4. **TasksController.java** ✅ ÚTIL (para debugging)
- Creado un controlador personalizado con logs detallados
- Endpoints:
  - `GET /assessment/tasks` - Todas las tareas del usuario autenticado
  - `GET /assessment/{processInstanceId}/tasks` - Tareas de un proceso específico

### 5. **IdentityProviderConfig.java** ✅ ÚTIL (para debugging)
- Agregados logs DEBUG para ver qué roles retorna el `IdentityProvider`

### 6. **application.properties** ✅ ÚTIL (para debugging)
```properties
# Logs de DEBUG habilitados
logging.level.org.kie.kogito=DEBUG
logging.level.org.kie.kogito.usertask=DEBUG
logging.level.us.dit.muit.hsa.neurologicalassessment=DEBUG
```

## 🧪 CÓMO VERIFICAR QUE FUNCIONA

### 1. Iniciar la aplicación
```powershell
mvn spring-boot:run
```

### 2. Enviar un mensaje de Appointment (si no tienes un proceso activo)
```powershell
.\send-test-message.ps1
```

### 3. Probar el endpoint de tareas
```powershell
# Opción A: Usar el script de prueba
.\quick-test.ps1

# Opción B: Usar curl/PowerShell directamente
$credentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("doctorWho:doctorWho"))
Invoke-RestMethod -Uri "http://localhost:8080/assessment/tasks" `
  -Headers @{Authorization="Basic $credentials"}
```

### 4. Verificar en los logs

Ahora deberías ver en los logs:
```
potentialGroups=["practitioner"]  ← ✅ CORRECTO (antes era "Practitioner/3")
User roles: [practitioner]         ← ✅ CORRECTO
✓ Task painAssessment assigned to user doctorWho by GroupId: practitioner
```

## 🎯 ENDPOINTS DISPONIBLES

### Gestión de Tareas (nuevos endpoints personalizados)
- `GET /assessment/tasks` - Obtener todas las tareas del usuario autenticado
- `GET /assessment/{processInstanceId}/tasks` - Obtener tareas de un proceso específico

### Endpoints generados por Kogito
- `GET /assessment` - Listar instancias de proceso
- `GET /assessment/{processId}` - Obtener detalles de una instancia
- `POST /assessment/{processId}/painAssessment/{taskId}` - Completar tarea

## 🔧 TROUBLESHOOTING

### Si TODAVÍA no ves tareas:

1. **Verifica el GroupId en los logs:**
   ```
   Busca: "potentialGroups"
   Debe mostrar: ["practitioner"]
   NO debe mostrar: ["Practitioner/3"]
   ```

2. **Verifica las autoridades del usuario:**
   ```
   Busca: "Granted Authorities"
   Debe mostrar: [practitioner]
   NO debe mostrar: [ROLE_practitioner]
   ```

3. **Verifica que el IdentityProvider retorna los roles correctos:**
   ```
   Busca: "IdentityProvider.getRoles()"
   Debe mostrar: [practitioner]
   ```

4. **Reinicia completamente** la aplicación después de hacer cambios en el BPMN

5. **Elimina procesos antiguos** que se crearon con el BPMN incorrecto

## 📚 LECCIONES APRENDIDAS

1. **En BPMN, usa comillas para strings literales**: Sin comillas, Kogito lo interpreta como variable
2. **`.roles()` vs `.authorities()`**: `.roles()` añade prefijo `ROLE_`, `.authorities()` no
3. **Los logs son tu mejor amigo**: Los logs de eventos de Kogito muestran exactamente qué `potentialGroups` tiene la tarea
4. **El `IdentityProvider` debe retornar los mismos valores** que están en `potentialGroups` del BPMN

## 🎉 RESULTADO ESPERADO

```json
{
  "tasks": [
    {
      "id": "726632d8-ff53-4d45-b05e-6f23aa67495f",
      "name": "painAssessment",
      "processInstanceId": "07ab59b4-dcb2-4032-97e1-c3d745176159",
      "phase": "active",
      "phaseStatus": "active",
      "parameters": {
        "TaskName": "painAssessment",
        "GroupId": "practitioner",  ← ✅ AHORA ES CORRECTO
        "Skippable": "false",
        "dn4": null
      }
    }
  ],
  "userName": "doctorWho",
  "userRoles": ["practitioner"],
  "totalTasks": 1
}
```

---

**Fecha de solución**: 13 de noviembre de 2025  
**Tiempo invertido**: ~3 horas de debugging  
**Problema real**: Un simple carácter (comillas) en el BPMN 😅
