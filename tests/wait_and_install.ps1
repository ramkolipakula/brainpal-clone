$adb = "D:\tools\android-sdk\platform-tools\adb.exe"
$apk = "D:\APK-Rebuild\delivery\RebuiltApp.apk"

Write-Host "Waiting for device to connect via ADB..."
& $adb wait-for-device
Write-Host "Device connected! Installing update..."
& $adb install -r $apk
Write-Host "Launching updated BrainPal..."
& $adb shell am start -n com.brainrot.rebuilt/com.brainrot.rebuilt.MainActivity
Write-Host "DONE!"

