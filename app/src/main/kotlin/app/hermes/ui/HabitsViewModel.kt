package app.hermes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hermes.core.data.dao.HabitDao
import app.hermes.core.data.entity.HabitEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HabitsViewModel @Inject constructor(habitDao: HabitDao) : ViewModel() {
    val habits: StateFlow<List<HabitEntity>> = habitDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_MS), emptyList())
}
