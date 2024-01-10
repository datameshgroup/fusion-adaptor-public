package au.com.dmg.fusionadaptor.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

val Context.dataStore by preferencesDataStore(name = "config")

object Configuration {
    private lateinit var dataStore: DataStore<Preferences>

    private val IS_FIRST_LOAD = booleanPreferencesKey("is_first_load")
    //POSSettingData
    private val USE_TEST_ENVIRONMENT_KEY = booleanPreferencesKey("useTestEnvironment")
    private val ON_TERMINAL_KEY = booleanPreferencesKey("onTerminal") //MainActivity determines this
    private val SHOW_RESULT_SCREEN_KEY = booleanPreferencesKey("showResultScreen")
    private val ENABLE_TIP_KEY = booleanPreferencesKey("enableTip")
    private val POS_NAME_KEY = stringPreferencesKey("posName")

    //ConfigurationData
      //DatameshProvidedData //check if it's there, else default (override)
    private val PROVIDER_IDENTIFICATION_KEY = stringPreferencesKey("providerIdentification")
    private val APPLICATION_NAME_KEY = stringPreferencesKey("applicationName")
    private val SOFTWARE_VERSION_KEY = stringPreferencesKey("softwareVersion")
    private val CERTIFICATION_CODE_KEY = stringPreferencesKey("certificationCode")


    //PairingData
    private val SALE_ID_KEY = stringPreferencesKey("saleId")
    private val POI_ID_KEY = stringPreferencesKey("poiId")
    private val KEK_KEY = stringPreferencesKey("kek")

    fun initialize(context: Context) {
        dataStore = context.dataStore
        runBlocking {
            if (isFirstLoad()) {
                setUseTestEnvironment(context, false)
                setShowResultScreen(context, true)

                setPosName(context, "Hiopos") //TODO
                setFirstLoad(false)
            }
        }
    }

