package com.cafarovceyxun.anamuslim.compose.screens.qibla

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.prayer.CityPickerSheet
import com.cafarovceyxun.anamuslim.compose.components.qibla.QiblaCompassFace
import com.cafarovceyxun.anamuslim.compose.components.qibla.QiblaMapCanvas
import com.cafarovceyxun.anamuslim.compose.components.qibla.QiblaMapLayer
import com.cafarovceyxun.anamuslim.compose.components.qibla.QiblaTileStore
import com.cafarovceyxun.anamuslim.compose.utils.app.rememberLocationPermission
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrayerPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.QiblaPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_crosshair
import com.cafarovceyxun.anamuslim.resources.dr_icon_location
import com.cafarovceyxun.anamuslim.resources.prayerChooseLocation
import com.cafarovceyxun.anamuslim.resources.prayerUseMyLocation
import com.cafarovceyxun.anamuslim.resources.qiblaAligned
import com.cafarovceyxun.anamuslim.resources.qiblaAboveSeaLevel
import com.cafarovceyxun.anamuslim.resources.qiblaAtKaaba
import com.cafarovceyxun.anamuslim.resources.qiblaBearingValue
import com.cafarovceyxun.anamuslim.resources.qiblaCalibrate
import com.cafarovceyxun.anamuslim.resources.qiblaDistance
import com.cafarovceyxun.anamuslim.resources.qiblaHdHint
import com.cafarovceyxun.anamuslim.resources.qiblaInterference
import com.cafarovceyxun.anamuslim.resources.qiblaLayerSatellite
import com.cafarovceyxun.anamuslim.resources.qiblaLayerSatelliteHd
import com.cafarovceyxun.anamuslim.resources.qiblaLayerStreet
import com.cafarovceyxun.anamuslim.resources.qiblaMagneticOnly
import com.cafarovceyxun.anamuslim.resources.qiblaModeCompass
import com.cafarovceyxun.anamuslim.resources.qiblaModeMap
import com.cafarovceyxun.anamuslim.resources.qiblaNoLocation
import com.cafarovceyxun.anamuslim.resources.qiblaNoSensor
import com.cafarovceyxun.anamuslim.resources.qiblaPinHint
import com.cafarovceyxun.anamuslim.resources.qiblaPreciseOff
import com.cafarovceyxun.anamuslim.resources.qiblaTitle
import com.cafarovceyxun.anamuslim.resources.qiblaToKaaba
import com.cafarovceyxun.anamuslim.resources.qiblaTurnLeft
import com.cafarovceyxun.anamuslim.resources.qiblaTurnRight
import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint
import com.cafarovceyxun.anamuslim.utils.prayer.location.QIBLA_LOCATION_MAX_AGE_MILLIS
import com.cafarovceyxun.anamuslim.utils.qibla.AngleSmoother
import com.cafarovceyxun.anamuslim.utils.qibla.CompassCalibration
import com.cafarovceyxun.anamuslim.utils.qibla.HeadingReference
import com.cafarovceyxun.anamuslim.utils.qibla.QiblaHeading
import com.cafarovceyxun.anamuslim.utils.qibla.QiblaHeadingState
import com.cafarovceyxun.anamuslim.utils.qibla.QiblaMath
import com.cafarovceyxun.anamuslim.utils.qibla.compassReadings
import com.cafarovceyxun.anamuslim.utils.qibla.isCompassAvailable
import com.cafarovceyxun.anamuslim.utils.qibla.platformDeclinationDeg
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.viewModels.PrayerLocationViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Qiblə ekranı — iki rejim: **xəritə** və **kompas**.
 *
 * ### Niyə xəritə birincidir
 * Xəritə rejimi sensordan **tamamilə asılı deyil**: xəritə həqiqi şimala baxır, qiblə xətti isə sırf
 * həndəsədir. Ona görə o, kalibrsiz və ya maqnitometri olmayan cihazda da düzgün işləyir və
 * istifadəçi öz binasını tanıyırsa kompasdan **daha dəqiqdir**. Kompas onun üstünə gələn rahatlıqdır.
 *
 * ### Sensoru olmayan cihaz
 * [isCompassAvailable] `false` qaytaranda kompas tab-ı **ümumiyyətlə göstərilmir** və bir sətirlik
 * izah verilir — basılan, amma heç nə etməyən düymə qoymaq qadağandır (CLAUDE.md).
 */
