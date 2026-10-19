package com.ghostpocket.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun CategoryIcon(
    categoryName: String,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    // Dynamic matching based on keywords in custom category names, or default spooky fallback
    val lower = categoryName.lowercase()
    val icon: ImageVector = when {
        lower.contains("food") || lower.contains("eat") || lower.contains("meal") || lower.contains("lunch") || lower.contains("dinner") -> Icons.Default.Restaurant
        lower.contains("chai") || lower.contains("coffee") || lower.contains("tea") || lower.contains("cafe") -> Icons.Default.LocalCafe
        lower.contains("travel") || lower.contains("metro") || lower.contains("bus") || lower.contains("cab") || lower.contains("auto") -> Icons.Default.DirectionsSubway
        lower.contains("college") || lower.contains("book") || lower.contains("study") || lower.contains("fee") || lower.contains("class") -> Icons.Default.School
        lower.contains("shop") || lower.contains("dress") || lower.contains("amazon") || lower.contains("buy") -> Icons.Default.ShoppingBag
        lower.contains("movie") || lower.contains("game") || lower.contains("fun") || lower.contains("party") -> Icons.Default.Movie
        lower.contains("hostel") || lower.contains("rent") || lower.contains("room") || lower.contains("stay") -> Icons.Default.Home
        lower.contains("friend") || lower.contains("gang") || lower.contains("squad") || lower.contains("bro") -> Icons.Default.Group
        lower.contains("snack") || lower.contains("maggie") || lower.contains("pizza") || lower.contains("burger") -> Icons.Default.Fastfood
        lower.contains("recharge") || lower.contains("bill") || lower.contains("wifi") || lower.contains("phone") -> Icons.Default.Bolt
        else -> Icons.Default.LocalFireDepartment // Spooky mystical default
    }
    Icon(
        imageVector = icon,
        contentDescription = categoryName,
        modifier = modifier,
        tint = tint
    )
}
