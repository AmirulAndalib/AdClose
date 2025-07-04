package com.close.hook.ads.data.repository

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.close.hook.ads.R
import com.close.hook.ads.closeApp
import com.close.hook.ads.data.model.AppFilterState
import com.close.hook.ads.data.model.AppInfo
import com.close.hook.ads.hook.preference.PreferencesHelper
import com.close.hook.ads.ui.activity.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

class AppRepository(private val packageManager: PackageManager) {

    private val prefsHelper by lazy { PreferencesHelper(closeApp) }

    private val enableKeys = arrayOf(
        "switch_one_", "switch_two_", "switch_three_", "switch_four_",
        "switch_five_", "switch_six_", "switch_seven_", "switch_eight_"
    )

    fun getAllAppsFlow(): Flow<List<AppInfo>> = flow {
        val packages = runCatching {
            packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        }.getOrElse { emptyList<PackageInfo>() }

        val moduleActive = MainActivity.isModuleActivated()
        val list = mutableListOf<AppInfo>()

        for (pkg in packages) {
            val app = pkg.applicationInfo
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) 
                pkg.longVersionCode.toInt() 
            else 
                pkg.versionCode

            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM != 0) ||
                           (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0)

            val isAppEnable = packageManager.getApplicationEnabledSetting(pkg.packageName) != 
                             PackageManager.COMPONENT_ENABLED_STATE_DISABLED

            val isEnable = if (moduleActive && enableKeys.any { 
                prefsHelper.getBoolean(it + pkg.packageName, false) 
            }) 1 else 0

            list.add(
                AppInfo(
                    appName = app.loadLabel(packageManager).toString(),
                    packageName = pkg.packageName,
                    versionName = pkg.versionName.orEmpty(),
                    versionCode = versionCode,
                    firstInstallTime = pkg.firstInstallTime,
                    lastUpdateTime = pkg.lastUpdateTime,
                    size = File(app.sourceDir).length(),
                    targetSdk = app.targetSdkVersion,
                    minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) app.minSdkVersion else 0,
                    isAppEnable = if (isAppEnable) 1 else 0,
                    isEnable = isEnable,
                    isSystem = isSystem
                )
            )
        }
        emit(list)
    }.flowOn(Dispatchers.IO)

    fun filterAndSortApps(apps: List<AppInfo>, filter: AppFilterState): List<AppInfo> {
        val now = System.currentTimeMillis()
        val comparator = getComparator(filter.filterOrder, filter.isReverse)
        return apps.asSequence()
            .filter {
                when (filter.appType) {
                    "user" -> !it.isSystem
                    "system" -> it.isSystem
                    "configured" -> it.isEnable == 1
                    else -> true
                }
            }
            .filter {
                (filter.keyword.isBlank() || it.appName.contains(filter.keyword, true) || it.packageName.contains(filter.keyword, true)) &&
                (!filter.showConfigured || it.isEnable == 1) &&
                (!filter.showUpdated || now - it.lastUpdateTime < 259200000L) &&  // 3天
                (!filter.showDisabled || it.isAppEnable == 0)
            }
            .sortedWith(comparator)
            .toList()
    }

    private fun getComparator(sortBy: Int, reverse: Boolean): Comparator<AppInfo> {
        val comparator = when (sortBy) {
            R.string.sort_by_app_size -> compareBy<AppInfo> { it.size }
            R.string.sort_by_last_update -> compareBy { it.lastUpdateTime }
            R.string.sort_by_install_date -> compareBy { it.firstInstallTime }
            R.string.sort_by_target_version -> compareBy { it.targetSdk }
            else -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName }
        }
        return if (reverse) comparator.reversed() else comparator
    }
}
