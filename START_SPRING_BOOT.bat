@echo off
echo ==============================================================================
echo TPO Buddy - Spring Boot 3 & Embedded Tomcat Server (IntelliJ IDEA Ready)
echo ==============================================================================
cd /d "%~dp0"

:: Auto-detect Java Runtime (Java 21 / 25)
if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
) else if exist "C:\Users\Admin\.p2\pool\plugins\org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_25.0.4.v20260826-0822\jre\bin\java.exe" (
    set "JAVA_HOME=C:\Users\Admin\.p2\pool\plugins\org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_25.0.4.v20260826-0822\jre"
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

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
    "%JAVA_HOME%\bin\java.exe" -jar target\tpo-buddy.war
) else if exist "D:\maven\apache-maven-3.9.6\bin\mvn.cmd" (
    echo [3/3] Launching with Maven spring-boot:run...
    call "D:\maven\apache-maven-3.9.6\bin\mvn.cmd" spring-boot:run
) else (
    echo [3/3] Launching with mvnw...
    call .\mvnw.cmd spring-boot:run
)

pause
