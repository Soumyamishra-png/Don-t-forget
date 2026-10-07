package com.thingstoorder.app.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceParserTest {
    private fun p(text: String) = VoiceParser.parse(text)

    @Test
    fun designExampleSplitsAndTranslates() {
        assertEquals(
            listOf(ParsedItem("Milk", "2 L"), ParsedItem("Eggs", null), ParsedItem("Bread", null)),
            p("2 litre doodh, eggs aur bread"),
        )
    }

    @Test
    fun secondDesignExample() {
        assertEquals(
            listOf(ParsedItem("Atta", "5 kg"), ParsedItem("Paneer", null)),
            p("atta paanch kilo aur paneer"),
        )
    }

    @Test
    fun quantityCanComeAfterTheName() {
        assertEquals(listOf(ParsedItem("Milk", "2 L")), p("doodh 2 litre"))
    }

    @Test
    fun gluedNumbersAndUnitsAreRead() {
        assertEquals(listOf(ParsedItem("Sugar", "500 g")), p("500g sugar"))
        assertEquals(listOf(ParsedItem("Milk", "2 L")), p("2l milk"))
    }

    @Test
    fun halfAndDecimalQuantities() {
        assertEquals(listOf(ParsedItem("Sugar", "0.5 kg")), p("half kilo cheeni"))
        assertEquals(listOf(ParsedItem("Flour", "1.5 kg")), p("1.5 kg flour"))
    }

    @Test
    fun dozenWithoutANumberMeansOne() {
        assertEquals(listOf(ParsedItem("Eggs", "1 dozen")), p("a dozen eggs"))
    }

    @Test
    fun bareCountIsKeptButAmbiguousHindiWordsNeedAUnit() {
        assertEquals(listOf(ParsedItem("Apples", "3")), p("3 apples"))
        assertEquals(listOf(ParsedItem("Do something", null)), p("do something"))
    }

    @Test
    fun fillerWordsAreDropped() {
        assertEquals(listOf(ParsedItem("Milk", null), ParsedItem("Bread", null)), p("please add milk and bread"))
    }

    @Test
    fun emptyOrOnlyFillerGivesNothing() {
        assertEquals(emptyList<ParsedItem>(), p(""))
        assertEquals(emptyList<ParsedItem>(), p("please and also"))
    }

    @Test
    fun multiWordHindiPhrasesTranslate() {
        assertEquals(listOf(ParsedItem("Capsicum", "1 kg")), p("ek kilo shimla mirch"))
    }
}
