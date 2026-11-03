$sourceDir = "D:\APK-Rebuild\source\app\src\main"
$files = Get-ChildItem -Path $sourceDir -Recurse -File | Where-Object { $_.Extension -eq ".kt" }
$files | Select-String -Pattern "FloatingBubble|Bubble|floating"

