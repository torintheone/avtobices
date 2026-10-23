package app.steamgarage.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.steamgarage.ui.theme.Steam
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * QR-код на латунной табличке. Тёмные модули на светлом поле — так его уверенно читают камеры телефонов;
 * поле вокруг кода («тихая зона») оставлено по стандарту.
 */
@Composable
fun QrCode(text: String, contentDescription: String, modifier: Modifier = Modifier, size: Dp = 200.dp) {
    val palette = Steam.palette
    val matrix = remember(text) {
        QRCodeWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            0,
            0,
            mapOf(EncodeHintType.MARGIN to 0, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M),
        )
    }
    Box(
        modifier = modifier
            .background(brassBrush(palette), CutCornerShape(6.dp))
            .padding(8.dp)
            .background(QR_LIGHT)
            .padding(QUIET_ZONE)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Canvas(Modifier.size(size)) {
            val cell = this.size.width / matrix.width
            for (y in 0 until matrix.height) {
                for (x in 0 until matrix.width) {
                    if (matrix[x, y]) {
                        drawRect(QR_DARK, topLeft = Offset(x * cell, y * cell), size = Size(cell + 0.5f, cell + 0.5f))
                    }
                }
            }
        }
    }
}

private val QR_LIGHT = Color(0xFFF6EFD9)
private val QR_DARK = Color(0xFF1E1611)
private val QUIET_ZONE = 14.dp
