package app.hermes.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector
import app.hermes.R

/** Every navigation destination in the app, as a stable route string. */
object Routes {
    const val ONBOARDING = "onboarding"

    // Bottom-nav roots (+ capture, the centre action).
    const val TODAY = "today"
    const val INBOX = "inbox"
    const val CAPTURE = "capture"
    const val CHAT = "chat"
    const val MORE = "more"

    // Secondary, reached from More (or, later, deep links).
    const val MEMORY = "memory"
    const val DOCUMENTS = "documents"
    const val MONEY = "money"
    const val HABITS = "habits"
    const val PROJECTS = "projects"
    const val PEOPLE = "people"
    const val BRIEFS = "briefs"
    const val SETTINGS = "settings"
}

/**
 * The five bottom-nav slots: four tabs plus the centre [CAPTURE] action. Capture is NOT
 * a persistent tab — it is a full-screen destination reachable from anywhere (the centre
 * button today, an external share intent in M2), so its navigation must not depend on a
 * parent tab (D33).
 */
enum class TopLevelDestination(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    TODAY(Routes.TODAY, R.string.nav_today, Icons.Filled.Today),
    INBOX(Routes.INBOX, R.string.nav_inbox, Icons.Filled.Inbox),
    CAPTURE(Routes.CAPTURE, R.string.nav_capture, Icons.Filled.Add),
    CHAT(Routes.CHAT, R.string.nav_chat, Icons.AutoMirrored.Filled.Chat),
    MORE(Routes.MORE, R.string.nav_more, Icons.Filled.MoreHoriz),
}

/** Routes that keep the bottom bar visible (the real tabs; capture hides it). */
val BOTTOM_BAR_ROUTES: Set<String> = setOf(Routes.TODAY, Routes.INBOX, Routes.CHAT, Routes.MORE)

@StringRes
fun titleForRoute(route: String?): Int = when (route) {
    Routes.TODAY -> R.string.title_today
    Routes.INBOX -> R.string.title_inbox
    Routes.CAPTURE -> R.string.title_capture
    Routes.CHAT -> R.string.title_chat
    Routes.MORE -> R.string.title_more
    Routes.MEMORY -> R.string.title_memory
    Routes.DOCUMENTS -> R.string.title_documents
    Routes.MONEY -> R.string.title_money
    Routes.HABITS -> R.string.title_habits
    Routes.PROJECTS -> R.string.title_projects
    Routes.PEOPLE -> R.string.title_people
    Routes.BRIEFS -> R.string.title_briefs
    Routes.SETTINGS -> R.string.title_settings
    else -> R.string.app_name
}