    fun getSaleId(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[SALE_ID_KEY] ?: "" // Default value if not found
        }
    }


    private suspend fun setSaleId(context: Context, saleId: String) {
        context.dataStore.edit { preferences ->
            preferences[SALE_ID_KEY] = saleId
        }
    }

    fun getDataStore(): DataStore<Preferences> {
        return dataStore
    }
    private fun isFirstLoad(): Boolean = runBlocking {
        if (isDataStoreInitialized()) {
            dataStore.data.first()[IS_FIRST_LOAD] ?: true
        } else {
            true
        }
    }

    private suspend fun setFirstLoad(isFirstLoad: Boolean) {
        dataStore.edit {
            it[IS_FIRST_LOAD] = isFirstLoad
        }
    }

    private fun isDataStoreInitialized(): Boolean {
        return this::dataStore.isInitialized
    }
    fun getUseTestEnvironment(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[USE_TEST_ENVIRONMENT_KEY] ?: false
        }
    }
    suspend fun setUseTestEnvironment(context: Context, value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_TEST_ENVIRONMENT_KEY] = value
        }

        if(value){
            //Dev
            updateProviderIdentification(context, "DMG")
            updateApplicationName(context, "FusionAdaptorAndroid")
            updateSoftwareVersion(context, "01.00.00")
            updateCertificationCode(context, "d605f5e9-44b5-47ae-ad64-14694ba6e772")
        }else{
            //Prod
            updateProviderIdentification(context, "DMG")
            updateApplicationName(context, "FusionAdaptorAndroid")
            updateSoftwareVersion(context, "01.00.00")
            updateCertificationCode(context, "141f5bac-7cf2-49b7-82d4-c2752842226d")
        }
    }

    fun getOnTerminal(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[ON_TERMINAL_KEY] ?: false // Default value if not found
        }
    }
    suspend fun setOnTerminal(context: Context, onTerminal: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ON_TERMINAL_KEY] = onTerminal
        }
    }

    fun getPosName(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[POS_NAME_KEY] ?: "" // Default value if not found
        }
    }


    private suspend fun setPosName(context: Context, posName: String) {
        context.dataStore.edit { preferences ->
            preferences[POS_NAME_KEY] = posName
        }
    }

    fun getPoiId(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[POI_ID_KEY] ?: "" // Default value if not found
        }
    }

    suspend fun setPoiId(context: Context, poiId: String) {
        context.dataStore.edit { preferences ->
            preferences[POI_ID_KEY] = poiId
        }
    }

    fun getPOSSettingsData(context: Context): Flow<POSSettingData> {
        return context.dataStore.data.map { preferences ->
            POSSettingData(
                useTestEnvironment = preferences[USE_TEST_ENVIRONMENT_KEY] ?: true,
                showResultScreen = preferences[SHOW_RESULT_SCREEN_KEY] ?: true,
                enableTip = preferences[ENABLE_TIP_KEY] ?: true,
                posName = preferences[POS_NAME_KEY] ?: ""
            )
        }
    }
    suspend fun updatePOSSettingsData(
        context: Context,
        updatedPOSSettings: POSSettingData
    ) {
        context.dataStore.edit { preferences ->
            preferences[USE_TEST_ENVIRONMENT_KEY] = updatedPOSSettings.useTestEnvironment
            preferences[SHOW_RESULT_SCREEN_KEY] = updatedPOSSettings.showResultScreen
            preferences[ENABLE_TIP_KEY] = updatedPOSSettings.enableTip
            preferences[POS_NAME_KEY] = updatedPOSSettings.posName
        }
    }

    fun getConfiguration(context: Context): Flow<ConfigurationData> {
        return context.dataStore.data.map { preferences ->
            ConfigurationData(
                saleId = preferences[SALE_ID_KEY] ?: "",
                poiId = preferences[POI_ID_KEY] ?: "",
                kek = preferences[KEK_KEY] ?: "",
                posName = preferences[POS_NAME_KEY] ?: "",
                providerIdentification = preferences[PROVIDER_IDENTIFICATION_KEY] ?: "",
                applicationName = preferences[APPLICATION_NAME_KEY] ?: "",
                softwareVersion = preferences[SOFTWARE_VERSION_KEY] ?: "",
                certificationCode = preferences[CERTIFICATION_CODE_KEY] ?: ""
            )
        }
    }
    suspend fun updateConfiguration(
        context: Context,
        updatedConfig: ConfigurationData
    ) {
        context.dataStore.edit { preferences ->
            preferences[SALE_ID_KEY] = updatedConfig.saleId
            preferences[POI_ID_KEY] = updatedConfig.poiId
            preferences[KEK_KEY] = updatedConfig.kek
            preferences[PROVIDER_IDENTIFICATION_KEY] = updatedConfig.providerIdentification
            preferences[APPLICATION_NAME_KEY] = updatedConfig.applicationName
            preferences[SOFTWARE_VERSION_KEY] = updatedConfig.softwareVersion
            preferences[CERTIFICATION_CODE_KEY] = updatedConfig.certificationCode
        }
    }

    fun getPairingData(context: Context): Flow<PairingData> {
        return context.dataStore.data.map { preferences ->
            PairingData(
                saleId = preferences[SALE_ID_KEY] ?: "",
                poiId = preferences[POI_ID_KEY] ?: "",
                kek = preferences[KEK_KEY] ?: "",
            )
        }
    }
    suspend fun updatePairingData(
        context: Context,
        pairingData: PairingData
    ) {
        context.dataStore.edit { preferences ->
            preferences[SALE_ID_KEY] = pairingData.saleId
            preferences[POI_ID_KEY] = pairingData.poiId
            preferences[KEK_KEY] = pairingData.kek
        }
    }

    fun getDatameshProvidedData(context: Context): Flow<DatameshProvidedData> {
        return context.dataStore.data.map { preferences ->
            DatameshProvidedData(
                providerIdentification = preferences[PROVIDER_IDENTIFICATION_KEY] ?: "",
                applicationName = preferences[APPLICATION_NAME_KEY] ?: "",
                softwareVersion = preferences[SOFTWARE_VERSION_KEY] ?: "",
                certificationCode = preferences[CERTIFICATION_CODE_KEY] ?: ""
            )
        }
    }
    suspend fun updateDatameshProvidedData(
        context: Context,
        updatedConfig: DatameshProvidedData
    ) {
        context.dataStore.edit { preferences ->
            preferences[PROVIDER_IDENTIFICATION_KEY] = updatedConfig.providerIdentification
            preferences[APPLICATION_NAME_KEY] = updatedConfig.applicationName
            preferences[SOFTWARE_VERSION_KEY] = updatedConfig.softwareVersion
            preferences[CERTIFICATION_CODE_KEY] = updatedConfig.certificationCode
        }
    }

    fun getShowResultScreen(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[SHOW_RESULT_SCREEN_KEY] ?: false // Default value if not found
        }
    }

    suspend fun setShowResultScreen(context: Context, showResultScreen: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_RESULT_SCREEN_KEY] = showResultScreen
        }
    }

    fun getEnableTip(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[ENABLE_TIP_KEY] ?: false // Default value if not found
        }
    }

    suspend fun setEnableTip(context: Context, enableTip: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_TIP_KEY] = enableTip
        }
    }

    fun getProviderIdentification(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[PROVIDER_IDENTIFICATION_KEY] ?: ""
        }
    }

    suspend fun updateProviderIdentification(context: Context, value: String) {
        context.dataStore.edit { preferences ->
            preferences[PROVIDER_IDENTIFICATION_KEY] = value
        }
    }

    fun getApplicationName(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[APPLICATION_NAME_KEY] ?: ""
        }
    }

    suspend fun updateApplicationName(context: Context, value: String) {
        context.dataStore.edit { preferences ->
            preferences[APPLICATION_NAME_KEY] = value
        }
    }

    fun getSoftwareVersion(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[SOFTWARE_VERSION_KEY] ?: ""
        }
    }

    suspend fun updateSoftwareVersion(context: Context, value: String) {
        context.dataStore.edit { preferences ->
            preferences[SOFTWARE_VERSION_KEY] = value
        }
    }

    fun getCertificationCode(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[CERTIFICATION_CODE_KEY] ?: ""
        }
    }

    suspend fun updateCertificationCode(context: Context, value: String) {
        context.dataStore.edit { preferences ->
            preferences[CERTIFICATION_CODE_KEY] = value
        }
    }

    data class POSSettingData(
        val useTestEnvironment: Boolean,
        val showResultScreen: Boolean,
        val enableTip: Boolean,
        val posName: String
    )
    data class ConfigurationData(
        val saleId: String,
        val poiId: String,
        val kek: String,
        val posName: String,
        val providerIdentification: String,
        val applicationName: String,
        val softwareVersion: String,
        val certificationCode: String
    )
    data class PairingData(
        val saleId: String,
        val poiId: String,
        val kek: String
    )

    data class DatameshProvidedData(
        val providerIdentification: String,
        val applicationName: String,
        val softwareVersion: String,
        val certificationCode: String
    )
}
