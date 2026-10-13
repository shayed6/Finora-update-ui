package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import com.example.data.local.entity.InvoiceWithItems
import com.example.data.local.entity.PdfReportHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    // --- Invoices ---
    @Transaction
    @Query("SELECT * FROM invoices ORDER BY invoice_date DESC, created_at DESC")
    fun getAllInvoicesWithItems(): Flow<List<InvoiceWithItems>>

    @Transaction
    @Query("SELECT * FROM invoices ORDER BY invoice_date DESC, created_at DESC")
    suspend fun getAllInvoicesWithItemsList(): List<InvoiceWithItems>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    fun getInvoiceWithItemsById(id: Long): Flow<InvoiceWithItems?>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceWithItemsByIdSync(id: Long): InvoiceWithItems?

    @Transaction
    @Query("""
        SELECT * FROM invoices 
        WHERE invoice_number LIKE '%' || :query || '%' 
           OR customer_name LIKE '%' || :query || '%'
           OR customer_phone LIKE '%' || :query || '%'
        ORDER BY invoice_date DESC, created_at DESC
    """)
    fun searchInvoices(query: String): Flow<List<InvoiceWithItems>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE status = :status ORDER BY invoice_date DESC, created_at DESC")
    fun getInvoicesByStatus(status: String): Flow<List<InvoiceWithItems>>

    @Query("SELECT invoice_number FROM invoices ORDER BY id DESC LIMIT 1")
    suspend fun getLatestInvoiceNumber(): String?

    @Query("SELECT COUNT(*) FROM invoices")
    suspend fun getInvoiceCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Delete
    suspend fun deleteInvoice(invoice: InvoiceEntity)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoiceById(id: Long)

    // --- Invoice Items ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>): List<Long>

    @Query("DELETE FROM invoice_items WHERE invoice_id = :invoiceId")
    suspend fun deleteItemsForInvoice(invoiceId: Long): Int

    @Transaction
    suspend fun saveInvoiceWithItems(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>
    ): Long {
        val invoiceId = if (invoice.id > 0) {
            updateInvoice(invoice.copy(updatedAt = System.currentTimeMillis()))
            deleteItemsForInvoice(invoice.id)
            invoice.id
        } else {
            insertInvoice(invoice)
        }

        val itemsWithId = items.mapIndexed { index, item ->
            item.copy(invoiceId = invoiceId, sortOrder = index)
        }
        if (itemsWithId.isNotEmpty()) {
            insertInvoiceItems(itemsWithId)
        }
        return invoiceId
    }

    // --- PDF Reports History ---
    @Query("SELECT * FROM pdf_reports ORDER BY created_at DESC")
    fun getAllPdfReports(): Flow<List<PdfReportHistoryEntity>>

    @Query("SELECT * FROM pdf_reports WHERE report_type = :type ORDER BY created_at DESC")
    fun getPdfReportsByType(type: String): Flow<List<PdfReportHistoryEntity>>

    @Query("SELECT * FROM pdf_reports WHERE id = :id LIMIT 1")
    suspend fun getPdfReportById(id: Long): PdfReportHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPdfReport(report: PdfReportHistoryEntity): Long

    @Delete
    suspend fun deletePdfReport(report: PdfReportHistoryEntity)

    @Query("DELETE FROM pdf_reports WHERE id = :id")
    suspend fun deletePdfReportById(id: Long)
}
