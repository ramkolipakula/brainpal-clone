$adb = "D:\tools\android-sdk\platform-tools\adb.exe"
Write-Host "=== Connected Devices ==="
& $adb devices -l

Write-Host "`n=== Crash Logs (logcat -d -b crash) ==="
& $adb logcat -d -b crash

Write-Host "`n=== Android Runtime / FATAL EXCEPTION logs ==="
& $adb logcat -d | Select-String -Pattern "AndroidRuntime|FATAL EXCEPTION|com.brainrot.rebuilt" -Context 2, 10

