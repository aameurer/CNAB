@echo off
echo ==========================================
echo      Iniciando CNAB Dashboard PRO
echo ==========================================
echo.
echo Verificando instalacao do Java...
java -version
if %errorlevel% neq 0 (
    echo ERRO: Java nao encontrado! Instale o JDK 17 ou superior.
    pause
    exit /b
)

echo.
echo Compilando e Iniciando o Servidor Spring Boot...
cd cnab-dashboard
call mvnw spring-boot:run
if %errorlevel% neq 0 (
    echo.
    echo Tentando usar 'mvn' do sistema caso mvnw falhe...
    mvn spring-boot:run
)

pause
