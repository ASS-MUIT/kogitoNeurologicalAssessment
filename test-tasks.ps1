# Script para probar los endpoints de tareas en Kogito
# Ejecutar después de tener la aplicación corriendo

$processId = "82f142fc-81ac-4f13-b40f-bc0acaccb48b"
$baseUrl = "http://localhost:8080"
$user = "doctorWho"
$pass = "doctorWho"

# Crear credenciales Base64
$credentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${user}:${pass}"))
$headers = @{
    Authorization = "Basic $credentials"
    "Content-Type" = "application/json"
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Testing Kogito Task Endpoints" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "1. Testing GET /assessment - List all process instances" -ForegroundColor Yellow
Write-Host "URL: $baseUrl/assessment" -ForegroundColor Gray
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/assessment" -Headers $headers -Method GET
    Write-Host "✓ Success!" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 5
} catch {
    Write-Host "✗ Error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

Write-Host "2. Testing GET /assessment/{processId} - Get specific process instance" -ForegroundColor Yellow
Write-Host "URL: $baseUrl/assessment/$processId" -ForegroundColor Gray
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/assessment/$processId" -Headers $headers -Method GET
    Write-Host "✓ Success!" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 5
} catch {
    Write-Host "✗ Error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

Write-Host "3. Testing GET /assessment/{processId}/tasks - Tasks for specific process" -ForegroundColor Yellow
Write-Host "URL: $baseUrl/assessment/$processId/tasks" -ForegroundColor Gray
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/assessment/$processId/tasks" -Headers $headers -Method GET
    Write-Host "✓ Success!" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 5
} catch {
    Write-Host "✗ Error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

Write-Host "4. Testing GET /assessment/tasks - All tasks for authenticated user" -ForegroundColor Yellow
Write-Host "URL: $baseUrl/assessment/tasks" -ForegroundColor Gray
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/assessment/tasks" -Headers $headers -Method GET
    Write-Host "✓ Success!" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 5
} catch {
    Write-Host "✗ Error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

Write-Host "5. Testing GET /{processId}/tasks (alternative endpoint)" -ForegroundColor Yellow
Write-Host "URL: $baseUrl/$processId/tasks" -ForegroundColor Gray
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/$processId/tasks" -Headers $headers -Method GET
    Write-Host "✓ Success!" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 5
} catch {
    Write-Host "✗ Error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test completed!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
