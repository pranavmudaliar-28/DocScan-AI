'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { useRouter } from 'next/navigation';
import { api } from '@/lib/axios';
import { useAuthStore } from '@/store/authStore';
import { toast } from 'sonner';
import Link from 'next/link';
import {
  Eye, EyeOff, Sparkles, FileText, Brain, Lock,
  ChevronRight, Loader2, AlertCircle,
} from 'lucide-react';

import { Button } from '@/components/ui/button';
import {
  Form, FormControl, FormField, FormItem, FormLabel, FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';

const formSchema = z.object({
  email: z.string().email('Please enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
});

const features = [
  {
    icon: FileText,
    title: 'Instant OCR Extraction',
    desc: 'Pull text from any PDF or image in seconds',
  },
  {
    icon: Brain,
    title: 'AI-Powered Analysis',
    desc: 'Auto-identify vendors, totals, and key data fields',
  },
  {
    icon: Lock,
    title: 'Enterprise Security',
    desc: 'Bank-grade encryption keeps your documents safe',
  },
];

export default function LoginPage() {
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');
  const router = useRouter();
  const setAuth = useAuthStore((state) => state.setAuth);

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: { email: '', password: '' },
  });

  async function onSubmit(values: z.infer<typeof formSchema>) {
    try {
      setIsSubmitting(true);
      setError('');
      const response = await api.post('/auth/login', values);
      setAuth(response.data.user, response.data.access_token);
      router.push('/');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Incorrect email or password. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  }

  const handleGuestLogin = async () => {
    try {
      setIsSubmitting(true);
      const toastId = toast.loading('Creating guest session...');
      const response = await api.post('/auth/guest');
      setAuth(response.data.user, response.data.access_token);
      toast.success('Logged in as Guest!', { id: toastId });
      router.push('/');
    } catch {
      toast.error('Failed to create guest session');
      setIsSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen">
      {/* ── Left panel: brand ── */}
      <div className="hidden lg:flex lg:w-[44%] flex-col bg-zinc-950 relative overflow-hidden p-12">
        {/* Subtle grid */}
        <div className="absolute inset-0 bg-[linear-gradient(to_right,#ffffff07_1px,transparent_1px),linear-gradient(to_bottom,#ffffff07_1px,transparent_1px)] bg-[size:32px_32px] pointer-events-none" />
        {/* Ambient orbs */}
        <div className="absolute top-[-20%] left-[-15%] w-[70%] h-[70%] rounded-full bg-indigo-600/15 blur-[140px] pointer-events-none" />
        <div className="absolute bottom-[-10%] right-[-10%] w-[50%] h-[50%] rounded-full bg-violet-600/10 blur-[120px] pointer-events-none" />

        {/* Logo */}
        <div className="relative z-10 flex items-center gap-2.5">
          <div className="w-9 h-9 bg-white rounded-xl flex items-center justify-center shadow-sm">
            <Sparkles className="w-4 h-4 text-zinc-900" />
          </div>
          <span className="font-semibold text-lg text-white tracking-tight">DocScan AI</span>
        </div>

        {/* Headline + feature list */}
        <div className="relative z-10 flex-1 flex flex-col justify-center">
          <div className="mb-10">
            <h2 className="text-3xl font-bold text-white leading-tight tracking-tight mb-3">
              Transform documents<br />into structured data.
            </h2>
            <p className="text-zinc-400 text-sm leading-relaxed max-w-xs">
              Join thousands of teams using DocScan AI to automate their document processing workflows.
            </p>
          </div>

          <div className="space-y-6">
            {features.map(({ icon: Icon, title, desc }) => (
              <div key={title} className="flex items-start gap-4">
                <div className="w-10 h-10 rounded-xl bg-white/5 border border-white/10 flex items-center justify-center shrink-0">
                  <Icon className="w-4 h-4 text-indigo-400" />
                </div>
                <div>
                  <p className="text-white font-medium text-sm mb-0.5">{title}</p>
                  <p className="text-zinc-500 text-sm leading-relaxed">{desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>

        <p className="relative z-10 text-zinc-700 text-xs">
          © 2025 DocScan AI. All rights reserved.
        </p>
      </div>

      {/* ── Right panel: form ── */}
      <div className="flex-1 flex flex-col bg-white dark:bg-zinc-950 relative">
        {/* Mobile header */}
        <div className="lg:hidden flex items-center gap-2 px-6 py-5 border-b border-zinc-100 dark:border-zinc-900">
          <div className="w-8 h-8 bg-zinc-900 dark:bg-white rounded-lg flex items-center justify-center">
            <Sparkles className="w-4 h-4 text-white dark:text-zinc-900" />
          </div>
          <span className="font-semibold text-base text-zinc-900 dark:text-white">DocScan AI</span>
        </div>

        {/* Desktop: sign-up shortcut in top-right */}
        <div className="hidden lg:flex absolute top-8 right-8 items-center gap-1.5 text-sm">
          <span className="text-zinc-500">New to DocScan?</span>
          <Link
            href="/signup"
            className="font-medium text-zinc-900 dark:text-white hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors flex items-center gap-0.5"
          >
            Create account <ChevronRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        {/* Centered form */}
        <div className="flex-1 flex items-center justify-center px-6 sm:px-12 py-12">
          <div className="w-full max-w-[380px]">

            <div className="mb-8">
              <h1 className="text-2xl font-bold tracking-tight text-zinc-900 dark:text-white mb-1.5">
                Welcome back
              </h1>
              <p className="text-zinc-500 text-sm">
                Sign in to access your documents
              </p>
            </div>

            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} noValidate className="space-y-5">
                {/* Email */}
                <FormField
                  control={form.control}
                  name="email"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="text-sm font-medium text-zinc-700 dark:text-zinc-300">
                        Email
                      </FormLabel>
                      <FormControl>
                        <Input
                          placeholder="you@company.com"
                          type="email"
                          autoComplete="email"
                          className="h-11 bg-zinc-50 dark:bg-zinc-900 border-zinc-200 dark:border-zinc-800 placeholder:text-zinc-400 transition-colors"
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                {/* Password */}
                <FormField
                  control={form.control}
                  name="password"
                  render={({ field }) => (
                    <FormItem>
                      <div className="flex items-center justify-between mb-1.5">
                        <FormLabel className="text-sm font-medium text-zinc-700 dark:text-zinc-300">
                          Password
                        </FormLabel>
                        <Link
                          href="/forgot-password"
                          className="text-xs text-zinc-500 hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors"
                        >
                          Forgot password?
                        </Link>
                      </div>
                      <div className="relative">
                        <FormControl>
                          <Input
                            type={showPassword ? 'text' : 'password'}
                            autoComplete="current-password"
                            className="h-11 pr-10 bg-zinc-50 dark:bg-zinc-900 border-zinc-200 dark:border-zinc-800 transition-colors"
                            {...field}
                          />
                        </FormControl>
                        <button
                          type="button"
                          onClick={() => setShowPassword((v) => !v)}
                          className="absolute right-3 top-1/2 -translate-y-1/2 text-zinc-400 hover:text-zinc-600 dark:hover:text-zinc-300 transition-colors"
                          aria-label={showPassword ? 'Hide password' : 'Show password'}
                        >
                          {showPassword
                            ? <EyeOff className="w-4 h-4" />
                            : <Eye className="w-4 h-4" />
                          }
                        </button>
                      </div>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                {/* Error */}
                {error && (
                  <div className="flex items-start gap-2.5 text-sm text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-950/30 border border-red-200 dark:border-red-900 rounded-lg px-3.5 py-3">
                    <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" />
                    <span>{error}</span>
                  </div>
                )}

                {/* Submit */}
                <Button
                  type="submit"
                  disabled={isSubmitting}
                  className="w-full h-11 bg-zinc-900 hover:bg-zinc-800 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-100 font-medium shadow-sm transition-all"
                >
                  {isSubmitting ? (
                    <>
                      <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                      Signing in…
                    </>
                  ) : (
                    'Sign in'
                  )}
                </Button>
              </form>
            </Form>

            {/* Divider */}
            <div className="relative my-6">
              <div className="absolute inset-0 flex items-center">
                <span className="w-full border-t border-zinc-200 dark:border-zinc-800" />
              </div>
              <div className="relative flex justify-center">
                <span className="bg-white dark:bg-zinc-950 px-3 text-xs text-zinc-500 uppercase tracking-wide">
                  or
                </span>
              </div>
            </div>

            {/* Guest */}
            <Button
              type="button"
              variant="outline"
              onClick={handleGuestLogin}
              disabled={isSubmitting}
              className="w-full h-11 border-zinc-200 dark:border-zinc-800 text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50 dark:hover:bg-zinc-900 font-medium transition-colors"
            >
              Continue as Guest
            </Button>

            {/* Mobile: sign-up link */}
            <p className="mt-6 text-center text-sm text-zinc-500 lg:hidden">
              Don&apos;t have an account?{' '}
              <Link
                href="/signup"
                className="font-medium text-zinc-900 dark:text-white hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors"
              >
                Sign up
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
