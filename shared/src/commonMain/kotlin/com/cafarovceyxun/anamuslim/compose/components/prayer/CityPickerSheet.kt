package com.cafarovceyxun.anamuslim.compose.components.prayer

import androidx.compose.foundation.clickable
import com.cafarovceyxun.anamuslim.utils.prayer.location.City
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.PaddingValues
import org.jetbrains.compose.resources.painterResource
import com.cafarovceyxun.anamuslim.resources.dr_icon_crosshair
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheet
import com.cafarovceyxun.anamuslim.compose.components.prayer.PrayerUiFormat.ltrDigits
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.app.openAppSettings
import com.cafarovceyxun.anamuslim.compose.utils.app.rememberLocationPermission
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_location
import com.cafarovceyxun.anamuslim.resources.prayerChooseLocation
import com.cafarovceyxun.anamuslim.resources.prayerCityAttribution
import com.cafarovceyxun.anamuslim.resources.prayerCoordinatesInvalid
import com.cafarovceyxun.anamuslim.resources.prayerCoordinatesTitle
import com.cafarovceyxun.anamuslim.resources.prayerLatitude
import com.cafarovceyxun.anamuslim.resources.prayerLocationFailed
import com.cafarovceyxun.anamuslim.resources.prayerLocationPermissionMsg
import com.cafarovceyxun.anamuslim.resources.prayerLocationPermissionTitle
import com.cafarovceyxun.anamuslim.resources.prayerLongitude
import com.cafarovceyxun.anamuslim.resources.prayerNoCityFound
import com.cafarovceyxun.anamuslim.resources.prayerSearchCity
import com.cafarovceyxun.anamuslim.resources.prayerUseMyLocation
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelGotIt
import com.cafarovceyxun.anamuslim.viewModels.PrayerLocationViewModel
import androidx.compose.runtime.rememberCoroutineScope
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrayerPreferences
import com.cafarovceyxun.anamuslim.resources.prayerRemovePlace
import com.cafarovceyxun.anamuslim.resources.prayerSavedPlaces
import com.cafarovceyxun.anamuslim.resources.prayerSavedPlacesHint
import com.cafarovceyxun.anamuslim.utils.prayer.SavedPlace
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Yer seçimi vərəqi: cihaz mövqeyi → şəhər axtarışı → əl ilə koordinat.
 *
 * Üç yol qəsdən bir yerdədir. GPS icazəsi rədd ediləndə funksiya **itmir**: siyahı tamamilə
 * oflayndır (3521 şəhər tətbiqin içindədir), koordinat sahəsi isə siyahıda olmayan yerləri örtür.
 */
@Composable
fun CityPickerSheet(
    isOpen: Boolean,
    onClose: () -> Unit,
    vm: PrayerLocationViewModel = viewModel { PrayerLocationViewModel() },
) {
    val query by vm.query.collectAsState()
    val results by vm.results.collectAsState()
    val locating by vm.locating.collectAsState()
    val locationFailed by vm.locationFailed.collectAsState()

    val permission = rememberLocationPermission()
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showCoordinates by remember { mutableStateOf(false) }

    // İcazə verilən kimi mövqeyi al: istifadəçi dialoqda «icazə ver» deyəndən sonra düyməni
    // ikinci dəfə basmalı olmasın.
    var awaitingPermission by remember { mutableStateOf(false) }
    LaunchedEffect(permission.isGranted, awaitingPermission) {
        if (awaitingPermission && permission.isGranted) {
            awaitingPermission = false
            vm.useDeviceLocation(onSaved = onClose)
        }
    }

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onClose,
        icon = Res.drawable.dr_icon_location,
        title = stringResource(Res.string.prayerChooseLocation),
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PrimaryAction(
                text = stringResource(Res.string.prayerUseMyLocation),
                loading = locating,
                onClick = {
                    if (permission.isGranted) {
                        vm.useDeviceLocation(onSaved = onClose)
                    } else if (permission.shouldShowRationale) {
                        awaitingPermission = true
                        permission.request()
                    } else {
                        showPermissionDialog = true
                    }
                },
            )

            if (locationFailed) {
                Text(
                    text = stringResource(Res.string.prayerLocationFailed),
                    style = typography.bodySmall,
                    color = colorScheme.error,
                )
            }

            SavedPlacesSection(onSelect = { place -> vm.setSavedPlace(place, onSaved = onClose) })

            OutlinedTextField(
                value = query,
                onValueChange = vm::search,
                label = { Text(stringResource(Res.string.prayerSearchCity)) },
                singleLine = true,
                shape = shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )

            when {
                query.isNotBlank() && results.isEmpty() -> Text(
                    text = stringResource(Res.string.prayerNoCityFound),
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                )

                results.isNotEmpty() -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // `LazyColumn` YOX: vərəq onsuz da sürüşəndir, içəridə ikinci sürüşən konteyner
                    // jesti bölür. Nəticə sayı onsuz da 20 ilə məhduddur.
                    results.forEach { city ->
                        CityRow(city = city) { vm.selectCity(city, onSaved = onClose) }
                    }
                }
            }

            TextButton(
                onClick = { showCoordinates = !showCoordinates },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            ) {
                Text(stringResource(Res.string.prayerCoordinatesTitle))
            }

            if (showCoordinates) {
                ManualCoordinates { latitude, longitude ->
                    vm.setManualPoint(latitude, longitude, onSaved = onClose)
                }
            }

            // GeoNames CC BY 4.0 atribusiya tələb edir.
            Text(
                text = stringResource(Res.string.prayerCityAttribution),
                style = typography.labelSmall,
                color = colorScheme.onSurfaceVariant.alpha(0.75f),
            )
        }
    }

    AlertDialog(
        isOpen = showPermissionDialog,
        onClose = { showPermissionDialog = false },
        title = stringResource(Res.string.prayerLocationPermissionTitle),
        actions = listOf(
            AlertDialogAction(text = stringResource(Res.string.strLabelCancel)),
            AlertDialogAction(
                text = stringResource(Res.string.strLabelGotIt),
                style = AlertDialogActionStyle.Primary,
                onClick = {
                    // `shouldShowRationale` false = sistem artıq soruşmayacaq → yeganə yol Ayarlardır.
                    if (permission.shouldShowRationale) {
                        awaitingPermission = true
                        permission.request()
                    } else {
                        openAppSettings()
                    }
                    showPermissionDialog = false
                },
            ),
        ),
        content = {
            Text(
                text = stringResource(Res.string.prayerLocationPermissionMsg),
                style = typography.bodyMedium,
            )
        },
    )
}

