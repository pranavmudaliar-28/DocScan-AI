$files = @(
    "c:\Slasheasy Client Project Detail\slasheasy Ai project\DocScan AI\docscan-android\app\src\main\java\com\example\docscanai\ui\main\MainScreen.kt",
    "c:\Slasheasy Client Project Detail\slasheasy Ai project\DocScan AI\docscan-android\app\src\main\java\com\example\docscanai\ui\search\SearchScreen.kt"
)

$replacements = [ordered]@{
    "Color(0xFFFAFAFA)" = "androidx.compose.material3.MaterialTheme.colorScheme.background"
    "Color.White" = "androidx.compose.material3.MaterialTheme.colorScheme.surface"
    "Color(0xFF111827)" = "androidx.compose.material3.MaterialTheme.colorScheme.onBackground"
    "Color(0xFF6B7280)" = "androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant"
    "Color(0xFF9CA3AF)" = "androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)"
    "Color(0xFF4B5563)" = "androidx.compose.material3.MaterialTheme.colorScheme.onSurface"
    "Color(0xFFE5E7EB)" = "androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant"
    "Color(0xFFF3F4F6)" = "androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant"
    "Color(0xFFE8EAED)" = "androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant"
    "Color(0xFFF5F6FA)" = "androidx.compose.material3.MaterialTheme.colorScheme.background"
    "Color(0xFFDBEAFE)" = "androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer"
    "Color(0xFF2563EB)" = "androidx.compose.material3.MaterialTheme.colorScheme.primary"
    "Color(0xFFEEF2FF)" = "androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer"
}

foreach ($file in $files) {
    $content = Get-Content -Raw -Encoding UTF8 $file
    foreach ($key in $replacements.Keys) {
        $content = $content.Replace($key, $replacements[$key])
    }
    Set-Content -Path $file -Value $content -Encoding UTF8
}

Write-Host "Colors refactored successfully!"
