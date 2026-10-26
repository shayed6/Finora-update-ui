package com.example

import com.example.model.HomeSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSectionReorderTest {

    @Test
    fun testDefaultOrder_containsAllSectionsWithoutDuplicates() {
        val defaultList = HomeSection.DEFAULT_ORDER
        assertEquals(7, defaultList.size)
        assertEquals(7, defaultList.distinct().size)
        assertTrue(defaultList.contains(HomeSection.PORTFOLIO_SUMMARY))
        assertTrue(defaultList.contains(HomeSection.FINANCIAL_OVERVIEW))
        assertTrue(defaultList.contains(HomeSection.QUICK_ACTIONS))
        assertTrue(defaultList.contains(HomeSection.FAVORITE_CALCULATORS))
        assertTrue(defaultList.contains(HomeSection.PRIMARY_NAV_CARDS))
        assertTrue(defaultList.contains(HomeSection.RECENT_TRANSACTIONS))
        assertTrue(defaultList.contains(HomeSection.PRIVACY_INDICATOR))
    }

    @Test
    fun testSerializeAndParseOrder_preservesCustomOrder() {
        // Move Quick Actions to top, and Privacy Indicator above favorites
        val customOrder = listOf(
            HomeSection.QUICK_ACTIONS,
            HomeSection.PORTFOLIO_SUMMARY,
            HomeSection.PRIMARY_NAV_CARDS,
            HomeSection.FINANCIAL_OVERVIEW,
            HomeSection.PRIVACY_INDICATOR,
            HomeSection.FAVORITE_CALCULATORS,
            HomeSection.RECENT_TRANSACTIONS
        )

        val serialized = HomeSection.serialize(customOrder)
        val deserialized = HomeSection.parseOrder(serialized)

        assertEquals(customOrder, deserialized)
    }

    @Test
    fun testParseOrder_whenEmptyOrInvalid_fallsBackToDefaultOrder() {
        val parsedNull = HomeSection.parseOrder(null)
        val parsedEmpty = HomeSection.parseOrder("")
        val parsedBlank = HomeSection.parseOrder("   ")

        assertEquals(HomeSection.DEFAULT_ORDER, parsedNull)
        assertEquals(HomeSection.DEFAULT_ORDER, parsedEmpty)
        assertEquals(HomeSection.DEFAULT_ORDER, parsedBlank)
    }

    @Test
    fun testParseOrder_whenMissingSomeSections_appendsMissingSections() {
        // Only 2 sections specified
        val partialString = "primary_nav_cards,portfolio_summary"
        val parsed = HomeSection.parseOrder(partialString)

        assertEquals(7, parsed.size)
        assertEquals(HomeSection.PRIMARY_NAV_CARDS, parsed[0])
        assertEquals(HomeSection.PORTFOLIO_SUMMARY, parsed[1])
        // Remaining 5 sections appended without duplicates
        assertEquals(7, parsed.distinct().size)
    }

    @Test
    fun testReordering_swappingAdjacentItems() {
        val list = HomeSection.DEFAULT_ORDER.toMutableList()
        // Move primary_nav_cards (index 4) up to index 3
        val item = list.removeAt(4)
        list.add(3, item)

        assertEquals(HomeSection.PRIMARY_NAV_CARDS, list[3])
        assertEquals(HomeSection.FAVORITE_CALCULATORS, list[4])
        assertEquals(7, list.size)
    }
}
