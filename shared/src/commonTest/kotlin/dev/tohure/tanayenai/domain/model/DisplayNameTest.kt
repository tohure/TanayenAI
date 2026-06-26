package dev.tohure.tanayenai.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class DisplayNameTest {
    @Test
    fun `resolveDisplayName_blank_returnsHola`() {
        assertEquals("Hola", resolveDisplayName(""))
        assertEquals("Hola", resolveDisplayName("   "))
    }

    @Test
    fun `resolveDisplayName_oneWord_returnsAsIs`() {
        assertEquals("Carlo", resolveDisplayName("Carlo"))
        assertEquals("Carlo", resolveDisplayName("  Carlo  "))
    }

    @Test
    fun `resolveDisplayName_twoShortWords_returnsFullName`() {
        assertEquals("Ana López", resolveDisplayName("Ana López"))
        assertEquals("Carlo H", resolveDisplayName("Carlo H"))
    }

    @Test
    fun `resolveDisplayName_twoLongWords_truncatesToFirstAndInitial`() {
        assertEquals("Bartholomew R.", resolveDisplayName("Bartholomew Richardson"))
    }

    @Test
    fun `resolveDisplayName_threeOrMoreWords_truncatesToFirstAndLastInitial`() {
        assertEquals("Carlo T.", resolveDisplayName("Carlo Renzo Huaman Torres"))
        assertEquals("Carlo H.", resolveDisplayName("Carlo Renzo Huaman"))
        assertEquals("Juan G.", resolveDisplayName("Juan Carlos Garcia"))
    }

    @Test
    fun `resolveDisplayName_extraSpacesBetweenWords_normalizes`() {
        assertEquals("Carlo T.", resolveDisplayName("Carlo  Renzo  Huaman  Torres"))
    }
}
