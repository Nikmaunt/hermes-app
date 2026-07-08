package app.hermes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hermes.core.data.dao.FollowupDao
import app.hermes.core.data.entity.FollowupEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class TodayViewModel @Inject constructor(followupDao: FollowupDao) : ViewModel() {
    val followups: StateFlow<List<FollowupEntity>> = followupDao.observePending()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_MS), emptyList())
}
