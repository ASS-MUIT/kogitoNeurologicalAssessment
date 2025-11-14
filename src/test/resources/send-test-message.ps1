# Script para enviar mensaje de prueba a Kafka en formato CloudEvents
# Este mensaje inicia el proceso de assessment en Kogito

Write-Host "=== Envio de Mensaje CloudEvents a Kafka ===" -ForegroundColor Green
Write-Host ""

# Mensaje en formato CloudEvents para Kogito
# El campo 'data' contiene la URL del Appointment
$cloudEventJson = @'
{
  "specversion": "1.0",
  "type": "appointments",
  "source": "test-script",
  "id": "test-123",
  "datacontenttype": "text/plain",
  "data": "https://hapi.fhir.org/baseR5/Appointment/773551"
}
'@

# Guardar en archivo temporal
$cloudEventJson | Out-File -FilePath "cloudevent-message.json" -Encoding UTF8 -NoNewline

Write-Host "Mensaje CloudEvent generado:" -ForegroundColor Yellow
Write-Host $cloudEventJson
Write-Host ""

# También crear un mensaje simple de texto por si acaso
$simpleMessage = "http://hapi.fhir.org/baseR5/Appointment/123456"
$simpleMessage | Out-File -FilePath "simple-message.txt" -Encoding UTF8 -NoNewline

Write-Host "=== OPCION 1: Mensaje CloudEvents (RECOMENDADO) ===" -ForegroundColor Cyan
Write-Host "Ejecuta este comando en tu terminal de Kafka:" -ForegroundColor White
Write-Host "Get-Content cloudevent-message.json | kafka-console-producer.bat --broker-list localhost:9092 --topic appointments" -ForegroundColor Yellow
Write-Host ""

Write-Host "=== OPCION 2: Mensaje simple de texto ===" -ForegroundColor Cyan
Write-Host "Si CloudEvents no funciona, prueba con texto simple:" -ForegroundColor White
Write-Host "Get-Content simple-message.txt | kafka-console-producer.bat --broker-list localhost:9092 --topic appointments" -ForegroundColor Yellow
Write-Host ""

Write-Host "Archivos creados:" -ForegroundColor Green
Write-Host "  - cloudevent-message.json (formato CloudEvents)" -ForegroundColor White
Write-Host "  - simple-message.txt (formato simple)" -ForegroundColor White
