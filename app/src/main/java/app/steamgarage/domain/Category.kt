package app.steamgarage.domain

/** Категории расходников. Категория определяет иконку и цвет таблички. */
enum class Category {
    OILS,
    FILTERS,
    FLUIDS,
    IGNITION,
    BELTS,
    BRAKES,
    TIRES,
    ELECTRICS,
    OTHER,
    ;

    companion object {
        /** Безопасное чтение из базы или облака: неизвестное значение превращается в «Другое». */
        fun parse(value: String?): Category = entries.firstOrNull { it.name == value } ?: OTHER
    }
}
