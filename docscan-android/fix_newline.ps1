$files = @(
    "c:\Slasheasy Client Project Detail\slasheasy Ai project\DocScan AI\docscan-android\app\src\main\java\com\example\docscanai\ui\main\MainScreen.kt",
    "c:\Slasheasy Client Project Detail\slasheasy Ai project\DocScan AI\docscan-android\app\src\main\java\com\example\docscanai\ui\search\SearchScreen.kt"
)

foreach ($file in $files) {
    $content = Get-Content -Raw -Encoding UTF8 $file
    
    # Replace the literal `n with actual newline
    $content = $content.Replace("``n", "`n")
    
    Set-Content -Path $file -Value $content -Encoding UTF8
}

Write-Host "Fixed literal newline!"
