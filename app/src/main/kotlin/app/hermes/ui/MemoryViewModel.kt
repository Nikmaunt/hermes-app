package app.hermes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hermes.core.data.dao.MemoryReadDao
import app.hermes.core.data.entity.MemoryFactEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MemoryViewModel @Inject constructor(memoryReadDao: MemoryReadDao) : ViewModel() {
    val facts: StateFlow<List<MemoryFactEntity>> = memoryReadDao.observeLive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_MS), emptyList())
}
