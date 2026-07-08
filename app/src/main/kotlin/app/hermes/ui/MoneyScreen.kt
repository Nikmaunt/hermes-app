package app.hermes.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hermes.R
import app.hermes.core.model.sumByCurrency

/** Money: transactions with a per-currency total (D13 — never a cross-currency sum). */
@Composable
fun MoneyScreen(modifier: Modifier = Modifier, viewModel: MoneyViewModel = hiltViewModel()) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    DemoScreen(isEmpty = transactions.isEmpty(), emptyIcon = Icons.Filled.Payments, modifier = modifier) {
        val totals = transactions.map { it.money }.sumByCurrency()
        if (totals.isNotEmpty()) {
            InfoCard(
                title = stringResource(R.string.money_total),
                subtitle = totals.entries.joinToString("  ") { DemoFormat.money(it.value, it.key) },
            )
        }
        transactions.forEach { tx ->
            InfoCard(
                title = tx.merchant ?: tx.category ?: DemoFormat.money(tx.amountMinor, tx.currency),
                subtitle = DemoFormat.money(tx.amountMinor, tx.currency),
                meta = DemoFormat.instant(tx.occurredAt),
            )
        }
    }
}
