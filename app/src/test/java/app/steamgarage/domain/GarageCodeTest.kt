package app.steamgarage.domain

import java.security.SecureRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GarageCodeTest {

    @Test
    fun generatedCodeIsValid() {
        repeat(100) {
            val code = GarageCode.generate()
            assertEquals(20, code.length)
            assertTrue(code.all { it in GarageCode.ALPHABET })
            assertEquals(code, GarageCode.normalize(code))
        }
    }

    @Test
    fun formatAndNormalizeRoundTrip() {
        val code = GarageCode.generate(SecureRandom.getInstance("SHA1PRNG").apply { setSeed(7) })
        val shown = GarageCode.format(code)
        assertEquals(24, shown.length)
        assertEquals(4, shown.indexOf('-'))
        assertEquals(code, GarageCode.normalize(shown))
        assertEquals(code, GarageCode.normalize(" ${shown.lowercase()} "))
    }

    @Test
    fun lookalikeLettersAreAccepted() {
        val code = "0" + GarageCode.generate().drop(1)
        val fixed = code.dropLast(1) + GarageCode.checkChar(code.dropLast(1))
        assertNotNull(GarageCode.normalize(fixed.replaceFirst('0', 'O')))
    }

    @Test
    fun typosAreRejected() {
        val code = GarageCode.generate()
        // Замена одного символа.
        val i = 5
        val replacement = GarageCode.ALPHABET.first { it != code[i] && it != '0' && code[i] != '0' }
        assertNull(GarageCode.normalize(code.substring(0, i) + replacement + code.substring(i + 1)))
        // Неверная длина и чужие символы.
        assertNull(GarageCode.normalize(code.dropLast(1)))
        assertNull(GarageCode.normalize(code.dropLast(1) + "U"))
        assertNull(GarageCode.normalize("привет"))
    }
}
