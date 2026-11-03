$adb = "D:\tools\android-sdk\platform-tools\adb.exe"

Write-Host "=== 1. Checking running process for com.brainrot.rebuilt ==="
& $adb shell "ps -A | grep brainrot"

Write-Host "`n=== 2. Check current focused window / activity ==="
& $adb shell "dumpsys window | grep -E 'mCurrentFocus|mFocusedApp'"

Write-Host "`n=== 3. Clear crash log and launch app fresh ==="
& $adb logcat -c
& $adb shell am start -n com.brainrot.rebuilt/com.brainrot.rebuilt.MainActivity
Start-Sleep -Seconds 2

Write-Host "`n=== 4. Check for any NEW crash after launch ==="
& $adb logcat -d -b crash

