package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val customCss: String = "",
    val customJs: String = ""
)

@Entity(tableName = "components")
data class ComponentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: Int,
    val parentId: String? = null,
    val type: String, // "heading", "paragraph", "button", "link", "image", "input", "divider", "container", "card_hero", "card_pricing"
    val text: String,
    val orderIndex: Int,
    val bgStyle: String = "transparent", // transparent, white, dark, primary, secondary, accent, glass
    val textColorStyle: String = "dark",  // dark, light, primary, accent
    val paddingStyle: String = "medium",  // none, small, medium, large
    val marginStyle: String = "none",     // none, small, medium, large
    val roundedStyle: String = "none",    // none, small, medium, large, full
    val widthStyle: String = "full",      // auto, half, full
    val alignment: String = "center",     // left, center, right
    val isFlexRow: Boolean = false,
    val href: String = "",
    val imageUrl: String = "https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=500",
    val placeholder: String = ""
)
