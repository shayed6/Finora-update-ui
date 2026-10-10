package com.example.ui.screens.summary

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.data.local.AppDatabase
import com.example.data.local.dao.IncomeExpenseDao
import com.example.data.local.dao.LedgerDao
import com.example.data.local.dao.SavingsGoalDao
import com.example.data.local.entity.IncomeSourceEntity
import com.example.data.local.entity.LedgerEntryType
import com.example.data.preferences.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class AccountsSummaryRepository(
    private val incomeExpenseDao: IncomeExpenseDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val ledgerDao: LedgerDao,
    private val context: Context
) {
    companion object {
        private val CHART_TYPE_PREF_KEY = stringPreferencesKey("accounts_summary_chart_type")

        @Volatile
        private var INSTANCE: AccountsSummaryRepository? = null

        fun getInstance(context: Context): AccountsSummaryRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context.applicationContext)
                val instance = AccountsSummaryRepository(
                    db.incomeExpenseDao(),
                    db.savingsGoalDao(),
                    db.ledgerDao(),
                    context.applicationContext
                )
                INSTANCE = instance
                instance
            }
        }
    }

    // In-memory cache for period summaries
    private val periodCache = ConcurrentHashMap<Pair<Long, Long>, PeriodSummary>()

    fun clearCache() {
        periodCache.clear()
    }

    val chartTypePreference: Flow<ChartType> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw e
        }
        .map { prefs ->
            val name = prefs[CHART_TYPE_PREF_KEY] ?: ChartType.PIE.name
            try {
                ChartType.valueOf(name)
            } catch (_: Exception) {
                ChartType.PIE
            }
        }

    suspend fun saveChartTypePreference(type: ChartType) {
        context.dataStore.edit { prefs ->
            prefs[CHART_TYPE_PREF_KEY] = type.name
        }
    }

    val allIncomeSources: Flow<List<IncomeSourceEntity>> = incomeExpenseDao.getAllIncomeSources()

    suspend fun updateIncomeSourceStartDay(id: Long, startDay: Int?) = withContext(Dispatchers.IO) {
        val current = incomeExpenseDao.getIncomeSourceById(id) ?: return@withContext
        val updated = current.copy(startDay = if (startDay in 1..28) startDay else null)
        incomeExpenseDao.updateIncomeSource(updated)
        clearCache()
    }

    suspend fun calculatePeriodSummary(startMs: Long, endMs: Long): PeriodSummary = withContext(Dispatchers.IO) {
        val key = Pair(startMs, endMs)
        periodCache[key]?.let { return@withContext it }

        val txAgg = incomeExpenseDao.getAggregateInRange(startMs, endMs)
        val netSavings = savingsGoalDao.getNetSavingsInRange(startMs, endMs)
        val savingsCount = savingsGoalDao.countSavingsEntriesInRange(startMs, endMs)

        val income = txAgg.totalIncome
        val expense = txAgg.totalExpense
        val savings = netSavings
        val remaining = income - expense - savings
        val totalCount = txAgg.txCount + savingsCount

        val summary = PeriodSummary(
            incomePaisa = income,
            expensePaisa = expense,
            savingsPaisa = savings,
            remainingPaisa = remaining,
            totalTransactionsCount = totalCount
        )

        periodCache[key] = summary
        summary
    }

    suspend fun calculateSixMonthsTrend(selectedMonthYear: MonthYear): List<MonthlyTrendPoint> = withContext(Dispatchers.IO) {
        val months = mutableListOf<MonthYear>()
        var curr = selectedMonthYear
        // Collect 6 months in reverse, then reverse back
        val listRev = mutableListOf<MonthYear>()
        repeat(6) {
            listRev.add(curr)
            curr = curr.previous()
        }
        months.addAll(listRev.reversed())

        months.map { my ->
            val s = my.toStartTimestamp()
            val e = my.toEndTimestamp()
            val txAgg = incomeExpenseDao.getAggregateInRange(s, e)
            val netSavings = savingsGoalDao.getNetSavingsInRange(s, e)
            MonthlyTrendPoint(
                monthYear = my,
                monthLabel = SummaryDateFormatter.formatMonthLabel(my, true),
                incomePaisa = txAgg.totalIncome,
                expensePaisa = txAgg.totalExpense,
                savingsPaisa = netSavings
            )
        }
    }

    suspend fun calculateSourceCards(
        sources: List<IncomeSourceEntity>,
        sourcePeriods: Map<Long, PeriodRange>
    ): List<IncomeSourceCardData> = withContext(Dispatchers.IO) {
        val activeSourceIds = incomeExpenseDao.getSourceIdsWithTransactions().toSet()
        val relevantSources = sources.filter { it.id in activeSourceIds }

        relevantSources.map { src ->
            val period = sourcePeriods[src.id] ?: computeDefaultCycle(src.startDay)
            val inc = incomeExpenseDao.getIncomeForSourceInRange(src.id, period.startTimestamp, period.endTimestamp)
            val exp = incomeExpenseDao.getTaggedExpenseForSourceInRange(src.id, period.startTimestamp, period.endTimestamp)
            val rem = inc - exp
            IncomeSourceCardData(
                sourceId = src.id,
                sourceName = src.name,
                startDay = src.startDay,
                currentPeriod = period,
                incomePaisa = inc,
                taggedExpensePaisa = exp,
                remainingPaisa = rem
            )
        }
    }

    suspend fun calculateLedgerOverview(): LedgerOverview = withContext(Dispatchers.IO) {
        val parties = ledgerDao.getAllPartiesList()
        val entries = ledgerDao.getAllEntriesList()

        val entriesByParty = entries.groupBy { it.partyId }
        var totalPabo = 0L
        var totalDebo = 0L

        parties.forEach { party ->
            val partyEntries = entriesByParty[party.id] ?: emptyList()
            var gave = 0L
            var received = 0L
            partyEntries.forEach { entry ->
                if (entry.type == LedgerEntryType.GAVE.name) gave += entry.amountPaisa
                else if (entry.type == LedgerEntryType.RECEIVED.name) received += entry.amountPaisa
            }
            val net = gave - received
            if (net > 0L) {
                totalPabo += net
            } else if (net < 0L) {
                totalDebo += abs(net)
            }
        }

        LedgerOverview(
            totalPaboPaisa = totalPabo,
            totalDeboPaisa = totalDebo
        )
    }
}
