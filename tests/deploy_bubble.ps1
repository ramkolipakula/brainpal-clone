$adb = "D:\tools\android-sdk\platform-tools\adb.exe"
$apk = "D:\APK-Rebuild\delivery\RebuiltApp.apk"

Write-Host "=== 1. Installing APK with Floating Bubble Support ==="
$res = & $adb install -r $apk
Write-Host "Install: $res"

Write-Host "`n=== 2. Enabling Accessibility Service via ADB ==="
& $adb shell settings put secure enabled_accessibility_services com.brainrot.rebuilt/com.brainrot.rebuilt.service.ReelsAccessibilityService
& $adb shell settings put secure accessibility_enabled 1
$enabled = & $adb shell settings get secure enabled_accessibility_services
Write-Host "Current enabled accessibility services: $enabled"

Write-Host "`n=== 3. Ensuring Overlay Permission is Granted ==="
& $adb shell appops set com.brainrot.rebuilt SYSTEM_ALERT_WINDOW allow
Write-Host "SYSTEM_ALERT_WINDOW: ALLOWED"

Write-Host "`n=== 4. Launching BrainPal ==="
& $adb shell am start -n com.brainrot.rebuilt/com.brainrot.rebuilt.MainActivity

