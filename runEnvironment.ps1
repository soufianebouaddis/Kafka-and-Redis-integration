# 1. Start Redis
Write-Host "Starting Redis..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd 'C:\Java\Redis-x64-5.0.14.1'; .\redis-server.exe"

# 2. Start PostgreSQL
Write-Host "Starting PostgreSQL..." -ForegroundColor Elephant
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd 'C:\Java\pgsql\bin'; .\postgres.exe -D 'C:\Java\pgsql\data'"

# 3. Start Kafka (using Java 21 environment)
Write-Host "Starting Kafka..." -ForegroundColor Yellow

$KafkaCommand = @"
# Set JAVA_HOME
`$env:JAVA_HOME = 'C:\Downloads\jdk-21.0.5_windows-x64_bin\jdk-21.0.5'

# Update Path
`$env:Path = `$env:JAVA_HOME + '\bin;' + `$env:Path

# Go to Kafka directory
cd 'C:\kafka\bin\windows'

# Start Kafka
.\kafka-server-start.bat 'C:\kafka\config\server.properties'
"@

Start-Process powershell -ArgumentList "-NoExit", "-Command", $KafkaCommand
