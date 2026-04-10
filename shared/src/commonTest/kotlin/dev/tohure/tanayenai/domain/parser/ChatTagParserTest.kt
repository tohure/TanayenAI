package dev.tohure.tanayenai.domain.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChatTagParserTest {
    // ── extractPantryIngredients ──────────────────────────────────────────────

    @Test
    fun extractPantryIngredients_returnsIngredientsSplitByPipe() {
        val response = "Veo que compraste avena y almendras. [PANTRY:avena|almendras|yogur griego]"
        val result = ChatTagParser.extractPantryIngredients(response)
        assertEquals(listOf("avena", "almendras", "yogur griego"), result)
    }

    @Test
    fun extractPantryIngredients_trimsWhitespaceAroundEachIngredient() {
        val response = "[PANTRY: avena | almendras | yogur griego ]"
        val result = ChatTagParser.extractPantryIngredients(response)
        assertEquals(listOf("avena", "almendras", "yogur griego"), result)
    }

    @Test
    fun extractPantryIngredients_returnsNullWhenNoTagPresent() {
        val response = "Te recomiendo cereal A porque tiene menos azúcar."
        assertNull(ChatTagParser.extractPantryIngredients(response))
    }

    @Test
    fun extractPantryIngredients_handlesSingleIngredient() {
        val response = "[PANTRY:avena]"
        assertEquals(listOf("avena"), ChatTagParser.extractPantryIngredients(response))
    }

    @Test
    fun extractPantryIngredients_pipePreservesIngredientNamesWithCommas() {
        // Con pipe como separador, "sal, pimienta" es UN ingrediente
        val response = "[PANTRY:sal, pimienta|aceite de oliva]"
        assertEquals(listOf("sal, pimienta", "aceite de oliva"), ChatTagParser.extractPantryIngredients(response))
    }

    // ── extractRecAction ──────────────────────────────────────────────────────

    @Test
    fun extractRecAction_parsesTypeTitleAndIngredients() {
        val response = "Aquí va mi respuesta. [REC:MEAL:Avena con frutas:avena|plátano|miel]"
        val result = ChatTagParser.extractRecAction(response)
        assertEquals("MEAL", result?.type)
        assertEquals("Avena con frutas", result?.title)
        assertEquals(listOf("avena", "plátano", "miel"), result?.ingredients)
    }

    @Test
    fun extractRecAction_normalizesTypeToUppercase() {
        val response = "[REC:snack:Nueces:nueces|almendras]"
        assertEquals("SNACK", ChatTagParser.extractRecAction(response)?.type)
    }

    @Test
    fun extractRecAction_returnsNullWhenNoTagPresent() {
        assertNull(ChatTagParser.extractRecAction("Respuesta sin tag."))
    }

    @Test
    fun extractRecAction_handlesIngredientNamesWithSpaces() {
        val response = "[REC:MEAL:Ensalada mediterránea:aceite de oliva|tomate cherry|queso feta]"
        val result = ChatTagParser.extractRecAction(response)
        assertEquals(listOf("aceite de oliva", "tomate cherry", "queso feta"), result?.ingredients)
    }

    // ── stripTags ─────────────────────────────────────────────────────────────

    @Test
    fun stripTags_removesPantryTagFromResponse() {
        val response = "Veo que compraste avena. [PANTRY:avena|almendras]"
        assertEquals("Veo que compraste avena.", ChatTagParser.stripTags(response))
    }

    @Test
    fun stripTags_removesRecTagFromResponse() {
        val response = "Te recomiendo avena. [REC:MEAL:Avena con frutas:avena|plátano]"
        assertEquals("Te recomiendo avena.", ChatTagParser.stripTags(response))
    }

    @Test
    fun stripTags_leavesTextUnchangedWhenNoTagsPresent() {
        val response = "Respuesta sin ningún tag."
        assertEquals(response, ChatTagParser.stripTags(response))
    }

    // ── extractClinicalJson ───────────────────────────────────────────────────

    @Test
    fun extractClinicalJson_returnsJsonFromTag() {
        val response = "Tu glucosa está bien. [CLINICAL:{\"fasting_glucose\":94}]"
        assertEquals("{\"fasting_glucose\":94}", ChatTagParser.extractClinicalJson(response))
    }

    @Test
    fun extractClinicalJson_returnsJsonWithMultipleFields() {
        val response = "Guardé tu presión. [CLINICAL:{\"systolic_pressure\":125,\"diastolic_pressure\":82}]"
        assertEquals(
            "{\"systolic_pressure\":125,\"diastolic_pressure\":82}",
            ChatTagParser.extractClinicalJson(response),
        )
    }

    @Test
    fun extractClinicalJson_returnsNullWhenNoTagPresent() {
        assertNull(ChatTagParser.extractClinicalJson("Respuesta sin tag clínico."))
    }

    @Test
    fun stripTags_removesClinicalTagFromResponse() {
        val response = "Tu glucosa está en rango normal. [CLINICAL:{\"fasting_glucose\":94}]"
        assertEquals("Tu glucosa está en rango normal.", ChatTagParser.stripTags(response))
    }

    @Test
    fun stripForStreaming_hidesClinicalTagDuringStream() {
        val partialResponse = "Tu glucosa está bien. [CLINICAL"
        assertEquals("Tu glucosa está bien.", ChatTagParser.stripForStreaming(partialResponse))
    }

    // ── stripForStreaming ─────────────────────────────────────────────────────

    @Test
    fun stripForStreaming_hidesPartialPantryTagDuringStream() {
        val partialResponse = "Veo que compraste avena. [PANTRY"
        val result = ChatTagParser.stripForStreaming(partialResponse)
        assertEquals("Veo que compraste avena.", result)
    }

    @Test
    fun stripForStreaming_hidesPartialRecTagDuringStream() {
        val partialResponse = "Te recomiendo avena. [RE"
        val result = ChatTagParser.stripForStreaming(partialResponse)
        assertEquals("Te recomiendo avena.", result)
    }

    @Test
    fun stripForStreaming_removesCompleteTagOnceStreamEnds() {
        val full = "Aquí tu respuesta. [PANTRY:avena|almendras]"
        assertEquals("Aquí tu respuesta.", ChatTagParser.stripForStreaming(full))
    }
}