/** Vərəqin əsas hərəkəti — dolu, tam enli, yüklənəndə göstərici ilə. */
@Composable
private fun PrimaryAction(text: String, loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !loading,
        shape = shapes.large,
        contentPadding = PaddingValues(vertical = 14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = colorScheme.onPrimary,
                )
            } else {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_crosshair),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(text, style = typography.titleSmall)
        }
    }
}

/** Axtarış nəticəsi — ad solda, ölkə kodu sağda rozet kimi. */
@Composable
private fun CityRow(city: City, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = city.name,
            style = typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = city.country,
            style = typography.labelMedium.ltrDigits(),
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(shapes.small)
                .background(colorScheme.surfaceVariant.alpha(0.6f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/**
 * İşlədilmiş yerlər. Ayrıca «yadda saxla» düyməsi yoxdur — hər təyin edilən yer siyahıya düşür,
 * ona görə səyahətdən sonra köhnə şəhərə qayıtmaq bir toxunuşdur.
 *
 * Cari yer siyahıda birincidir; onu göstərmək faydalıdır (istifadəçi hansı yerdə olduğunu görür),
 * amma silinməsi mənasızdır — silmə düyməsi yalnız qalanlarda var.
 */
@Composable
private fun SavedPlacesSection(onSelect: (SavedPlace) -> Unit) {
    val places = PrayerPreferences.observeSavedPlaces()
    if (places.isEmpty()) return

    val scope = rememberCoroutineScope()
    val current = PrayerPreferences.observeSettings()

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(Res.string.prayerSavedPlaces),
            style = typography.labelLarge,
            color = colorScheme.primary,
            modifier = Modifier.padding(bottom = 2.dp),
        )

        places.forEach { place ->
            // ⚠️ Ada görə müqayisə etmirik: ad indi platformanın geocoder-indən gəlir və eyni
            // nöqtə üçün Android «Xırdalan», iOS «Khirdalan» qaytara bilər. `isSameSpot` koordinatı
            // iki onluğa yuvarlaqlaşdırır — siyahının dublikat filtri də eyni açardan istifadə edir.
            val isCurrent = current.point?.let { place.isSameSpot(SavedPlace(place.name, it)) } == true

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.medium)
                    .background(
                        if (isCurrent) colorScheme.primary.alpha(0.12f) else Color.Transparent
                    )
                    .clickable { onSelect(place) }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = place.name,
                    style = typography.bodyLarge,
                    color = if (isCurrent) colorScheme.primary else colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )

                if (!isCurrent) {
                    Text(
                        text = stringResource(Res.string.prayerRemovePlace),
                        style = typography.labelMedium,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { scope.launch { PrayerPreferences.removeSavedPlace(place) } }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }

        Text(
            text = stringResource(Res.string.prayerSavedPlacesHint),
            style = typography.labelSmall,
            color = colorScheme.onSurfaceVariant.alpha(0.8f),
        )
    }
}

@Composable
private fun ManualCoordinates(onSubmit: (Double, Double) -> Unit) {
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }

    val parsedLatitude = latitude.trim().toDoubleOrNull()
    val parsedLongitude = longitude.trim().toDoubleOrNull()
    val valid = parsedLatitude != null && parsedLongitude != null &&
        parsedLatitude in -90.0..90.0 && parsedLongitude in -180.0..180.0

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = latitude,
                onValueChange = { latitude = it },
                label = { Text(stringResource(Res.string.prayerLatitude)) },
                singleLine = true,
                keyboardOptions = numericKeyboard(),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = longitude,
                onValueChange = { longitude = it },
                label = { Text(stringResource(Res.string.prayerLongitude)) },
                singleLine = true,
                keyboardOptions = numericKeyboard(),
                modifier = Modifier.weight(1f),
            )
        }

        if ((latitude.isNotBlank() || longitude.isNotBlank()) && !valid) {
            Text(
                text = stringResource(Res.string.prayerCoordinatesInvalid),
                style = typography.bodySmall,
                color = colorScheme.error,
            )
        }

        Button(
            onClick = { if (valid) onSubmit(parsedLatitude!!, parsedLongitude!!) },
            enabled = valid,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.prayerCoordinatesTitle))
        }
    }
}

private fun numericKeyboard() = androidx.compose.foundation.text.KeyboardOptions(
    keyboardType = KeyboardType.Decimal,
)
