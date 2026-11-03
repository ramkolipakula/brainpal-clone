$adb = "D:\tools\android-sdk\platform-tools\adb.exe"
Write-Host "Installing APK..."
& $adb install -r "D:\APK-Rebuild\delivery\RebuiltApp.apk"

Write-Host "Setting permissions..."
& $adb shell appops set com.brainrot.rebuilt GET_USAGE_STATS allow
& $adb shell appops set com.brainrot.rebuilt SYSTEM_ALERT_WINDOW allow
& $adb shell pm grant com.brainrot.rebuilt android.permission.POST_NOTIFICATIONS

$current = (& $adb shell settings get secure enabled_accessibility_services).Trim()
Write-Host "Current accessibility services: '$current'"
$svc = "com.brainrot.rebuilt/com.brainrot.rebuilt.service.ReelsAccessibilityService"

if (-not $current.Contains($svc)) {
    if ($current -eq "null" -or [string]::IsNullOrWhiteSpace($current)) {
        $newSvc = $svc
    } else {
        $newSvc = "${current}:${svc}"
    }
    Write-Host "Enabling accessibility service: $newSvc"
    & $adb shell settings put secure enabled_accessibility_services "$newSvc"
} else {
    Write-Host "Service already in enabled list."
}

& $adb shell settings put secure accessibility_enabled 1
$updated = (& $adb shell settings get secure enabled_accessibility_services).Trim()
Write-Host "Updated enabled_accessibility_services: '$updated'"

# Restart app
Write-Host "Restarting BrainPal..."
& $adb shell am force-stop com.brainrot.rebuilt
& $adb shell am start -n com.brainrot.rebuilt/com.brainrot.rebuilt.MainActivity
Start-Sleep -Seconds 2

# Verify services running
Write-Host "Checking running services..."
& $adb shell dumpsys activity services com.brainrot.rebuilt
