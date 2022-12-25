import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class MarkupTextStringTest {

    @Test
    fun shouldNotInitWithAsciidocInlineBlocks() {
        assertThrows(Exception::class.java) {
            val str = AdocDSLText("Paragraph `monospaced`")
        }
    }

    @Test
    fun shouldInitWithNoAsciidocInlineBlocks() {
        val str = AdocDSLText("Paragraph with no monospaced")
    }
}
