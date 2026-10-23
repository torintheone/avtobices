package app.steamgarage.domain

import java.security.SecureRandom

/**
 * Код приглашения в гараж: 20 символов алфавита Crockford Base32 (без похожих I, L, O, U),
 * последний — контрольный: опечатка в коде распознаётся сразу, без обращения к облаку.
 */
object GarageCode {
    const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private const val CHECK_MODULUS = 31

    fun generate(random: SecureRandom = SecureRandom()): String {
        val body = buildString {
            repeat(Defaults.GARAGE_CODE_LENGTH - 1) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
        }
        return body + checkChar(body)
    }

    /** Контрольный символ: взвешенная сумма по модулю простого числа ловит замену и перестановку соседних символов. */
    fun checkChar(body: String): Char {
        var sum = 0
        body.forEachIndexed { index, c -> sum += (index + 1) * ALPHABET.indexOf(c) }
        return ALPHABET[sum % CHECK_MODULUS]
    }

    /**
     * Приводит введённый текст к коду: регистр, дефисы и пробелы не важны, O читается как 0, I и L — как 1.
     * null — это не код гаража (не та длина, чужие символы или не сходится контрольный символ).
     */
    fun normalize(input: String): String? {
        val code = input.uppercase()
            .filterNot { it == '-' || it.isWhitespace() }
            .map {
                when (it) {
                    'O' -> '0'
                    'I', 'L' -> '1'
                    else -> it
                }
            }
            .joinToString("")
        if (code.length != Defaults.GARAGE_CODE_LENGTH) return null
        if (code.any { it !in ALPHABET }) return null
        val body = code.dropLast(1)
        return if (checkChar(body) == code.last()) code else null
    }

    /** Группы по 4 через дефис: K7QM-2XRA-… */
    fun format(code: String): String = code.chunked(Defaults.GARAGE_CODE_GROUP).joinToString("-")
}
