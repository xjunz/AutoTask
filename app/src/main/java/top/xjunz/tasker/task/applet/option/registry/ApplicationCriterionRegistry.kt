/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import android.content.ComponentName
import android.content.pm.ApplicationInfo
import androidx.annotation.StringRes
import top.xjunz.tasker.R
import top.xjunz.tasker.bridge.PackageManagerBridge
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.applet.criterion.collectionCriterion
import top.xjunz.tasker.engine.applet.criterion.equalCriterion
import top.xjunz.tasker.engine.applet.criterion.propertyCriterion
import top.xjunz.tasker.engine.applet.criterion.referenceValueCriterion
import top.xjunz.tasker.ktx.bold
import top.xjunz.tasker.ktx.foreColored
import top.xjunz.tasker.ktx.formatSpans
import top.xjunz.tasker.service.uiAutomatorBridge
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.flow.ref.ComponentInfoWrapper
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.value.VariantArgType
import top.xjunz.tasker.ui.model.PackageInfoWrapper.Companion.wrapped

/**
 * @author xjunz 2022/09/22
 */
class ApplicationCriterionRegistry(id: Int) : AppletOptionRegistry(id) {

    private fun invertibleApplicationOption(
        @StringRes name: Int, creator: () -> Applet
    ): AppletOption {
        return invertibleAppletOption(name, creator)
            .withRefArgument<ComponentInfoWrapper>(R.string.app, substitution = R.string.empty)
            .hasCompositeTitle()
    }

    @AppletOrdinal(0x00_00)
    val isCertainApp = invertibleApplicationOption(R.string.is_certain_app) {
        referenceValueCriterion<ComponentInfoWrapper, Any?> { target, value ->
            val pkg = when (value) {
                is String -> value
                is ComponentInfoWrapper -> value.packageName
                else -> null
            }
            AppletResult.resultOf(target.packageName) {
                it == pkg
            }
        }
    }.withBinaryArgument<ComponentInfoWrapper, String>(
        name = R.string.is_which_app,
        substitution = R.string.a_certain_app,
        VariantArgType.TEXT_PACKAGE_NAME,
    ).withSingleValueDescriber<String> {
        PackageManagerBridge.loadLabelOfPackage(it)
    }

    @AppletOrdinal(0x00_01)
    val appCollection = invertibleApplicationOption(R.string.in_app_collection) {
        collectionCriterion<ComponentInfoWrapper, String> {
            it.packageName
        }
    }.withValueArgument<String>(R.string.app_collection, VariantArgType.TEXT_PACKAGE_NAME, true)
        .withSingleValueDescriber<Collection<String>> { value ->
            if (value.size == 1) {
                PackageManagerBridge.loadLabelOfPackage(value.first())
            } else {
                R.string.format_pkg_collection_desc.formatSpans(
                    value.asSequence().filterIndexed { index, _ -> index <= 2 }.map { name ->
                        PackageManagerBridge.loadLabelOfPackage(name)
                    }.joinToString("、"), value.size.toString().bold()
                )
            }
        }

    @AppletOrdinal(0x00_02)
    val activityCollection = invertibleApplicationOption(R.string.in_activity_collection) {
        collectionCriterion<ComponentInfoWrapper, String> {
            it.getComponentName()?.flattenToShortString()
        }
    }.withValueArgument<String>(R.string.activity_collection, VariantArgType.TEXT_ACTIVITY, true)
        .withSingleValueDescriber<Collection<String>> {
            if (it.size == 1) {
                val comp = ComponentName.unflattenFromString(it.single())!!
                (PackageManagerBridge.loadPackageInfo(comp.packageName)
                    ?.wrapped()?.label?.toString()
                    ?: comp.packageName) + "/" + it.single().substringAfterLast('/')
            } else {
                R.string.format_act_collection_desc.formatSpans(it.size.toString().foreColored())
            }
        }.withTitleModifier("Activity")

    @AppletOrdinal(0x00_03)
    val paneTitle = appletOption(R.string.with_pane_title) {
        equalCriterion<ComponentInfoWrapper, String> {
            it.paneTitle
        }
    }.withRefArgument<ComponentInfoWrapper>(R.string.app, substitution = R.string.empty)
        .hasCompositeTitle()
        .withValueArgument<String>(R.string.pane_title, VariantArgType.TEXT_PANE_TITLE)

    @AppletOrdinal(0x01_00)
    private val isSystem = invertibleApplicationOption(R.string.is_system_app) {
        propertyCriterion<ComponentInfoWrapper> {
            it.packageInfo.applicationInfo.flags and
                    (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        }
    }

    @AppletOrdinal(0x01_01)
    private val isLauncher = invertibleApplicationOption(R.string.is_launcher) {
        propertyCriterion<ComponentInfoWrapper> {
            it.packageName == uiAutomatorBridge.launcherPackageName
        }
    }

    override val categoryNames: IntArray =
        intArrayOf(R.string.component_info, R.string.property)

}