$sourceDir = "D:\APK-Rebuild\source\app\src\main"
$files = Get-ChildItem -Path $sourceDir -Recurse -File | Where-Object { $_.Extension -in @(".kt", ".xml", ".kts", ".json") }

$patterns = @("TODO", "FIXME", "placeholder", "dummy", "fake", "secret", "password", "api_key", "apikey")

foreach ($pat in $patterns) {
    $matches = $files | Select-String -Pattern $pat -SimpleMatch
    if ($matches) {
        Write-Host "Matches found for pattern: $pat"
        foreach ($m in $matches) {
            Write-Host "  $($m.Filename):$($m.LineNumber): $($m.Line.Trim())"
        }
    } else {
        Write-Host "No matches for: $pat"
    }
}

