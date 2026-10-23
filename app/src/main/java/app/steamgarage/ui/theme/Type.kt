package app.steamgarage.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.steamgarage.R

/** Заголовки: Playfair Display SC (OFL, полная кириллица). */
val HeadingFamily = FontFamily(
    Font(R.font.playfair_sc_regular, FontWeight.Normal),
    Font(R.font.playfair_sc_bold, FontWeight.Bold),
)

/** Основной текст: Old Standard TT (OFL, полная кириллица). */
val BodyFamily = FontFamily(
    Font(R.font.old_standard_regular, FontWeight.Normal),
    Font(R.font.old_standard_bold, FontWeight.Bold),
    Font(R.font.old_standard_italic, FontWeight.Normal, FontStyle.Italic),
)

/** Цифры одометра и манометров: PT Mono (OFL, полная кириллица). */
val NumberFamily = FontFamily(
    Font(R.font.pt_mono, FontWeight.Normal),
)

/** Текст не мельче 14 sp (раздел «Удобство важнее декора»); размеры в sp учитывают системный масштаб. */
val SteamTypography = Typography(
    displayLarge = TextStyle(fontFamily = HeadingFamily, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 48.sp),
    displayMedium = TextStyle(fontFamily = HeadingFamily, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 42.sp),
    displaySmall = TextStyle(fontFamily = HeadingFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp),
    headlineLarge = TextStyle(fontFamily = HeadingFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = HeadingFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = HeadingFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = HeadingFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = HeadingFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = HeadingFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodySmall = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    labelMedium = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
)

/** Дополнительные стили, которых нет в Material 3. */
@Immutable
data class SteamTextStyles(
    /** Крупные цифры одометра. */
    val odometer: TextStyle = TextStyle(fontFamily = NumberFamily, fontSize = 26.sp, lineHeight = 30.sp),
    /** Цифры в небольших окнах (манометр карточки, мини-одометр). */
    val odometerSmall: TextStyle = TextStyle(fontFamily = NumberFamily, fontSize = 18.sp, lineHeight = 22.sp),
    /** Прочие числа в тексте: даты, суммы, артикулы. */
    val number: TextStyle = TextStyle(fontFamily = NumberFamily, fontSize = 16.sp, lineHeight = 22.sp),
    /** Надпись на латунной табличке. */
    val plateTitle: TextStyle = TextStyle(fontFamily = HeadingFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    /** Подпись под надписью на табличке. */
    val plateCaption: TextStyle = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
)
