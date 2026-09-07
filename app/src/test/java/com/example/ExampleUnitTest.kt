package com.example

import com.example.model.CalculatorRepository
import com.example.util.BengaliFormatter
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.pow

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun sipCalculation_isAccurate() {
    val monthly = 5000.0
    val annualRate = 12.0
    val years = 10.0
    val r = annualRate / 12.0 / 100.0
    val n = years * 12.0

    val fv = monthly * (((1.0 + r).pow(n) - 1.0) / r) * (1.0 + r)
    val totalInvested = monthly * n
    val wealthGain = fv - totalInvested

    assertEquals(600000.0, totalInvested, 0.01)
    assertTrue("Future value should be greater than 11 Lakhs", fv > 1150000.0 && fv < 1170000.0)
    assertTrue("Wealth gain should be greater than 5.5 Lakhs", wealthGain > 550000.0)
  }

  @Test
  fun sipCalculatorDef_repositoryLookup() {
    val sipDef = CalculatorRepository.getById("sip_calc")
    assertNotNull(sipDef)
    assertEquals("SIP Calculator (মাসিক এসআইপি)", sipDef?.titleBn)

    val result = sipDef?.calculate?.invoke(
      mapOf("monthly_inv" to 5000.0, "annual_rate" to 12.0, "years" to 10.0),
      true
    )
    assertNotNull(result)
    assertFalse(result?.isWarning ?: true)
  }

  @Test
  fun compactTaka_formatsLakhsAndCrores() {
    val lakhValue = 1161695.0
    val formattedLakh = BengaliFormatter.formatCompactTaka(lakhValue, false)
    assertTrue(formattedLakh.contains("11.62 লাখ"))

    val croreValue = 17649472.0
    val formattedCrore = BengaliFormatter.formatCompactTaka(croreValue, false)
    assertTrue(formattedCrore.contains("1.76 কোটি"))
  }

  @Test
  fun inflationCalculation_isAccurate() {
    val currentAmount = 10000.0
    val inflationRate = 8.0
    val years = 10.0
    val factor = (1.0 + inflationRate / 100.0).pow(years)
    val futureCost = currentAmount * factor
    val realPurchasingPower = currentAmount / factor

    // (1.08)^10 is approx 2.1589
    assertTrue("Future cost of 10000 at 8% for 10 years should be ~21589", futureCost > 21500.0 && futureCost < 21700.0)
    assertTrue("Purchasing power of 10000 at 8% for 10 years should be ~4631", realPurchasingPower > 4600.0 && realPurchasingPower < 4700.0)
  }

  @Test
  fun inflationCalculatorDef_repositoryLookup() {
    val inflationDef = CalculatorRepository.getById("inflation_calc")
    assertNotNull(inflationDef)
    assertEquals("Inflation Calculator (মূল্যস্ফীতি ক্যালকুলেটর)", inflationDef?.titleBn)

    val result = inflationDef?.calculate?.invoke(
      mapOf("current_amount" to 10000.0, "inflation_rate" to 8.0, "years" to 10.0),
      true
    )
    assertNotNull(result)
    assertFalse(result?.isWarning ?: true)
    assertEquals("ভবিষ্যতে এই জিনিসের জন্য প্রয়োজন হবে", result?.primaryLabelBn)
  }
}
