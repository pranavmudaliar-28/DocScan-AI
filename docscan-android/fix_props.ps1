$files = @(
    "c:\Slasheasy Client Project Detail\slasheasy Ai project\DocScan AI\docscan-android\app\src\main\java\com\example\docscanai\ui\main\MainScreen.kt",
    "c:\Slasheasy Client Project Detail\slasheasy Ai project\DocScan AI\docscan-android\app\src\main\java\com\example\docscanai\ui\search\SearchScreen.kt"
)

foreach ($file in $files) {
    $content = Get-Content -Raw -Encoding UTF8 $file
    
    # Replace the incorrectly formatted properties
    $content = $content -replace '(?m)^private val (\w+)\s*=\s*androidx\.compose\.material3\.MaterialTheme\.colorScheme', 'private val $1: androidx.compose.ui.graphics.Color`n    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme'
    
    Set-Content -Path $file -Value $content -Encoding UTF8
}

Write-Host "Fixed composable properties!"
