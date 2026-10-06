package com.example.ui.screens.ledger

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.LedgerEntryEntity
import com.example.data.local.entity.LedgerEntryType
import com.example.data.local.entity.LedgerPartyEntity
import com.example.data.repository.LedgerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LedgerListUiState(
    val parties: List<PartyWithBalance> = emptyList(),
    val summary: LedgerSummary = LedgerSummary(0L, 0L, 0),
    val searchQuery: String = "",
    val isLoading: Boolean = false
) {
    val filteredParties: List<PartyWithBalance>
        get() {
            if (searchQuery.isBlank()) return parties
            val q = searchQuery.trim().lowercase()
            val qNorm = com.example.util.BengaliFormatter.normalizeToEnglishDigits(q)
            return parties.filter { item ->
                val nameMatches = item.party.name.lowercase().contains(q)
                val phone = item.party.phone?.lowercase() ?: ""
                val phoneNorm = com.example.util.BengaliFormatter.normalizeToEnglishDigits(phone)
                val phoneMatches = phone.contains(q) || (phoneNorm.isNotBlank() && phoneNorm.contains(qNorm))
                nameMatches || phoneMatches
            }
        }
}

class LedgerViewModel(
    application: Application,
    private val repository: LedgerRepository = LedgerRepository.getInstance(application)
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Combined state: all parties + all entries -> reactive balance calculation
    val uiState: StateFlow<LedgerListUiState> = combine(
        repository.allParties,
        repository.allEntries,
        _searchQuery
    ) { partiesList, entriesList, query ->
        // Group entries by partyId
        val entriesByParty = entriesList.groupBy { it.partyId }

        val partiesWithBalance = partiesList.map { party ->
            val partyEntries = entriesByParty[party.id] ?: emptyList()
            var gave = 0L
            var received = 0L
            partyEntries.forEach { entry ->
                if (entry.type == LedgerEntryType.GAVE.name) {
                    gave += entry.amountPaisa
                } else if (entry.type == LedgerEntryType.RECEIVED.name) {
                    received += entry.amountPaisa
                }
            }
            val net = gave - received
            val lastDate = partyEntries.maxOfOrNull { it.entryDate }

            PartyWithBalance(
                party = party,
                totalGavePaisa = gave,
                totalReceivedPaisa = received,
                netBalancePaisa = net,
                entryCount = partyEntries.size,
                lastEntryDate = lastDate
            )
        }

        // Summary calculations
        var totalPabo = 0L
        var totalDebo = 0L
        partiesWithBalance.forEach { p ->
            if (p.netBalancePaisa > 0L) {
                totalPabo += p.netBalancePaisa
            } else if (p.netBalancePaisa < 0L) {
                totalDebo += kotlin.math.abs(p.netBalancePaisa)
            }
        }

        LedgerListUiState(
            parties = partiesWithBalance,
            summary = LedgerSummary(
                totalPaboPaisa = totalPabo,
                totalDeboPaisa = totalDebo,
                totalPartiesCount = partiesWithBalance.size
            ),
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LedgerListUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    // Party CRUD
    fun addParty(name: String, phone: String?, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.addParty(name, phone)
            onComplete?.invoke(id)
        }
    }

    fun updateParty(party: LedgerPartyEntity) {
        viewModelScope.launch {
            repository.updateParty(party)
        }
    }

    fun deleteParty(partyId: Long) {
        viewModelScope.launch {
            repository.deleteParty(partyId)
        }
    }

    // Entry CRUD
    fun addEntry(
        partyId: Long,
        type: LedgerEntryType,
        amountPaisa: Long,
        note: String?,
        entryDate: Long = System.currentTimeMillis(),
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            repository.addEntry(
                partyId = partyId,
                type = type,
                amountPaisa = amountPaisa,
                note = note,
                entryDate = entryDate
            )
            onComplete?.invoke()
        }
    }

    fun updateEntry(entry: LedgerEntryEntity, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.updateEntry(entry)
            onComplete?.invoke()
        }
    }

    fun deleteEntry(entryId: Long, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteEntry(entryId)
            onComplete?.invoke()
        }
    }

    // Party detail query
    fun getPartyEntries(partyId: Long) = repository.getEntriesForParty(partyId)
    fun getParty(partyId: Long) = repository.getParty(partyId)
}
