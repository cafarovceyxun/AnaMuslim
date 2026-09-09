package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.repository.supabase.AdminCountsRepository
import com.cafarovceyxun.anamuslim.repository.supabase.AdminPendingCounts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * İdarəetmə panelindəki sətirlərin yanındakı qırmızı nişanların mənbəyi.
 *
 * Say **gözləyən sətirlərin ümumi sayıdır**, «son baxışdan sonrakılar» deyil: admin təsdiq/rədd
 * etdikcə say öz-özünə azalır, ona görə cihazda «oxundu» vəziyyəti saxlamağa ehtiyac qalmır.
 */
class AdminBadgeViewModel : ViewModel() {

    private val _counts = MutableStateFlow(AdminPendingCounts())
    val counts = _counts.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _counts.value = AdminCountsRepository.fetch()
        }
    }
}
