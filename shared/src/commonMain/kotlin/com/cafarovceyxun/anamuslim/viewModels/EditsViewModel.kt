package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import com.cafarovceyxun.anamuslim.utils.supabase.QuranEdit
import com.cafarovceyxun.anamuslim.utils.supabase.HadithEdit
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditsViewModel : ViewModel() {

    private val _quranEdits = MutableStateFlow<List<QuranEdit>>(emptyList())
    val quranEdits = _quranEdits.asStateFlow()

    private val _hadithEdits = MutableStateFlow<List<HadithEdit>>(emptyList())
    val hadithEdits = _hadithEdits.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _quranFilter = MutableStateFlow("All")
    val quranFilter = _quranFilter.asStateFlow()

    private val _hadithFilter = MutableStateFlow("All")
    val hadithFilter = _hadithFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedQuranEditIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedQuranEditIds = _selectedQuranEditIds.asStateFlow()

    private val _selectedHadithEditIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedHadithEditIds = _selectedHadithEditIds.asStateFlow()

    fun toggleQuranEditSelection(id: Long) {
        val current = _selectedQuranEditIds.value
        _selectedQuranEditIds.value = if (current.contains(id)) current - id else current + id
    }

    fun selectAllQuranEdits(edits: List<QuranEdit>) {
        _selectedQuranEditIds.value = edits.mapNotNull { it.id }.toSet()
    }

    fun clearQuranSelection() {
        _selectedQuranEditIds.value = emptySet()
    }

    fun toggleHadithEditSelection(id: Long) {
        val current = _selectedHadithEditIds.value
        _selectedHadithEditIds.value = if (current.contains(id)) current - id else current + id
    }

    fun selectAllHadithEdits(edits: List<HadithEdit>) {
        _selectedHadithEditIds.value = edits.mapNotNull { it.id }.toSet()
    }

    fun clearHadithSelection() {
        _selectedHadithEditIds.value = emptySet()
    }

    fun setQuranFilter(filter: String) {
        _quranFilter.value = filter
    }

    fun setHadithFilter(filter: String) {
        _hadithFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun fetchQuranEdits() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                AppLogger.d("EditsVM", "Fetching Quran Edits...")
                val result = SupabaseProvider.client.from("quran_edits").select {
                    order("created_at", order = Order.DESCENDING)
                }
                val decoded = result.decodeList<QuranEdit>()
                _quranEdits.value = decoded
                AppLogger.d("EditsVM", "Fetched ${decoded.size} quran edits")
            } catch (e: Exception) {
                AppLogger.d("EditsVM", "Quran fetch error: ${e.message}")
                _error.value = "Quran error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchHadithEdits() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                AppLogger.d("EditsVM", "Fetching Hadith Edits...")
                val result = SupabaseProvider.client.from("hadith_edits").select {
                    order("created_at", order = Order.DESCENDING)
                }
                
                val decoded = result.decodeList<HadithEdit>()
                _hadithEdits.value = decoded
                AppLogger.d("EditsVM", "Decoded ${decoded.size} hadith edits")
                
                if (decoded.isEmpty()) {
                    AppLogger.d("EditsVM", "Hadith table returned 0 rows. Check RLS or content.")
                }
            } catch (e: Exception) {
                AppLogger.d("EditsVM", "Hadith fetch error details: ${e.stackTraceToString()}")
                _error.value = "Hadith error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun approveQuranEdit(edit: QuranEdit) {
        viewModelScope.launch {
            try {
                val id = edit.id ?: return@launch
                AppLogger.d("EditsVM", "Approving Quran Edit ID: $id")
                SupabaseProvider.client.from("quran_edits").update(
                    mapOf("is_approved" to true)
                ) {
                    filter { eq("id", id) }
                }
                fetchQuranEdits()
            } catch (e: Exception) {
                AppLogger.d("EditsVM", "Approve failed: ${e.message}")
                _error.value = "Approve failed: ${e.message}"
            }
        }
    }

    fun deleteQuranEdit(edit: QuranEdit) {
        viewModelScope.launch {
            try {
                val id = edit.id ?: return@launch
                AppLogger.d("EditsVM", "Attempting to delete Quran Edit ID: $id")
                
                SupabaseProvider.client.from("quran_edits").delete {
                    filter { eq("id", id) }
                }
                
                AppLogger.d("EditsVM", "Delete command sent for ID: $id")
                fetchQuranEdits()
            } catch (e: Exception) {
                AppLogger.d("EditsVM", "Delete operation exception: ${e.message}")
                _error.value = "Delete error: ${e.message}"
            }
        }
    }

    fun updateHadithStatus(edit: HadithEdit, status: String) {
        viewModelScope.launch {
            try {
                val id = edit.id ?: return@launch
                AppLogger.d("EditsVM", "Updating Hadith Status ID: $id to $status")
                SupabaseProvider.client.from("hadith_edits").update(
                    mapOf("status" to status)
                ) {
                    filter { eq("id", id) }
                }
                fetchHadithEdits()
            } catch (e: Exception) {
                AppLogger.d("EditsVM", "Hadith update error: ${e.message}")
                _error.value = "Update error: ${e.message}"
            }
        }
    }

    fun deleteHadithEdit(edit: HadithEdit) {
        viewModelScope.launch {
            try {
                val id = edit.id ?: return@launch
                AppLogger.d("EditsVM", "Deleting Hadith Edit ID: $id")
                SupabaseProvider.client.from("hadith_edits").delete {
                    filter { eq("id", id) }
                }
                fetchHadithEdits()
            } catch (e: Exception) {
                AppLogger.d("EditsVM", "Hadith delete error: ${e.message}")
                _error.value = "Delete error: ${e.message}"
            }
        }
    }

    fun approveSelectedQuranEdits() {
        viewModelScope.launch {
            try {
                val ids = _selectedQuranEditIds.value
                if (ids.isEmpty()) return@launch
                _isLoading.value = true
                SupabaseProvider.client.from("quran_edits").update(
                    mapOf("is_approved" to true)
                ) {
                    filter { isIn("id", ids.toList()) }
                }
                clearQuranSelection()
                fetchQuranEdits()
            } catch (e: Exception) {
                _error.value = "Bulk approve failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteSelectedQuranEdits() {
        viewModelScope.launch {
            try {
                val ids = _selectedQuranEditIds.value
                if (ids.isEmpty()) return@launch
                _isLoading.value = true
                SupabaseProvider.client.from("quran_edits").delete {
                    filter { isIn("id", ids.toList()) }
                }
                clearQuranSelection()
                fetchQuranEdits()
            } catch (e: Exception) {
                _error.value = "Bulk delete failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateSelectedHadithStatus(status: String) {
        viewModelScope.launch {
            try {
                val ids = _selectedHadithEditIds.value
                if (ids.isEmpty()) return@launch
                _isLoading.value = true
                SupabaseProvider.client.from("hadith_edits").update(
                    mapOf("status" to status)
                ) {
                    filter { isIn("id", ids.toList()) }
                }
                clearHadithSelection()
                fetchHadithEdits()
            } catch (e: Exception) {
                _error.value = "Bulk update failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteSelectedHadithEdits() {
        viewModelScope.launch {
            try {
                val ids = _selectedHadithEditIds.value
                if (ids.isEmpty()) return@launch
                _isLoading.value = true
                SupabaseProvider.client.from("hadith_edits").delete {
                    filter { isIn("id", ids.toList()) }
                }
                clearHadithSelection()
                fetchHadithEdits()
            } catch (e: Exception) {
                _error.value = "Bulk delete failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
