package app.steamgarage.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Токены цвета «Парового гаража». Значения — из раздела «Стимпанк-стиль» SPEC.md.
 * Пары «текст — фон» проверяются тестом ContrastTest (не ниже 4,5:1, WCAG AA).
 */
@Immutable
data class SteamPalette(
    val isDark: Boolean,
    /** Фон экранов: тёмное дерево или пергамент. */
    val background: Color,
    /** Карточки и панели. */
    val surface: Color,
    /** Утопленные поверхности: окна одометра, пазы рычагов. */
    val surfaceDeep: Color,
    /** Латунь: начало и конец градиента, тень кромки. */
    val brassDark: Color,
    val brassLight: Color,
    val brassShadow: Color,
    /** Медь: акцент и рычаги (только графика, не текст). */
    val copper: Color,
    val copperLight: Color,
    val text: Color,
    val textSecondary: Color,
    /** «Гравированный» текст на латуни и его светлая кромка. */
    val engraved: Color,
    val engravedHighlight: Color,
    /** Цвета статусов: зоны манометра и бейджи. */
    val ok: Color,
    val soon: Color,
    val overdue: Color,
    val noData: Color,
    /** Текст поверх бейджа соответствующего статуса. */
    val onOk: Color,
    val onSoon: Color,
    val onOverdue: Color,
    val onNoData: Color,
    /** Циферблат манометра и краска рисок/цифр на нём. */
    val gaugeFace: Color,
    val gaugeInk: Color,
    /** Окно одометра и цифры в нём. */
    val odometerWindow: Color,
    val odometerDigit: Color,
    /** Воронёная сталь: стрелки, заклёпки в тени. */
    val iron: Color,
    /** Утопленная плашка выбранной вкладки на латунной панели (подпись — [brassLight]). */
    val navSelected: Color,
    /** Текст ошибки в полях ввода (эмаль слишком тёмная для текста на тёмном дереве). */
    val errorText: Color,
)

val DarkPalette = SteamPalette(
    isDark = true,
    background = Color(0xFF1E1611),
    surface = Color(0xFF2B2019),
    surfaceDeep = Color(0xFF15100C),
    brassDark = Color(0xFFB5A642),
    brassLight = Color(0xFFE1C16E),
    brassShadow = Color(0xFF6E5F22),
    copper = Color(0xFFB87333),
    copperLight = Color(0xFFD9955A),
    text = Color(0xFFE8DCC4),
    textSecondary = Color(0xFFB9A88A),
    engraved = Color(0xFF2B1D0E),
    engravedHighlight = Color(0x80FFF4D0),
    ok = Color(0xFF43B3AE),
    soon = Color(0xFFD9822B),
    overdue = Color(0xFFA4161A),
    noData = Color(0xFF7A7D80),
    onOk = Color(0xFF0B0806),
    onSoon = Color(0xFF0B0806),
    onOverdue = Color(0xFFF5EEDD),
    onNoData = Color(0xFF000000),
    gaugeFace = Color(0xFFEDE3C8),
    gaugeInk = Color(0xFF2B1D0E),
    odometerWindow = Color(0xFF120D0A),
    odometerDigit = Color(0xFFEDE3C8),
    iron = Color(0xFF26211E),
    navSelected = Color(0xFF3D3414),
    errorText = Color(0xFFF28B82),
)

val LightPalette = SteamPalette(
    isDark = false,
    background = Color(0xFFEFE4CC),
    surface = Color(0xFFE2D3B3),
    surfaceDeep = Color(0xFFD3C29E),
    brassDark = Color(0xFFB5A642),
    brassLight = Color(0xFFE1C16E),
    brassShadow = Color(0xFF6A5A1E),
    copper = Color(0xFF9A5B28),
    copperLight = Color(0xFFB87333),
    text = Color(0xFF3B2A1A),
    textSecondary = Color(0xFF6B5640),
    engraved = Color(0xFF2B1D0E),
    engravedHighlight = Color(0x80FFF4D0),
    ok = Color(0xFF2E7F7B),
    soon = Color(0xFFB5651D),
    overdue = Color(0xFF8E1216),
    noData = Color(0xFF5E6164),
    onOk = Color(0xFFFFFFFF),
    onSoon = Color(0xFF000000),
    onOverdue = Color(0xFFFFFFFF),
    onNoData = Color(0xFFFFFFFF),
    gaugeFace = Color(0xFFF6EFDC),
    gaugeInk = Color(0xFF2B1D0E),
    odometerWindow = Color(0xFF1E1611),
    odometerDigit = Color(0xFFEDE3C8),
    iron = Color(0xFF2E2824),
    navSelected = Color(0xFF3D3414),
    errorText = Color(0xFF8E1216),
)
