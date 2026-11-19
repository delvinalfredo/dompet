package id.dompetbersama.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SourcePrefsTest {

    @Test
    fun newDefaultReachesExistingUserButTheirChoicesWin() {
        val defaults = setOf("bank.a", "bank.b", "bank.new")
        val enabled = resolveEnabled(defaults, userOn = setOf("chat.app"), userOff = setOf("bank.b"))
        assertEquals(setOf("bank.a", "bank.new", "chat.app"), enabled)
    }
}
