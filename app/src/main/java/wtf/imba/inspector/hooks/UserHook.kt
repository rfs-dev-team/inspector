package wtf.imba.inspector.hooks

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedBridge.log
import de.robv.android.xposed.XposedHelpers
import org.json.JSONArray
import org.json.JSONObject
import wtf.imba.inspector.MainHook
import wtf.imba.inspector.hooks.entity.HookItem
import wtf.imba.inspector.hooks.entity.HookList
import wtf.imba.inspector.utils.Config
import wtf.imba.inspector.utils.Util
import java.lang.reflect.Modifier


object UserHook: YukiBaseHooker() {

    const val TAG = "INSPECT_UserHook"
    private val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
    private var sPrefs: XSharedPreferences? = null

    override fun onHook() {
        userHook()
    }

    private fun userHook() {
        loadPrefs()
        val json = "{\"hookJson\": " + sPrefs!!.getString(Config.SP_USER_HOOKS, "") + "}"
        try {
            if (json.trim { it <= ' ' } != "{\"hookJson\":}") {
                val hookList: HookList? = gson.fromJson(json, HookList::class.java) as HookList
                for (hookItem in hookList?.hookList!!) {
                    if (hookItem.state) {
                        hook(hookItem, appClassLoader)
                    }
                }
            }
        } catch (ex: JsonSyntaxException) {
        }
    }

     fun loadPrefs() {
         sPrefs = XSharedPreferences(MainHook::class.java.`package`?.name ?: "", MainHook.PREFS)
         sPrefs?.makeWorldReadable()
     }

    fun hook(item: HookItem, classLoader: ClassLoader?) {
        try {
            val hookClass = XposedHelpers.findClass(item.className, classLoader)
            if (hookClass != null) {
                if (item.method != null && item.method != "") {
                    for (method in hookClass.declaredMethods) {
                        if (method.name == item.method && !Modifier.isAbstract(method.modifiers)) {
                            XposedBridge.hookMethod(method, methodHook)
                        }
                    }
                } else {
                    for (method in hookClass.declaredMethods) {
                        if (!Modifier.isAbstract(method.modifiers)) {
                            XposedBridge.hookMethod(method, methodHook)
                        }
                    }
                }
                if (item.constructor) {
                    for (constructor in hookClass.declaredConstructors) {
                        XposedBridge.hookMethod(constructor, methodHook)
                    }
                }
            } else {
                log(TAG + "class not found.")
            }
        } catch (e: Error) {
            Log.d(TAG, "hook: error: ${e.message}")
        }
    }

    var methodHook: XC_MethodHook = object : XC_MethodHook() {
        @Throws(Throwable::class)
        override fun beforeHookedMethod(param: MethodHookParam) {
//            loadPrefs()
            Log.d(TAG, "beforeHookedMethod: param is: $param")
        }

        @Throws(Throwable::class)
        override fun afterHookedMethod(param: MethodHookParam) {
//            loadPrefs()
            Log.d(TAG, "afterHookedMethod: param is: $param")
            parseParam(param)
        }
    }

    fun parseParam(param: MethodHookParam) {
        try {
            val hookData = JSONObject()
            hookData.put("class", param.method.declaringClass.name)
            if (param.method != null) hookData.put("method", param.method.name)
            val args = JSONArray()
            if (param.args != null) {
                for (`object` in param.args as Array<Any?>) {
                    if (`object` != null) {
                        if (`object`.javaClass == ByteArray::class.java) {
                            val result: String = Util.byteArrayToString(`object` as ByteArray?)
                            args.put(gson.toJson(result))
                        } else {
                            args.put(gson.toJson(`object`))
                        }
                    }
                }
                hookData.put("args", args)
            }
            if (param.result != null) {
                var result = ""
                if (param.result.javaClass == ByteArray::class.java) {
                    result = Util.byteArrayToString(param.result as ByteArray)
                    hookData.put("result", gson.toJson(result))
                } else {
                    hookData.put("result", gson.toJson(param.result))
                }
            }
            log(TAG + hookData.toString())
        } catch (e: Exception) {
            e.message
        }
    }
}