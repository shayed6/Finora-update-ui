package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.dao.InvoiceDao
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import com.example.data.local.entity.InvoiceWithItems
import com.example.data.local.entity.PdfReportEntity
import com.example.data.preferences.OrganizationInfoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InvoiceRepository(
    private val dao: InvoiceDao,
    private val orgInfoRepo: OrganizationInfoRepository
) {

    val allInvoices: Flow<List<InvoiceWithItems>> = dao.getAllInvoicesWithItems()
    val allPdfReports: Flow<List<PdfReportEntity>> = dao.getAllPdfReports()

    fun searchInvoices(query: String): Flow<List<InvoiceWithItems>> =
        dao.searchInvoices(query.trim())

    fun filterByStatus(status: String): Flow<List<InvoiceWithItems>> =
        dao.getInvoicesByStatus(status)

    fun getInvoiceById(id: Long): Flow<InvoiceWithItems?> =
        dao.getInvoiceWithItemsById(id)

    suspend fun getInvoiceByIdSync(id: Long): InvoiceWithItems? = withContext(Dispatchers.IO) {
        dao.getInvoiceWithItemsByIdSync(id)
    }

    suspend fun generateNextInvoiceNumber(): String = withContext(Dispatchers.IO) {
        val orgInfo = orgInfoRepo.organizationInfo.first()
        val prefix = orgInfo.invoicePrefix.ifBlank { "INV-" }
        val count = dao.getInvoiceCount()
        val currentYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        val sequence = String.format(Locale.US, "%04d", count + 1)
        "$prefix$currentYear-$sequence"
    }

    suspend fun saveInvoice(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>
    ): Long = withContext(Dispatchers.IO) {
        dao.saveInvoiceWithItems(invoice, items)
    }

    suspend fun deleteInvoice(invoiceId: Long, deletePdfFile: Boolean = true) = withContext(Dispatchers.IO) {
        val invoiceWithItems = dao.getInvoiceWithItemsByIdSync(invoiceId)
        if (deletePdfFile && invoiceWithItems?.invoice?.pdfFilePath != null) {
            try {
                val file = File(invoiceWithItems.invoice.pdfFilePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {
            }
        }
        dao.deleteInvoiceById(invoiceId)
    }

    // --- Report History ---
    fun getReportsByType(type: String): Flow<List<PdfReportEntity>> =
        dao.getPdfReportsByType(type)

    suspend fun savePdfReport(report: PdfReportEntity): Long = withContext(Dispatchers.IO) {
        dao.insertPdfReport(report)
    }

    suspend fun deletePdfReport(id: Long, deleteFile: Boolean = true) = withContext(Dispatchers.IO) {
        val report = dao.getPdfReportById(id)
        if (deleteFile && report != null) {
            try {
                val file = File(report.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {
            }
        }
        dao.deletePdfReportById(id)
    }

    companion object {
        @Volatile
        private var INSTANCE: InvoiceRepository? = null

        fun getInstance(context: Context): InvoiceRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context.applicationContext)
                val orgRepo = OrganizationInfoRepository.getInstance(context.applicationContext)
                val instance = InvoiceRepository(db.invoiceDao(), orgRepo)
                INSTANCE = instance
                instance
            }
        }
    }
}
