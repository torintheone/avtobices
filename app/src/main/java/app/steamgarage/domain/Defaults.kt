package app.steamgarage.domain

/**
 * Все числа по умолчанию — в одном месте (требование раздела «Указания для Claude Code»).
 * Меняются здесь, остальной код читает их отсюда.
 */
object Defaults {

    // ── Пороги предупреждения ──────────────────────────────────────────────

    /** Порог «Скоро» по умолчанию — 10 % интервала… */
    const val WARN_PERCENT = 10

    /** …но не меньше 500 км… */
    const val WARN_KM_MIN = 500

    /** …и не больше 2000 км. */
    const val WARN_KM_MAX = 2_000

    /** По времени — не меньше 7 дней… */
    const val WARN_DAYS_MIN = 7

    /** …и не больше 30 дней. */
    const val WARN_DAYS_MAX = 30

    /** Сколько дней считать в одном месяце интервала при расчёте порога в днях. */
    const val DAYS_PER_MONTH = 30

    // ── Пробег и прогноз ───────────────────────────────────────────────────

    /** Средний суточный пробег считается по показаниям за последние 90 дней… */
    const val AVERAGE_WINDOW_DAYS = 90

    /** …и только если показаний в этом окне хотя бы 2. */
    const val AVERAGE_MIN_READINGS = 2

    /** Напомнить «Обновите пробег», если показаний не было столько дней (меняется в настройках). */
    const val MILEAGE_REMINDER_DAYS = 14

    /** Максимум, который можно ввести на барабанах одометра (7 разрядов). */
    const val ODOMETER_MAX_KM = 9_999_999

    // ── Уведомления ────────────────────────────────────────────────────────

    /** Время ежедневной проверки сроков: 10:00. */
    const val CHECK_HOUR = 10
    const val CHECK_MINUTE = 0

    /** Допустимый сдвиг ежедневной проверки (окно WorkManager), минут. */
    const val CHECK_FLEX_MINUTES = 60L

    /** «Пора менять» повторяется раз в 7 дней, пока замену не записали. */
    const val OVERDUE_REPEAT_DAYS = 7

    /** «Напомнить через неделю» откладывает уведомления расходника на 7 дней. */
    const val SNOOZE_DAYS = 7

    // ── Синхронизация ──────────────────────────────────────────────────────

    /** Фоновая синхронизация при наличии сети — раз в 6 часов. */
    const val SYNC_PERIOD_HOURS = 6L

    /** Длина кода гаража: 19 случайных символов + 1 контрольный. */
    const val GARAGE_CODE_LENGTH = 20

    /** Код показывается группами по 4 символа: K7QM-2XRA-… */
    const val GARAGE_CODE_GROUP = 4

    // ── Звук и голос ───────────────────────────────────────────────────────

    /** Громкость звуков по умолчанию (0..1). */
    const val SOUND_VOLUME = 0.8f

    /** Высота голоса: чуть ниже обычного (1.0 — норма). */
    const val VOICE_PITCH = 0.85f

    /** Голосовое предупреждение при открытии приложения — не чаще раза в сутки. */
    const val VOICE_WARNING_INTERVAL_DAYS = 1

    // ── Шаблон расходников по умолчанию ────────────────────────────────────
    // Типовые значения; в интерфейсе подпись «проверьте по регламенту своей машины».
    // null — интервал не задан. Названия — в strings.xml (template_<key>).

    val TEMPLATE: List<TemplateEntry> = listOf(
        TemplateEntry("engine_oil", Category.OILS, km = 10_000, months = 12),
        TemplateEntry("oil_filter", Category.FILTERS, km = 10_000, months = 12),
        TemplateEntry("air_filter", Category.FILTERS, km = 20_000, months = 24),
        TemplateEntry("cabin_filter", Category.FILTERS, km = 15_000, months = 12),
        TemplateEntry("fuel_filter", Category.FILTERS, km = 30_000, months = null),
        TemplateEntry("spark_plugs", Category.IGNITION, km = 30_000, months = null),
        // Ремень ГРМ: интервал сильно зависит от мотора — пользователь заполняет по регламенту.
        TemplateEntry("timing_belt", Category.BELTS, km = null, months = null),
        TemplateEntry("drive_belt", Category.BELTS, km = 60_000, months = null),
        TemplateEntry("brake_fluid", Category.FLUIDS, km = null, months = 24),
        TemplateEntry("coolant", Category.FLUIDS, km = 60_000, months = 36),
        TemplateEntry("gearbox_oil", Category.OILS, km = 60_000, months = null),
        TemplateEntry("front_brake_pads", Category.BRAKES, km = 30_000, months = null),
        TemplateEntry("wiper_blades", Category.OTHER, km = null, months = 12),
    )
}

/** Строка шаблона: ключ названия в strings.xml, категория и интервалы. */
data class TemplateEntry(
    val key: String,
    val category: Category,
    val km: Int?,
    val months: Int?,
)
