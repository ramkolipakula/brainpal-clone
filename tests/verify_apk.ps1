$env:JAVA_HOME = "D:\tools\jdk-17\jdk-17.0.20.1+1"
$env:PATH = "D:\tools\jdk-17\jdk-17.0.20.1+1\bin;D:\tools\android-sdk\build-tools\34.0.0;D:\tools\android-sdk\cmdline-tools\latest\bin;$env:PATH"

$apkPath = 'D:\APK-Rebuild\delivery\RebuiltApp.apk'

Write-Host "=== 1. Checksum & Basic File Info ==="
$item = Get-Item $apkPath
Write-Host "Path: $($item.FullName)"
Write-Host "Size: $($item.Length) bytes"
$sha256 = (Get-FileHash -Path $apkPath -Algorithm SHA256).Hash
Write-Host "SHA256: $sha256"

Write-Host "`n=== 2. Signature & Certificate Details ==="
& "D:\tools\android-sdk\build-tools\34.0.0\apksigner.bat" verify --verbose --print-certs $apkPath

Write-Host "`n=== 3. APK Summary & SDK Targets ==="
& "D:\tools\android-sdk\cmdline-tools\latest\bin\apkanalyzer.bat" apk summary $apkPath

Write-Host "`n=== 4. Manifest Permissions & Components ==="
& "D:\tools\android-sdk\cmdline-tools\latest\bin\apkanalyzer.bat" manifest permissions $apkPath
& "D:\tools\android-sdk\cmdline-tools\latest\bin\apkanalyzer.bat" manifest target-sdk $apkPath
& "D:\tools\android-sdk\cmdline-tools\latest\bin\apkanalyzer.bat" manifest min-sdk $apkPath
& "D:\tools\android-sdk\cmdline-tools\latest\bin\apkanalyzer.bat" manifest version-name $apkPath
& "D:\tools\android-sdk\cmdline-tools\latest\bin\apkanalyzer.bat" manifest version-code $apkPath
