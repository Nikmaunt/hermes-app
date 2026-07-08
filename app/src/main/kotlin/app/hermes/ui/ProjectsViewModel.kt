package app.hermes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.hermes.core.data.dao.DecisionDao
import app.hermes.core.data.dao.ProjectDao
import app.hermes.core.data.entity.DecisionEntity
import app.hermes.core.data.entity.ProjectEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProjectsUi(
    val projects: List<ProjectEntity> = emptyList(),
    val decisions: List<DecisionEntity> = emptyList(),
)

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    projectDao: ProjectDao,
    decisionDao: DecisionDao,
) : ViewModel() {
    val state: StateFlow<ProjectsUi> =
        combine(projectDao.observeAll(), decisionDao.observeAll()) { projects, decisions ->
            ProjectsUi(projects, decisions)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_MS), ProjectsUi())
}
