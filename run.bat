@echo off
echo ========================================================
echo Starting TPO Buddy - AI-Based Placement Management System
echo ========================================================
cd /d "%~dp0"

:: Auto-detect Java Runtime
if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
) else if exist "C:\Users\Admin\.p2\pool\plugins\org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_25.0.4.v20260826-0822\jre\bin\java.exe" (
    set "JAVA_HOME=C:\Users\Admin\.p2\pool\plugins\org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_25.0.4.v20260826-0822\jre"
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

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
