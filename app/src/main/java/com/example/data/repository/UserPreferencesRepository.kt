package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.security.CryptoManager
import com.example.domain.AutoTrackMode
import com.example.domain.AutoTrackSource
import com.example.domain.DashboardWidgetType
import com.example.domain.MaterialYouPalette
import com.example.domain.ThemeMode
import com.example.domain.UserPreferencesState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "mymoney_pro_user_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val BASE_CURRENCY = stringPreferencesKey("base_currency")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val SEED_PALETTE = stringPreferencesKey("seed_palette")
        val INCLUDE_BF_IN_NET = booleanPreferencesKey("include_bf_in_net")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val TX_ALERTS_ENABLED = booleanPreferencesKey("tx_alerts_enabled")
        val BUDGET_ALERTS_ENABLED = booleanPreferencesKey("budget_alerts_enabled")
        val AUTO_TRACK_ENABLED = booleanPreferencesKey("auto_track_enabled")
        val AUTO_TRACK_MODE = stringPreferencesKey("auto_track_mode")
        val ENABLED_AUTO_TRACK_SOURCES = stringPreferencesKey("enabled_auto_track_sources")
        val PRIVACY_MASK_ENABLED = booleanPreferencesKey("privacy_mask_enabled")
        val FX_CHART_EXPANDED = booleanPreferencesKey("fx_chart_expanded")
        val FONT_STYLE = stringPreferencesKey("font_style")
        val ORDERED_WIDGETS = stringPreferencesKey("ordered_widgets")
        val ENABLED_WIDGETS = stringPreferencesKey("enabled_widgets")
    }

    private val defaultPinHash = CryptoManager.hashPin("1234")

    val preferencesFlow: Flow<UserPreferencesState> = context.dataStore.data.map { prefs ->
        val baseCurrency = prefs[Keys.BASE_CURRENCY] ?: "USD"
        val themeMode = prefs[Keys.THEME_MODE]?.let {
            runCatching { ThemeMode.valueOf(it) }.getOrNull()
        } ?: ThemeMode.LIGHT
        val dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR_ENABLED] ?: true
        val seedPalette = prefs[Keys.SEED_PALETTE]?.let {
            runCatching { MaterialYouPalette.valueOf(it) }.getOrNull()
        } ?: MaterialYouPalette.OCEAN_BLUE

        val includeBfInNet = prefs[Keys.INCLUDE_BF_IN_NET] ?: true
        val appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false
        val pinHash = prefs[Keys.PIN_HASH]?.takeIf { it.isNotEmpty() } ?: defaultPinHash
        val notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
        val txAlertsEnabled = prefs[Keys.TX_ALERTS_ENABLED] ?: true
        val budgetAlertsEnabled = prefs[Keys.BUDGET_ALERTS_ENABLED] ?: true
        val autoTrackEnabled = prefs[Keys.AUTO_TRACK_ENABLED] ?: true
        val autoTrackMode = prefs[Keys.AUTO_TRACK_MODE]?.let {
            runCatching { AutoTrackMode.valueOf(it) }.getOrNull()
        } ?: AutoTrackMode.AUTO_SAVE

        val enabledAutoTrackSources = prefs[Keys.ENABLED_AUTO_TRACK_SOURCES]?.let { raw ->
            if (raw == "NONE") {
                emptySet()
            } else {
                raw.split(",")
                    .mapNotNull { runCatching { AutoTrackSource.valueOf(it) }.getOrNull() }
                    .toSet()
            }
        } ?: AutoTrackSource.entries.toSet()

        val privacyMaskEnabled = prefs[Keys.PRIVACY_MASK_ENABLED] ?: false
        val isFxChartExpanded = prefs[Keys.FX_CHART_EXPANDED] ?: false
        val fontStyle = prefs[Keys.FONT_STYLE] ?: "Default"

        val orderedWidgets = prefs[Keys.ORDERED_WIDGETS]
            ?.split(",")
            ?.mapNotNull { runCatching { DashboardWidgetType.valueOf(it) }.getOrNull() }
            ?.takeIf { it.isNotEmpty() }
            ?: DashboardWidgetType.entries

        val missingWidgets = DashboardWidgetType.entries.filter { it !in orderedWidgets }
        val finalOrderedWidgets = orderedWidgets + missingWidgets

        val enabledWidgets = prefs[Keys.ENABLED_WIDGETS]
            ?.split(",")
            ?.mapNotNull { runCatching { DashboardWidgetType.valueOf(it) }.getOrNull() }
            ?.toSet()
            ?: DashboardWidgetType.entries.toSet()

        UserPreferencesState(
            baseCurrency = baseCurrency,
            themeMode = themeMode,
            dynamicColorEnabled = dynamicColorEnabled,
            seedPalette = seedPalette,
            includeBroughtForwardInNet = includeBfInNet,
            appLockEnabled = appLockEnabled,
            pinHash = pinHash,
            notificationsEnabled = notificationsEnabled,
            txAlertsEnabled = txAlertsEnabled,
            budgetAlertsEnabled = budgetAlertsEnabled,
            autoTrackEnabled = autoTrackEnabled,
            autoTrackMode = autoTrackMode,
            enabledAutoTrackSources = enabledAutoTrackSources,
            privacyMaskEnabled = privacyMaskEnabled,
            isFxChartExpanded = isFxChartExpanded,
            fontStyle = fontStyle,
            orderedWidgets = finalOrderedWidgets,
            enabledWidgets = enabledWidgets
        )
    }

    suspend fun setBaseCurrency(currencyCode: String) {
        context.dataStore.edit { it[Keys.BASE_CURRENCY] = currencyCode }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR_ENABLED] = enabled }
    }

    suspend fun setSeedPalette(palette: MaterialYouPalette) {
        context.dataStore.edit { it[Keys.SEED_PALETTE] = palette.name }
    }

    suspend fun setIncludeBroughtForwardInNet(include: Boolean) {
        context.dataStore.edit { it[Keys.INCLUDE_BF_IN_NET] = include }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[Keys.APP_LOCK_ENABLED] = enabled
            if (it[Keys.PIN_HASH].isNullOrEmpty()) {
                it[Keys.PIN_HASH] = defaultPinHash
            }
        }
    }

    suspend fun updatePin(newPin: String) {
        val clean = newPin.filter { it.isDigit() }.take(4)
        if (clean.length == 4) {
            context.dataStore.edit {
                it[Keys.PIN_HASH] = CryptoManager.hashPin(clean)
            }
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setTxAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TX_ALERTS_ENABLED] = enabled }
    }

    suspend fun setBudgetAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BUDGET_ALERTS_ENABLED] = enabled }
    }

    suspend fun setAutoTrackEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_TRACK_ENABLED] = enabled }
    }

    suspend fun setAutoTrackMode(mode: AutoTrackMode) {
        context.dataStore.edit { it[Keys.AUTO_TRACK_MODE] = mode.name }
    }

    suspend fun setAutoTrackSourceEnabled(source: AutoTrackSource, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.ENABLED_AUTO_TRACK_SOURCES]?.let { raw ->
                if (raw == "NONE") {
                    mutableSetOf()
                } else {
                    raw.split(",")
                        .mapNotNull { runCatching { AutoTrackSource.valueOf(it) }.getOrNull() }
                        .toMutableSet()
                }
            } ?: AutoTrackSource.entries.toMutableSet()

            if (enabled) current.add(source) else current.remove(source)
            prefs[Keys.ENABLED_AUTO_TRACK_SOURCES] = if (current.isEmpty()) {
                "NONE"
            } else {
                current.joinToString(",") { it.name }
            }
        }
    }

    suspend fun setWidgetEnabled(widget: DashboardWidgetType, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.ENABLED_WIDGETS]
                ?.split(",")
                ?.mapNotNull { runCatching { DashboardWidgetType.valueOf(it) }.getOrNull() }
                ?.toMutableSet()
                ?: DashboardWidgetType.entries.toMutableSet()

            if (enabled) current.add(widget) else current.remove(widget)
            prefs[Keys.ENABLED_WIDGETS] = current.joinToString(",") { it.name }
        }
    }

    suspend fun moveWidget(widget: DashboardWidgetType, moveUp: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.ORDERED_WIDGETS]
                ?.split(",")
                ?.mapNotNull { runCatching { DashboardWidgetType.valueOf(it) }.getOrNull() }
                ?.toMutableList()
                ?: DashboardWidgetType.entries.toMutableList()

            DashboardWidgetType.entries.forEach { w ->
                if (w !in current) current.add(w)
            }

            val index = current.indexOf(widget)
            if (index != -1) {
                val targetIndex = if (moveUp) index - 1 else index + 1
                if (targetIndex in current.indices) {
                    val temp = current[targetIndex]
                    current[targetIndex] = current[index]
                    current[index] = temp
                    prefs[Keys.ORDERED_WIDGETS] = current.joinToString(",") { it.name }
                }
            }
        }
    }

    suspend fun setPrivacyMaskEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PRIVACY_MASK_ENABLED] = enabled }
    }

    suspend fun setFxChartExpanded(expanded: Boolean) {
        context.dataStore.edit { it[Keys.FX_CHART_EXPANDED] = expanded }
    }

    suspend fun setFontStyle(style: String) {
        context.dataStore.edit { it[Keys.FONT_STYLE] = style }
    }
}
