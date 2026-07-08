package app.hermes.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hermes.R

/** Documents: contracts/subscriptions with optional money. renewsOn is display + the
 *  30-day window only — never an alarm (anything that must fire becomes a followup, D8). */
@Composable
fun DocumentsScreen(modifier: Modifier = Modifier, viewModel: DocumentsViewModel = hiltViewModel()) {
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    DemoScreen(isEmpty = documents.isEmpty(), emptyIcon = Icons.Filled.Description, modifier = modifier) {
        documents.forEach { doc ->
            val money = doc.money?.let { DemoFormat.money(it.amountMinor, it.currency) }
            val renews = doc.renewsOn?.let { stringResource(R.string.demo_renews, DemoFormat.dateOnly(it)) }
            InfoCard(
                title = doc.title,
                subtitle = doc.provider,
                meta = listOfNotNull(money, doc.billingPeriod?.wire, renews).joinToString(" · ").ifEmpty { null },
            )
        }
    }
}
