package com.bertschell.immichcontacts.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bertschell.immichcontacts.ui.screens.*

object Routes {
    const val SETUP = "setup"
    const val CONTACTS = "contacts"
    const val PERSON_ASSETS = "person_assets/{personId}/{personName}/{contactId}/{contactName}"
    const val LIBRARY = "library/{contactId}/{contactName}"
    const val CROP = "crop/{assetId}/{contactId}/{contactName}"
    const val SETTINGS = "settings"

    fun personAssets(personId: String, personName: String, contactId: Long, contactName: String) =
        "person_assets/$personId/$personName/$contactId/$contactName"
    fun library(contactId: Long, contactName: String) =
        "library/$contactId/$contactName"
    fun crop(assetId: String, contactId: Long, contactName: String) =
        "crop/$assetId/$contactId/$contactName"
}

@Composable
fun ImmichContactsApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SETUP) {
        composable(Routes.SETUP) {
            SetupScreen(
                onConnected = {
                    navController.navigate(Routes.CONTACTS) { popUpTo(Routes.SETUP) { inclusive = true } }
                }
            )
        }
        composable(Routes.CONTACTS) {
            ContactsScreen(
                onContactWithPerson = { contact, personId, personName ->
                    navController.navigate(Routes.personAssets(personId, personName, contact.id, contact.displayName))
                },
                onContactBrowseLibrary = { contact ->
                    navController.navigate(Routes.library(contact.id, contact.displayName))
                },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(
            route = Routes.PERSON_ASSETS,
            arguments = listOf(
                navArgument("personId") { type = NavType.StringType },
                navArgument("personName") { type = NavType.StringType },
                navArgument("contactId") { type = NavType.LongType },
                navArgument("contactName") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments!!
            PersonAssetsScreen(
                personId = args.getString("personId")!!,
                personName = args.getString("personName")!!,
                contactId = args.getLong("contactId"),
                contactName = args.getString("contactName")!!,
                onAssetSelected = { assetId ->
                    navController.navigate(Routes.crop(assetId, args.getLong("contactId"), args.getString("contactName")!!))
                },
                onBrowseLibrary = {
                    navController.navigate(Routes.library(args.getLong("contactId"), args.getString("contactName")!!))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.LIBRARY,
            arguments = listOf(
                navArgument("contactId") { type = NavType.LongType },
                navArgument("contactName") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments!!
            LibraryScreen(
                contactId = args.getLong("contactId"),
                contactName = args.getString("contactName")!!,
                onAssetSelected = { assetId ->
                    navController.navigate(Routes.crop(assetId, args.getLong("contactId"), args.getString("contactName")!!))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.CROP,
            arguments = listOf(
                navArgument("assetId") { type = NavType.StringType },
                navArgument("contactId") { type = NavType.LongType },
                navArgument("contactName") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments!!
            CropScreen(
                assetId = args.getString("assetId")!!,
                contactId = args.getLong("contactId"),
                contactName = args.getString("contactName")!!,
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