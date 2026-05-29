package com.example.docscanai.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.docscanai.data.local.AnnotationEntity

@Composable
fun AnnotationLayer(
    annotations: List<AnnotationEntity>,
    isDrawingMode: Boolean = false,
    currentTool: String = "PEN", // PEN, HIGHLIGHTER, MARKER, RECTANGLE, CIRCLE, ARROW, LINE
    currentColor: Color = Color.Red,
    currentStrokeWidth: Float = 8f,
    onAnnotationAdded: (AnnotationEntity) -> Unit
) {
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var startOffset by remember { mutableStateOf<Offset?>(null) }
    var currentOffset by remember { mutableStateOf<Offset?>(null) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isDrawingMode, currentTool) {
                if (isDrawingMode) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            startOffset = offset
                            currentOffset = offset
                            if (currentTool in listOf("PEN", "HIGHLIGHTER", "MARKER")) {
                                currentPath = Path().apply { moveTo(offset.x, offset.y) }
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentOffset = change.position
                            if (currentTool in listOf("PEN", "HIGHLIGHTER", "MARKER")) {
                                currentPath?.lineTo(change.position.x, change.position.y)
                            }
                        },
                        onDragEnd = {
                            // On drag end, we'd normally save this to the annotations list
                            // But for simplicity in this MVP, we just reset the path if it's not being saved
                            val newAnnotation = AnnotationEntity(
                                documentId = "temp", // Usually passed from parent
                                pageNumber = 0,
                                x = startOffset?.x ?: 0f,
                                y = startOffset?.y ?: 0f,
                                width = (currentOffset?.x ?: 0f) - (startOffset?.x ?: 0f),
                                height = (currentOffset?.y ?: 0f) - (startOffset?.y ?: 0f),
                                annotationType = currentTool,
                                color = currentColor.value.toInt(), // A bit hacky, normally map to ARGB
                                strokeWidth = currentStrokeWidth,
                                pathData = null // we'd serialize the Path here if needed
                            )
                            onAnnotationAdded(newAnnotation)
                            currentPath = null
                            startOffset = null
                            currentOffset = null
                        }
                    )
                }
            }
    ) {
        // Draw existing annotations (simplified for MVP)
        annotations.forEach { ann ->
            val color = ann.color?.let { Color(it.toULong()) } ?: Color.Red
            val stroke = ann.strokeWidth ?: 8f

            when (ann.annotationType) {
                "RECTANGLE" -> {
                    drawRect(
                        color = color,
                        topLeft = Offset(ann.x, ann.y),
                        size = Size(ann.width, ann.height),
                        style = Stroke(width = stroke)
                    )
                }
                "CIRCLE" -> {
                    val radius = Math.hypot(ann.width.toDouble(), ann.height.toDouble()).toFloat()
                    drawCircle(
                        color = color,
                        radius = radius,
                        center = Offset(ann.x, ann.y),
                        style = Stroke(width = stroke)
                    )
                }
                "LINE" -> {
                    drawLine(
                        color = color,
                        start = Offset(ann.x, ann.y),
                        end = Offset(ann.x + ann.width, ann.y + ann.height),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
                // Stamps / Text skipped in Canvas draw for MVP; usually rendered as Composable overlays
            }
        }

        // Draw current active drawing
        if (isDrawingMode) {
            val alpha = if (currentTool == "HIGHLIGHTER") 0.5f else 1f
            val cColor = currentColor.copy(alpha = alpha)

            if (currentTool in listOf("PEN", "HIGHLIGHTER", "MARKER") && currentPath != null) {
                drawPath(
                    path = currentPath!!,
                    color = cColor,
                    style = Stroke(
                        width = currentStrokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            } else if (startOffset != null && currentOffset != null) {
                when (currentTool) {
                    "RECTANGLE" -> {
                        drawRect(
                            color = cColor,
                            topLeft = Offset(
                                minOf(startOffset!!.x, currentOffset!!.x),
                                minOf(startOffset!!.y, currentOffset!!.y)
                            ),
                            size = Size(
                                Math.abs(currentOffset!!.x - startOffset!!.x),
                                Math.abs(currentOffset!!.y - startOffset!!.y)
                            ),
                            style = Stroke(width = currentStrokeWidth)
                        )
                    }
                    "CIRCLE" -> {
                        val radius = Math.hypot(
                            (currentOffset!!.x - startOffset!!.x).toDouble(),
                            (currentOffset!!.y - startOffset!!.y).toDouble()
                        ).toFloat()
                        drawCircle(
                            color = cColor,
                            radius = radius,
                            center = startOffset!!,
                            style = Stroke(width = currentStrokeWidth)
                        )
                    }
                    "LINE" -> {
                        drawLine(
                            color = cColor,
                            start = startOffset!!,
                            end = currentOffset!!,
                            strokeWidth = currentStrokeWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}
