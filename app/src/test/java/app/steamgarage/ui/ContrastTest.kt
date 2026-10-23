package app.steamgarage.ui

import androidx.compose.ui.graphics.Color
import app.steamgarage.ui.theme.DarkPalette
import app.steamgarage.ui.theme.LightPalette
import app.steamgarage.ui.theme.SteamPalette
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

/** Все пары «текст — фон» темы дают контраст не ниже 4,5:1 (WCAG AA), как требует SPEC.md. */
class ContrastTest {

    @Test
    fun darkThemeMeetsWcagAa() = assertPairs(DarkPalette)

    @Test
    fun lightThemeMeetsWcagAa() = assertPairs(LightPalette)

    private fun assertPairs(p: SteamPalette) {
        val titleOnBackground = if (p.isDark) p.brassLight else p.text
        val pairs = listOf(
            Triple("текст на фоне", p.text, p.background),
            Triple("текст на карточке", p.text, p.surface),
            Triple("текст на утопленной панели", p.text, p.surfaceDeep),
            Triple("подпись на фоне", p.textSecondary, p.background),
            Triple("подпись на карточке", p.textSecondary, p.surface),
            Triple("заголовок на фоне", titleOnBackground, p.background),
            Triple("гравировка на тёмной латуни", p.engraved, p.brassDark),
            Triple("гравировка на светлой латуни", p.engraved, p.brassLight),
            Triple("выбранная вкладка", p.brassLight, p.navSelected),
            Triple("бейдж «Норма»", p.onOk, p.ok),
            Triple("бейдж «Скоро»", p.onSoon, p.soon),
            Triple("бейдж «Пора менять»", p.onOverdue, p.overdue),
            Triple("бейдж «Нет данных»", p.onNoData, p.noData),
            Triple("цифры на циферблате", p.gaugeInk, p.gaugeFace),
            Triple("цифры одометра", p.odometerDigit, p.odometerWindow),
            Triple("ошибка на фоне", p.errorText, p.background),
            Triple("ошибка на карточке", p.errorText, p.surface),
        )
        val failures = pairs.mapNotNull { (name, fg, bg) ->
            val ratio = contrast(fg, bg)
            if (ratio < 4.5) "$name: ${"%.2f".format(ratio)}:1" else null
        }
        assertTrue("Недостаточный контраст (${if (p.isDark) "тёмная" else "дневная"} тема): $failures", failures.isEmpty())
    }

    /** Относительная яркость по WCAG 2.x. */
    private fun luminance(c: Color): Double {
        fun channel(v: Float): Double {
            val d = v.toDouble()
            return if (d <= 0.04045) d / 12.92 else ((d + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }
}
