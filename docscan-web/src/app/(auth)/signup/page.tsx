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
  Eye, EyeOff, Sparkles, ChevronRight,
  Loader2, AlertCircle, CheckCircle2,
} from 'lucide-react';

import { Button } from '@/components/ui/button';
import {
  Form, FormControl, FormField, FormItem, FormLabel, FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';

const formSchema = z.object({
  email: z.string().email('Please enter a valid email address'),
  password: z
    .string()
    .min(8, 'Password must be at least 8 characters'),
  confirmPassword: z.string(),
}).refine((d) => d.password === d.confirmPassword, {
  message: "Passwords don't match",
  path: ['confirmPassword'],
});

function getPasswordStrength(password: string): { level: 0 | 1 | 2 | 3; label: string } {
  if (!password) return { level: 0, label: '' };
  const hasNumber = /\d/.test(password);
  const hasSpecial = /[^a-zA-Z0-9]/.test(password);
  if (password.length >= 12 && hasNumber && hasSpecial) return { level: 3, label: 'Strong' };
  if (password.length >= 8 && (hasNumber || hasSpecial)) return { level: 2, label: 'Good' };
  return { level: 1, label: 'Weak' };
}

const strengthColor = ['', 'bg-red-500', 'bg-amber-500', 'bg-emerald-500'];
const strengthText  = ['', 'text-red-500', 'text-amber-500', 'text-emerald-600'];

const benefits = [
  'Extract text from PDFs, images & scans',
  'AI identifies vendors, dates, and totals',
  'Unlimited document storage on Pro plan',
  'Real-time processing status updates',
];

export default function SignupPage() {
  const [showPassword, setShowPassword]        = useState(false);
  const [showConfirm, setShowConfirm]          = useState(false);
  const [isSubmitting, setIsSubmitting]        = useState(false);
  const [error, setError]                      = useState('');
  const router = useRouter();
  const setAuth = useAuthStore((state) => state.setAuth);

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: { email: '', password: '', confirmPassword: '' },
  });

  const passwordValue = form.watch('password');
  const strength = getPasswordStrength(passwordValue);

  async function onSubmit(values: z.infer<typeof formSchema>) {
    try {
      setIsSubmitting(true);
      setError('');
      const response = await api.post('/auth/register', {
        email: values.email,
        password: values.password,
      });
      setAuth(response.data.user, response.data.access_token);
      router.push('/');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create account. Please try again.');
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
        <div className="absolute top-[-10%] right-[-15%] w-[65%] h-[65%] rounded-full bg-indigo-600/12 blur-[140px] pointer-events-none" />
        <div className="absolute bottom-[-15%] left-[-10%] w-[55%] h-[55%] rounded-full bg-violet-600/10 blur-[120px] pointer-events-none" />

        {/* Logo */}
        <div className="relative z-10 flex items-center gap-2.5">
          <div className="w-9 h-9 bg-white rounded-xl flex items-center justify-center shadow-sm">
            <Sparkles className="w-4 h-4 text-zinc-900" />
          </div>
          <span className="font-semibold text-lg text-white tracking-tight">DocScan AI</span>
        </div>

        {/* Content */}
        <div className="relative z-10 flex-1 flex flex-col justify-center">
          <div className="mb-10">
            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-white/5 border border-white/10 mb-6">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
              <span className="text-xs text-zinc-400 font-medium">Free to get started</span>
            </div>
            <h2 className="text-3xl font-bold text-white leading-tight tracking-tight mb-3">
              Your documents,<br />intelligently organized.
            </h2>
            <p className="text-zinc-400 text-sm leading-relaxed max-w-xs">
              Create a free account and start extracting structured data from your documents in minutes.
            </p>
          </div>

          <div className="space-y-4">
            {benefits.map((b) => (
              <div key={b} className="flex items-center gap-3">
                <CheckCircle2 className="w-4 h-4 text-indigo-400 shrink-0" />
                <span className="text-zinc-300 text-sm">{b}</span>
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

        {/* Desktop: sign-in shortcut in top-right */}
        <div className="hidden lg:flex absolute top-8 right-8 items-center gap-1.5 text-sm">
          <span className="text-zinc-500">Already have an account?</span>
          <Link
            href="/login"
            className="font-medium text-zinc-900 dark:text-white hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors flex items-center gap-0.5"
          >
            Sign in <ChevronRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        {/* Centered form */}
        <div className="flex-1 flex items-center justify-center px-6 sm:px-12 py-12">
          <div className="w-full max-w-[380px]">

            <div className="mb-8">
              <h1 className="text-2xl font-bold tracking-tight text-zinc-900 dark:text-white mb-1.5">
                Create your account
              </h1>
              <p className="text-zinc-500 text-sm">
                Start for free — no credit card required
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
                      <FormLabel className="text-sm font-medium text-zinc-700 dark:text-zinc-300">
                        Password
                      </FormLabel>
                      <div className="relative">
                        <FormControl>
                          <Input
                            type={showPassword ? 'text' : 'password'}
                            autoComplete="new-password"
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
                      {/* Password strength indicator */}
                      {passwordValue && (
                        <div className="mt-2 space-y-1.5">
                          <div className="flex gap-1">
                            {[1, 2, 3].map((i) => (
                              <div
                                key={i}
                                className={`h-1 flex-1 rounded-full transition-all duration-300 ${
                                  i <= strength.level
                                    ? strengthColor[strength.level]
                                    : 'bg-zinc-200 dark:bg-zinc-800'
                                }`}
                              />
                            ))}
                          </div>
                          <p className={`text-xs font-medium ${strengthText[strength.level]}`}>
                            {strength.label}
                            {strength.level < 3 && (
                              <span className="text-zinc-400 font-normal ml-1">
                                — add {strength.level < 2 ? 'numbers or symbols' : 'more characters'}
                              </span>
                            )}
                          </p>
                        </div>
                      )}
                      <FormMessage />
                    </FormItem>
                  )}
                />

                {/* Confirm password */}
                <FormField
                  control={form.control}
                  name="confirmPassword"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="text-sm font-medium text-zinc-700 dark:text-zinc-300">
                        Confirm Password
                      </FormLabel>
                      <div className="relative">
                        <FormControl>
                          <Input
                            type={showConfirm ? 'text' : 'password'}
                            autoComplete="new-password"
                            className="h-11 pr-10 bg-zinc-50 dark:bg-zinc-900 border-zinc-200 dark:border-zinc-800 transition-colors"
                            {...field}
                          />
                        </FormControl>
                        <button
                          type="button"
                          onClick={() => setShowConfirm((v) => !v)}
                          className="absolute right-3 top-1/2 -translate-y-1/2 text-zinc-400 hover:text-zinc-600 dark:hover:text-zinc-300 transition-colors"
                          aria-label={showConfirm ? 'Hide password' : 'Show password'}
                        >
                          {showConfirm
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
                      Creating account…
                    </>
                  ) : (
                    'Create account'
                  )}
                </Button>

                <p className="text-center text-xs text-zinc-400 leading-relaxed">
                  By creating an account you agree to our{' '}
                  <Link href="/terms" className="underline underline-offset-2 hover:text-zinc-600 dark:hover:text-zinc-300 transition-colors">
                    Terms of Service
                  </Link>{' '}
                  and{' '}
                  <Link href="/privacy" className="underline underline-offset-2 hover:text-zinc-600 dark:hover:text-zinc-300 transition-colors">
                    Privacy Policy
                  </Link>.
                </p>
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

            {/* Mobile: sign-in link */}
            <p className="mt-6 text-center text-sm text-zinc-500 lg:hidden">
              Already have an account?{' '}
              <Link
                href="/login"
                className="font-medium text-zinc-900 dark:text-white hover:text-indigo-600 dark:hover:text-indigo-400 transition-colors"
              >
                Sign in
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
