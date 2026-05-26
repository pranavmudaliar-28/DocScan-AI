'use client';

import { useState, useEffect, useRef } from 'react';
import { useAuthStore } from '@/store/authStore';
import { useSocket } from '@/components/providers/SocketProvider';
import { UploadModal } from '@/components/UploadModal';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { 
  FileText, LogOut, Sparkles, UploadCloud, FileCheck, FileMinus, FileSearch,
  MoreVertical, Download, Edit2, Trash2, ArrowUpDown
} from 'lucide-react';
import { toast } from 'sonner';
import { api } from '@/lib/axios';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter
} from '@/components/ui/dialog';

export default function Home() {
  const { token, logout, setAuth } = useAuthStore();
  const { socket } = useSocket();
  const isAuthenticated = !!token;
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [documents, setDocuments] = useState<any[]>([]);
  const [loadingDocs, setLoadingDocs] = useState(true);

  // Sorting
  const [sortField, setSortField] = useState<'createdAt' | 'filename' | 'sizeBytes' | 'status'>('createdAt');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');

  // Actions state
  const [activeDropdown, setActiveDropdown] = useState<string | null>(null);
  const [editingDoc, setEditingDoc] = useState<any>(null);
  const [newFilename, setNewFilename] = useState('');

  // Fetch documents on mount
  useEffect(() => {
    if (isAuthenticated) {
      fetchDocuments();
    }
  }, [isAuthenticated]);

  // Click outside to close dropdown
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (!(e.target as Element).closest('.dropdown-container')) {
        setActiveDropdown(null);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

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
      setDocuments((prevDocs) => {
        const existingDocIndex = prevDocs.findIndex(d => d._id === data.documentId);
        if (existingDocIndex >= 0) {
          const newDocs = [...prevDocs];
          newDocs[existingDocIndex] = {
            ...newDocs[existingDocIndex],
            status: data.status,
            aiMetadata: data.aiMetadata
          };
          return newDocs;
        } else {
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

  const handleGuestLogin = async () => {
    try {
      const toastId = toast.loading('Creating guest session...');
      const response = await api.post('/auth/guest');
      setAuth(response.data.user, response.data.access_token);
      toast.success('Logged in as Guest!', { id: toastId });
    } catch (err) {
      toast.error('Failed to create guest session');
    }
  };

  const handleDownload = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setActiveDropdown(null);
    try {
      const toastId = toast.loading('Generating download link...');
      const res = await api.get(`/documents/${id}/download`);
      window.open(res.data.downloadUrl, '_blank');
      toast.success('Download started', { id: toastId });
    } catch (err) {
      toast.error('Failed to download document');
    }
  };

  const handleDelete = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setActiveDropdown(null);
    if (!confirm('Are you sure you want to delete this document?')) return;
    
    try {
      const toastId = toast.loading('Deleting document...');
      await api.delete(`/documents/${id}`);
      setDocuments(docs => docs.filter(d => d._id !== id));
      toast.success('Document deleted', { id: toastId });
    } catch (err) {
      toast.error('Failed to delete document');
    }
  };

  const openEditModal = (doc: any, e: React.MouseEvent) => {
    e.stopPropagation();
    setActiveDropdown(null);
    setEditingDoc(doc);
    setNewFilename(doc.filename);
  };

  const handleRename = async () => {
    if (!editingDoc || !newFilename.trim()) return;
    try {
      const toastId = toast.loading('Renaming document...');
      await api.patch(`/documents/${editingDoc._id}`, { filename: newFilename });
      setDocuments(docs => docs.map(d => d._id === editingDoc._id ? { ...d, filename: newFilename } : d));
      setEditingDoc(null);
      toast.success('Document renamed', { id: toastId });
    } catch (err) {
      toast.error('Failed to rename document');
    }
  };

  const sortedDocuments = [...documents].sort((a, b) => {
    let comparison = 0;
    if (sortField === 'createdAt') comparison = new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
    else if (sortField === 'sizeBytes') comparison = a.sizeBytes - b.sizeBytes;
    else if (sortField === 'filename') comparison = a.filename.localeCompare(b.filename);
    else if (sortField === 'status') comparison = a.status.localeCompare(b.status);
    
    return sortOrder === 'asc' ? comparison : -comparison;
  });

  const toggleSort = (field: typeof sortField) => {
    if (sortField === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortOrder('desc'); // Default new sort to descending
    }
  };

  if (!isAuthenticated) {
    return (
      <main className="flex min-h-screen flex-col bg-white dark:bg-[#09090b] relative selection:bg-zinc-200 dark:selection:bg-zinc-800">
        {/* Navigation Bar */}
        <nav className="flex items-center justify-between px-6 py-4 border-b border-zinc-100 dark:border-zinc-900 absolute top-0 w-full z-50 bg-white/80 dark:bg-[#09090b]/80 backdrop-blur-md">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 bg-zinc-900 dark:bg-white rounded-lg flex items-center justify-center shadow-sm">
              <Sparkles className="w-4 h-4 text-white dark:text-zinc-900" />
            </div>
            <span className="font-semibold text-lg tracking-tight text-zinc-900 dark:text-white">DocScan</span>
          </div>
          <div className="flex items-center gap-4">
            <Button variant="ghost" className="hidden md:flex text-sm font-medium text-zinc-600 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-white">Features</Button>
            <Button variant="ghost" className="hidden md:flex text-sm font-medium text-zinc-600 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-white">Pricing</Button>
            <div className="w-px h-4 bg-zinc-200 dark:bg-zinc-800 hidden md:block"></div>
            <Button variant="ghost" onClick={() => window.location.href='/login'} className="text-sm font-medium text-zinc-900 dark:text-white">Log in</Button>
            <Button onClick={handleGuestLogin} className="text-sm font-medium bg-zinc-900 text-white hover:bg-zinc-800 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200 rounded-full px-5 shadow-sm">
              Try for free
            </Button>
          </div>
        </nav>

        {/* Subtle Grid Background */}
        <div className="absolute inset-0 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] opacity-[0.02] mix-blend-multiply dark:mix-blend-overlay pointer-events-none"></div>
        <div className="absolute inset-0 bg-[linear-gradient(to_right,#8080800a_1px,transparent_1px),linear-gradient(to_bottom,#8080800a_1px,transparent_1px)] bg-[size:24px_24px] pointer-events-none"></div>

        {/* Hero Section */}
        <div className="flex flex-col items-center justify-center flex-1 w-full pt-32 pb-16 px-6 sm:pt-40 z-10 text-center max-w-5xl mx-auto">
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-zinc-100/80 dark:bg-zinc-900/80 border border-zinc-200 dark:border-zinc-800 mb-8 animate-in fade-in zoom-in-95 duration-700 backdrop-blur-sm">
            <span className="flex h-2 w-2 rounded-full bg-indigo-500 animate-pulse"></span>
            <span className="text-sm font-medium text-zinc-600 dark:text-zinc-300">DocScan AI Platform is now live</span>
          </div>
          
          <h1 className="text-5xl sm:text-7xl font-bold tracking-tighter text-zinc-900 dark:text-white leading-[1.1] mb-6 animate-in fade-in slide-in-from-bottom-4 duration-700 delay-100">
            Turn unstructured documents into <span className="text-zinc-400 dark:text-zinc-500">structured data.</span>
          </h1>
          
          <p className="text-lg sm:text-xl text-zinc-500 dark:text-zinc-400 max-w-2xl mb-10 leading-relaxed animate-in fade-in slide-in-from-bottom-5 duration-700 delay-200">
            Automate your document processing workflow. Instantly extract text, tables, and key-value pairs from PDFs and images with human-level accuracy.
          </p>

          <div className="flex flex-col sm:flex-row items-center gap-4 w-full sm:w-auto animate-in fade-in slide-in-from-bottom-6 duration-700 delay-300">
            <Button size="lg" onClick={handleGuestLogin} className="w-full sm:w-auto rounded-full px-8 h-12 bg-zinc-900 hover:bg-zinc-800 text-white dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-100 font-medium transition-all shadow-md hover:shadow-xl hover:-translate-y-0.5">
              Start Building for Free
            </Button>
            <Button size="lg" variant="outline" className="w-full sm:w-auto rounded-full px-8 h-12 border-zinc-200 dark:border-zinc-800 text-zinc-900 dark:text-white hover:bg-zinc-50 dark:hover:bg-zinc-900 font-medium transition-colors">
              View Documentation
            </Button>
          </div>
        </div>

        {/* Minimal Dashboard Mockup Preview */}
        <div className="w-full max-w-5xl mx-auto px-6 pb-24 z-10 animate-in fade-in slide-in-from-bottom-8 duration-1000 delay-500">
          <div className="w-full h-auto rounded-2xl sm:rounded-[2rem] border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-[#09090b] shadow-[0_20px_50px_-12px_rgba(0,0,0,0.1)] dark:shadow-[0_20px_50px_-12px_rgba(0,0,0,0.5)] overflow-hidden flex flex-col ring-1 ring-zinc-950/5 dark:ring-white/10">
            {/* Mockup Header */}
            <div className="h-12 border-b border-zinc-100 dark:border-zinc-800/50 flex items-center px-4 gap-3 bg-zinc-50/50 dark:bg-zinc-900/20 backdrop-blur-md">
              <div className="flex gap-1.5">
                <div className="w-3 h-3 rounded-full bg-zinc-300 dark:bg-zinc-700"></div>
                <div className="w-3 h-3 rounded-full bg-zinc-300 dark:bg-zinc-700"></div>
                <div className="w-3 h-3 rounded-full bg-zinc-300 dark:bg-zinc-700"></div>
              </div>
              <div className="flex-1 flex justify-center">
                <div className="h-6 w-48 bg-zinc-200/50 dark:bg-zinc-800/50 rounded-md"></div>
              </div>
            </div>
            {/* Mockup Content */}
            <div className="p-6 sm:p-10 flex gap-8 flex-col sm:flex-row bg-white dark:bg-[#09090b]">
              <div className="w-full sm:w-1/3 flex flex-col gap-6">
                <div className="h-8 w-2/3 bg-zinc-100 dark:bg-zinc-900 rounded-lg"></div>
                <div className="h-32 w-full bg-zinc-50 dark:bg-zinc-900/50 border border-dashed border-zinc-200 dark:border-zinc-800 rounded-xl flex flex-col items-center justify-center gap-2">
                   <UploadCloud className="w-6 h-6 text-zinc-400 dark:text-zinc-600" />
                   <div className="h-2 w-20 bg-zinc-200 dark:bg-zinc-800 rounded"></div>
                </div>
                <div className="h-24 w-full bg-zinc-50 dark:bg-zinc-900/30 border border-zinc-100 dark:border-zinc-800/50 rounded-xl p-4 flex flex-col gap-3">
                   <div className="flex items-center gap-3">
                     <div className="w-8 h-8 rounded bg-zinc-200 dark:bg-zinc-800"></div>
                     <div className="flex-1 flex flex-col gap-2">
                       <div className="h-2 w-full bg-zinc-200 dark:bg-zinc-800 rounded"></div>
                       <div className="h-2 w-2/3 bg-zinc-200 dark:bg-zinc-800 rounded"></div>
                     </div>
                   </div>
                   <div className="h-2 w-1/3 bg-zinc-200 dark:bg-zinc-800 rounded mt-auto"></div>
                </div>
              </div>
              <div className="w-full sm:w-2/3 flex flex-col gap-4">
                <div className="h-14 w-full bg-zinc-50 dark:bg-zinc-900/30 border border-zinc-100 dark:border-zinc-800/50 rounded-xl flex items-center px-5 justify-between">
                   <div className="flex items-center gap-3">
                     <div className="w-5 h-5 rounded-full bg-indigo-100 dark:bg-indigo-900/50 flex items-center justify-center"><div className="w-2 h-2 rounded-full bg-indigo-500"></div></div>
                     <div className="h-3 w-24 bg-zinc-200 dark:bg-zinc-800 rounded"></div>
                   </div>
                   <div className="h-5 w-16 bg-zinc-200 dark:bg-zinc-800 rounded-full"></div>
                </div>
                <div className="h-48 w-full bg-zinc-50 dark:bg-zinc-900/30 border border-zinc-100 dark:border-zinc-800/50 rounded-xl p-6 flex flex-col">
                   <div className="h-3 w-32 bg-zinc-200 dark:bg-zinc-800 rounded mb-6"></div>
                   <div className="space-y-4 flex-1">
                     <div className="flex justify-between items-center">
                       <div className="h-2 w-1/4 bg-zinc-200 dark:bg-zinc-800 rounded"></div>
                       <div className="h-2 w-1/3 bg-zinc-200 dark:bg-zinc-800 rounded"></div>
                     </div>
                     <div className="w-full h-px bg-zinc-200 dark:bg-zinc-800/50"></div>
                     <div className="flex justify-between items-center">
                       <div className="h-2 w-1/5 bg-zinc-200 dark:bg-zinc-800 rounded"></div>
                       <div className="h-2 w-1/4 bg-zinc-200 dark:bg-zinc-800 rounded"></div>
                     </div>
                     <div className="w-full h-px bg-zinc-200 dark:bg-zinc-800/50"></div>
                     <div className="flex justify-between items-center">
                       <div className="h-2 w-1/3 bg-zinc-200 dark:bg-zinc-800 rounded"></div>
                       <div className="h-2 w-1/5 bg-zinc-200 dark:bg-zinc-800 rounded"></div>
                     </div>
                   </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    );
  }

  // Dashboard View
  return (
    <main className="min-h-screen bg-zinc-50 dark:bg-[#09090b] font-sans selection:bg-zinc-200 dark:selection:bg-zinc-800">
      {/* Header */}
      <header className="sticky top-0 z-40 w-full backdrop-blur-2xl bg-white/80 dark:bg-[#09090b]/80 border-b border-zinc-200 dark:border-zinc-900 shadow-sm">
        <div className="container flex h-16 items-center justify-between px-4 md:px-8 max-w-7xl mx-auto">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-zinc-900 dark:bg-white rounded-lg shadow-sm">
              <Sparkles className="w-5 h-5 text-white dark:text-zinc-900" />
            </div>
            <span className="text-xl font-bold tracking-tight text-zinc-900 dark:text-white">DocScan</span>
          </div>
          <div className="flex items-center gap-4">
            <div className="hidden sm:block text-xs font-medium text-zinc-500 dark:text-zinc-400 bg-zinc-100 dark:bg-zinc-900 px-3 py-1.5 rounded-full border border-zinc-200 dark:border-zinc-800 uppercase tracking-widest">
              Pro Tier
            </div>
            <Button variant="ghost" size="sm" onClick={logout} className="text-zinc-600 hover:text-zinc-900 hover:bg-zinc-100 dark:text-zinc-400 dark:hover:text-white dark:hover:bg-zinc-800 rounded-full font-medium">
              <LogOut className="w-4 h-4 mr-2" />
              Logout
            </Button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <div className="container px-4 md:px-8 max-w-7xl mx-auto py-12">
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-10 gap-6">
          <div>
            <h2 className="text-3xl font-bold tracking-tight text-zinc-900 dark:text-white">Documents</h2>
            <p className="text-zinc-500 dark:text-zinc-400 mt-2">Manage and extract data from your scanned files.</p>
          </div>
          <div className="flex items-center gap-4 w-full md:w-auto">
            {/* Sorting controls */}
            <div className="flex items-center bg-white dark:bg-zinc-900 rounded-lg p-1 border border-zinc-200 dark:border-zinc-800 shadow-sm overflow-hidden text-sm w-full md:w-auto overflow-x-auto">
              {(['createdAt', 'filename', 'sizeBytes', 'status'] as const).map(field => (
                <button
                  key={field}
                  onClick={() => toggleSort(field)}
                  className={`px-4 py-2 rounded-md whitespace-nowrap transition-all flex items-center gap-1.5 font-medium ${sortField === field ? 'bg-zinc-100 dark:bg-zinc-800 text-zinc-900 dark:text-white shadow-sm' : 'text-zinc-500 hover:text-zinc-900 dark:hover:text-zinc-300'}`}
                >
                  {field === 'createdAt' ? 'Date' : field === 'filename' ? 'Name' : field === 'sizeBytes' ? 'Size' : 'Status'}
                  {sortField === field && (
                    <ArrowUpDown className={`w-3 h-3 ${sortOrder === 'desc' ? 'rotate-180' : ''} transition-transform`} />
                  )}
                </button>
              ))}
            </div>
            
            <Button onClick={() => setIsUploadOpen(true)} className="bg-zinc-900 hover:bg-zinc-800 text-white dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200 rounded-lg px-6 h-10 shadow-sm whitespace-nowrap shrink-0 font-medium">
              <UploadCloud className="w-4 h-4 mr-2" />
              Upload
            </Button>
          </div>
        </div>

        {/* Loading State */}
        {loadingDocs ? (
          <div className="flex justify-center items-center py-32">
            <div className="relative w-8 h-8">
              <div className="w-8 h-8 border-2 border-zinc-200 dark:border-zinc-800 rounded-full"></div>
              <div className="w-8 h-8 border-2 border-zinc-900 dark:border-white rounded-full border-t-transparent animate-spin absolute top-0 left-0"></div>
            </div>
          </div>
        ) : documents.length === 0 ? (
          /* Empty State */
          <div className="flex flex-col items-center justify-center py-32 px-4 border border-dashed border-zinc-300 dark:border-zinc-800 rounded-2xl bg-white dark:bg-[#09090b] relative overflow-hidden group shadow-sm">
            <div className="p-4 bg-zinc-50 dark:bg-zinc-900/50 rounded-full mb-6 border border-zinc-100 dark:border-zinc-800/50">
              <FileText className="w-8 h-8 text-zinc-400 dark:text-zinc-500" />
            </div>
            <h3 className="text-xl font-bold text-zinc-900 dark:text-white mb-2 tracking-tight">No documents found</h3>
            <p className="text-zinc-500 mb-8 text-center max-w-sm">
              Upload your first invoice or receipt to begin the extraction process.
            </p>
            <Button onClick={() => setIsUploadOpen(true)} className="rounded-lg shadow-sm bg-zinc-900 hover:bg-zinc-800 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200 text-white px-6 font-medium">
              Upload Document
            </Button>
          </div>
        ) : (
          /* Document Grid */
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {sortedDocuments.map((doc) => (
              <div key={doc._id} className="group relative bg-white dark:bg-zinc-950 border border-zinc-200 dark:border-zinc-800/80 rounded-2xl p-6 shadow-sm hover:shadow-md transition-all duration-300 hover:border-zinc-300 dark:hover:border-zinc-700 flex flex-col">
                
                {/* Actions Dropdown */}
                <div className="absolute top-4 right-4 dropdown-container z-20">
                  <button 
                    onClick={() => setActiveDropdown(activeDropdown === doc._id ? null : doc._id)}
                    className="p-1.5 text-zinc-400 hover:text-zinc-900 dark:hover:text-white hover:bg-zinc-100 dark:hover:bg-zinc-800 rounded-md transition-colors opacity-0 group-hover:opacity-100 focus:opacity-100 data-[active=true]:opacity-100 data-[active=true]:bg-zinc-100 dark:data-[active=true]:bg-zinc-800"
                    data-active={activeDropdown === doc._id}
                  >
                    <MoreVertical className="w-4 h-4" />
                  </button>
                  
                  {activeDropdown === doc._id && (
                    <div className="absolute right-0 mt-2 w-48 bg-white dark:bg-zinc-900 rounded-lg shadow-lg border border-zinc-200 dark:border-zinc-800 py-1 animate-in fade-in zoom-in-95 duration-100 origin-top-right">
                      <button onClick={(e) => handleDownload(doc._id, e)} className="w-full text-left px-3 py-2 text-sm hover:bg-zinc-50 dark:hover:bg-zinc-800 flex items-center text-zinc-700 dark:text-zinc-300 transition-colors">
                        <Download className="w-4 h-4 mr-2.5 text-zinc-500" /> Download
                      </button>
                      <button onClick={(e) => openEditModal(doc, e)} className="w-full text-left px-3 py-2 text-sm hover:bg-zinc-50 dark:hover:bg-zinc-800 flex items-center text-zinc-700 dark:text-zinc-300 transition-colors">
                        <Edit2 className="w-4 h-4 mr-2.5 text-zinc-500" /> Rename
                      </button>
                      <div className="h-px bg-zinc-100 dark:bg-zinc-800 my-1"></div>
                      <button onClick={(e) => handleDelete(doc._id, e)} className="w-full text-left px-3 py-2 text-sm hover:bg-red-50 dark:hover:bg-red-900/20 flex items-center text-red-600 dark:text-red-400 transition-colors">
                        <Trash2 className="w-4 h-4 mr-2.5" /> Delete
                      </button>
                    </div>
                  )}
                </div>

                <div className="flex justify-between items-start mb-6">
                  <div className="p-3 bg-zinc-100 dark:bg-zinc-900 rounded-xl border border-zinc-200/50 dark:border-zinc-800">
                    <FileText className="w-5 h-5 text-zinc-700 dark:text-zinc-300" />
                  </div>
                  {doc.status === 'COMPLETED' ? (
                    <span className="inline-flex items-center px-2 py-1 rounded bg-zinc-100 dark:bg-zinc-900 text-zinc-900 dark:text-zinc-100 text-[10px] font-semibold tracking-wider uppercase border border-zinc-200 dark:border-zinc-800">
                      <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 mr-1.5"></span> Extracted
                    </span>
                  ) : doc.status === 'FAILED' ? (
                    <span className="inline-flex items-center px-2 py-1 rounded bg-zinc-100 dark:bg-zinc-900 text-zinc-900 dark:text-zinc-100 text-[10px] font-semibold tracking-wider uppercase border border-zinc-200 dark:border-zinc-800">
                      <span className="w-1.5 h-1.5 rounded-full bg-red-500 mr-1.5"></span> Failed
                    </span>
                  ) : (
                    <span className="inline-flex items-center px-2 py-1 rounded bg-zinc-100 dark:bg-zinc-900 text-zinc-900 dark:text-zinc-100 text-[10px] font-semibold tracking-wider uppercase border border-zinc-200 dark:border-zinc-800">
                      <span className="w-1.5 h-1.5 rounded-full bg-amber-500 mr-1.5 animate-pulse"></span> Processing
                    </span>
                  )}
                </div>
                
                <h3 className="font-semibold text-zinc-900 dark:text-white truncate mb-1 text-base tracking-tight" title={doc.filename}>
                  {doc.filename}
                </h3>
                <p className="text-xs text-zinc-500 mb-6 font-medium flex items-center gap-1.5">
                  <span>{(doc.sizeBytes / 1024 / 1024).toFixed(2)} MB</span>
                  <span className="w-0.5 h-0.5 rounded-full bg-zinc-400 dark:bg-zinc-600"></span>
                  <span>{new Date(doc.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}</span>
                </p>

                {doc.aiMetadata && doc.aiMetadata.extractedData ? (
                  <div className="mt-auto pt-4 border-t border-zinc-100 dark:border-zinc-800/80">
                    <div className="flex justify-between items-center mb-2">
                      <span className="text-[11px] font-semibold text-zinc-400 uppercase tracking-wider">Vendor</span>
                      <span className="font-medium text-sm text-zinc-900 dark:text-white truncate max-w-[140px]" title={doc.aiMetadata.extractedData.vendorName}>
                        {doc.aiMetadata.extractedData.vendorName || 'Unknown'}
                      </span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-[11px] font-semibold text-zinc-400 uppercase tracking-wider">Total</span>
                      <span className="font-bold text-zinc-900 dark:text-white text-base">
                        {doc.aiMetadata.extractedData.totalAmount ? `$${doc.aiMetadata.extractedData.totalAmount}` : '-'}
                      </span>
                    </div>
                  </div>
                ) : (
                  <div className="mt-auto pt-4 border-t border-zinc-100 dark:border-zinc-800/80">
                    <div className="flex justify-center items-center h-10 bg-zinc-50 dark:bg-zinc-900/30 rounded-lg border border-dashed border-zinc-200 dark:border-zinc-800">
                      <span className="text-xs text-zinc-400 font-medium">No extraction data</span>
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
          toast.success('File uploaded successfully! Processing started.');
        }}
      />

      {/* Edit Dialog */}
      <Dialog open={!!editingDoc} onOpenChange={(open) => !open && setEditingDoc(null)}>
        <DialogContent className="sm:max-w-[425px] rounded-2xl p-6 bg-white dark:bg-zinc-950 border-zinc-200 dark:border-zinc-800 shadow-xl">
          <DialogHeader>
            <DialogTitle className="text-xl tracking-tight">Rename Document</DialogTitle>
            <DialogDescription className="text-zinc-500">
              Enter a new name for your document.
            </DialogDescription>
          </DialogHeader>
          <div className="py-4">
            <Input 
              value={newFilename} 
              onChange={(e) => setNewFilename(e.target.value)}
              placeholder="Document Name"
              autoFocus
              className="bg-white dark:bg-zinc-900 border-zinc-200 dark:border-zinc-800 rounded-lg h-10 focus-visible:ring-zinc-900 dark:focus-visible:ring-zinc-300"
            />
          </div>
          <DialogFooter className="gap-2 sm:gap-0 mt-2">
            <Button variant="outline" onClick={() => setEditingDoc(null)} className="rounded-lg h-10 border-zinc-200 dark:border-zinc-800 font-medium text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50 dark:hover:bg-zinc-900">Cancel</Button>
            <Button onClick={handleRename} className="rounded-lg h-10 bg-zinc-900 text-white hover:bg-zinc-800 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200 font-medium shadow-sm">Save Changes</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </main>
  );
}
