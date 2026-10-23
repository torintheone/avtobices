package app.steamgarage.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.steamgarage.R
import app.steamgarage.data.ThemeMode
import app.steamgarage.domain.ItemStatus
import app.steamgarage.ui.components.BrassPlate
import app.steamgarage.ui.components.EngravedText
import app.steamgarage.ui.components.GaugeReading
import app.steamgarage.ui.components.OdometerDisplay
import app.steamgarage.ui.components.PipeDivider
import app.steamgarage.ui.components.SectionTitle
import app.steamgarage.ui.components.StatusBadge
import app.steamgarage.ui.components.SteamGauge
import app.steamgarage.ui.components.SteamGears
import app.steamgarage.ui.components.SteamLever
import app.steamgarage.ui.components.SteamPanel
import app.steamgarage.ui.components.SteamSwitchRow
import app.steamgarage.ui.components.ValveButton
import app.steamgarage.ui.components.statusLabel
import app.steamgarage.ui.theme.Steam

/** Витрина механизмов (этап 0): все базовые компоненты с тестовыми данными. */
@Composable
fun ShowcaseScreen(
    contentPadding: PaddingValues,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val palette = Steam.palette
    var leverCount by rememberSaveable { mutableIntStateOf(0) }
    var lastValve by rememberSaveable { mutableStateOf("") }
    var liveWear by rememberSaveable { mutableFloatStateOf(0.45f) }
    var sound by rememberSaveable { mutableStateOf(true) }
    var voice by rememberSaveable { mutableStateOf(true) }
    var gears by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.showcase_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.text,
        )

        // ── Латунная табличка ──
        SectionTitle(stringResource(R.string.showcase_plate))
        BrassPlate(modifier = Modifier.fillMaxWidth(), onClick = {}) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    EngravedText(stringResource(R.string.showcase_plate_car), style = Steam.text.plateTitle)
                    EngravedText(stringResource(R.string.showcase_plate_number), style = Steam.text.plateCaption)
                }
                EngravedText(stringResource(R.string.showcase_plate_shared), style = Steam.text.plateCaption)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OdometerDisplay("085 247", style = Steam.text.odometer)
                Spacer(Modifier.width(8.dp))
                EngravedText(stringResource(R.string.unit_km), style = Steam.text.plateCaption)
                Spacer(Modifier.weight(1f))
                SteamGauge(GaugeReading(ItemStatus.OVERDUE, 1.1f), Modifier.size(44.dp), compact = true)
                Spacer(Modifier.width(6.dp))
                SteamGauge(GaugeReading(ItemStatus.SOON, 0.93f), Modifier.size(44.dp), compact = true)
                Spacer(Modifier.width(6.dp))
                SteamGauge(GaugeReading(ItemStatus.OK, 0.4f), Modifier.size(44.dp), compact = true)
            }
        }

        PipeDivider()

        // ── Манометры ──
        SectionTitle(stringResource(R.string.showcase_gauges))
        Text(
            text = stringResource(R.string.showcase_gauges_hint),
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SampleGaugeCard(
                name = stringResource(R.string.sample_engine_oil),
                reading = GaugeReading(ItemStatus.SOON, 0.94f, 0.1f, "600", stringResource(R.string.unit_km_left)),
                modifier = Modifier.weight(1f),
            )
            SampleGaugeCard(
                name = stringResource(R.string.sample_cabin_filter),
                reading = GaugeReading(ItemStatus.OK, 0.35f, 0.1f, "9 800", stringResource(R.string.unit_km_left)),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SampleGaugeCard(
                name = stringResource(R.string.sample_brake_fluid),
                reading = GaugeReading(ItemStatus.OVERDUE, 1.08f, 0.04f, "58", stringResource(R.string.unit_days_over)),
                modifier = Modifier.weight(1f),
            )
            SampleGaugeCard(
                name = stringResource(R.string.sample_timing_belt),
                reading = GaugeReading(ItemStatus.NO_DATA, null),
                modifier = Modifier.weight(1f),
            )
        }
        SteamPanel(Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.showcase_gauge_live),
                style = MaterialTheme.typography.bodyMedium,
            )
            val liveStatus = when {
                liveWear >= 1f -> ItemStatus.OVERDUE
                liveWear >= 0.9f -> ItemStatus.SOON
                else -> ItemStatus.OK
            }
            SteamGauge(
                reading = GaugeReading(
                    status = liveStatus,
                    wear = liveWear,
                    warnFraction = 0.1f,
                    readout = "${(liveWear * 100).toInt()}",
                    unit = stringResource(R.string.unit_percent),
                ),
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(220.dp)
                    .align(Alignment.CenterHorizontally),
                readoutStyle = Steam.text.odometer,
            )
            val liveLabel = stringResource(R.string.showcase_gauge_live_label)
            Slider(
                value = liveWear,
                onValueChange = { liveWear = it },
                valueRange = 0f..1.3f,
                modifier = Modifier.semantics { contentDescription = liveLabel },
                colors = SliderDefaults.colors(
                    thumbColor = palette.brassLight,
                    activeTrackColor = palette.copper,
                    inactiveTrackColor = palette.surfaceDeep,
                ),
            )
        }

        PipeDivider()

        // ── Рычаг ──
        SectionTitle(stringResource(R.string.showcase_lever))
        Text(
            text = stringResource(R.string.showcase_lever_hint),
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
        )
        SteamLever(
            text = stringResource(R.string.sample_lever_replaced),
            onPull = { leverCount++ },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.showcase_lever_count, leverCount),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.text,
        )

        PipeDivider()

        // ── Вентили ──
        SectionTitle(stringResource(R.string.showcase_valves))
        val valveAdd = stringResource(R.string.sample_valve_add_item)
        val valveEdit = stringResource(R.string.sample_valve_edit)
        val valveService = stringResource(R.string.sample_valve_service)
        ValveButton(text = valveAdd, onClick = { lastValve = valveAdd })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ValveButton(text = valveEdit, onClick = { lastValve = valveEdit })
            ValveButton(text = valveService, onClick = { lastValve = valveService })
        }
        if (lastValve.isNotEmpty()) {
            Text(
                text = stringResource(R.string.showcase_valve_last, lastValve),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.text,
            )
        }

        PipeDivider()

        // ── Тумблеры ──
        SectionTitle(stringResource(R.string.showcase_toggles))
        SteamPanel(Modifier.fillMaxWidth()) {
            SteamSwitchRow(
                text = stringResource(R.string.settings_theme_day_toggle),
                checked = themeMode == ThemeMode.LIGHT,
                onCheckedChange = { onThemeModeChange(if (it) ThemeMode.LIGHT else ThemeMode.DARK) },
            )
            SteamSwitchRow(
                text = stringResource(R.string.settings_sound_toggle),
                supportingText = stringResource(R.string.settings_toggle_soon),
                checked = sound,
                onCheckedChange = { sound = it },
            )
            SteamSwitchRow(
                text = stringResource(R.string.settings_voice_toggle),
                supportingText = stringResource(R.string.settings_toggle_soon),
                checked = voice,
                onCheckedChange = { voice = it },
            )
        }

        PipeDivider()

        // ── Одометр и шестерни ──
        SectionTitle(stringResource(R.string.showcase_odometer))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OdometerDisplay("0 120 455", style = Steam.text.odometer)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.unit_km), style = MaterialTheme.typography.bodyLarge, color = palette.text)
        }
        SectionTitle(stringResource(R.string.showcase_gears))
        SteamPanel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SteamGears(spinning = gears, size = 56.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.showcase_gears_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    modifier = Modifier.weight(1f),
                )
            }
            SteamSwitchRow(
                text = stringResource(R.string.showcase_gears_toggle),
                checked = gears,
                onCheckedChange = { gears = it },
            )
        }

        PipeDivider()

        // ── Шрифты ──
        SectionTitle(stringResource(R.string.showcase_typography))
        SteamPanel(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.showcase_typography_heading), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.showcase_typography_body), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.showcase_typography_secondary),
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SampleGaugeCard(name: String, reading: GaugeReading, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.gauge_description, name, statusLabel(reading.status))
    SteamPanel(modifier = modifier, contentPadding = PaddingValues(12.dp)) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            modifier = Modifier.height(44.dp),
        )
        SteamGauge(
            reading = reading,
            contentDescription = description,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        )
        StatusBadge(reading.status, Modifier.align(Alignment.CenterHorizontally))
    }
}
