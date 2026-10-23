package app.steamgarage.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import app.steamgarage.ui.theme.Steam
import java.io.File
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Фон экранов: процедурная текстура тёмного дерева (или пергамента в дневной теме) с виньеткой.
 * Текстура генерируется один раз, кешируется в памяти и в файле, затем повторяется плиткой.
 */
@Composable
fun SteamBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = Steam.palette
    val context = LocalContext.current
    val texture by produceState(initialValue = TextureCache.peek(palette.isDark), palette.isDark) {
        value = TextureCache.load(context.applicationContext, palette.isDark)
    }
    val brush = remember(texture) {
        texture?.let { ShaderBrush(ImageShader(it, TileMode.Repeated, TileMode.Repeated)) }
    }
    val vignette = if (palette.isDark) Color.Black.copy(alpha = 0.45f) else Color(0x403B2A1A)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .drawBehind {
                if (brush != null) drawRect(brush)
                drawRect(
                    Brush.radialGradient(
                        0.55f to Color.Transparent,
                        1.0f to vignette,
                        center = center,
                        radius = size.maxDimension * 0.75f,
                    ),
                )
            },
        content = content,
    )
}

/** Кеш текстур: в памяти на время работы и PNG-файлом в cacheDir между запусками. */
object TextureCache {
    private const val SIZE = 512
    private const val VERSION = 1

    @Volatile private var wood: ImageBitmap? = null
    @Volatile private var parchment: ImageBitmap? = null

    fun peek(dark: Boolean): ImageBitmap? = if (dark) wood else parchment

    suspend fun load(context: Context, dark: Boolean): ImageBitmap = withContext(Dispatchers.Default) {
        peek(dark) ?: run {
            val file = File(context.cacheDir, "texture_${if (dark) "wood" else "parchment"}_v$VERSION.png")
            val cached = runCatching { BitmapFactory.decodeFile(file.path) }.getOrNull()
            val bitmap = cached ?: Bitmap.createBitmap(
                if (dark) TextureGenerator.wood(SIZE) else TextureGenerator.parchment(SIZE),
                SIZE,
                SIZE,
                Bitmap.Config.ARGB_8888,
            ).also { generated ->
                runCatching { file.outputStream().use { generated.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            }
            bitmap.asImageBitmap().also { if (dark) wood = it else parchment = it }
        }
    }
}

/**
 * Генератор бесшовных текстур на периодическом value-noise. Чистая математика, без android.*:
 * возвращает пиксели ARGB.
 */
object TextureGenerator {

    /** Тёмное дерево: горизонтальные доски с волокнами; яркость держится у токена фона #1E1611. */
    fun wood(size: Int, seed: Long = 1871L): IntArray {
        val warp = PeriodicNoise(seed, 2, 8)
        val warpFine = PeriodicNoise(seed + 1, 8, 32)
        val fiber = PeriodicNoise(seed + 2, 4, 256)
        val tone = PeriodicNoise(seed + 3, 4, 16)
        val streak = PeriodicNoise(seed + 4, 3, 64)
        val planks = 4
        val plankHeight = size / planks
        val random = java.util.Random(seed)
        val plankShift = FloatArray(planks) { random.nextFloat() * 40f }
        val plankTone = FloatArray(planks) { random.nextFloat() * 0.16f - 0.08f }
        val dark = intArrayOf(0x18, 0x11, 0x0C)
        val light = intArrayOf(0x36, 0x27, 0x1B)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            val plank = y / plankHeight
            val inPlank = y - plank * plankHeight
            for (x in 0 until size) {
                val u = x.toFloat() / size
                val v = y.toFloat() / size
                val fine = warpFine.sample(u * 8, v * 32)
                val w = warp.fbm(u * 2, v * 8, 3) * 30f + fine * 5f
                val phase = (inPlank + plankShift[plank] + w) / 9f
                val rings = 0.5f + 0.5f * sin(phase * PI + fine * 2.0).toFloat()
                val grain = rings.pow(4)
                val fibers = fiber.sample(u * 4, v * 256) * 0.28f
                val streaks = (streak.sample(u * 3, v * 64) - 0.5f) * 0.35f
                var t = 0.10f + grain * 0.40f + fibers + streaks + (tone.sample(u * 4, v * 16) - 0.5f) * 0.25f + plankTone[plank]
                if (inPlank < 2 || inPlank >= plankHeight - 1) t = -0.3f
                pixels[y * size + x] = mix(dark, light, t.coerceIn(-0.45f, 1f))
            }
        }
        return pixels
    }

    /** Пергамент: светлая бумага с пятнами и волокнами; яркость держится у токена #EFE4CC. */
    fun parchment(size: Int, seed: Long = 1888L): IntArray {
        val blotch = PeriodicNoise(seed, 4, 4)
        val mottle = PeriodicNoise(seed + 1, 16, 16)
        val fiber = PeriodicNoise(seed + 2, 32, 128)
        val dark = intArrayOf(0xE6, 0xD9, 0xBB)
        val light = intArrayOf(0xF6, 0xEE, 0xDA)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val u = x.toFloat() / size
                val v = y.toFloat() / size
                val t = 0.55f +
                    (blotch.fbm(u * 4, v * 4, 3) - 0.5f) * 0.9f +
                    (mottle.sample(u * 16, v * 16) - 0.5f) * 0.25f +
                    (fiber.sample(u * 32, v * 128) - 0.5f) * 0.18f
                pixels[y * size + x] = mix(dark, light, t.coerceIn(0f, 1f))
            }
        }
        return pixels
    }

    private fun mix(a: IntArray, b: IntArray, t: Float): Int {
        fun ch(i: Int) = (a[i] + (b[i] - a[i]) * t).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(0) shl 16) or (ch(1) shl 8) or ch(2)
    }
}

/**
 * Периодический value-noise: решётка [periodX]×[periodY], значения 0..1, плавная интерполяция.
 * Координаты от 0 до периода дают ровно одну бесшовную плитку.
 */
internal class PeriodicNoise(seed: Long, private val periodX: Int, private val periodY: Int) {
    private val values = java.util.Random(seed).let { r -> FloatArray(periodX * periodY) { r.nextFloat() } }

    fun sample(x: Float, y: Float): Float {
        val x0 = floor(x).toInt()
        val y0 = floor(y).toInt()
        val fx = x - x0
        val fy = y - y0
        val sx = fx * fx * (3 - 2 * fx)
        val sy = fy * fy * (3 - 2 * fy)
        val a = at(x0, y0)
        val b = at(x0 + 1, y0)
        val c = at(x0, y0 + 1)
        val d = at(x0 + 1, y0 + 1)
        val top = a + (b - a) * sx
        val bottom = c + (d - c) * sx
        return top + (bottom - top) * sy
    }

    /** Сумма октав; координаты удваиваются, поэтому бесшовность сохраняется. */
    fun fbm(x: Float, y: Float, octaves: Int): Float {
        var sum = 0f
        var amp = 0.5f
        var norm = 0f
        var fx = x
        var fy = y
        repeat(octaves) {
            sum += sample(fx, fy) * amp
            norm += amp
            amp *= 0.5f
            fx *= 2
            fy *= 2
        }
        return sum / norm
    }

    private fun at(ix: Int, iy: Int): Float {
        val px = ((ix % periodX) + periodX) % periodX
        val py = ((iy % periodY) + periodY) % periodY
        return values[py * periodX + px]
    }
}
