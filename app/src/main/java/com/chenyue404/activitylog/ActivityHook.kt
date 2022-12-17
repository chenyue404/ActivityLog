package com.chenyue404.activitylog

import android.app.ActivityOptions
import android.app.AndroidAppHelper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers.*
import de.robv.android.xposed.callbacks.XC_LoadPackage

class ActivityHook : IXposedHookLoadPackage {
    companion object {
        private const val HOOK_TARGET_PACKAGENAME = "android"
        const val TAG = "ActivityHook-"
        const val PREF_NAME = "main_prefs"
        const val KEY_HOOK_SWITCH = "key_hook_switch"
        private const val SWITCH_NULL = "null"
        const val SWITCH_TRUE = "true"
        const val SWITCH_FALSE = "false"
        var hookSwitch = SWITCH_NULL

        private val xSharedPreferences = XSharedPreferences(
            BuildConfig.APPLICATION_ID,
            PREF_NAME
        )
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val packageName = lpparam.packageName
        val classLoader = lpparam.classLoader

        if (packageName != HOOK_TARGET_PACKAGENAME) {
            return
        }
        hookCheckBroadcastFromSystem(classLoader)

        val osVersion = Build.VERSION.SDK_INT
        val activityRecordHook = getActivityRecordHook(osVersion)
        when {
            osVersion < Build.VERSION_CODES.Q -> {
                findAndHookConstructor(
                    "com.android.server.am.ActivityRecord",
                    classLoader,
                    activityRecordHook
                )
            }
            osVersion < Build.VERSION_CODES.S -> {
                findAndHookConstructor(
                    "com.android.server.wm.ActivityRecord",
                    classLoader,
                    activityRecordHook
                )
            }
            else -> {
                findAndHookMethod(
                    "com.android.server.wm.ActivityRecord.Builder",
                    classLoader,
                    "build",
                    activityRecordHook
                )
            }
        }

    }

    private fun log(str: String) {
        XposedBridge.log("$TAG-$str")
    }

    private fun sendBroadcast(str: String) {
        val context = AndroidAppHelper.currentApplication().applicationContext
        Handler(context.mainLooper).post {
            context.sendBroadcast(Intent().apply {
                action = LogReceiver.action
                putExtra(LogReceiver.extraKey, str)
            })
        }
    }

    /**
     * 解除系统不能发自定义广播的限制
     */
    private fun hookCheckBroadcastFromSystem(classLoader: ClassLoader) {
        val ProcessRecord = findClass("com.android.server.am.ProcessRecord", classLoader)
        findAndHookMethod(
            "com.android.server.am.ActivityManagerService", classLoader,
            "checkBroadcastFromSystem",
            Intent::class.java,
            ProcessRecord,
            String::class.java,
            Int::class.java,
            Boolean::class.java,
            List::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent: Intent = param.args[0] as Intent
                    if (intent.action == LogReceiver.action) {
                        param.args[4] = true
                        param.result = true
                    }
                }
            }
        )
    }

    private fun readHookSwitchIsFalse(): Boolean {
        xSharedPreferences.reload()
        hookSwitch = xSharedPreferences.getString(KEY_HOOK_SWITCH, SWITCH_TRUE) ?: SWITCH_TRUE
        log("hookSwitch=$hookSwitch")
        return hookSwitch == SWITCH_FALSE
    }

    private fun getActivityRecordHook(version: Int): XC_MethodHook = object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            val intentData = getIntentData(version, param)
            val intent = intentData.intent
            if (intentData.needAddFlag) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val intentStr = intent.transToStr(
                intentData.fromStr,
                intentData.bundle
            )
//            log("intent=$intentStr")

            if (!readHookSwitchIsFalse()) {
                sendBroadcast(intentStr)
            }
        }
    }

    private fun getIntentData(version: Int, param: XC_MethodHook.MethodHookParam): IntentData {
        var intentAttrName = ""
        var optionsAttrName = ""
        var launchedFromPackageAttrName = ""
        var requestCodeAttrName = ""
        var activityInfoAttrName = ""
        var launchedFromUidAttrName = ""

        if (version >= Build.VERSION_CODES.S) {
            intentAttrName = "mIntent"
            optionsAttrName = "mOptions"
            launchedFromPackageAttrName = "mLaunchedFromPackage"
            requestCodeAttrName = "mRequestCode"
            activityInfoAttrName = "mActivityInfo"
            launchedFromUidAttrName = "mLaunchedFromUid"
        } else {
            intentAttrName = "intent"
            optionsAttrName = "pendingOptions"
            launchedFromPackageAttrName = "launchedFromPackage"
            requestCodeAttrName = "requestCode"
            activityInfoAttrName = "info"
            launchedFromUidAttrName = "launchedFromUid"
        }

        val intent = getObjectField(param.thisObject, intentAttrName) as Intent
        val launchedFromPackage =
            getObjectField(param.thisObject, launchedFromPackageAttrName) as String?
        val activityOptions = getObjectField(param.thisObject, optionsAttrName) as ActivityOptions?
        val requestCode = getIntField(param.thisObject, requestCodeAttrName)
        val uid = (getObjectField(
            param.thisObject,
            activityInfoAttrName
        ) as ActivityInfo).applicationInfo.uid
        val launchedFromUid = getIntField(param.thisObject, launchedFromUidAttrName)

        val noNeedAddFlag = requestCode != -1
                || intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK == Intent.FLAG_ACTIVITY_NEW_TASK
                || intent.action == null
                || uid == launchedFromUid
        val intentData = IntentData(
            intent,
            launchedFromPackage ?: "",
            activityOptions?.toBundle(),
            !noNeedAddFlag
        )
        return intentData
    }

    private data class IntentData(
        var intent: Intent,
        var fromStr: String,
        var bundle: Bundle?,
        var needAddFlag: Boolean,
    )
}