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
fun TermsAndConditionsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms & Conditions", style = MaterialTheme.typography.titleMedium) },
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
                    text = "Terms & Conditions",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            item {
                Text(
                    text = "Effective Date: [Insert Date]\n\nPlease read these terms and conditions carefully before using DocScan AI.\n\n1. Acceptance of Terms\nBy accessing and using our application, you accept and agree to be bound by the terms and provisions of this agreement.\n\n2. User Responsibilities\nYou are responsible for the documents you scan and store using our service. You agree not to use the app for any illegal or unauthorized purpose.\n\n3. Subscription and Billing\nCertain features may be subject to a subscription fee. You will be billed in advance on a recurring and periodic basis depending on your subscription plan.\n\n4. Intellectual Property\nThe app and its original content, features, and functionality are and will remain the exclusive property of DocScan AI and its licensors.\n\n5. Termination\nWe may terminate or suspend access to our service immediately, without prior notice or liability, for any reason whatsoever, including without limitation if you breach the Terms.\n\n6. Changes to Terms\nWe reserve the right, at our sole discretion, to modify or replace these Terms at any time.\n\nFor more information, please contact support@docscanai.com.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
