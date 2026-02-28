package com.klecer.gottado

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.klecer.gottado.ui.navigation.AppNavigation
import com.klecer.gottado.ui.navigation.NavRoutes
import com.klecer.gottado.ui.theme.GottaDoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val navigateTo = intent?.getStringExtra("navigate_to")
        setContent {
            GottaDoTheme {
                MainScaffold(initialRoute = navigateTo)
            }
        }
    }
}

private data class TopTab(val route: String, val labelRes: Int)

private val TOP_TABS = listOf(
    TopTab(NavRoutes.WIDGET_LIST, R.string.tab_widgets),
    TopTab(NavRoutes.CATEGORY_LIST, R.string.tab_categories),
    TopTab(NavRoutes.RECORD_EDIT_OPTIONS, R.string.tab_edit_options),
    TopTab(NavRoutes.TRASH, R.string.tab_trash)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(initialRoute: String? = null) {
    val context = LocalContext.current
    val navController = rememberNavController()
    var navigated by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val isTopLevel = TOP_TABS.any { it.route == currentRoute }
    val selectedTabIndex = TOP_TABS.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    androidx.compose.runtime.LaunchedEffect(initialRoute) {
        if (!navigated && initialRoute != null) {
            navigated = true
            navController.navigate(initialRoute)
        }
    }

    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text(stringResource(R.string.support_dialog_title)) },
            text = {
                Text(
                    stringResource(R.string.support_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSupportDialog = false
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://tomasklecer.gumroad.com/l/rjaup"))
                    )
                }) {
                    Text(stringResource(R.string.support_dialog_open))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSupportDialog = false }) {
                    Text(stringResource(R.string.support_dialog_close))
                }
            }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(stringResource(R.string.help_dialog_title)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    HelpSection(stringResource(R.string.help_adding_widget))
                    HelpBody(stringResource(R.string.help_adding_widget_body))
                    HelpSection(stringResource(R.string.help_widget_buttons))
                    HelpBody(stringResource(R.string.help_widget_buttons_body))
                    HelpBody(stringResource(R.string.help_widget_buttons_list))
                    HelpSection(stringResource(R.string.help_managing_entries))
                    HelpBody(stringResource(R.string.help_managing_entries_body))
                    HelpSection(stringResource(R.string.help_categories))
                    HelpBody(stringResource(R.string.help_categories_body))
                    HelpSection(stringResource(R.string.help_calendar_sync))
                    HelpBody(stringResource(R.string.help_calendar_sync_body))
                    HelpSection(stringResource(R.string.help_notifications))
                    HelpBody(stringResource(R.string.help_notifications_body))
                    HelpSection(stringResource(R.string.help_routines))
                    HelpBody(stringResource(R.string.help_routines_body))
                    HelpSection(stringResource(R.string.help_templates))
                    HelpBody(stringResource(R.string.help_templates_body))
                    HelpSection(stringResource(R.string.help_entry_options))
                    HelpBody(stringResource(R.string.help_entry_options_body))
                    HelpSection(stringResource(R.string.help_history))
                    HelpBody(stringResource(R.string.help_history_body))
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(stringResource(R.string.help_dialog_close))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    navigationIcon = {
                        if (!isTopLevel) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showHelpDialog = true }) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = stringResource(R.string.help_dialog_title),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(onClick = { showSupportDialog = true }) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = stringResource(R.string.support_dialog_title),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(onClick = {
                            navController.navigate(NavRoutes.SETTINGS) {
                                launchSingleTop = true
                            }
                        }) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                        }
                    }
                )
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 0.dp
                ) {
                    TOP_TABS.forEachIndexed { index, tab ->
                        Tab(
                            selected = index == selectedTabIndex,
                            onClick = {
                                if (tab.route != currentRoute) {
                                    navController.navigate(tab.route) {
                                        popUpTo(NavRoutes.WIDGET_LIST) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            text = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        AppNavigation(
            navController = navController,
            context = context,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun HelpSection(title: String) {
    Spacer(Modifier.height(12.dp))
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun HelpBody(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}
