# Script rápido para probar las tareas
# Asegúrate de tener la aplicación corriendo primero

$processId = "82f142fc-81ac-4f13-b40f-bc0acaccb48b"  # Cambia esto por tu ID de proceso
$user = "doctorWho"
$pass = "doctorWho"

$credentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${user}:${pass}"))
$headers = @{
    Authorization = "Basic $credentials"
    "Accept" = "application/json"
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Probando Endpoints de Tareas de Kogito" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "Usuario: $user" -ForegroundColor Yellow
Write-Host "Proceso: $processId`n" -ForegroundColor Yellow

# Test 1: Todas las tareas del usuario
Write-Host "1. Obteniendo TODAS las tareas del usuario..." -ForegroundColor Green
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/assessment/tasks" `
        -Headers $headers -Method GET
    
    Write-Host "   ✓ Respuesta recibida!" -ForegroundColor Green
    Write-Host "`n   Detalles de la respuesta:" -ForegroundColor White
    $response | ConvertTo-Json -Depth 10
    
    if ($response.tasks -and $response.tasks.Count -gt 0) {
        Write-Host "`n   → Se encontraron $($response.tasks.Count) tarea(s)" -ForegroundColor Cyan
    } else {
        Write-Host "`n   ⚠ No se encontraron tareas" -ForegroundColor Yellow
    }
} catch {
    Write-Host "   ✗ Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   Detalle: $($_.ErrorDetails.Message)" -ForegroundColor Red
}

Write-Host "`n----------------------------------------`n" -ForegroundColor Gray

# Test 2: Tareas de un proceso específico
Write-Host "2. Obteniendo tareas del proceso $processId..." -ForegroundColor Green
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/assessment/$processId/tasks" `
        -Headers $headers -Method GET
    
    Write-Host "   ✓ Respuesta recibida!" -ForegroundColor Green
    Write-Host "`n   Detalles de la respuesta:" -ForegroundColor White
    $response | ConvertTo-Json -Depth 10
    
    if ($response.tasks -and $response.tasks.Count -gt 0) {
        Write-Host "`n   → Se encontraron $($response.tasks.Count) tarea(s)" -ForegroundColor Cyan
        Write-Host "`n   📝 Información de la primera tarea:" -ForegroundColor Magenta
        $task = $response.tasks[0]
        Write-Host "      ID: $($task.id)" -ForegroundColor White
        Write-Host "      Nombre: $($task.name)" -ForegroundColor White
        Write-Host "      Estado: $($task.phaseStatus)" -ForegroundColor White
    } else {
        Write-Host "`n   ⚠ No se encontraron tareas para este proceso" -ForegroundColor Yellow
    }
} catch {
    Write-Host "   ✗ Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   Detalle: $($_.ErrorDetails.Message)" -ForegroundColor Red
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Prueba completada!" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "💡 Tip: Si no ves tareas, verifica:" -ForegroundColor Yellow
Write-Host "   1. Que el proceso esté activo: curl -u doctorWho:doctorWho http://localhost:8080/assessment" -ForegroundColor Gray
Write-Host "   2. Los logs de la aplicación para mensajes de debug" -ForegroundColor Gray
Write-Host "   3. Que el ID del proceso sea correcto`n" -ForegroundColor Gray
