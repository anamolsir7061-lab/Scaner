package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.analyzer.BarcodeFormatHelper
import com.example.data.model.ScanItem
import com.example.data.repository.ScanRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScanHistoryFilter {
    ALL,
    BOOKMARKED,
    TWO_D,
    ONE_D
}

data class ScanHistoryUiState(
    val items: List<ScanItem> = emptyList(),
    val totalCount: Int = 0,
    val searchQuery: String = "",
    val activeFilter: ScanHistoryFilter = ScanHistoryFilter.ALL,
    val isLoading: Boolean = false
)

class ScanHistoryViewModel(
    private val repository: ScanRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow(ScanHistoryFilter.ALL)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val queriedScans = combine(_searchQuery, _activeFilter) { query, filter ->
        query.trim() to filter
    }.flatMapLatest { (trimmedQuery, filter) ->
        when {
            trimmedQuery.isNotBlank() -> repository.searchScans(trimmedQuery)
            filter == ScanHistoryFilter.BOOKMARKED -> repository.bookmarkedScans
            else -> repository.allScans
        }
    }

    val uiState: StateFlow<ScanHistoryUiState> = combine(
        queriedScans,
        repository.allScans,
        _searchQuery,
        _activeFilter
    ) { queryResults, allItems, query, filter ->
        val filtered = queryResults.filter { item ->
            when (filter) {
                ScanHistoryFilter.ALL -> true
                ScanHistoryFilter.BOOKMARKED -> item.isBookmarked
                ScanHistoryFilter.TWO_D -> {
                    val cat = BarcodeFormatHelper.getFormatInfo(
                        BarcodeFormatHelper.parseFormatCode(item.format)
                    ).typeCategory
                    cat.startsWith("2D")
                }
                ScanHistoryFilter.ONE_D -> {
                    val cat = BarcodeFormatHelper.getFormatInfo(
                        BarcodeFormatHelper.parseFormatCode(item.format)
                    ).typeCategory
                    cat.startsWith("1D")
                }
            }
        }
        ScanHistoryUiState(
            items = filtered,
            totalCount = allItems.size,
            searchQuery = query,
            activeFilter = filter,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ScanHistoryUiState(isLoading = true)
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: ScanHistoryFilter) {
        _activeFilter.value = filter
    }

    fun toggleBookmark(item: ScanItem) {
        viewModelScope.launch {
            repository.toggleBookmark(item)
        }
    }

    fun deleteItem(item: ScanItem) {
        viewModelScope.launch {
            repository.delete(item)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun exportJson(items: List<ScanItem>): String = repository.exportToJson(items)

    fun exportCsv(items: List<ScanItem>): String = repository.exportToCsv(items)

    class Factory(private val repository: ScanRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScanHistoryViewModel(repository) as T
        }
    }
}
