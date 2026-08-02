package com.cafarovceyxun.anamuslim.compose.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.ui.graphics.vector.ImageVector

object AppIcons {
    val Home = Icons.Rounded.Home
    val Hadith = Icons.Rounded.AutoStories
    val Search = Icons.Rounded.Search
    val Settings = Icons.Rounded.Settings
    
    val History = Icons.Rounded.History
    val Bookmarks = Icons.Rounded.Bookmark
    
    val VolumeUp = Icons.AutoMirrored.Rounded.VolumeUp
    val VolumeOff = Icons.AutoMirrored.Rounded.VolumeOff

    /** Filled sun = the screen is being held awake, outlined = it may sleep on the system timeout. */
    val ScreenAwake = Icons.Rounded.LightMode
    val ScreenAwakeOff = Icons.Outlined.LightMode

    val ScrollDown = Icons.Rounded.ArrowDownward
    val Quran = Icons.AutoMirrored.Rounded.MenuBook
    val Translation = Icons.Rounded.Translate
}
