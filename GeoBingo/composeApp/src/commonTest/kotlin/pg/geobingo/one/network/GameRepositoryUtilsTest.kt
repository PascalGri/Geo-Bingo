package pg.geobingo.one.network

import androidx.compose.ui.graphics.Color
import kotlin.test.*

class GameRepositoryUtilsTest {

    @Test
    fun generateCode_length6() {
        val code = generateCode()
        assertEquals(6, code.length)
    }

    @Test
    fun generateCode_validCharacters() {
        val validChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toSet()
        repeat(20) {
            val code = generateCode()
            code.forEach { char ->
                assertTrue(char in validChars, "Invalid char '$char' in code '$code'")
            }
        }
    }

    @Test
    fun generateCode_excludesAmbiguousChars() {
        // Should not contain I, O, 0, 1 (ambiguous characters)
        repeat(100) {
            val code = generateCode()
            assertFalse('I' in code)
            assertFalse('O' in code)
            assertFalse('0' in code)
            assertFalse('1' in code)
        }
    }

    @Test
    fun colorToHex_basic() {
        assertEquals("#ff0000", Color.Red.toHex())
        assertEquals("#00ff00", Color.Green.toHex())
        assertEquals("#0000ff", Color.Blue.toHex())
    }

    @Test
    fun parseHexColor_validColors() {
        val red = parseHexColor("#ff0000")
        assertEquals(1f, red.red, 0.01f)
        assertEquals(0f, red.green, 0.01f)
        assertEquals(0f, red.blue, 0.01f)
    }

    @Test
    fun parseHexColor_withoutHash() {
        val blue = parseHexColor("0000ff")
        assertEquals(0f, blue.red, 0.01f)
        assertEquals(0f, blue.green, 0.01f)
        assertEquals(1f, blue.blue, 0.01f)
    }

    @Test
    fun parseHexColor_invalid_returnsFallback() {
        val result = parseHexColor("invalid")
        // Should return first player color as fallback
        assertNotNull(result)
    }

    @Test
    fun colorRoundTrip() {
        val original = Color(0xFFEC4899)
        val hex = original.toHex()
        val parsed = parseHexColor(hex)
        assertEquals(original.red, parsed.red, 0.01f)
        assertEquals(original.green, parsed.green, 0.01f)
        assertEquals(original.blue, parsed.blue, 0.01f)
    }

    @Test
    fun voteKeys_stepKey() {
        val key = VoteKeys.stepKey("cat1", "player1")
        assertEquals("cat1__player1", key)
    }

    @Test
    fun voteKeys_constants() {
        assertEquals("__end_vote__", VoteKeys.END_VOTE)
        assertEquals("__all_captured__", VoteKeys.ALL_CAPTURED)
    }
}
