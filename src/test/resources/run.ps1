# Script para ejecutar la aplicación Spring Boot
# Solución al error 206 de Windows (classpath demasiado largo)

Write-Host "Ejecutando aplicación Kogito Spring Boot..." -ForegroundColor Green

# Ruta al JAR generado
$jarPath = ".\target\neurological-assessment-1.0.0-SNAPSHOT.jar"

# Verificar si el JAR existe
if (Test-Path $jarPath) {
    Write-Host "JAR encontrado: $jarPath" -ForegroundColor Green
    java -jar $jarPath
} else {
    Write-Host "ERROR: JAR no encontrado. Ejecuta primero: mvn clean package -DskipTests" -ForegroundColor Red
    Write-Host "Intentando compilar ahora..." -ForegroundColor Yellow
    mvn clean package -DskipTests
    if ($LASTEXITCODE -eq 0) {
        Write-Host "`nCompilación exitosa. Ejecutando aplicación..." -ForegroundColor Green
        java -jar $jarPath
    } else {
        Write-Host "`nError en la compilación." -ForegroundColor Red
    }
}
