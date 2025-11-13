# Script de prueba rápida para el proceso específico
# Actualiza el $processId con el ID de tu proceso activo

$processId = "b6817f8d-7c40-4f84-8745-7600a62f8153"  # Cambiar por tu ID de proceso activo
$user = "doctorWho"
$pass = "doctorWho"
$baseUrl = "http://localhost:8080"

$credentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${user}:${pass}"))
$headers = @{
    Authorization = "Basic $credentials"
}

Write-Host "`n=====================================" -ForegroundColor Cyan
Write-Host "Test de Tareas - Kogito Assessment" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "Usuario: $user" -ForegroundColor Yellow
Write-Host "Proceso ID: $processId`n" -ForegroundColor Yellow

# Primero verificar que el proceso existe
Write-Host "1. Verificando que el proceso existe..." -ForegroundColor Green
try {
    $process = Invoke-RestMethod -Uri "$baseUrl/assessment/$processId" -Headers $headers
    Write-Host "   ✓ Proceso encontrado!" -ForegroundColor Green
    Write-Host "   Estado: $($process.id)`n" -ForegroundColor White
} catch {
    Write-Host "   ✗ Error: El proceso no existe o no es accesible" -ForegroundColor Red
    Write-Host "   $($_.Exception.Message)`n" -ForegroundColor Red
    exit
}

# Probar el endpoint personalizado de tareas
Write-Host "2. Probando endpoint personalizado /assessment/$processId/tasks" -ForegroundColor Green
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/assessment/$processId/tasks" -Headers $headers
    
    Write-Host "   ✓ Respuesta recibida!" -ForegroundColor Green
    Write-Host "`n   === RESPUESTA COMPLETA ===" -ForegroundColor Cyan
    $response | ConvertTo-Json -Depth 10
    Write-Host "   ========================`n" -ForegroundColor Cyan
    
    if ($response.tasks) {
        $count = if ($response.tasks -is [array]) { $response.tasks.Count } else { 1 }
        Write-Host "   Total de tareas encontradas: $count" -ForegroundColor Magenta
        
        if ($count -gt 0) {
            Write-Host "`n   📋 Primera tarea:" -ForegroundColor Yellow
            $task = if ($response.tasks -is [array]) { $response.tasks[0] } else { $response.tasks }
            Write-Host "      - ID: $($task.id)" -ForegroundColor White
            Write-Host "      - Nombre: $($task.name)" -ForegroundColor White
            Write-Host "      - Fase: $($task.phase)" -ForegroundColor White
            Write-Host "      - Estado de Fase: $($task.phaseStatus)" -ForegroundColor White
            
            if ($task.parameters -and $task.parameters.GroupId) {
                Write-Host "      - GroupId: $($task.parameters.GroupId)" -ForegroundColor Cyan
            }
        }
    } else {
        Write-Host "   ⚠ No se encontraron tareas" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "   ✗ Error al obtener tareas" -ForegroundColor Red
    Write-Host "   $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails.Message) {
        Write-Host "   Detalle: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
}

Write-Host "`n=====================================" -ForegroundColor Cyan
Write-Host "Si no ves tareas, revisa los logs de la aplicación" -ForegroundColor Yellow
Write-Host "Busca mensajes que digan:" -ForegroundColor Yellow
Write-Host "  - 'Getting tasks for process'" -ForegroundColor Gray
Write-Host "  - 'Checking assignment for task'" -ForegroundColor Gray
Write-Host "  - 'User roles:' debe mostrar [practitioner]" -ForegroundColor Gray
Write-Host "=====================================" -ForegroundColor Cyan
