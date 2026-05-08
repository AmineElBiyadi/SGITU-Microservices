@echo off

echo ================================
echo DUMP POM.XML
echo ================================
type pom.xml
echo.

echo ================================
echo DUMP APPLICATION.PROPERTIES
echo ================================
type src\main\resources\application.properties
echo.

echo ================================
echo DUMP DOCKERFILE
echo ================================
type Dockerfile
echo.

echo ================================
echo DUMP DOCKER-COMPOSE
echo ================================
type docker-compose.yml
echo.

echo ================================
echo DUMP ALL JAVA FILES
echo ================================

for /R src %%f in (*.java) do (
    echo -------------------------------
    echo FILE: %%f
    echo -------------------------------
    type "%%f"
    echo.
)

pause