package com.example.docscanai.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            item {
                Text(
                    text = "Effective Date: [Insert Date]\n\nAt DocScan AI, your privacy is our priority. This Privacy Policy outlines how we collect, use, and protect your information when you use our document scanning and OCR services.\n\n1. Information We Collect\nWe may collect information you provide directly to us (e.g., account details) and data automatically collected when you use our app (e.g., usage statistics and device information).\n\n2. Use of Information\nWe use the collected information to operate, maintain, and improve our services, communicate with you, and personalize your experience.\n\n3. Data Security\nWe implement reasonable security measures to protect your documents and personal information. However, no method of transmission over the internet or electronic storage is 100% secure.\n\n4. Cloud Sync and Storage\nIf you choose to sync your documents, they are securely stored using our cloud provider (Supabase). You retain full ownership of your documents.\n\n5. Changes to This Policy\nWe may update this Privacy Policy from time to time. We will notify you of any changes by posting the new policy in the app.\n\nIf you have any questions about this Privacy Policy, please contact us at support@docscanai.com.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
