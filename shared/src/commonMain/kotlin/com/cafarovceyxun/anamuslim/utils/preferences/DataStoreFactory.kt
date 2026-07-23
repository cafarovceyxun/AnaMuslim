package com.cafarovceyxun.anamuslim.utils.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

expect fun createDataStore(producePath: () -> String): DataStore<Preferences>

internal const val DATASTORE_FILE_NAME = "app_preferences.preferences_pb"
