package top.xjunz.tasker.ui.base

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * @author xjunz 2022/10/25
 */
open class SavedStateViewModel(private val states: SavedStateHandle) : ViewModel() {

    fun <T> get(key: String): MutableLiveData<T> {
        return states.getLiveData(key)
    }

    fun <T> setValue(key: String, value: T?) {
        get<T>(key).value = value
    }

    class SavedStateProperty<T> : ReadOnlyProperty<SavedStateViewModel, MutableLiveData<T>> {

        override fun getValue(thisRef: SavedStateViewModel, property: KProperty<*>): MutableLiveData<T> {
            return thisRef.get(property.name)
        }
    }

    fun <T> savedLiveData(): SavedStateProperty<T> {
        return SavedStateProperty()
    }
}