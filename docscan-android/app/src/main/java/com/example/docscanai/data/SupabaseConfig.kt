package com.example.docscanai.data

/**
 * Fill these in from: Supabase Dashboard → Project Settings → API
 * URL      = "Project URL"
 * ANON_KEY = "anon public" key (safe to include in client apps)
 */
object SupabaseConfig {
    const val URL      = "https://vycrfsyqumdblpncsgpp.supabase.co"
    const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ5Y3Jmc3lxdW1kYmxwbmNzZ3BwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzk4MzEzNDAsImV4cCI6MjA5NTQwNzM0MH0.IYvKaS2L8We41CVXCiW7bRSl92t_BAoynIYPUP_WfLY"

    // Supabase Storage bucket name — create this bucket in your Supabase dashboard
    const val DOCUMENTS_BUCKET = "documents"

    // OAuth 2.0 Web Client ID from Google Cloud Console (not the Android client ID).
    // Required steps:
    //   1. Google Cloud Console → APIs & Services → Credentials → Web Client
    //   2. Supabase Dashboard → Auth → Providers → Google → enable + paste Client ID & Secret
    const val GOOGLE_WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
}
