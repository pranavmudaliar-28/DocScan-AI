'use client';

import { useState, useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { Progress } from '@/components/ui/progress';
import { Button } from '@/components/ui/button';
import { api } from '@/lib/axios';
import { UploadCloud, FileText, CheckCircle2, AlertCircle, X } from 'lucide-react';
import axios from 'axios';

interface UploadModalProps {
  isOpen: boolean;
  onClose: () => void;
  onUploadComplete: () => void;
}

export function UploadModal({ isOpen, onClose, onUploadComplete }: UploadModalProps) {
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState('');

  const onDrop = useCallback((acceptedFiles: File[]) => {
    if (acceptedFiles.length > 0) {
      setFile(acceptedFiles[0]);
      setError('');
    }
  }, []);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'application/pdf': ['.pdf'],
      'image/jpeg': ['.jpg', '.jpeg'],
      'image/png': ['.png']
    },
    maxFiles: 1,
    maxSize: 10 * 1024 * 1024 // 10MB limit
  });

  const handleUpload = async () => {
    if (!file) return;

    try {
      setUploading(true);
      setError('');
      setProgress(0);

      // 1. Get Pre-signed URL
      const intentRes = await api.post('/documents/upload-intent', {
        filename: file.name,
        mimeType: file.type,
        sizeBytes: file.size
      });

      const { uploadUrl, documentId } = intentRes.data;

      // 2. Upload directly to S3
      await axios.put(uploadUrl, file, {
        headers: { 'Content-Type': file.type },
        onUploadProgress: (progressEvent) => {
          const percentCompleted = Math.round((progressEvent.loaded * 100) / (progressEvent.total || file.size));
          setProgress(percentCompleted);
        }
      });

      // 3. Confirm upload
      await api.post('/documents/confirm-upload', { documentId });

      onUploadComplete();
      handleClose();
    } catch (err: any) {
      setError(err.message || 'Upload failed. Please try again.');
    } finally {
      setUploading(false);
    }
  };

  const handleClose = () => {
    setFile(null);
    setUploading(false);
    setProgress(0);
    setError('');
    onClose();
  };

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && !uploading && handleClose()}>
      <DialogContent className="sm:max-w-md rounded-2xl p-6 bg-white dark:bg-zinc-950 border-zinc-200 dark:border-zinc-800 shadow-xl">
        <DialogHeader>
          <DialogTitle className="text-xl tracking-tight">Upload Document</DialogTitle>
          <DialogDescription className="text-zinc-500">
            Upload an invoice, receipt, or any document to extract data.
          </DialogDescription>
        </DialogHeader>

        <div className="my-4">
          {!file ? (
            <div 
              {...getRootProps()} 
              className={`border-2 border-dashed rounded-2xl p-8 flex flex-col items-center justify-center cursor-pointer transition-all ${
                isDragActive ? 'border-zinc-900 bg-zinc-50 dark:border-zinc-300 dark:bg-zinc-900/50' : 'border-zinc-200 dark:border-zinc-800 hover:bg-zinc-50 dark:hover:bg-zinc-900/30'
              }`}
            >
              <input {...getInputProps()} />
              <UploadCloud className="w-10 h-10 text-zinc-400 dark:text-zinc-500 mb-4" />
              <p className="text-sm text-zinc-600 dark:text-zinc-400 text-center">
                <span className="font-semibold text-zinc-900 dark:text-white">Click to upload</span> or drag and drop
              </p>
              <p className="text-xs text-zinc-500 mt-2 font-medium">PDF, PNG, JPG (max 10MB)</p>
            </div>
          ) : (
            <div className="space-y-4">
              <div className="flex items-center p-4 border rounded-xl border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-900/30">
                <div className="p-2 bg-white dark:bg-zinc-800 rounded-lg shadow-sm border border-zinc-100 dark:border-zinc-700 mr-4">
                  <FileText className="w-6 h-6 text-zinc-700 dark:text-zinc-300" />
                </div>
                <div className="flex-1 truncate">
                  <p className="text-sm font-medium truncate text-zinc-900 dark:text-white">{file.name}</p>
                  <p className="text-xs text-zinc-500 font-medium mt-0.5">{(file.size / 1024 / 1024).toFixed(2)} MB</p>
                </div>
                {!uploading && (
                  <button onClick={() => setFile(null)} aria-label="Remove file" className="p-2 text-zinc-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-colors">
                    <X className="w-4 h-4" />
                  </button>
                )}
              </div>
              
              {uploading && (
                <div className="space-y-2">
                  <div className="flex justify-between text-xs text-zinc-500 font-medium uppercase tracking-wider">
                    <span>Uploading...</span>
                    <span>{progress}%</span>
                  </div>
                  <Progress value={progress} className="h-2 bg-zinc-100 dark:bg-zinc-800" />
                </div>
              )}
              {error && (
                <div className="flex items-center text-sm text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20 p-3 rounded-lg">
                  <AlertCircle className="w-4 h-4 mr-2" />
                  {error}
                </div>
              )}
            </div>
          )}
        </div>

        <DialogFooter className="gap-2 sm:gap-0 mt-2">
          <Button variant="outline" onClick={handleClose} disabled={uploading} className="rounded-lg h-10 border-zinc-200 dark:border-zinc-800 font-medium text-zinc-700 dark:text-zinc-300 hover:bg-zinc-50 dark:hover:bg-zinc-900">
            Cancel
          </Button>
          <Button 
            onClick={handleUpload} 
            disabled={!file || uploading}
            className="rounded-lg h-10 bg-zinc-900 text-white hover:bg-zinc-800 dark:bg-white dark:text-zinc-900 dark:hover:bg-zinc-200 font-medium shadow-sm"
          >
            {uploading ? 'Processing...' : 'Upload File'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
