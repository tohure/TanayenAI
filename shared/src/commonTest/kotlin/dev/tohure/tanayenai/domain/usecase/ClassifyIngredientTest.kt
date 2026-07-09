package dev.tohure.tanayenai.domain.usecase

import dev.tohure.tanayenai.domain.model.IngredientCategory
import dev.tohure.tanayenai.domain.model.classifyIngredient
import kotlin.test.Test
import kotlin.test.assertEquals

class ClassifyIngredientTest {
    @Test
    fun `pollo is proteins`() {
        assertEquals(IngredientCategory.PROTEINS, classifyIngredient("pechuga de pollo"))
    }

    @Test
    fun `avena is grains`() {
        assertEquals(IngredientCategory.GRAINS, classifyIngredient("avena"))
    }

    @Test
    fun `leche is dairy`() {
        assertEquals(IngredientCategory.DAIRY, classifyIngredient("leche entera"))
    }

    @Test
    fun `manzana is fruits`() {
        assertEquals(IngredientCategory.FRUITS, classifyIngredient("manzana verde"))
    }

    @Test
    fun `unknown is other`() {
        assertEquals(IngredientCategory.OTHER, classifyIngredient("xantana"))
    }

    @Test
    fun `classification is case insensitive`() {
        assertEquals(IngredientCategory.PROTEINS, classifyIngredient("POLLO"))
        assertEquals(IngredientCategory.DAIRY, classifyIngredient("LECHE"))
    }

    // ── Regresión: matching por palabra completa, no subcadena ──────────────────

    @Test
    fun `fresa is fruits not proteins`() {
        // "fresa" contiene "res" (carne de res) — no debe caer en PROTEINS.
        assertEquals(IngredientCategory.FRUITS, classifyIngredient("Fresas"))
    }

    @Test
    fun `chocolate is not vegetables`() {
        // "chocolate" contiene "col" — no debe caer en VEGETABLES.
        assertEquals(IngredientCategory.OTHER, classifyIngredient("Grajeas de chocolate"))
    }

    @Test
    fun `ajonjoli is nuts not vegetables`() {
        // "ajonjolí" contiene "ajo" — no debe caer en VEGETABLES.
        assertEquals(IngredientCategory.NUTS, classifyIngredient("Roscas de ajonjolí"))
    }

    @Test
    fun `carne de res still proteins`() {
        assertEquals(IngredientCategory.PROTEINS, classifyIngredient("carne de res"))
    }

    @Test
    fun `plural matches singular keyword`() {
        assertEquals(IngredientCategory.PROTEINS, classifyIngredient("huevos"))
        assertEquals(IngredientCategory.LEGUMES, classifyIngredient("frijoles"))
    }
}
