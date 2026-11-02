package com.bertschell.immichcontacts.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bertschell.immichcontacts.ui.screens.*

object Routes {
    const val SETUP = "setup"
    const val CONTACTS = "contacts"
    const val PERSON_ASSETS = "person_assets/{personId}/{personName}"
    const val LIBRARY = "library"
    const val CROP = "crop/{assetId}/{contactId}/{contactName}"
    const val SETTINGS = "settings"

    fun personAssets(personId: String, personName: String) = "person_assets/$personId/$personName"
    fun crop(assetId: String, contactId: Long, contactName: String) = "crop/$assetId/$contactId/$contactName"
}

@Composable
fun ImmichContactsApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SETUP) {
        composable(Routes.SETUP) {
            SetupScreen(
                onConnected = { navController.navigate(Routes.CONTACTS) { popUpTo(Routes.SETUP) { inclusive = true } } }
            )
        }
        composable(Routes.CONTACTS) {
            ContactsScreen(
                onContactClick = { contact, personId, personName ->
                    if (personId != null) {
                        navController.navigate(Routes.personAssets(personId, personName ?: ""))
                    } else {
                        navController.navigate(Routes.LIBRARY)
                    }
                },
                onBrowseLibrary = { navController.navigate(Routes.LIBRARY) },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.PERSON_ASSETS) { backStackEntry ->
            val personId = backStackEntry.arguments?.getString("personId") ?: return@composable
            val personName = backStackEntry.arguments?.getString("personName") ?: ""
            PersonAssetsScreen(
                personId = personId,
                personName = personName,
                onAssetSelected = { assetId, contactId, contactName ->
                    navController.navigate(Routes.crop(assetId, contactId, contactName))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onAssetSelected = { assetId, contactId, contactName ->
                    navController.navigate(Routes.crop(assetId, contactId, contactName))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.CROP) { backStackEntry ->
            val assetId = backStackEntry.arguments?.getString("assetId") ?: return@composable
            val contactId = backStackEntry.arguments?.getString("contactId")?.toLongOrNull() ?: return@composable
            val contactName = backStackEntry.arguments?.getString("contactName") ?: ""
            CropScreen(
                assetId = assetId,
                contactId = contactId,
                contactName = contactName,
                onDone = { navController.popBackStack(Routes.CONTACTS, inclusive = false) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onDisconnected = {
                    navController.navigate(Routes.SETUP) { popUpTo(0) { inclusive = true } }
                }
            )
        }
    }
}