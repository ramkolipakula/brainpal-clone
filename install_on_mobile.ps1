$adb = "D:\tools\android-sdk\platform-tools\adb.exe"
$apk = "D:\APK-Rebuild\delivery\RebuiltApp.apk"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  BrainPal Live Mobile Deployment & Installation Tool     " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

Write-Host "`n[1/5] Checking for connected Android devices via ADB..." -ForegroundColor Yellow
$devices = & $adb devices
$deviceList = $devices | Where-Object { $_ -match "\tdevice$" }

if (-not $deviceList) {
    Write-Host "`nWaiting for device to connect..." -ForegroundColor Yellow
    Write-Host "Make sure:" -ForegroundColor Gray
    Write-Host " 1. USB Debugging is ON in Settings -> Developer Options" -ForegroundColor Gray
    Write-Host " 2. USB connection mode is set to 'File Transfer / MTP'" -ForegroundColor Gray
    Write-Host " 3. Look at your phone screen and tap 'Always allow from this computer' -> 'Allow'" -ForegroundColor Gray
    Write-Host "`nPolling for device (Ctrl+C to cancel)..." -ForegroundColor Yellow
    & $adb wait-for-device
}

$model = (& $adb shell getprop ro.product.model).Trim()
$brand = (& $adb shell getprop ro.product.brand).Trim()
$androidVer = (& $adb shell getprop ro.build.version.release).Trim()
$sdkVer = (& $adb shell getprop ro.build.version.sdk).Trim()

Write-Host "`n[2/5] Connected Device Detected!" -ForegroundColor Green
Write-Host "  Model: $brand $model" -ForegroundColor Green
Write-Host "  Android Version: $androidVer (API $sdkVer)" -ForegroundColor Green

Write-Host "`n[3/5] Installing RebuiltApp.apk..." -ForegroundColor Yellow
$installResult = & $adb install -r $apk
Write-Host $installResult

if ($installResult -match "Success") {
    Write-Host "`n[4/5] Pre-granting necessary wellbeing permissions..." -ForegroundColor Yellow
    & $adb shell appops set com.brainrot.rebuilt GET_USAGE_STATS allow
    Write-Host "  - Usage Access (GET_USAGE_STATS): GRANTED" -ForegroundColor Green
    & $adb shell appops set com.brainrot.rebuilt SYSTEM_ALERT_WINDOW allow
    Write-Host "  - Display Over Other Apps (SYSTEM_ALERT_WINDOW): GRANTED" -ForegroundColor Green

    # Grant Notifications permission
    & $adb shell pm grant com.brainrot.rebuilt android.permission.POST_NOTIFICATIONS
    Write-Host "  - Notifications (POST_NOTIFICATIONS): GRANTED" -ForegroundColor Green

    # Enable Accessibility Service for user 0 so floating reel pill is immediately active
    & $adb shell settings --user 0 put secure enabled_accessibility_services com.brainrot.rebuilt/com.brainrot.rebuilt.service.ReelsAccessibilityService
    & $adb shell settings --user 0 put secure accessibility_enabled 1
    Write-Host "  - Accessibility Service (Reels Counter): ENABLED" -ForegroundColor Green

    Write-Host "`n[5/5] Launching BrainPal on your mobile screen..." -ForegroundColor Cyan
    & $adb shell am start -n com.brainrot.rebuilt/com.brainrot.rebuilt.MainActivity

    Write-Host "`n==========================================================" -ForegroundColor Green
    Write-Host "  BrainPal is now running live on your mobile! 🚀         " -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Green
    Write-Host "`nHow to test live on your phone:" -ForegroundColor White
    Write-Host " 1. BrainPal will open to the Home Screen with your Brain Mascot." -ForegroundColor Gray
    Write-Host " 2. Open Instagram or YouTube Shorts -> The floating reel counter pill '🧠 X / limit' will appear!" -ForegroundColor Gray
    Write-Host " 3. Swipe through short videos -> The counter pill increments live on your screen!" -ForegroundColor Gray
} else {
    Write-Host "`nInstallation failed. Output: $installResult" -ForegroundColor Red
}
