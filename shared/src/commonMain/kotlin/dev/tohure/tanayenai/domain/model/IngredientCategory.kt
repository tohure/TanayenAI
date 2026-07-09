package dev.tohure.tanayenai.domain.model

enum class IngredientCategory(
    val displayName: String,
    val emoji: String,
) {
    PROTEINS("Proteínas", "🥩"),
    DAIRY("Lácteos y huevos", "🥛"),
    VEGETABLES("Verduras", "🥦"),
    FRUITS("Frutas", "🍎"),
    GRAINS("Granos y cereales", "🌾"),
    LEGUMES("Legumbres", "🫘"),
    NUTS("Frutos secos", "🥜"),
    OILS("Aceites y grasas", "🫙"),
    CONDIMENTS("Condimentos", "🧂"),
    BEVERAGES("Bebidas", "🧃"),
    FROZEN("Congelados", "🧊"),
    SUPPLEMENTS("Suplementos", "💊"),
    OTHER("Otros", "📦"),
}

/**
 * Clasifica un nombre de ingrediente en una categoría.
 * Lookup por palabras clave — cubre el ~90% de ingredientes comunes en español.
 */
fun classifyIngredient(name: String): IngredientCategory {
    val lower = name.lowercase().trim()
    return when {
        lower.containsAny(
            "pollo",
            "res",
            "carne",
            "atún",
            "salmón",
            "pescado",
            "camarón",
            "cerdo",
            "pavo",
            "whey",
            "huevo",
        ) -> IngredientCategory.PROTEINS

        lower.containsAny(
            "leche",
            "yogur",
            "queso",
            "mantequilla",
            "crema",
            "kéfir",
            "lácteo",
        ) -> IngredientCategory.DAIRY

        lower.containsAny(
            "lechuga",
            "espinaca",
            "brócoli",
            "zanahoria",
            "tomate",
            "cebolla",
            "ajo",
            "pimiento",
            "calabaza",
            "pepino",
            "apio",
            "acelga",
            "col",
            "repollo",
            "espárrago",
            "champiñón",
            "hongo",
            "verdura",
            "vegetal",
        ) -> IngredientCategory.VEGETABLES

        lower.containsAny(
            "manzana",
            "plátano",
            "banana",
            "naranja",
            "limón",
            "fresa",
            "arándano",
            "mango",
            "piña",
            "uva",
            "pera",
            "durazno",
            "aguacate",
            "fruta",
        ) -> IngredientCategory.FRUITS

        lower.containsAny(
            "arroz",
            "avena",
            "quinoa",
            "trigo",
            "pan",
            "pasta",
            "harina",
            "cereal",
            "granola",
            "maíz",
            "tortilla",
            "galleta",
        ) -> IngredientCategory.GRAINS

        lower.containsAny(
            "frijol",
            "lenteja",
            "garbanzo",
            "chícharo",
            "soya",
            "tofu",
            "edamame",
            "legumbre",
        ) -> IngredientCategory.LEGUMES

        lower.containsAny(
            "almendra",
            "nuez",
            "cacahuate",
            "chía",
            "linaza",
            "ajonjolí",
            "semilla",
            "pistacho",
        ) -> IngredientCategory.NUTS

        lower.containsAny("aceite", "oliva", "coco", "manteca", "ghee") -> IngredientCategory.OILS

        lower.containsAny(
            "sal",
            "pimienta",
            "canela",
            "cúrcuma",
            "comino",
            "orégano",
            "vinagre",
            "salsa",
            "mostaza",
            "miel",
            "azúcar",
            "condimento",
            "especia",
        ) -> IngredientCategory.CONDIMENTS

        lower.containsAny("agua", "jugo", "té", "café", "bebida", "electrolito") -> IngredientCategory.BEVERAGES

        lower.containsAny("congelado", "helado") -> IngredientCategory.FROZEN

        lower.containsAny(
            "vitamina",
            "mineral",
            "suplemento",
            "omega",
            "colágeno",
            "magnesio",
            "zinc",
            "proteína en polvo",
        ) -> IngredientCategory.SUPPLEMENTS

        else -> IngredientCategory.OTHER
    }
}

private val wordSplit = Regex("[^\\p{L}]+")

/**
 * Matchea por PALABRA COMPLETA (con tolerancia a plural -s/-es), no por subcadena.
 * Evita falsos positivos clásicos: "fresa" contiene "res" (carne), "chocolate" contiene
 * "col", "ajonjolí" contiene "ajo". Los keywords multipalabra ("proteína en polvo") caen
 * a búsqueda por subcadena porque no son un token único.
 */
private fun String.containsAny(vararg keywords: String): Boolean {
    val lower = lowercase()
    val words = lower.split(wordSplit).filter { it.isNotBlank() }
    return keywords.any { keyword ->
        val k = keyword.lowercase()
        if (k.contains(' ')) {
            lower.contains(k)
        } else {
            words.any { w -> w == k || w == "${k}s" || w == "${k}es" }
        }
    }
}
