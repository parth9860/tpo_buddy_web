@echo off
echo ========================================================
echo Starting TPO Buddy - AI-Based Placement Management System
echo ========================================================
cd /d "%~dp0"

:: Auto-detect Java Runtime
if exist "D:\IntelliJ IDEA 2026.2.3\jbr\bin\java.exe" (
    set "JAVA_HOME=D:\IntelliJ IDEA 2026.2.3\jbr"
) else if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
) else if exist "C:\Users\Admin\.p2\pool\plugins\org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_25.0.4.v20260826-0822\jre\bin\java.exe" (
    set "JAVA_HOME=C:\Users\Admin\.p2\pool\plugins\org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_25.0.4.v20260826-0822\jre"
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

:: Load local configuration from .env if present (excluded from Git)
if exist ".env" (
    for /f "usebackq tokens=1,* delims==" %%A in (".env") do (
        if /i "%%A"=="GEMINI_API_KEY" if "%GEMINI_API_KEY%"=="" set "GEMINI_API_KEY=%%B"
        if /i "%%A"=="GEMINI_MODEL" if "%GEMINI_MODEL%"=="" set "GEMINI_MODEL=%%B"
        if /i "%%A"=="FIREBASE_API_KEY" if "%FIREBASE_API_KEY%"=="" set "FIREBASE_API_KEY=%%B"
    )
)
if "%GEMINI_MODEL%"=="" set "GEMINI_MODEL=gemini-flash-lite-latest"

:: Run Spring Boot via Maven or Executable WAR
if exist "target\tpo-buddy.war" (
    echo Launching pre-built Spring Boot WAR...
    "%JAVA_HOME%\bin\java.exe" -jar target\tpo-buddy.war
) else if exist "D:\maven\apache-maven-3.9.6\bin\mvn.cmd" (
    echo Compiling and running Spring Boot with embedded Tomcat...
    call "D:\maven\apache-maven-3.9.6\bin\mvn.cmd" spring-boot:run
) else (
    echo Error: Neither target\tpo-buddy.war nor Maven executable found.
)

pause
