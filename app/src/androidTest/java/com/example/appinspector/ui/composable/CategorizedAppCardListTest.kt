package com.example.appinspector.ui.composable

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.appinspector.data.model.AppCardItem
import com.example.appinspector.ui.theme.AppInspectorTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class CategorizedAppCardListTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun list_rendersAppNameAndSubtitle() {
        val items = persistentListOf(
            AppCardItem(name = "Chrome", subtitle = "com.android.chrome"),
            AppCardItem(name = "Gmail", subtitle = "com.google.android.gm"),
        )

        composeTestRule.setContent {
            AppInspectorTheme {
                CategorizedAppCardList(items = items)
            }
        }

        composeTestRule.onNodeWithText("Chrome").assertIsDisplayed()
        composeTestRule.onNodeWithText("com.android.chrome").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gmail").assertIsDisplayed()
    }

    @Test
    fun appCard_click_callsCallbackWithCorrectItem() {
        val items = persistentListOf(
            AppCardItem(name = "Chrome", subtitle = "com.android.chrome"),
            AppCardItem(name = "Gmail", subtitle = "com.google.android.gm"),
        )
        var clicked: AppCardItem? = null

        composeTestRule.setContent {
            AppInspectorTheme {
                CategorizedAppCardList(
                    items = items,
                    onItemClick = { clicked = it },
                )
            }
        }

        composeTestRule.onNodeWithText("Chrome").performClick()

        assertNotNull(clicked)
        assertEquals("com.android.chrome", clicked?.subtitle)
    }

    @Test
    fun longList_scrollAndClick_worksForFarItem() {
        val items = (1..1000).map { index ->
            AppCardItem(
                name = "App $index",
                subtitle = "com.example.app$index",
            )
        }.toPersistentList()

        var clicked: AppCardItem? = null

        composeTestRule.setContent {
            AppInspectorTheme {
                CategorizedAppCardList(
                    items = items,
                    onItemClick = { clicked = it },
                )
            }
        }

        composeTestRule.onNodeWithText("App 1000").performScrollTo().performClick()

        assertEquals("com.example.app1000", clicked?.subtitle)
    }

    @Test
    fun list_groupsItemsByFirstLetter_showsHeaders() {
        val items = persistentListOf(
            AppCardItem(name = "Alpha", subtitle = "com.example.alpha"),
            AppCardItem(name = "Beta", subtitle = "com.example.beta"),
            AppCardItem(name = "Another", subtitle = "com.example.another"),
        )

        composeTestRule.setContent {
            AppInspectorTheme {
                CategorizedAppCardList(items = items)
            }
        }

        composeTestRule.onNodeWithText("A").assertIsDisplayed()
        composeTestRule.onNodeWithText("Beta").performScrollTo()
        composeTestRule.onNodeWithText("B").assertIsDisplayed()
    }

    @Test
    fun duplicateNames_withDifferentPackage_doesNotBreakScrollAndClick() {
        val duplicated = (1..30).map { index ->
            AppCardItem(
                name = "Main components",
                subtitle = "com.example.maincomponents.$index",
            )
        }.toPersistentList()
        var clicked: AppCardItem? = null

        composeTestRule.setContent {
            AppInspectorTheme {
                CategorizedAppCardList(
                    items = duplicated,
                    onItemClick = { clicked = it },
                )
            }
        }

        // Click first visible item to verify no crash with duplicate keys
        composeTestRule.onNodeWithTag("com.example.maincomponents.1").performClick()
        assertEquals("com.example.maincomponents.1", clicked?.subtitle)
        
        // Click another item to verify stable keys
        composeTestRule.onNodeWithTag("com.example.maincomponents.2").performClick()
        assertEquals("com.example.maincomponents.2", clicked?.subtitle)
    }
}
