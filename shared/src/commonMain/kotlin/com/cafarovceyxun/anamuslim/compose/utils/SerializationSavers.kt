package com.cafarovceyxun.anamuslim.compose.utils

import androidx.compose.runtime.saveable.Saver
import com.cafarovceyxun.anamuslim.api.JsonHelper
import kotlinx.serialization.KSerializer

/**
 * Builds a [Saver] that persists a kotlinx-`@Serializable` value as its JSON string.
 *
 * A JSON [String] is one of the few types the platform save registry can store (e.g. an Android
 * `Bundle`), so this lets `rememberSaveable` work for the KMP model classes that replaced the old
 * `@Parcelize` ones — they are `@Serializable` but neither `Parcelable` nor `java.io.Serializable`,
 * which is why saving them directly throws `IllegalStateException` from the SaveableStateRegistry.
 *
 * Pass a `.nullable` serializer when the state value may be null:
 * ```
 * rememberSaveable(stateSaver = serializableStateSaver(HadithVolume.serializer().nullable)) {
 *     mutableStateOf<HadithVolume?>(null)
 * }
 * ```
 */
fun <T> serializableStateSaver(serializer: KSerializer<T>): Saver<T, String> = Saver(
    save = { JsonHelper.json.encodeToString(serializer, it) },
    restore = { JsonHelper.json.decodeFromString(serializer, it) },
)
