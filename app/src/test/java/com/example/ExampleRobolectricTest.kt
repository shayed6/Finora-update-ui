package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.model.CalculatorRepository
import com.example.util.BengaliFormatter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `savings goals report summary calculations are accurate`() {
    val goals = listOf(
      SavingsGoalEntity(
        id = 1,
        name = "জরুরি ফান্ড (Emergency Fund)",
        targetAmount = 10000000L,
        targetDate = 1798761600000L
      ),
      SavingsGoalEntity(
        id = 2,
        name = "ল্যাপটপ ক্রয়",
        targetAmount = 8000000L,
        targetDate = 1792022400000L
      )
    )

    val totalTarget = goals.sumOf { (it.targetAmount ?: 0L) / 100.0 }
    assertEquals(180000.0, totalTarget, 0.01)

    val formattedTaka = BengaliFormatter.formatTaka(totalTarget, true)
    assertTrue(formattedTaka.contains("১৮০,০০০") || formattedTaka.contains("১,৮০,০০০"))
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Finora", appName)
  }

  @Test
  fun `theme preferences repository default and persistence`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = UserPreferencesRepository.getInstance(context)
    repo.setThemeMode(AppThemeMode.DARK)
    val mode = repo.themeMode.first()
    assertEquals(AppThemeMode.DARK, mode)
    repo.setThemeMode(AppThemeMode.SYSTEM)
    assertEquals(AppThemeMode.SYSTEM, repo.themeMode.first())
  }

  @Test
  fun `bengali formatter converts digits correctly`() {
    val bengali = BengaliFormatter.toBengaliDigits("12345.67")
    assertEquals("১২৩৪৫.৬৭", bengali)

    val normalized = BengaliFormatter.normalizeToEnglishDigits("১২৩৪৫.৬৭")
    assertEquals("12345.67", normalized)
  }

  @Test
  fun `all 37 calculators are loaded`() {
    assertEquals(37, CalculatorRepository.allCalculators.size)
  }

  @Test
  fun `loan emi calculator computes monthly installment accurately`() {
    val emiCalc = CalculatorRepository.getById("loan_emi")
    org.junit.Assert.assertNotNull(emiCalc)

    val inputs = mapOf(
      "principal" to 1000000.0,
      "interest_rate" to 12.0,
      "tenure_years" to 3.0,
      "tenure_months" to 0.0
    )

    val result = emiCalc!!.calculate(inputs, false)
    // EMI for 10 Lakh at 12% for 36 months is ~33,214.31
    assertTrue(result.primaryValueBn.contains("33,214"))
    assertEquals(false, result.isWarning)
    assertEquals(5, result.subResults.size)
  }

  @Test
  fun `loan emi calculator handles invalid inputs gracefully`() {
    val emiCalc = CalculatorRepository.getById("loan_emi")
    org.junit.Assert.assertNotNull(emiCalc)

    val inputs = mapOf(
      "principal" to 0.0,
      "interest_rate" to 10.0,
      "tenure_years" to 0.0,
      "tenure_months" to 0.0
    )

    val result = emiCalc!!.calculate(inputs, true)
    assertEquals(true, result.isWarning)
  }

  @Test
  fun `drawer destination contains contact us and order app`() {
    val destinations = com.example.ui.components.DrawerDestination.values()
    assertTrue(destinations.contains(com.example.ui.components.DrawerDestination.CONTACT_US))
    assertTrue(destinations.contains(com.example.ui.components.DrawerDestination.ORDER_APP))
  }

  @Test
  fun `20-share City Bank averaging with live price and unrealized GL integration`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.local.AppDatabase.getDatabase(context)
    val portfolioDao = db.portfolioDao()
    val livePriceDao = db.livePriceDao()
    val repo = com.example.data.repository.PortfolioRepository(portfolioDao, livePriceDao)

    // Clear any previous holdings for clean test
    val existing = portfolioDao.getHoldingByExchangeAndStock("DSE", "CITYBANK")
    if (existing != null) {
      repo.deleteHolding(existing)
    }

    // Step 1: Buy 10 shares of City Bank at 25 BDT with 0.40% commission
    repo.recordBuyTransaction(
      exchange = "DSE",
      stockName = "CITYBANK",
      buyingPrice = 25.0,
      quantity = 10,
      commissionPercent = 0.40
    )

    var holding = portfolioDao.getHoldingByExchangeAndStock("DSE", "CITYBANK")
    org.junit.Assert.assertNotNull(holding)
    assertEquals(10, holding!!.quantity)
    assertEquals(25.10, holding.averagePrice, 0.001)

    // Step 2: Buy another 10 shares of City Bank at 30 BDT with 0.40% commission -> 20 shares total
    repo.recordBuyTransaction(
      exchange = "DSE",
      stockName = "CITYBANK",
      buyingPrice = 30.0,
      quantity = 10,
      commissionPercent = 0.40
    )

    holding = portfolioDao.getHoldingByExchangeAndStock("DSE", "CITYBANK")
    org.junit.Assert.assertNotNull(holding)
    assertEquals(20, holding!!.quantity)

    // Expected weighted average: (10 * 25.10 + 10 * 30.12) / 20 = 552.20 / 20 = 27.61
    assertEquals(27.61, holding.averagePrice, 0.001)
    val totalPrice = holding.quantity * holding.averagePrice
    assertEquals(552.20, totalPrice, 0.01)

    // Step 3: Add dividend of 50.0 BDT
    repo.addDividend(holding.id, 50.0)
    val divs = repo.getDividendsList(holding.id)
    assertEquals(1, divs.size)
    assertEquals(50.0, divs[0].amount, 0.01)
    val returnPerShare = 50.0 / holding.quantity
    assertEquals(2.50, returnPerShare, 0.001)

    // Step 4: Insert scraped live price for DSE:CITYBANK (e.g. 29.60 BDT)
    val livePrice = com.example.data.local.entity.LivePriceEntity(
      exchange = "DSE",
      symbol = "CITYBANK",
      ltp = 29.60,
      changePercent = -0.67,
      delta = -0.20,
      lastUpdated = System.currentTimeMillis()
    )
    repo.saveLivePrices(listOf(livePrice))

    val fetchedLive = repo.getLivePriceSync("DSE", "CITYBANK")
    org.junit.Assert.assertNotNull(fetchedLive)
    assertEquals(29.60, fetchedLive!!.ltp, 0.01)

    // Step 5: Verify Unrealized Gain/Loss = (Current Price * Quantity) - Total Price
    val currentMarketValue = fetchedLive.ltp * holding.quantity
    val unrealizedGL = currentMarketValue - totalPrice
    val unrealizedGLPercent = (unrealizedGL / totalPrice) * 100.0

    assertEquals(592.00, currentMarketValue, 0.01)
    assertEquals(39.80, unrealizedGL, 0.01)
    assertEquals(7.207, unrealizedGLPercent, 0.05)
  }

  @Test
  fun `20-share City Bank averaging example 10 at 30 and 10 at 20 with commission`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.local.AppDatabase.getDatabase(context)
    val portfolioDao = db.portfolioDao()
    val repo = com.example.data.repository.PortfolioRepository(portfolioDao, db.livePriceDao())

    // Clear any previous holdings for clean test
    val existing = portfolioDao.getHoldingByExchangeAndStock("DSE", "CITYBANK_TEST")
    if (existing != null) {
      repo.deleteHolding(existing)
    }

    // Step 1: Buy 10 shares of City Bank @ 30 BDT with 0.40% commission
    // Effective price = 30 * 1.004 = 30.12, Total cost = 301.20
    repo.recordBuyTransaction(
      exchange = "DSE",
      stockName = "CITYBANK_TEST",
      buyingPrice = 30.0,
      quantity = 10,
      commissionPercent = 0.40
    )

    var holding = portfolioDao.getHoldingByExchangeAndStock("DSE", "CITYBANK_TEST")
    org.junit.Assert.assertNotNull(holding)
    assertEquals(10, holding!!.quantity)
    assertEquals(30.12, holding.averagePrice, 0.001)

    // Step 2: Buy 10 shares of City Bank @ 20 BDT with 0.40% commission
    // Effective price = 20 * 1.004 = 20.08, Total cost = 200.80
    repo.recordBuyTransaction(
      exchange = "DSE",
      stockName = "CITYBANK_TEST",
      buyingPrice = 20.0,
      quantity = 10,
      commissionPercent = 0.40
    )

    holding = portfolioDao.getHoldingByExchangeAndStock("DSE", "CITYBANK_TEST")
    org.junit.Assert.assertNotNull(holding)
    assertEquals(20, holding!!.quantity)

    // Weighted average buying price: (301.20 + 200.80) / 20 = 502.00 / 20 = 25.10
    assertEquals(25.10, holding.averagePrice, 0.001)
    val totalPrice = holding.quantity * holding.averagePrice
    assertEquals(502.00, totalPrice, 0.01)

    // Live price matched from DSE (e.g. 29.60)
    val currentPrice = 29.60
    val currentMarketValue = currentPrice * holding.quantity // 20 * 29.60 = 592.00
    val unrealizedGL = currentMarketValue - totalPrice // 592.00 - 502.00 = +90.00
    val unrealizedGLPercent = (unrealizedGL / totalPrice) * 100.0 // +17.928%

    assertEquals(592.00, currentMarketValue, 0.01)
    assertEquals(90.00, unrealizedGL, 0.01)
    assertEquals(17.928, unrealizedGLPercent, 0.01)

    // Clean up
    repo.deleteHolding(holding)
  }

  @Test
  fun `unmatched holding symbol handles missing live price gracefully with nulls`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.local.AppDatabase.getDatabase(context)
    val repo = com.example.data.repository.PortfolioRepository(db.portfolioDao(), db.livePriceDao())

    val missingPrice = repo.getLivePriceSync("DSE", "NON_EXISTENT_DELISTED_XYZ")
    org.junit.Assert.assertNull(missingPrice)
  }

  @Test
  fun `csv export generates valid formatted file with unrealized gain loss and summary`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val holding = com.example.data.local.entity.HoldingEntity(
      id = 1,
      exchange = "DSE",
      stockName = "GP",
      quantity = 50,
      averagePrice = 240.0,
      currentPrice = 242.40
    )
    val holdingWithDiv = com.example.ui.screens.portfolio.HoldingWithDividends(
      holding = holding,
      totalDividend = 650.0,
      dividendCount = 2,
      currentPrice = 242.40,
      unrealizedGainLoss = 120.0,
      unrealizedGainLossPercent = 1.0,
      isPriceOutdated = false
    )
    val summary = com.example.ui.screens.portfolio.PortfolioSummary(
      totalInvested = 12000.0,
      currentTotalValue = 12120.0,
      totalUnrealizedGainLoss = 120.0,
      totalDividend = 650.0,
      hasAnyLivePrice = true,
      lastUpdatedTimestamp = System.currentTimeMillis()
    )

    val file = com.example.util.CsvReportGenerator.exportPortfolioCsv(
      context = context,
      holdings = listOf(holding),
      summary = summary,
      useBengaliDigits = false,
      holdingsWithDividends = listOf(holdingWithDiv)
    )

    org.junit.Assert.assertNotNull(file)
    org.junit.Assert.assertTrue(file!!.exists())
    val content = file.readText()
    org.junit.Assert.assertTrue(content.contains("Finora Portfolio Status"))
    org.junit.Assert.assertTrue(content.contains("DSE,GP,50,240.00,12000.00,242.40,12120.00,+120.00,+1.00%,650.00,2,+770.00,Live Scraped"))
    org.junit.Assert.assertTrue(content.contains("Total Unrealized Gain/Loss (BDT),\"+120.00\""))
    org.junit.Assert.assertTrue(content.contains("Total Dividend Income Received (BDT),\"650.00\""))
  }
}


