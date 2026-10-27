@echo off
echo ==============================================================================
echo TPO Buddy - Spring Boot 3 & Embedded Tomcat Server (IntelliJ IDEA Ready)
echo ==============================================================================
cd /d "%~dp0"

:: Auto-detect Java Runtime (Java 21 / 25)
if exist "D:\IntelliJ IDEA 2026.2.3\jbr\bin\java.exe" (
    set "JAVA_HOME=D:\IntelliJ IDEA 2026.2.3\jbr"
) else if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
) else if exist "C:\Users\Admin\.p2\pool\plugins\org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_25.0.4.v20260826-0822\jre\bin\java.exe" (
    set "JAVA_HOME=C:\Users\Admin\.p2\pool\plugins\org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_25.0.4.v20260826-0822\jre"
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

:: Load local environment variables from .env if present (excluded from Git)
if exist ".env" (
    for /f "usebackq tokens=1,* delims==" %%A in (".env") do (
        if /i "%%A"=="GEMINI_API_KEY" if "%GEMINI_API_KEY%"=="" set "GEMINI_API_KEY=%%B"
        if /i "%%A"=="GEMINI_MODEL" if "%GEMINI_MODEL%"=="" set "GEMINI_MODEL=%%B"
        if /i "%%A"=="FIREBASE_API_KEY" if "%FIREBASE_API_KEY%"=="" set "FIREBASE_API_KEY=%%B"
    )
)
if "%GEMINI_MODEL%"=="" set "GEMINI_MODEL=gemini-flash-lite-latest"

echo [1/3] Using Java runtime:
"%JAVA_HOME%\bin\java.exe" -version

echo.
echo [2/3] Application Endpoints:
echo   - Web Application: http://localhost:8085/
echo   - Login Portal:    http://localhost:8085/login.jsp
echo   - AI Interview:    http://localhost:8085/interview
echo   - React Portal:    http://localhost:8085/react-portal
echo   - Health Check:    http://localhost:8085/actuator/health
echo ==============================================================================
echo.

if exist "target\tpo-buddy.war" (
    echo [3/3] Launching packaged WAR...
    "%JAVA_HOME%\bin\java.exe" -Xmx512m -jar target\tpo-buddy.war
) else if exist "D:\maven\apache-maven-3.9.6\bin\mvn.cmd" (
    echo [3/3] Launching with Maven spring-boot:run...
    call "D:\maven\apache-maven-3.9.6\bin\mvn.cmd" spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx512m"
) else (
    echo [3/3] Launching with mvnw...
    call .\mvnw.cmd spring-boot:run
)

pause
