@echo off
set DIRNAME=%~dp0
if "%XIZHI_JAVA%"=="" set XIZHI_JAVA=%DIRNAME%.env\jdk\jdk17\bin\java.exe
if not exist "%XIZHI_JAVA%" set XIZHI_JAVA=java
"%XIZHI_JAVA%" -classpath "%DIRNAME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
