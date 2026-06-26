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
}
