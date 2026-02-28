package com.klecer.gottado.ui.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.klecer.gottado.R

@Composable
fun HsvColorPickerDialog(
    currentColor: Int,
    defaultColors: List<Int>,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val hsv = remember { FloatArray(3) }
    android.graphics.Color.colorToHSV(currentColor, hsv)
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }

    fun currentArgb(): Int = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)) or (0xFF shl 24)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_color_picker_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(currentArgb()))
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                )

                Spacer(Modifier.height(16.dp))

                SvPanel(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    onSatValChange = { s, v -> saturation = s; value = v },
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                )

                Spacer(Modifier.height(12.dp))

                HueBar(
                    hue = hue,
                    onHueChange = { hue = it },
                    modifier = Modifier.fillMaxWidth().height(32.dp)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    stringResource(R.string.settings_color_picker_defaults),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    defaultColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .border(1.dp, Color.LightGray, CircleShape)
                                .clickable {
                                    val dHsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(color, dHsv)
                                    hue = dHsv[0]; saturation = dHsv[1]; value = dHsv[2]
                                }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onColorSelected(currentArgb()); onDismiss() }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun SvPanel(
    hue: Float,
    saturation: Float,
    value: Float,
    onSatValChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val s = (offset.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                    onSatValChange(s, v)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val s = (change.position.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onSatValChange(s, v)
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val pureColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
        drawRect(Brush.horizontalGradient(listOf(Color.White, pureColor)))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val cx = saturation * w
        val cy = (1f - value) * h
        drawCircle(Color.White, radius = 8.dp.toPx(), center = Offset(cx, cy))
        drawCircle(Color.Black, radius = 8.dp.toPx(), center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
    }
}

@Composable
private fun HueBar(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val hueColors = remember {
        (0..360 step 1).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it.toFloat(), 1f, 1f))) }
    }
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onHueChange((offset.x / size.width * 360f).coerceIn(0f, 360f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onHueChange((change.position.x / size.width * 360f).coerceIn(0f, 360f))
                }
            }
    ) {
        drawRect(Brush.horizontalGradient(hueColors))
        val cx = hue / 360f * size.width
        drawCircle(Color.White, radius = 10.dp.toPx(), center = Offset(cx, size.height / 2))
        drawCircle(Color.Black, radius = 10.dp.toPx(), center = Offset(cx, size.height / 2), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
    }
}
