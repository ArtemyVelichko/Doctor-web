@file:OptIn(ExperimentalFoundationApi::class)

package com.example.appinspector.ui.composable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.appinspector.R
import com.example.appinspector.data.model.AppCardItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

/**
 * Группирует [AppCardItem] по первой букве имени (алфавит) и возвращает отсортированный список пар (буква, элементы).
 */
private fun ImmutableList<AppCardItem>.groupByFirstLetter(): List<Pair<String, ImmutableList<AppCardItem>>> {
    return groupBy { item ->
        item.name.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
    }
        .mapValues { (_, list) -> list.sortedBy { it.name.lowercase() }.toPersistentList() }
        .toSortedMap()
        .entries
        .map { (letter, list) -> letter to list }
}

@Composable
fun AppCardListHeader(
    letter: String,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = dimensionResource(R.dimen.list_horizontal_padding)
    val verticalPadding = dimensionResource(R.dimen.header_vertical_padding)
    Text(
        text = letter,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF455A64))
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    )
}

@Composable
fun CategorizedAppCardList(
    items: ImmutableList<AppCardItem>,
    onItemClick: (AppCardItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val categories = items.groupByFirstLetter()
    val listHorizontal = dimensionResource(R.dimen.list_horizontal_padding)
    val listVertical = dimensionResource(R.dimen.list_item_vertical_padding)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
    ) {
        categories.forEach { (letter, list) ->
            stickyHeader(key = letter) {
                AppCardListHeader(letter = letter)
            }
            itemsIndexed(
                items = list,
                key = { index, item ->
                    // Делаем уникальный ключ
                    item.subtitle ?: "${item.name}_$index"
                },
            ) { _, item ->
                AppCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = listHorizontal, vertical = listVertical),
                )
            }
        }
    }
}
