'use client';

import { useState, useEffect } from 'react';
import { useAuthStore } from '@/store/authStore';
import { useSocket } from '@/components/providers/SocketProvider';
import { UploadModal } from '@/components/UploadModal';
import { Button } from '@/components/ui/button';
import { FileText, LogOut, Sparkles, UploadCloud, FileCheck, FileMinus, FileSearch } from 'lucide-react';
import { toast } from 'sonner';
import { api } from '@/lib/axios';

export default function Home() {
  const { token, logout, setAuth } = useAuthStore();
  const { socket } = useSocket();
  const isAuthenticated = !!token;
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [documents, setDocuments] = useState<any[]>([]);
  const [loadingDocs, setLoadingDocs] = useState(true);

  // Fetch documents on mount
  useEffect(() => {
    if (isAuthenticated) {
      fetchDocuments();
    }
  }, [isAuthenticated]);

  const fetchDocuments = async () => {
    try {
      setLoadingDocs(true);
      const res = await api.get('/documents');
      setDocuments(res.data);
    } catch (error) {
      console.error('Failed to fetch documents', error);
      toast.error('Failed to load documents');
    } finally {
      setLoadingDocs(false);
    }
  };

  // Listen to Real-Time Updates from Socket.io
  useEffect(() => {
    if (!socket) return;

    const handleDocumentProcessed = (data: any) => {
      // Update the document in the list, or fetch all if it's easier
      setDocuments((prevDocs) => {
        const existingDocIndex = prevDocs.findIndex(d => d._id === data.documentId);
        if (existingDocIndex >= 0) {
          // Update existing
          const newDocs = [...prevDocs];
          newDocs[existingDocIndex] = {
            ...newDocs[existingDocIndex],
            status: data.status,
            aiMetadata: data.aiMetadata
          };
          return newDocs;
        } else {
          // If it's totally new and not in the list (fallback)
          fetchDocuments();
          return prevDocs;
        }
      });
    };

    socket.on('document.processed', handleDocumentProcessed);
    
    return () => {
      socket.off('document.processed', handleDocumentProcessed);
    };
  }, [socket]);

  // Mock login for testing the UI flow without the full backend auth endpoints hooked up yet
  const handleMockLogin = () => {
    setAuth(
      { id: '123', email: 'demo@docscan.ai', profile: {}, tier: 'pro' },
      'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjMiLCJlbWFpbCI6ImRlbW9AZG9jc2Nhbi5haSIsImlhdCI6MTc3OTYwNTc3M30.XiXFJL0lAlkZTdvt5DIped8ESZhRhs4wfjDCzn7zT2w'
    );
    toast.success('Logged in as demo user!');
  };

  if (!isAuthenticated) {
    return (
      <main className="flex min-h-screen flex-col items-center justify-center bg-gradient-to-b from-slate-50 to-slate-100 p-6 dark:from-slate-950 dark:to-slate-900">
        <div className="absolute top-0 w-full h-96 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] opacity-20 pointer-events-none mix-blend-overlay"></div>
        
        <div className="z-10 flex flex-col items-center text-center space-y-8 max-w-3xl">
          <div className="p-4 bg-white/50 dark:bg-slate-800/50 rounded-2xl shadow-sm backdrop-blur-xl border border-slate-200/50 dark:border-slate-700/50 mb-4">
            <Sparkles className="w-12 h-12 text-indigo-500" />
          </div>
          
          <h1 className="text-5xl md:text-7xl font-bold tracking-tight text-slate-900 dark:text-white">
            Enterprise Document <br />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-indigo-500 to-cyan-400">
              Intelligence
            </span>
          </h1>
          
          <p className="text-xl text-slate-600 dark:text-slate-400 max-w-2xl leading-relaxed">
            Upload, scan, and extract structured data instantly using our state-of-the-art OCR and LLM pipeline. Built for scale.
          </p>

          <div className="flex gap-4 pt-8">
            <Button size="lg" onClick={handleMockLogin} className="rounded-full px-8 text-md h-14 bg-indigo-600 hover:bg-indigo-700 text-white shadow-lg shadow-indigo-200 dark:shadow-none transition-all hover:scale-105">
              Start Free Trial
            </Button>
            <Button size="lg" variant="outline" className="rounded-full px-8 text-md h-14 bg-white/50 backdrop-blur-sm border-slate-200 hover:bg-slate-50 transition-all">
              Documentation
            </Button>
          </div>
        </div>
      </main>
    );
  }

  // Dashboard View
  return (
    <main className="min-h-screen bg-slate-50 dark:bg-slate-950">
      {/* Header */}
      <header className="sticky top-0 z-40 w-full backdrop-blur-xl bg-white/70 dark:bg-slate-900/70 border-b border-slate-200 dark:border-slate-800">
        <div className="container flex h-16 items-center justify-between px-4 md:px-8 max-w-7xl mx-auto">
          <div className="flex items-center gap-2">
            <div className="p-2 bg-indigo-600 rounded-lg">
              <Sparkles className="w-5 h-5 text-white" />
            </div>
            <span className="text-lg font-semibold tracking-tight">DocScan AI</span>
          </div>
          <Button variant="ghost" size="sm" onClick={logout} className="text-slate-500 hover:text-slate-900 dark:hover:text-white">
            <LogOut className="w-4 h-4 mr-2" />
            Logout
          </Button>
        </div>
      </header>

      {/* Main Content */}
      <div className="container px-4 md:px-8 max-w-7xl mx-auto py-12">
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-12 gap-4">
          <div>
            <h2 className="text-3xl font-bold tracking-tight text-slate-900 dark:text-white">Documents</h2>
            <p className="text-slate-500 mt-1">Manage and extract data from your scanned files.</p>
          </div>
          <Button onClick={() => setIsUploadOpen(true)} className="bg-indigo-600 hover:bg-indigo-700 text-white rounded-full px-6 shadow-sm">
            <UploadCloud className="w-4 h-4 mr-2" />
            Upload Document
          </Button>
        </div>

        {/* Loading State */}
        {loadingDocs ? (
          <div className="flex justify-center items-center py-24">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
          </div>
        ) : documents.length === 0 ? (
          /* Empty State */
          <div className="flex flex-col items-center justify-center py-24 px-4 border-2 border-dashed border-slate-200 dark:border-slate-800 rounded-3xl bg-white/30 dark:bg-slate-900/30">
            <div className="p-4 bg-indigo-50 dark:bg-indigo-900/20 rounded-full mb-4">
              <FileText className="w-8 h-8 text-indigo-500" />
            </div>
            <h3 className="text-xl font-medium text-slate-900 dark:text-white">No documents yet</h3>
            <p className="text-slate-500 mt-2 text-center max-w-sm">
              Upload your first invoice, receipt, or document to see the AI extraction pipeline in action.
            </p>
            <Button onClick={() => setIsUploadOpen(true)} variant="outline" className="mt-6 rounded-full">
              Upload your first file
            </Button>
          </div>
        ) : (
          /* Document Grid */
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {documents.map((doc) => (
              <div key={doc._id} className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-6 shadow-sm hover:shadow-md transition-shadow">
                <div className="flex justify-between items-start mb-4">
                  <div className="p-3 bg-indigo-50 dark:bg-indigo-900/20 rounded-xl">
                    <FileText className="w-6 h-6 text-indigo-500" />
                  </div>
                  {doc.status === 'COMPLETED' ? (
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400">
                      <FileCheck className="w-3 h-3 mr-1" />
                      Extracted
                    </span>
                  ) : doc.status === 'FAILED' ? (
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400">
                      <FileMinus className="w-3 h-3 mr-1" />
                      Failed
                    </span>
                  ) : (
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400">
                      <FileSearch className="w-3 h-3 mr-1 animate-pulse" />
                      Processing AI...
                    </span>
                  )}
                </div>
                
                <h3 className="font-semibold text-slate-900 dark:text-white truncate mb-1" title={doc.filename}>
                  {doc.filename}
                </h3>
                <p className="text-xs text-slate-500 mb-4">
                  {(doc.sizeBytes / 1024 / 1024).toFixed(2)} MB • {new Date(doc.createdAt).toLocaleDateString()}
                </p>

                {doc.aiMetadata && doc.aiMetadata.extractedData && (
                  <div className="mt-4 pt-4 border-t border-slate-100 dark:border-slate-800 space-y-2">
                    <div className="flex justify-between text-sm">
                      <span className="text-slate-500">Vendor</span>
                      <span className="font-medium text-slate-900 dark:text-white truncate max-w-[120px]">
                        {doc.aiMetadata.extractedData.vendorName || '-'}
                      </span>
                    </div>
                    <div className="flex justify-between text-sm">
                      <span className="text-slate-500">Total</span>
                      <span className="font-semibold text-emerald-600 dark:text-emerald-400">
                        {doc.aiMetadata.extractedData.totalAmount ? `$${doc.aiMetadata.extractedData.totalAmount}` : '-'}
                      </span>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      <UploadModal 
        isOpen={isUploadOpen} 
        onClose={() => setIsUploadOpen(false)} 
        onUploadComplete={() => {
          setIsUploadOpen(false);
          fetchDocuments();
          toast.info('Upload finished. The AI is now processing your document in the background!');
        }}
      />
    </main>
  );
}
