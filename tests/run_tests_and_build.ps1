$env:JAVA_HOME = "D:\tools\jdk-17\jdk-17.0.20.1+1"
$env:ANDROID_HOME = "D:\tools\android-sdk"
$env:PATH = "D:\tools\jdk-17\jdk-17.0.20.1+1\bin;D:\tools\android-sdk\build-tools\34.0.0;D:\tools\android-sdk\platform-tools;D:\tools\gradle-8.7\bin;$env:PATH"

Set-Location "D:\APK-Rebuild\source"

Write-Host "=== 1. Running Unit Tests ==="
& "D:\tools\gradle-8.7\bin\gradle.bat" testDebugUnitTest --info
$testExit = $LASTEXITCODE
Write-Host "Test Exit Code: $testExit"

Write-Host "`n=== 2. Running Android Lint Analysis ==="
& "D:\tools\gradle-8.7\bin\gradle.bat" lintDebug
$lintExit = $LASTEXITCODE
Write-Host "Lint Exit Code: $lintExit"

Write-Host "`n=== 3. Rebuilding Debug APK ==="
& "D:\tools\gradle-8.7\bin\gradle.bat" assembleDebug
$buildExit = $LASTEXITCODE
Write-Host "Build Exit Code: $buildExit"

if ($testExit -ne 0 -or $lintExit -ne 0 -or $buildExit -ne 0) {
    Write-Error "Build or test step failed!"
    exit 1
}

Write-Host "`n=== 4. Comparing Fresh Built APK with Delivered APK ==="
$freshApk = "D:\APK-Rebuild\source\app\build\outputs\apk\debug\app-debug.apk"
$deliveredApk = "D:\APK-Rebuild\delivery\RebuiltApp.apk"

$freshHash = (Get-FileHash -Path $freshApk -Algorithm SHA256).Hash
$deliveredHash = (Get-FileHash -Path $deliveredApk -Algorithm SHA256).Hash
$freshSize = (Get-Item $freshApk).Length
$deliveredSize = (Get-Item $deliveredApk).Length

Write-Host "Fresh APK Size: $freshSize bytes | SHA256: $freshHash"
Write-Host "Delivered APK Size: $deliveredSize bytes | SHA256: $deliveredHash"

if ($freshHash -ne $deliveredHash) {
    Write-Host "Updating delivered APK with freshly built APK..."
    Copy-Item -Path $freshApk -Destination $deliveredApk -Force
    $deliveredHash = (Get-FileHash -Path $deliveredApk -Algorithm SHA256).Hash
    Write-Host "Updated Delivered APK SHA256: $deliveredHash"
} else {
    Write-Host "Delivered APK is up-to-date and matches fresh build exactly."
}

