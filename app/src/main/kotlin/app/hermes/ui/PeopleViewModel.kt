package app.hermes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hermes.core.data.dao.PersonDao
import app.hermes.core.data.entity.PersonEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class PeopleViewModel @Inject constructor(personDao: PersonDao) : ViewModel() {
    val people: StateFlow<List<PersonEntity>> = personDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_MS), emptyList())
}
