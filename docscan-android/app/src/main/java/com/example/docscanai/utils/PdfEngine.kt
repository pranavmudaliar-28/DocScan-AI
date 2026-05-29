package com.example.docscanai.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.multipdf.Splitter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object PdfEngine {

    private var isInitialized = false

    fun init(context: Context) {
        if (!isInitialized) {
            PDFBoxResourceLoader.init(context)
            isInitialized = true
        }
    }

    suspend fun mergePdfs(context: Context, uris: List<Uri>, outputUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            init(context)
            val merger = PDFMergerUtility()
            
            // Create a temp file to hold output
            val tempFile = File(context.cacheDir, "merged_temp_${System.currentTimeMillis()}.pdf")
            merger.destinationFileName = tempFile.absolutePath

            uris.forEach { uri ->
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val tempSource = File.createTempFile("merge_source_", ".pdf", context.cacheDir)
                    tempSource.outputStream().use { stream.copyTo(it) }
                    merger.addSource(tempSource)
                }
            }
            
            merger.mergeDocuments(null)
            
            // Copy tempFile to outputUri
            context.contentResolver.openOutputStream(outputUri)?.use { out ->
                tempFile.inputStream().use { it.copyTo(out) }
            }
            
            // Clean up
            tempFile.delete()
            
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun splitPdf(context: Context, sourceUri: Uri, pageRanges: List<Int>, outputUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            init(context)
            val tempSource = File.createTempFile("split_source_", ".pdf", context.cacheDir)
            context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                tempSource.outputStream().use { stream.copyTo(it) }
            }

            val document = PDDocument.load(tempSource)
            val newDocument = PDDocument()
            
            // PDFBox is 0-indexed for pages. Assume pageRanges are 1-indexed.
            pageRanges.forEach { pageNum ->
                val zeroIndexed = pageNum - 1
                if (zeroIndexed in 0 until document.numberOfPages) {
                    newDocument.addPage(document.getPage(zeroIndexed))
                }
            }

            val tempOutput = File.createTempFile("split_out_", ".pdf", context.cacheDir)
            newDocument.save(tempOutput)
            newDocument.close()
            document.close()
            tempSource.delete()

            context.contentResolver.openOutputStream(outputUri)?.use { out ->
                tempOutput.inputStream().use { it.copyTo(out) }
            }
            tempOutput.delete()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun imagesToPdf(context: Context, imageUris: List<Uri>, outputUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            init(context)
            val document = PDDocument()

            imageUris.forEach { uri ->
                val page = PDPage()
                document.addPage(page)

                val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }

                if (bitmap != null) {
                    val pdImage = JPEGFactory.createFromImage(document, bitmap)
                    val contentStream = PDPageContentStream(document, page)
                    
                    // Scale image to fit page, maintaining aspect ratio
                    val pw = page.mediaBox.width
                    val ph = page.mediaBox.height
                    val iw = pdImage.width.toFloat()
                    val ih = pdImage.height.toFloat()
                    
                    val scale = minOf(pw / iw, ph / ih)
                    val scaledW = iw * scale
                    val scaledH = ih * scale
                    val x = (pw - scaledW) / 2f
                    val y = (ph - scaledH) / 2f
                    
                    contentStream.drawImage(pdImage, x, y, scaledW, scaledH)
                    contentStream.close()
                }
            }

            val tempOutput = File.createTempFile("images_to_pdf_", ".pdf", context.cacheDir)
            document.save(tempOutput)
            document.close()

            context.contentResolver.openOutputStream(outputUri)?.use { out ->
                tempOutput.inputStream().use { it.copyTo(out) }
            }
            tempOutput.delete()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // Compression reduces file size by re-saving without meta-data and potentially downsampling.
    // For advanced compression, downsampling each PDImageXObject is required, but saving fresh helps.
    suspend fun compressPdf(context: Context, sourceUri: Uri, outputUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            init(context)
            val tempSource = File.createTempFile("compress_source_", ".pdf", context.cacheDir)
            context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                tempSource.outputStream().use { stream.copyTo(it) }
            }

            val document = PDDocument.load(tempSource)
            // Note: In PDFBox, saving a loaded document often naturally compresses it if it had unused objects
            // To actually downsample, we'd need to iterate resources, which is complex.
            // For now, simple open-save pass with a fresh file removes unreferenced objects.

            val tempOutput = File.createTempFile("compress_out_", ".pdf", context.cacheDir)
            document.save(tempOutput)
            document.close()
            tempSource.delete()

            context.contentResolver.openOutputStream(outputUri)?.use { out ->
                tempOutput.inputStream().use { it.copyTo(out) }
            }
            tempOutput.delete()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun insertSignature(
        context: Context,
        pdfUri: Uri,
        signatureUri: Uri,
        pageIndex: Int,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        outputUri: Uri
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            init(context)
            val tempSource = File.createTempFile("sign_source_", ".pdf", context.cacheDir)
            context.contentResolver.openInputStream(pdfUri)?.use { stream ->
                tempSource.outputStream().use { stream.copyTo(it) }
            }

            val document = PDDocument.load(tempSource)
            
            if (pageIndex in 0 until document.numberOfPages) {
                val page = document.getPage(pageIndex)
                
                // Read the signature image
                val signatureBitmap = if (signatureUri.scheme == "file") {
                    BitmapFactory.decodeFile(signatureUri.path)
                } else {
                    context.contentResolver.openInputStream(signatureUri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }
                
                if (signatureBitmap != null) {
                    // PDFBox uses a bottom-left coordinate system. We need to convert our (top-left) y to bottom-left y.
                    // But wait, the passed x, y might already be scaled to the PDF page size.
                    // We assume the caller scaled x, y, width, height relative to the PDPage's MediaBox.
                    val pdImage = com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(document, signatureBitmap)
                    
                    val contentStream = PDPageContentStream(
                        document, 
                        page, 
                        PDPageContentStream.AppendMode.APPEND, 
                        true, 
                        true
                    )
                    
                    // Convert Top-Left Y to Bottom-Left Y
                    val pdfHeight = page.mediaBox.height
                    val pdfY = pdfHeight - y - height
                    
                    contentStream.drawImage(pdImage, x, pdfY, width, height)
                    contentStream.close()
                }
            }

            val tempOutput = File.createTempFile("sign_out_", ".pdf", context.cacheDir)
            document.save(tempOutput)
            document.close()
            tempSource.delete()

            context.contentResolver.openOutputStream(outputUri)?.use { out ->
                tempOutput.inputStream().use { it.copyTo(out) }
            }
            tempOutput.delete()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