@Composable
fun QiblaScreen() {
    val settings = PrayerPreferences.observeSettings()
    val basePoint = settings.point

    val scope = rememberCoroutineScope()
    val locationVm: PrayerLocationViewModel = viewModel { PrayerLocationViewModel() }

    // Düyməyə hər basış bu sayğacı artırır; xəritə onu açar kimi işlədib mərkəzi sıfırlayır.
    // Yalnız koordinatın dəyişməsinə güvənmək kifayət deyil — eyni yerdə dayanıb düyməni basanda
    // GPS həmin nöqtəni qaytarır və xəritə sürüşdürüldüyü yerdə qalardı.
    var recenterTick by remember { mutableStateOf(0) }
    val locating by locationVm.locating.collectAsState()
    val permission = rememberLocationPermission()

    // ⚠️ Qiblə ekranı bir günlük köhnə mövqe ilə kifayətlənmir. Səbəb
    // [QIBLA_LOCATION_MAX_AGE_MILLIS] KDoc-undadır: istifadəçi Ciddədən Məkkəyə keçəndə köhnə
    // nöqtə qiblə bucağını 111°-dən 22°-yə yanıldırdı. Namaz vaxtları üçün eyni köhnəlik
    // zərərsizdir, ona görə düzəliş yalnız bu ekrandadır.
    LaunchedEffect(permission.isGranted, permission.isPrecise) {
        if (!permission.isGranted) return@LaunchedEffect
        if (PrayerPreferences.getLocationMode() != PrayerPreferences.MODE_GPS) return@LaunchedEffect

        val age = currentEpochMillis() - PrayerPreferences.getLocationUpdatedAt()
        if (age > QIBLA_LOCATION_MAX_AGE_MILLIS) {
            locationVm.useDeviceLocation(QIBLA_LOCATION_MAX_AGE_MILLIS)
        }
    }

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(Res.string.qiblaTitle),
                actions = {
                    when {
                        locating -> CircularProgressIndicator(
                            modifier = Modifier.padding(end = 16.dp).size(20.dp),
                            strokeWidth = 2.dp,
                        )

                        // İcazə yoxdursa düymə göstərilmir — basılıb heç nə etməsin.
                        permission.isGranted -> IconButton(
                            onClick = {
                                scope.launch {
                                    // Ardıcıllıq vacibdir: əvvəl sancaq atılır, sonra mövqe
                                    // istənilir — əks halda yeni koordinat gələndə köhnə sancaq
                                    // hələ yerində olur və xəritə ona qayıdır.
                                    QiblaPreferences.clearPin()
                                    locationVm.useDeviceLocation(QIBLA_LOCATION_MAX_AGE_MILLIS)
                                }
                                recenterTick++
                            },
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_crosshair),
                                contentDescription = stringResource(Res.string.prayerUseMyLocation),
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            if (basePoint == null) {
                MissingLocation()
            } else {
                QiblaContent(
                    basePoint = basePoint,
                    placeName = settings.placeName,
                    recenterKey = recenterTick,
                    isPrecise = permission.isPrecise,
                    // Xəbərdarlıq toxunulandır: sistem dialoqu hələ işləyirsə icazəni yüksəldir.
                    // İşləmirsə istifadəçi Ayarlara getməlidir — mətn onu onsuz da deyir.
                    onRequestPrecise = { if (permission.canPrompt) permission.request() },
                )
            }
        }
    }
}

@Composable
private fun MissingLocation() {
    var showCityPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.qiblaNoLocation),
            style = typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = { showCityPicker = true },
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_location),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(Res.string.prayerChooseLocation),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }

    // Mövcud vərəq olduğu kimi işlədilir: oflayn şəhər kataloqu, GPS düyməsi və əl ilə koordinat
    // onsuz da oradadır, yəni qiblə üçün ayrıca yer seçimi yazılmır.
    CityPickerSheet(isOpen = showCityPicker, onClose = { showCityPicker = false })
}

