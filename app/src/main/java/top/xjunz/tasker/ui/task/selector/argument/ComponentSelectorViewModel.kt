/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.selector.argument

import android.content.ComponentName
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import top.xjunz.shared.utils.illegalArgument
import top.xjunz.tasker.bridge.PackageManagerBridge
import top.xjunz.tasker.ktx.isNotTrue
import top.xjunz.tasker.ktx.isTrue
import top.xjunz.tasker.ktx.require
import top.xjunz.tasker.ui.base.SavedStateViewModel
import top.xjunz.tasker.ui.model.PackageInfoWrapper
import top.xjunz.tasker.util.ApplicationIconLoader

/**
 * @author xjunz 2022/10/09
 */
class ComponentSelectorViewModel(states: SavedStateHandle) : SavedStateViewModel(states) {

    var isSingleSelection: Boolean = false

    var mode = ComponentSelectorDialog.MODE_PACKAGE

    val currentItem = MutableLiveData<Int>()

    var shouldAnimateListItem = true

    val selectedCount = MutableLiveData(0)

    val selectedPackages = mutableListOf<String>()

    val selectedActivities = mutableListOf<ComponentName>()

    val selectedPackage = MutableLiveData<PackageInfoWrapper>()

    val iconLoader = ApplicationIconLoader()

    val addedItem = MutableLiveData<Any>()

    val itemToRemove = MutableLiveData<Any>()

    val onSelectionCleared = MutableLiveData<Boolean>()

    val showClearAllDialog = MutableLiveData<Boolean>()

    val currentPackages = MutableLiveData<List<PackageInfoWrapper>>()

    val isInSearchMode = MutableLiveData<Boolean>()

    val currentQuery = MutableLiveData<String?>()

    val currentSortBy = MutableLiveData(PackageInfoWrapper.SORT_BY_SUSPICION)

    val showSystemApps = MutableLiveData(false)

    val isOrderReversed = MutableLiveData(false)

    var title: CharSequence? = null

    lateinit var onCompleted: (Collection<String>) -> Unit

    lateinit var allPackages: List<PackageInfoWrapper>

    val appBarHeight = MutableLiveData<Int>()

    private var filteringJob: Job? = null

    fun findPackageInfo(pkgName: String): PackageInfoWrapper? {
        return allPackages.find { pkgName == it.packageName }
    }

    fun filterAndSortPackages() {
        val query = currentQuery.value
        val sortBy = currentSortBy.require()
        filteringJob?.cancel()
        filteringJob = viewModelScope.launch(Dispatchers.Default) {
            var finalPackages: List<PackageInfoWrapper>
            if (!::allPackages.isInitialized) {
                allPackages = PackageManagerBridge.loadAllPackages().map {
                    PackageInfoWrapper(it).also { pkgInfo ->
                        pkgInfo.selectedActCount = selectedActivities.count { comp ->
                            comp.packageName == pkgInfo.packageName
                        }
                    }
                }
            }
            finalPackages = if (query.isNullOrBlank()) {
                allPackages
            } else {
                allPackages.filter { it.label.contains(query,true) }
            }
            if (showSystemApps.isNotTrue) {
                finalPackages = finalPackages.filter { !it.isSystemApp() }
            }
            finalPackages = if (isOrderReversed.isTrue) {
                when (sortBy) {
                    PackageInfoWrapper.SORT_BY_LABEL -> finalPackages.sortedByDescending { it.label.toString() }
                    PackageInfoWrapper.SORT_BY_SUSPICION -> finalPackages.sortedBy { it.loadSuspicion() }
                    PackageInfoWrapper.SORT_BY_SIZE -> finalPackages.sortedBy { it.apkSize }
                    PackageInfoWrapper.SORT_BY_FIRST_INSTALL_TIME -> finalPackages.sortedBy { it.source.firstInstallTime }
                    else -> illegalArgument()
                }
            } else {
                when (sortBy) {
                    PackageInfoWrapper.SORT_BY_LABEL -> finalPackages.sortedBy { it.label.toString() }
                    PackageInfoWrapper.SORT_BY_SUSPICION -> finalPackages.sortedBy { -it.loadSuspicion() }
                    PackageInfoWrapper.SORT_BY_SIZE -> finalPackages.sortedBy { -it.apkSize }
                    PackageInfoWrapper.SORT_BY_FIRST_INSTALL_TIME -> finalPackages.sortedBy { -it.source.firstInstallTime }
                    else -> illegalArgument()
                }
            }
            currentPackages.postValue(finalPackages)
        }
    }

    fun complete(): Boolean {
        val collection = if (mode == ComponentSelectorDialog.MODE_ACTIVITY) selectedActivities.map {
            it.flattenToShortString()
        } else selectedPackages
        if (collection.isNotEmpty()) {
            onCompleted(collection)
            return true
        }
        return false
    }
}
