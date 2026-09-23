@REM Maven Wrapper Batch Script
@IF "%DEBUG%" == "" @ECHO OFF
@SETLOCAL
SET "MAVEN_CMD=C:\Users\nisht\apache-maven-3.9.6\bin\mvn.cmd"
IF EXIST "%MAVEN_CMD%" (
    "%MAVEN_CMD%" %*
) ELSE (
    mvn %*
)
