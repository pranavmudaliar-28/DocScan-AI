import os

files = [
    r"c:\Slasheasy Client Project Detail\slasheasy Ai project\DocScan AI\docscan-android\app\src\main\java\com\example\docscanai\ui\main\MainScreen.kt",
    r"c:\Slasheasy Client Project Detail\slasheasy Ai project\DocScan AI\docscan-android\app\src\main\java\com\example\docscanai\ui\search\SearchScreen.kt"
]

replacements = {
    "Color(0xFFFAFAFA)": "androidx.compose.material3.MaterialTheme.colorScheme.background",
    "Color.White": "androidx.compose.material3.MaterialTheme.colorScheme.surface",
    "Color(0xFF111827)": "androidx.compose.material3.MaterialTheme.colorScheme.onBackground",
    "Color(0xFF6B7280)": "androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant",
    "Color(0xFF9CA3AF)": "androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)",
    "Color(0xFF4B5563)": "androidx.compose.material3.MaterialTheme.colorScheme.onSurface",
    "Color(0xFFE5E7EB)": "androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant",
    "Color(0xFFF3F4F6)": "androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant",
    "Color(0xFFE8EAED)": "androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant",
    "Color(0xFFF5F6FA)": "androidx.compose.material3.MaterialTheme.colorScheme.background",
    "Color(0xFFDBEAFE)": "androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer",
    "Color(0xFF2563EB)": "androidx.compose.material3.MaterialTheme.colorScheme.primary",
    "Color(0xFFEEF2FF)": "androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer"
}

for file_path in files:
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()
    
    for old_color, new_color in replacements.items():
        content = content.replace(old_color, new_color)
        
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(content)

print("Colors refactored.")
