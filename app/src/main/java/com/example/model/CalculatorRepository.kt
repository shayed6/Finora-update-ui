package com.example.model

object CalculatorRepository {
    val allCalculators: List<CalculatorDef> by lazy {
        StockRatioCalculators.list + StockAverageCalculators.list + InvestmentBankingCalculators.list
    }

    fun getByCategory(category: CalculatorCategory): List<CalculatorDef> {
        return allCalculators.filter { it.category == category }
    }

    fun getById(id: String): CalculatorDef? {
        return allCalculators.find { it.id == id }
    }

    fun search(query: String): List<CalculatorDef> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return allCalculators
        return allCalculators.filter {
            it.titleBn.lowercase().contains(q) ||
            it.titleEn.lowercase().contains(q) ||
            it.descriptionBn.lowercase().contains(q) ||
            it.formulaSummaryBn.lowercase().contains(q)
        }
    }
}
