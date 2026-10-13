package com.example.util

import com.example.data.local.entity.InvoiceItemEntity
import com.example.data.local.entity.InvoiceStatus
import java.math.BigDecimal
import java.math.RoundingMode

object InvoiceCalculator {

    /**
     * Calculates the line item total in paisa given quantity and unit price in paisa.
     * Uses BigDecimal with HALF_UP rounding to avoid floating-point inaccuracies.
     */
    fun calculateItemTotalPaisa(quantity: Double, unitPricePaisa: Long): Long {
        if (quantity <= 0.0 || unitPricePaisa <= 0L) return 0L
        val qtyBd = BigDecimal.valueOf(quantity)
        val priceBd = BigDecimal.valueOf(unitPricePaisa)
        return qtyBd.multiply(priceBd)
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    }

    data class CalculationResult(
        val subtotalPaisa: Long,
        val discountPaisa: Long,
        val additionalChargesPaisa: Long,
        val grandTotalPaisa: Long,
        val paidAmountPaisa: Long,
        val dueAmountPaisa: Long,
        val status: InvoiceStatus
    )

    /**
     * Computes the complete totals for an invoice from its line items and modifiers.
     */
    fun calculateInvoiceTotals(
        items: List<InvoiceItemEntity>,
        discountPaisa: Long = 0L,
        additionalChargesPaisa: Long = 0L,
        paidAmountPaisa: Long = 0L
    ): CalculationResult {
        val safeDiscount = discountPaisa.coerceAtLeast(0L)
        val safeCharges = additionalChargesPaisa.coerceAtLeast(0L)
        val safePaid = paidAmountPaisa.coerceAtLeast(0L)

        val subtotal = items.sumOf { item ->
            // If item has precalculated totalPricePaisa > 0, trust or recompute:
            if (item.totalPricePaisa > 0L) item.totalPricePaisa
            else calculateItemTotalPaisa(item.quantity, item.unitPricePaisa)
        }

        val appliedDiscount = safeDiscount.coerceAtMost(subtotal)
        val grandTotal = (subtotal - appliedDiscount + safeCharges).coerceAtLeast(0L)
        val appliedPaid = safePaid.coerceAtMost(grandTotal)
        val dueAmount = (grandTotal - appliedPaid).coerceAtLeast(0L)

        val status = when {
            grandTotal == 0L -> InvoiceStatus.PAID
            dueAmount == 0L -> InvoiceStatus.PAID
            appliedPaid > 0L -> InvoiceStatus.PARTIAL
            else -> InvoiceStatus.DUE
        }

        return CalculationResult(
            subtotalPaisa = subtotal,
            discountPaisa = appliedDiscount,
            additionalChargesPaisa = safeCharges,
            grandTotalPaisa = grandTotal,
            paidAmountPaisa = appliedPaid,
            dueAmountPaisa = dueAmount,
            status = status
        )
    }

    /**
     * Converts a Taka string input (e.g. "150.50" or "১৫০.৫০") to paisa Long.
     */
    fun parseTakaToPaisa(input: String): Long {
        if (input.isBlank()) return 0L
        val clean = BengaliFormatter.bengaliToEnglishDigits(input.trim())
            .replace(",", "")
            .replace("৳", "")
            .trim()
        val bd = try {
            BigDecimal(clean)
        } catch (_: Exception) {
            BigDecimal.ZERO
        }
        return bd.multiply(BigDecimal(100))
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
            .coerceAtLeast(0L)
    }

    /**
     * Converts paisa Long to Taka decimal Double.
     */
    fun paisaToTaka(paisa: Long): Double {
        return paisa / 100.0
    }
}
