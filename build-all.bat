@echo off
setlocal

set "PROJECT_ROOT=%~dp0"
set "WEB_DIR=%PROJECT_ROOT%web"
set "ASSETS_DIR=%PROJECT_ROOT%app\src\main\assets\web"
set "GRADLE=%PROJECT_ROOT%gradlew.bat"
set "BUILD_TASK=assembleRelease"
set "APK_OUT=%PROJECT_ROOT%app\build\outputs\apk\release\app-release.apk"

echo ========================================
echo  Building Frontend
echo ========================================

cd /d "%WEB_DIR%"
if errorlevel 1 (
    echo ERROR: Cannot find web directory: %WEB_DIR%
    exit /b 1
)

echo Installing dependencies...
call npm install
if errorlevel 1 (
    echo ERROR: npm install failed
    exit /b 1
)

echo Building frontend...
call npm run build
if errorlevel 1 (
    echo ERROR: npm run build failed
    exit /b 1
)

echo Frontend build complete.

echo ========================================
echo  Packaging Frontend into Backend Assets
echo ========================================

if exist "%ASSETS_DIR%" (
    echo Cleaning old assets...
    rmdir /s /q "%ASSETS_DIR%"
)

echo Copying dist to assets...
xcopy /e /i /q "%WEB_DIR%\dist\*" "%ASSETS_DIR%"
if errorlevel 1 (
    echo ERROR: Failed to copy frontend assets
    exit /b 1
)

echo Assets packaged successfully.

echo ========================================
echo  Building Backend (Android APK)
echo ========================================

cd /d "%PROJECT_ROOT%"
echo Building signed release APK...
call "%GRADLE%" %BUILD_TASK%
if errorlevel 1 (
    echo ERROR: Gradle build failed
    exit /b 1
)

echo ========================================
echo  Build Complete!
echo ========================================
echo APK: %APK_OUT%

endlocal
