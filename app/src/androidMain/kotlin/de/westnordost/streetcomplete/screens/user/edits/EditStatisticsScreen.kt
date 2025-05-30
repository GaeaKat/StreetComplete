package de.westnordost.streetcomplete.screens.user.edits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.ui.common.CenteredLargeTitleHint
import de.westnordost.streetcomplete.ui.ktx.plus
import kotlinx.coroutines.launch

/** Shows the user's edit statistics, alternatively either grouped by edit type or by country */
@Composable
fun EditStatisticsScreen(
    viewModel: EditStatisticsViewModel
) {
    var isCurrentWeek by remember { mutableStateOf(false) }

    val hasEditsOverall by viewModel.hasEdits.collectAsState()
    val editTypeStatisticsOverall by viewModel.editTypeStatistics.collectAsState()
    val countryStatisticsOverall by viewModel.countryStatistics.collectAsState()

    val hasEditsCurrentWeek by viewModel.hasEditsCurrentWeek.collectAsState()
    val editTypeStatisticsCurrentWeek by viewModel.editTypeStatisticsCurrentWeek.collectAsState()
    val countryStatisticsCurrentWeek by viewModel.countryStatisticsCurrentWeek.collectAsState()

    val hasEdits: Boolean = if (isCurrentWeek) hasEditsCurrentWeek else hasEditsOverall
    val editTypeStatistics = if (isCurrentWeek) editTypeStatisticsCurrentWeek else editTypeStatisticsOverall
    val countryStatistics = if (isCurrentWeek) countryStatisticsCurrentWeek else countryStatisticsOverall

    val flagAlignments by viewModel.flagAlignments.collectAsState()

    Box {
        if (hasEdits) {
            Column {
                val scope = rememberCoroutineScope()
                val pagerState = rememberPagerState(pageCount = { EditStatisticsTab.entries.size })
                val page = pagerState.targetPage

                Box(Modifier.background(MaterialTheme.colors.primarySurface)) {
                    TabRow(
                        selectedTabIndex = page,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    ) {
                        for (tab in EditStatisticsTab.entries) {
                            val index = tab.ordinal
                            Tab(
                                selected = page == index,
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                text = { Text(stringResource(tab.textId)) }
                            )
                        }
                    }
                }

                val insets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                ).asPaddingValues()

                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false,
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.weight(1f).consumeWindowInsets(insets)
                ) { p ->
                    when (EditStatisticsTab.entries[p]) {
                        EditStatisticsTab.ByType -> {
                            LaunchedEffect(Unit) { viewModel.queryEditTypeStatistics() }
                            if (editTypeStatistics != null) {
                                EditTypeStatisticsColumn(
                                    statistics = editTypeStatistics,
                                    contentPadding = insets + PaddingValues(top = 16.dp)
                                )
                            }
                        }
                        EditStatisticsTab.ByCountry -> {
                            LaunchedEffect(Unit) { viewModel.queryCountryStatistics() }
                            val alignments = flagAlignments
                            if (countryStatistics != null && alignments != null) {
                                CountryStatisticsColumn(
                                    statistics = countryStatistics,
                                    flagAlignments = alignments,
                                    isCurrentWeek = isCurrentWeek,
                                    contentPadding = insets + PaddingValues(top = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            val isSynchronizingStatistics by viewModel.isSynchronizingStatistics.collectAsState()
            CenteredLargeTitleHint(
                stringResource(
                    if (isSynchronizingStatistics) R.string.stats_are_syncing
                    else R.string.quests_empty
                )
            )
        }
        OutlinedButton(
            onClick = { isCurrentWeek = !isCurrentWeek },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Text(stringResource(
                if (isCurrentWeek) R.string.user_profile_current_week_title
                else R.string.user_profile_all_time_title
            ))
        }
    }
}

private enum class EditStatisticsTab(val textId: Int) {
    ByType(textId = R.string.user_statistics_filter_by_quest_type),
    ByCountry(textId = R.string.user_statistics_filter_by_country)
}