@Composable
private fun QiblaContent(
    basePoint: GeoPoint,
    placeName: String,
    recenterKey: Int,
    isPrecise: Boolean,
    onRequestPrecise: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val compassAvailable = remember { isCompassAvailable() }

    // Kompas birincidir: ekranın əsas işi istiqamət göstərməkdir, xəritə isə onu dəqiqləşdirmək
    // üçündür.
    var mapMode by remember { mutableStateOf(false) }

    // Sancaq namaz koordinatından asılıdır: şəhər dəyişəndə köhnə sancaq özü atılır.
    var pin by remember(basePoint, recenterKey) {
        mutableStateOf(QiblaPreferences.pinFor(basePoint) ?: basePoint)
    }

    Column(Modifier.fillMaxSize()) {
        // ⚠️ Kobud mövqe Kəbəyə yaxın yerlərdə qibləni tamamilə yanıldır (bax
        // `LocationPermission.isPrecise`). Səssizcə səhv göstərməkdənsə açıq demək lazımdır.
        if (!isPrecise) {
            Text(
                text = stringResource(Res.string.qiblaPreciseOff),
                style = typography.bodySmall,
                color = colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.errorContainer)
                    .clickable(onClick = onRequestPrecise)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        if (compassAvailable) {
            TabRow(selectedTabIndex = if (mapMode) 1 else 0) {
                Tab(
                    selected = !mapMode,
                    onClick = { mapMode = false },
                    text = { Text(stringResource(Res.string.qiblaModeCompass)) },
                )
                Tab(
                    selected = mapMode,
                    onClick = { mapMode = true },
                    text = { Text(stringResource(Res.string.qiblaModeMap)) },
                )
            }
        }

        if (mapMode || !compassAvailable) {
            MapMode(
                // Lövbər namaz koordinatıdır və sancaq tərpənəndə DƏYİŞMİR — gəzmə həddi buna
                // görə ölçülür.
                anchor = basePoint,
                pin = pin,
                recenterKey = recenterKey,
                onPinChange = { moved ->
                    pin = moved
                    scope.launch { QiblaPreferences.setPin(moved) }
                },
                showNoSensorNote = !compassAvailable,
                modifier = Modifier.weight(1f),
            )
        } else {
            // ⚠️ Kompas **GPS koordinatından** işləyir, xəritədəki sancaqdan yox.
            // Sancaq yalnız xəritədə binaya görə düzləndirmək üçündür; onu sürüşdürmək kompasın
            // göstərdiyi istiqaməti dəyişməməlidir, çünki kompas cihazın harada **olduğunu**
            // göstərir, istifadəçinin xəritədə hara baxdığını yox.
            CompassMode(point = basePoint, placeName = placeName, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MapMode(
    anchor: GeoPoint,
    pin: GeoPoint,
    recenterKey: Int,
    onPinChange: (GeoPoint) -> Unit,
    showNoSensorNote: Boolean,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val layer = QiblaPreferences.observeLayer()
    val highResAvailable by QiblaTileStore.highResAvailable.collectAsState()

    // HD qatı serverdə söndürülübsə seçim də yox olur; açıq peyk qatına qayıdılır.
    LaunchedEffect(highResAvailable, layer) {
        if (!highResAvailable && layer == QiblaMapLayer.SATELLITE_HD) {
            QiblaPreferences.setLayer(QiblaMapLayer.SATELLITE)
        }
    }

    Column(modifier) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            QiblaMapCanvas(
                anchor = anchor,
                initialPin = pin,
                recenterKey = recenterKey,
                layer = layer,
                onPointChange = onPinChange,
                modifier = Modifier.fillMaxSize(),
            )

            // Atribusiya hüquqi tələbdir — həmişə görünməlidir.
            Text(
                text = layer.attribution,
                style = typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (option in QiblaMapLayer.entries) {
                    if (option == QiblaMapLayer.SATELLITE_HD && !highResAvailable) continue

                    FilterChip(
                        selected = option == layer,
                        onClick = { scope.launch { QiblaPreferences.setLayer(option) } },
                        label = { Text(stringResource(layerLabel(option))) },
                    )
                }
            }

            Text(
                text = stringResource(Res.string.qiblaPinHint),
                style = typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            if (layer == QiblaMapLayer.SATELLITE && highResAvailable) {
                Text(
                    text = stringResource(Res.string.qiblaHdHint),
                    style = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (showNoSensorNote) {
                Text(
                    text = stringResource(Res.string.qiblaNoSensor),
                    style = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            QiblaReadout(pin, Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun CompassMode(point: GeoPoint, placeName: String, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val smoother = remember(point) { AngleSmoother() }

    var state by remember(point) { mutableStateOf<QiblaHeadingState?>(null) }
    var smoothedHeading by remember(point) { mutableStateOf(0.0) }

    // Sapma hər oxunuşda yox, nöqtə başına bir dəfə hesablanır: o, metrlərlə yox, onlarla
    // kilometrlə dəyişir.
    val declination = remember(point) { platformDeclinationDeg(point, currentEpochMillis()) }

    DisposableEffect(point) {
        onDispose { smoother.reset() }
    }

    LaunchedEffect(point) {
        compassReadings().collect { reading ->
            val resolved = QiblaHeading.resolve(
                reading = reading,
                point = point,
                atMillis = currentEpochMillis(),
                platformDeclination = declination,
            )

            val wasAligned = state?.isAligned == true
            state = resolved
            smoothedHeading = smoother.next(resolved.trueHeadingDeg)

            if (resolved.isAligned && !wasAligned) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    val current = state

    val distanceMeters = remember(point) { QiblaMath.distanceToKaabaMeters(point) }
    val bearing = remember(point) { QiblaMath.bearingToKaaba(point) }

    val distanceLabel = if (distanceMeters < 1_000.0) {
        stringResource(Res.string.qiblaAtKaaba)
    } else {
        stringResource(Res.string.qiblaToKaaba, kilometreLabel(distanceMeters))
    }

    // Hündürlük yalnız **həqiqətən gələndə** göstərilir: tətbiq kobud mövqe istəyir, kobud mövqe
    // isə adətən hündürlük vermir və sahə 0 qalır. Sıfırı «dəniz səviyyəsi» kimi yazmaq yanlış
    // olardı.
    val elevationLabel = point.elevationMeters
        .takeIf { it >= 1.0 }
        ?.let { stringResource(Res.string.qiblaAboveSeaLevel, it.roundToInt().toString()) }

    Box(modifier.fillMaxSize()) {
        QiblaCompassFace(
            placeName = placeName.ifBlank { stringResource(Res.string.qiblaTitle) },
            subtitle = stringResource(Res.string.qiblaBearingValue, bearing.roundToInt().toString()),
            statusText = turnInstruction(current),
            // Üz hamarlanmış bucaqla çəkilir, qərarlar isə xam dəyərlə verilir: filtr gecikmə
            // gətirir, «düzləndi» siqnalı isə dərhal olmalıdır.
            trueHeadingDeg = smoothedHeading,
            qiblaBearingDeg = current?.qiblaBearingDeg ?: bearing,
            deltaDeg = current?.deltaDeg ?: 180.0,
            distanceLabel = distanceLabel,
            elevationLabel = elevationLabel,
        )

        current?.let {
            CompassWarnings(
                state = it,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun turnInstruction(state: QiblaHeadingState?): String = when {
    state == null -> ""
    state.isAligned -> stringResource(Res.string.qiblaAligned)
    state.deltaDeg >= 0.0 -> stringResource(Res.string.qiblaTurnRight, state.deltaDeg.roundToInt().toString())
    else -> stringResource(Res.string.qiblaTurnLeft, abs(state.deltaDeg).roundToInt().toString())
}

@Composable
private fun CompassWarnings(state: QiblaHeadingState, modifier: Modifier = Modifier) {
    val warnings = buildList {
        if (state.calibration == CompassCalibration.LOW ||
            state.calibration == CompassCalibration.UNRELIABLE
        ) {
            add(stringResource(Res.string.qiblaCalibrate))
        }
        if (state.hasInterference) add(stringResource(Res.string.qiblaInterference))
        if (state.reference == HeadingReference.MAGNETIC_ONLY) {
            add(stringResource(Res.string.qiblaMagneticOnly))
        }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        for (warning in warnings) {
                Text(
                text = warning,
                style = typography.bodySmall,
                color = colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp),
            )
        }
    }
}

/** Qiblə bucağı və Kəbəyə məsafə — hər iki rejimin altında eyni formada. */
@Composable
private fun QiblaReadout(point: GeoPoint, modifier: Modifier = Modifier) {
    val distanceMeters = remember(point) { QiblaMath.distanceToKaabaMeters(point) }

    if (distanceMeters < 1_000.0) {
        Text(
            text = stringResource(Res.string.qiblaAtKaaba),
            style = typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }

    val bearing = remember(point) { QiblaMath.bearingToKaaba(point) }

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(Res.string.qiblaBearingValue, bearing.roundToInt().toString()),
            style = typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.qiblaDistance, kilometreLabel(distanceMeters)),
            style = typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Məsafə etiketi: 10 km-ə qədər bir onluq rəqəmlə, sonra tam ədədlə.
 *
 * Yaxında tam kilometr çox kobuddur — Kəbəyə 1.2 km ilə 1.8 km arasındakı fərq istifadəçi üçün
 * mənalıdır; uzaqda isə onluq rəqəm mənasız dəqiqlik təəssüratı yaradır.
 */
private fun kilometreLabel(distanceMeters: Double): String {
    val km = distanceMeters / 1000.0
    if (km >= 10.0) return km.roundToInt().toString()

    val tenths = (km * 10.0).roundToInt()

    return "${tenths / 10}.${tenths % 10}"
}

private fun layerLabel(layer: QiblaMapLayer) = when (layer) {
    QiblaMapLayer.STREET -> Res.string.qiblaLayerStreet
    QiblaMapLayer.SATELLITE -> Res.string.qiblaLayerSatellite
    QiblaMapLayer.SATELLITE_HD -> Res.string.qiblaLayerSatelliteHd
}
