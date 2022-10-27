package top.xjunz.tasker.ui.task.editor.selector

import android.content.ComponentName
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.xjunz.tasker.ktx.require
import top.xjunz.tasker.ui.base.SavedStateViewModel
import top.xjunz.tasker.util.ApplicationIconLoader
import top.xjunz.tasker.util.PackageInfoLoader

/**
 * @author xjunz 2022/10/09
 */
class ComponentSelectorViewModel(states: SavedStateHandle) : SavedStateViewModel(states) {

    var mode = ComponentSelectorDialog.MODE_PACKAGE

    val currentItem = MutableLiveData<Int>()

    var shouldAnimateListItem = true

    val selectedCount = MutableLiveData(0)

    val selectedPackages = mutableListOf<String>()

    val selectedActivities = mutableListOf<ComponentName>()

    val selectedPackage = MutableLiveData<PackageInfoWrapper>()

    val iconLoader = ApplicationIconLoader()

    val addedItem = MutableLiveData<Any>()

    val removedItem = MutableLiveData<Any>()

    val currentPackages = MutableLiveData<List<PackageInfoWrapper>>()

    var title: CharSequence? = null

    private lateinit var allPackages: MutableList<PackageInfoWrapper>

    private lateinit var sortedPackages: List<PackageInfoWrapper>

    var sortBy: Int = -1

    var isOrderReversed: Boolean = false

    var showSystemApps = false

    val appBarHeight = MutableLiveData<Int>()

    fun findPackageInfo(pkgName: String): PackageInfoWrapper? {
        val index = sortedPackages.binarySearchBy(pkgName) { it.packageName }
        if (index >= 0) {
            return sortedPackages[index]
        }
        return null
    }

    fun sortPackagesBy(what: Int) {
        if (what == sortBy) return
        viewModelScope.launch(Dispatchers.Default) {
            if (!::allPackages.isInitialized) {
                allPackages = PackageInfoLoader.loadAllPackages().map {
                    PackageInfoWrapper(it).also { pkgInfo ->
                        pkgInfo.selectedActCount = selectedActivities.count { comp ->
                            comp.packageName == pkgInfo.packageName
                        }
                    }
                }.toMutableList()
                sortedPackages = allPackages.sortedBy { it.packageName }
            }
            if (isOrderReversed) {
                when (what) {
                    PackageInfoWrapper.SORT_BY_LABEL -> allPackages.sortByDescending { it.label.toString() }
                    PackageInfoWrapper.SORT_BY_SUSPICION -> allPackages.sortBy { it.loadSuspicion() }
                    PackageInfoWrapper.SORT_BY_SIZE -> allPackages.sortBy { it.apkSize }
                    PackageInfoWrapper.SORT_BY_FIRST_INSTALL_TIME -> allPackages.sortBy { it.source.firstInstallTime }
                }
            } else {
                when (what) {
                    PackageInfoWrapper.SORT_BY_LABEL -> allPackages.sortBy { it.label.toString() }
                    PackageInfoWrapper.SORT_BY_SUSPICION -> allPackages.sortBy { -it.loadSuspicion() }
                    PackageInfoWrapper.SORT_BY_SIZE -> allPackages.sortBy { -it.apkSize }
                    PackageInfoWrapper.SORT_BY_FIRST_INSTALL_TIME -> allPackages.sortBy { -it.source.firstInstallTime }
                }
            }
            filterSystemApps(showSystemApps, true)
            sortBy = what
        }
    }

    fun reverseOrder(reverse: Boolean) {
        if (reverse == isOrderReversed) return
        currentPackages.postValue(currentPackages.require().reversed())
        isOrderReversed = reverse
    }

    fun filterSystemApps(show: Boolean, force: Boolean = false) {
        if (!force && show == showSystemApps) return
        if (show) {
            currentPackages.postValue(allPackages)
        } else {
            currentPackages.postValue(allPackages.filter { !it.isSystemApp() })
        }
        showSystemApps = show
    }
}
