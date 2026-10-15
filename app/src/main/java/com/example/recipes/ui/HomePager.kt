package com.example.recipes.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@Composable
fun HomePager(
    onRecipeClick: (Int) -> Unit,
    onDrinkClick: (Int) -> Unit,
    onCreate: (typeId: Int, daytimeId: Int) -> Unit,
    onCreateDrink: (typeId: Int) -> Unit,
    refreshSignal: Boolean,
    onRefreshHandled: () -> Unit
) {
    val homeVm: HomeViewModel = viewModel()
    val drinksVm: DrinksViewModel = viewModel()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshSignal) {
        if (refreshSignal) {
            homeVm.refresh()
            drinksVm.refresh()
            onRefreshHandled()
        }
    }

    BackHandler(enabled = pagerState.currentPage != 0) {
        scope.launch { pagerState.animateScrollToPage(0) }
    }

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> HomeScreen(
                    onRecipeClick = onRecipeClick,
                    onCreate = onCreate,
                    refreshSignal = false,
                    onRefreshHandled = {},
                    viewModel = homeVm
                )
                else -> DrinksScreen(
                    onDrinkClick = onDrinkClick,
                    onCreate = onCreateDrink,
                    viewModel = drinksVm
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(2) { i ->
                val selected = pagerState.currentPage == i
                val dotWidth by animateDpAsState(if (selected) 20.dp else 6.dp, label = "dotW")
                val color by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    label = "dotC"
                )
                Box(
                    Modifier
                        .height(6.dp)
                        .width(dotWidth)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { scope.launch { pagerState.animateScrollToPage(i) } }
                )
            }
        }
    }
}