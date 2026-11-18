package io.github.philips148.wellnesscoach.ui.navigation

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable

private data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem(
            route = AppRoutes.DASHBOARD,
            label = "Home",
            icon = Icons.Outlined.Home
        ),
        BottomNavItem(
            route = AppRoutes.LOGS,
            label = "Logs",
            icon = Icons.AutoMirrored.Outlined.List
        ),
        BottomNavItem(
            route = AppRoutes.AI_COACH,
            label = "AI Coach",
            icon = Icons.Outlined.SmartToy
        ),
        BottomNavItem(
            route = AppRoutes.PROGRESS,
            label = "Progress",
            icon = Icons.Outlined.BarChart
        ),
        BottomNavItem(
            route = AppRoutes.ACCOUNT_SETTINGS,
            label = "Profile",
            icon = Icons.Outlined.AccountCircle
        )
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    onNavigate(item.route)
                },
                icon = {
                    Icon(
                    imageVector = item.icon,
                    contentDescription = item.label
                )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 12.sp
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedTextColor = MaterialTheme.colorScheme.onPrimary,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    }
}