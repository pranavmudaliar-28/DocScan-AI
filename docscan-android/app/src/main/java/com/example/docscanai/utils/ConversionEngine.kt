package com.example.docscanai.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ConversionEngine {
    
    private var isInitialized = false

    private fun init(context: Context) {
        if (!isInitialized) {
            PDFBoxResourceLoader.init(context)
            isInitialized = true
        }
    }

    suspend fun pdfToImages(context: Context, pdfUri: Uri, outputDir: File): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
                ?: return@withContext Result.failure(Exception("Cannot open PDF"))
            
            val renderer = PdfRenderer(pfd)
            val outputFiles = mutableListOf<File>()
            
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val width = context.resources.displayMetrics.widthPixels
                val height = (width.toFloat() / page.width * page.height).toInt()
                
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                val outFile = File(outputDir, "page_${i + 1}_${System.currentTimeMillis()}.jpg")
                FileOutputStream(outFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                
                outputFiles.add(outFile)
                page.close()
            }
            renderer.close()
            pfd.close()
            
            Result.success(outputFiles)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun pdfToText(context: Context, pdfUri: Uri, outputFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            init(context)
            val tempSource = File.createTempFile("extract_source_", ".pdf", context.cacheDir)
            context.contentResolver.openInputStream(pdfUri)?.use { stream ->
                tempSource.outputStream().use { stream.copyTo(it) }
            }

            val document = PDDocument.load(tempSource)
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            
            document.close()
            tempSource.delete()

            outputFile.writeText(text)
            Result.success(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // Creating a basic DOCX without Apache POI by generating the OOXML ZIP structure manually
    suspend fun pdfToDocx(context: Context, pdfUri: Uri, outputFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val textResult = pdfToText(context, pdfUri, File.createTempFile("temp_txt", ".txt", context.cacheDir))
            if (textResult.isFailure) throw textResult.exceptionOrNull()!!
            
            val text = textResult.getOrNull()?.readText() ?: ""
            textResult.getOrNull()?.delete()

            FileOutputStream(outputFile).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    // [Content_Types].xml
                    zos.putNextEntry(ZipEntry("[Content_Types].xml"))
                    zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                            <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                            <Default Extension="xml" ContentType="application/xml"/>
                            <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                        </Types>""".trimIndent().toByteArray())
                    zos.closeEntry()

                    // _rels/.rels
                    zos.putNextEntry(ZipEntry("_rels/.rels"))
                    zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                        </Relationships>""".trimIndent().toByteArray())
                    zos.closeEntry()

                    // word/document.xml
                    zos.putNextEntry(ZipEntry("word/document.xml"))
                    val sb = java.lang.StringBuilder()
                    sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                        <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                            <w:body>""")
                    text.split("\n").forEach { line ->
                        sb.append("<w:p><w:r><w:t>")
                        sb.append(line.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"))
                        sb.append("</w:t></w:r></w:p>")
                    }
                    sb.append("""</w:body></w:document>""")
                    zos.write(sb.toString().toByteArray())
                    zos.closeEntry()
                }
            }
            Result.success(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun docxToPdf(context: Context, docxUri: Uri, outputFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            init(context)
            val sb = java.lang.StringBuilder()
            context.contentResolver.openInputStream(docxUri)?.use { stream ->
                java.util.zip.ZipInputStream(stream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == "word/document.xml") {
                            val xml = zis.reader().readText()
                            // extremely basic xml text extraction (remove all tags)
                            var inTag = false
                            for (char in xml) {
                                if (char == '<') inTag = true
                                else if (char == '>') inTag = false
                                else if (!inTag) sb.append(char)
                            }
                        }
                        entry = zis.nextEntry
                    }
                }
            }
            
            val document = PDDocument()
            val page = PDPage()
            document.addPage(page)
            
            val contentStream = PDPageContentStream(document, page)
            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA, 12f)
            contentStream.setLeading(14.5f)
            
            // Start near the top left
            var yOffset = page.mediaBox.height - 50f
            contentStream.newLineAtOffset(50f, yOffset)
            
            // split into lines of roughly 80 chars max to avoid running off page
            val text = sb.toString()
            val words = text.split(Regex("\\s+"))
            var line = ""
            for (word in words) {
                if (line.length + word.length > 80) {
                    try { contentStream.showText(line.filter { it.code in 32..126 }) } catch(e:Exception){}
                    contentStream.newLine()
                    yOffset -= 14.5f
                    if (yOffset < 50f) {
                        // Very basic: just stop if we hit end of page for MVP
                        break 
                    }
                    line = "$word "
                } else {
                    line += "$word "
                }
            }
            if (line.isNotEmpty()) {
                try { contentStream.showText(line.filter { it.code in 32..126 }) } catch(e:Exception){}
            }
            
            contentStream.endText()
            contentStream.close()
            
            document.save(outputFile)
            document.close()
            
            Result.success(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun textToPdf(context: Context, textUri: Uri, outputFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            init(context)
            val text = context.contentResolver.openInputStream(textUri)?.bufferedReader()?.use { it.readText() } ?: ""
            
            val document = PDDocument()
            val page = PDPage()
            document.addPage(page)
            
            val contentStream = PDPageContentStream(document, page)
            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA, 12f)
            contentStream.setLeading(14.5f)
            contentStream.newLineAtOffset(50f, page.mediaBox.height - 50f)
            
            text.split("\n").forEach { line ->
                // PDFBox cannot handle characters outside standard 14 fonts easily without custom TTF,
                // replace non-ASCII if needed or just catch exception. For simple text it works.
                try {
                    contentStream.showText(line.filter { it.code in 32..126 }) // Keep it simple ASCII
                } catch (e: Exception) {
                    contentStream.showText("?")
                }
                contentStream.newLine()
            }
            
            contentStream.endText()
            contentStream.close()
            
            document.save(outputFile)
            document.close()
            
            Result.success(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun imageToFormat(context: Context, imageUri: Uri, outputFile: File, format: Bitmap.CompressFormat): Result<File> = withContext(Dispatchers.IO) {
        try {
            val bitmap = context.contentResolver.openInputStream(imageUri)?.use {
                BitmapFactory.decodeStream(it)
            } ?: return@withContext Result.failure(Exception("Cannot decode image"))

            FileOutputStream(outputFile).use { out ->
                bitmap.compress(format, 100, out)
            }
            
            Result.success(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
