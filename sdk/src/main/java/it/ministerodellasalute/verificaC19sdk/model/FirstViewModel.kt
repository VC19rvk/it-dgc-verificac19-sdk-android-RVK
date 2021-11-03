/*
 *  ---license-start
 *  eu-digital-green-certificates / dgca-verifier-app-android
 *  ---
 *  Copyright (C) 2021 T-Systems International GmbH and all other contributors
 *  ---
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ---license-end
 *
 *  Created by danielsp on 9/15/21, 2:28 PM
 */

package it.ministerodellasalute.verificaC19sdk.model

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.*
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import it.ministerodellasalute.verificaC19sdk.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import it.ministerodellasalute.verificaC19sdk.VerificaApplication
import it.ministerodellasalute.verificaC19sdk.data.VerifierRepository
import it.ministerodellasalute.verificaC19sdk.data.VerifierRepositoryImpl
import it.ministerodellasalute.verificaC19sdk.data.local.Preferences
import it.ministerodellasalute.verificaC19sdk.data.remote.model.Rule
import it.ministerodellasalute.verificaC19sdk.model.ValidationRulesEnum
import it.ministerodellasalute.verificaC19sdk.util.Utility
import it.ministerodellasalute.verificaC19sdk.worker.LoadKeysWorker
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class FirstViewModel @Inject constructor(
    val verifierRepository: VerifierRepository,
    private val preferences: Preferences
) : ViewModel(){

    val fetchStatus: MediatorLiveData<Boolean> = MediatorLiveData()

    init {
        fetchStatus.addSource(verifierRepository.getCertificateFetchStatus()) {
            fetchStatus.value = it
        }
    }

    fun callForDownloadChunk(){
        viewModelScope.launch {
            verifierRepository.downloadChunk()
        }
    }

    fun getResumeToken() = preferences.resumeToken

    /**
     *
     * This method gets the date of last fetch from the Shared Preferences.
     *
     */
    fun getDateLastSync() = preferences.dateLastFetch

    fun getDrlDateLastSync() = preferences.drlDateLastFetch
    fun getTotalSizeInByte() = preferences.totalSizeInByte

    fun getSizeSingleChunkInByte() = preferences.sizeSingleChunkInByte
    fun getTotalChunk() = preferences.totalChunk //total number of chunks in a specific version
    fun getIsSizeOverThreshold() = preferences.isSizeOverThreshold
    fun getLastDownloadedChunk() = preferences.lastDownloadedChunk
    fun getauthorizedToDownload() = preferences.authorizedToDownload
    fun setauthorizedToDownload() =
        run { preferences.authorizedToDownload = 1L }
    fun getAuthResume() = preferences.authToResume
    fun setAuthResume() =
        run { preferences.authToResume = 1L }
    fun setUnAuthResume() =
        run { preferences.authToResume = 0L }

    fun getIsPendingDownload(): Boolean {
            return preferences.currentVersion != preferences.requestedVersion
        }

    fun getIsDrlSyncActive() = preferences.isDrlSyncActive

    /*suspend fun startSync() =
        run {
            val verifierRepository: VerifierRepository
            val res = verifierRepository.syncData(VerificaApplication.applicationContext())
        }*/

    private fun getValidationRules():Array<Rule>{
        val jsonString = preferences.validationRulesJson
        return Gson().fromJson(jsonString, Array<Rule>::class.java)?: kotlin.run { emptyArray() }
    }

    fun getAppMinVersion(): String{
        return getValidationRules().find { it.name == ValidationRulesEnum.APP_MIN_VERSION.value}?.let {
            it.value
        } ?: run {
            ""
        }
    }

    private fun getSDKMinVersion(): String{
        return getValidationRules().find { it.name == ValidationRulesEnum.SDK_MIN_VERSION.value}?.let {
            it.value
        } ?: run {
            ""
        }
    }

    fun isSDKVersionObsoleted(): Boolean {
        this.getSDKMinVersion().let {
            if (Utility.versionCompare(it, BuildConfig.SDK_VERSION) > 0) {
                return true
            }
        }
        return false
    }
}
