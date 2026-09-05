@echo off
setlocal

set "BASE_DIR=%~dp0"
set "PROPS=%BASE_DIR%gradle\wrapper\gradle-wrapper.properties"
set "DIST_URL="
for /f "tokens=1,* delims==" %%A in ('findstr /b "distributionUrl=" "%PROPS%"') do set "DIST_URL=%%B"
set "DIST_URL=%DIST_URL:\:=%"
set "GRADLE_VERSION="
for /f "tokens=2 delims=-" %%A in ("%DIST_URL%") do set "GRADLE_VERSION=%%A"
set "GRADLE_HOME=%USERPROFILE%\.gradle"
set "INSTALL_DIR=%GRADLE_HOME%\jarvis-wrapper\gradle-%GRADLE_VERSION%"
set "ZIP=%GRADLE_HOME%\jarvis-wrapper\gradle-%GRADLE_VERSION%-bin.zip"

if not exist "%INSTALL_DIR%\bin\gradle.bat" (
  if not exist "%GRADLE_HOME%\jarvis-wrapper" mkdir "%GRADLE_HOME%\jarvis-wrapper"
  if not exist "%ZIP%" (
    echo Downloading Gradle %GRADLE_VERSION%...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%ZIP%'"
    if errorlevel 1 exit /b 1
  )
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%ZIP%' '%GRADLE_HOME%\jarvis-wrapper'"
  if errorlevel 1 exit /b 1
)

call "%INSTALL_DIR%\bin\gradle.bat" %*
endlocal
