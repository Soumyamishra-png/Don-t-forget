package com.thingstoorder.app.voice

data class ParsedItem(val name: String, val quantity: String?)

object VoiceParser {
    private val separators = Regex("""\s*(?:,|;|&|\+|\band\b|\baur\b|\bplus\b|\btatha\b|और)\s*""")
    private val glued = Regex("""(\d)([a-z]+)""")

    private val fillers = setOf(
        "please", "also", "add", "buy", "get", "need", "some", "a", "an", "the", "of",
        "ka", "ki", "ke", "chahiye", "lena", "lana", "hai", "mujhe", "hume", "humein", "bhi",
    )

    private val numberWords = mapOf(
        "one" to 1.0, "two" to 2.0, "three" to 3.0, "four" to 4.0, "five" to 5.0, "six" to 6.0,
        "seven" to 7.0, "eight" to 8.0, "nine" to 9.0, "ten" to 10.0, "half" to 0.5, "quarter" to 0.25,
        "ek" to 1.0, "do" to 2.0, "teen" to 3.0, "char" to 4.0, "chaar" to 4.0, "paanch" to 5.0,
        "panch" to 5.0, "paach" to 5.0, "chhe" to 6.0, "chheh" to 6.0, "saat" to 7.0, "aath" to 8.0,
        "nau" to 9.0, "das" to 10.0, "aadha" to 0.5, "adha" to 0.5, "dedh" to 1.5, "dhai" to 2.5,
    )

    // Hindi words that are also English words, so they only count as a number when a unit follows.
    private val needsUnit = setOf("do")

    private val units = mapOf(
        "kg" to "kg", "kgs" to "kg", "kilo" to "kg", "kilos" to "kg", "kilogram" to "kg", "kilograms" to "kg",
        "g" to "g", "gm" to "g", "gms" to "g", "gram" to "g", "grams" to "g",
        "l" to "L", "lit" to "L", "ltr" to "L", "litre" to "L", "litres" to "L", "liter" to "L", "liters" to "L",
        "ml" to "ml", "millilitre" to "ml", "millilitres" to "ml",
        "dozen" to "dozen", "darjan" to "dozen",
        "packet" to "pack", "packets" to "pack", "pack" to "pack", "packs" to "pack",
        "piece" to "pcs", "pieces" to "pcs", "pcs" to "pcs",
    )

    fun parse(text: String): List<ParsedItem> =
        text.lowercase()
            .replace(glued, "$1 $2")
            .split(separators)
            .mapNotNull { parseSegment(it.trim()) }

    private fun parseSegment(segment: String): ParsedItem? {
        val tokens = segment.split(Regex("\\s+")).filter { it.isNotEmpty() && it !in fillers }.toMutableList()
        if (tokens.isEmpty()) return null

        var quantity: String? = null
        val numberIndex = tokens.indexOfFirst { numberValue(it) != null }
        if (numberIndex >= 0) {
            val token = tokens[numberIndex]
            val value = numberValue(token)!!
            val unit = tokens.getOrNull(numberIndex + 1)?.let { units[it] }
            if (unit != null) {
                quantity = "${format(value)} $unit"
                tokens.removeAt(numberIndex + 1)
                tokens.removeAt(numberIndex)
            } else if (token !in needsUnit) {
                quantity = format(value)
                tokens.removeAt(numberIndex)
            }
        } else {
            val dozen = tokens.indexOfFirst { it == "dozen" || it == "darjan" }
            if (dozen >= 0) {
                quantity = "1 dozen"
                tokens.removeAt(dozen)
            }
        }

        val name = HindiGroceries.translate(tokens.joinToString(" "))
        return if (name.isBlank()) null else ParsedItem(name, quantity)
    }

    private fun numberValue(token: String): Double? = token.toDoubleOrNull() ?: numberWords[token]

    private fun format(value: Double): String =
        if (value == Math.floor(value)) value.toLong().toString() else value.toString().trimEnd('0').trimEnd('.')
}

object HindiGroceries {
    private val words = mapOf(
        "doodh" to "Milk", "dudh" to "Milk", "dood" to "Milk", "pyaj" to "Onions", "dahi" to "Curd", "chawal" to "Rice", "aloo" to "Potatoes", "alu" to "Potatoes",
        "pyaaz" to "Onions", "pyaz" to "Onions", "tamatar" to "Tomatoes", "adrak" to "Ginger",
        "lehsun" to "Garlic", "lahsun" to "Garlic", "mirch" to "Chilli", "dhania" to "Coriander",
        "nimbu" to "Lemon", "kela" to "Bananas", "kele" to "Bananas", "seb" to "Apples",
        "santra" to "Oranges", "aam" to "Mangoes", "anda" to "Eggs", "ande" to "Eggs", "makhan" to "Butter",
        "cheeni" to "Sugar", "chini" to "Sugar", "namak" to "Salt", "chai" to "Tea", "nariyal" to "Coconut",
        "tel" to "Oil", "bhindi" to "Okra", "gobhi" to "Cauliflower", "palak" to "Spinach",
        "matar" to "Peas", "gajar" to "Carrots", "mooli" to "Radish", "kheera" to "Cucumber",
        "baingan" to "Brinjal", "baigan" to "Brinjal", "sabzi" to "Vegetables", "sabji" to "Vegetables",
        "phal" to "Fruits", "roti" to "Roti", "sabun" to "Soap", "biscuit" to "Biscuits",
        "paani" to "Water", "pani" to "Water", "jeera" to "Cumin", "haldi" to "Turmeric",
    )
    private val phrases = mapOf(
        "shimla mirch" to "Capsicum", "chai patti" to "Tea", "hari mirch" to "Green chilli",
        "lal mirch" to "Red chilli", "kaali mirch" to "Black pepper",
    )

    fun translate(name: String): String {
        var text = name.trim()
        for ((phrase, english) in phrases) text = text.replace(Regex("\\b$phrase\\b"), english)
        val out = text.split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ") { words[it] ?: it }
        return out.replaceFirstChar { it.uppercase() }
    }
}
