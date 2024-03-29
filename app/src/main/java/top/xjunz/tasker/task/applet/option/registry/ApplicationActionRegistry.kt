/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.task.applet.option.registry

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import top.xjunz.tasker.R
import top.xjunz.tasker.bridge.ActivityManagerBridge
import top.xjunz.tasker.bridge.ContextBridge
import top.xjunz.tasker.bridge.PackageManagerBridge
import top.xjunz.tasker.engine.applet.action.doubleArgsAction
import top.xjunz.tasker.engine.applet.action.simpleSingleArgAction
import top.xjunz.tasker.engine.applet.action.simpleSingleNonNullArgAction
import top.xjunz.tasker.isPrivilegedProcess
import top.xjunz.tasker.service.ensurePremium
import top.xjunz.tasker.task.applet.anno.AppletOrdinal
import top.xjunz.tasker.task.applet.flow.ref.ComponentInfoWrapper
import top.xjunz.tasker.task.applet.value.VariantArgType

/**
 * @author xjunz 2023/01/06
 */
class ApplicationActionRegistry(id: Int) : AppletOptionRegistry(id) {

    @AppletOrdinal(0x0000)
    val forceStopApp = appletOption(R.string.format_force_stop) {
        simpleSingleArgAction<ComponentInfoWrapper> {
            checkNotNull(it)
            if (isPrivilegedProcess) {
                ActivityManagerBridge.forceStopPackage(it.packageName)
                true
            } else {
                false
            }
        }
    }.withBinaryArgument<ComponentInfoWrapper, String>(
        R.string.specified_app, variantValueType = VariantArgType.TEXT_PACKAGE_NAME
    ).withSingleValueDescriber<String> {
        PackageManagerBridge.loadLabelOfPackage(it)
    }.hasCompositeTitle().shizukuOnly()

    @AppletOrdinal(0x0001)
    val launchApp = appletOption(R.string.format_launch) {
        simpleSingleArgAction<Any?> {
            requireNotNull(it)
            val pkg = if (it is String) it else (it as ComponentInfoWrapper).packageName
            ActivityManagerBridge.startComponent(
                requireNotNull(PackageManagerBridge.getLaunchIntentFor(pkg)?.component) {
                    "Launch intent for $it not found!"
                }
            )
            true
        }
    }.hasCompositeTitle()
        .withBinaryArgument<ComponentInfoWrapper, String>(
            R.string.specified_app, variantValueType = VariantArgType.TEXT_PACKAGE_NAME
        )
        .withSingleValueDescriber<String> {
            PackageManagerBridge.loadLabelOfPackage(it)
        }

    @AppletOrdinal(0x0002)
    val launchActivity = appletOption(R.string.launch_activity) {
        simpleSingleNonNullArgAction<String> {
            ensurePremium()
            ActivityManagerBridge.startComponent(it)
            true
        }
    }.withValueArgument<String>(R.string.activity, VariantArgType.TEXT_ACTIVITY)
        .withSingleValueDescriber<String> {
            ensurePremium()
            val comp = ComponentName.unflattenFromString(it)!!
            PackageManagerBridge.loadLabelOfPackage(comp.packageName)
                .toString() + "/" + it.substringAfterLast("/")
        }.premiumOnly()

    @AppletOrdinal(0x0003)
    val viewUri = appletOption(R.string.view_uri) {
        simpleSingleNonNullArgAction<String> {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.data = Uri.parse(it)
            ContextBridge.getContext().startActivity(intent)
            true
        }
    }.withValueArgument<String>(R.string.uri)

    @AppletOrdinal(0x0004)
    val viewUriViaPackage = appletOption(R.string.view_uri_via_package) {
        doubleArgsAction<Any?, String> { arg0, uri, _ ->
            val pkg = when (arg0) {
                is String -> arg0
                is ComponentInfoWrapper -> arg0.packageName
                else -> null
            }
            checkNotNull(pkg) {
                "Arg [package name] is null"
            }
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.setPackage(pkg)
            intent.data = Uri.parse(uri)
            ContextBridge.getContext().startActivity(intent)
            true
        }
    }.withBinaryArgument<ComponentInfoWrapper, String>(
        R.string.specified_app, variantValueType = VariantArgType.TEXT_PACKAGE_NAME
    ).withValueArgument<String>(R.string.uri)

    @AppletOrdinal(0x0005)
    val disablePackage = appletOption(R.string.format_disable_package) {
        simpleSingleNonNullArgAction<Any> {
            val pkg = if (it is ComponentInfoWrapper) it.packageName else it.toString()
            PackageManagerBridge.disablePackage(pkg)
            true
        }
    }.withBinaryArgument<ComponentInfoWrapper, String>(
        R.string.app,
        R.string.specified_app,
        VariantArgType.TEXT_PACKAGE_NAME
    ).hasCompositeTitle()
        .withSingleValueDescriber<Any> {
            val pkg = if (it is ComponentInfoWrapper) it.packageName else it.toString()
            PackageManagerBridge.loadLabelOfPackage(pkg)
        }
        .shizukuOnly()

    @AppletOrdinal(0x0006)
    val enablePackage = appletOption(R.string.format_enable_package) {
        simpleSingleNonNullArgAction<Any> {
            val pkg = if (it is ComponentInfoWrapper) it.packageName else it.toString()
            PackageManagerBridge.enablePackage(pkg)
            true
        }
    }.withBinaryArgument<ComponentInfoWrapper, String>(
        R.string.app,
        R.string.specified_app,
        VariantArgType.TEXT_PACKAGE_NAME
    ).withSingleValueDescriber<Any> {
        val pkg = if (it is ComponentInfoWrapper) it.packageName else it.toString()
        PackageManagerBridge.loadLabelOfPackage(pkg)
    }.hasCompositeTitle()
        .shizukuOnly()
}