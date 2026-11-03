$adb = "D:\tools\android-sdk\platform-tools\adb.exe"
$apk = "D:\APK-Rebuild\delivery\RebuiltApp.apk"

Write-Host "=== 1. Checking Device ==="
& $adb devices -l

Write-Host "`n=== 2. Installing Updated APK on Device ==="
$installResult = & $adb install -r -d $apk
Write-Host "Install Result: $installResult"

Write-Host "`n=== 3. Pre-granting Permissions ==="
& $adb shell appops set com.brainrot.rebuilt GET_USAGE_STATS allow
Write-Host "Granted GET_USAGE_STATS"
& $adb shell appops set com.brainrot.rebuilt SYSTEM_ALERT_WINDOW allow
Write-Host "Granted SYSTEM_ALERT_WINDOW"

Write-Host "`n=== 4. Launching MainActivity ==="
& $adb shell am start -n com.brainrot.rebuilt/com.brainrot.rebuilt.MainActivity

Write-Host "`n=== 5. Checking for any Immediate Crashes ==="
Start-Sleep -Seconds 3
$crash = & $adb logcat -d -b crash
if ($crash) {
    Write-Host "Crash found:"
    Write-Host $crash
} else {
    Write-Host "No crash detected! App launched smoothly!"
}

