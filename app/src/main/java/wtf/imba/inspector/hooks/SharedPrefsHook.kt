package wtf.imba.inspector.hooks

import android.content.ContextWrapper
import android.util.Log
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.type.java.BooleanClass
import com.highcapable.yukihookapi.hook.type.java.BooleanType
import com.highcapable.yukihookapi.hook.type.java.FileClass
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.LongType
import com.highcapable.yukihookapi.hook.type.java.StringClass
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.getObjectField
import wtf.imba.inspector.MainHook
import java.io.File
import java.io.FileInputStream
import java.nio.channels.FileChannel
import java.nio.charset.Charset


object SharedPrefsHook: YukiBaseHooker() {

     const val TAG = "SharedPrefsHook"
    private var sPrefs: XSharedPreferences? = null
    var putFileName = ""
    var sb: StringBuffer? = null

    override fun onHook() {

        loadPrefs()
        prefHook()
    }

    fun loadPrefs() {
        sPrefs = XSharedPreferences(MainHook::class.java.`package`?.name ?: "", MainHook.PREFS)
        sPrefs?.makeWorldReadable()
    }

    private fun prefHook() {
        ContextWrapper::class.java.hook {
            injectMember {
                method {
                    name = "getSharedPreferences"
                    returnType = StringClass
                }
                afterHook {
                    val modeId = this.args[1] as Int
                    var mode = "MODE_PRIVATE"
                    if (modeId == 1) {
                        mode = "MODE_PRIVATE"
                    } else if (modeId == 2) {
                        mode = "MODE_WORLD_WRITEABLE"
                    } else if (modeId > 2) {
                        mode = "APPEND or MULTI_PROCESS"
                    }
                    sb = StringBuffer()
                    putFileName = "PUT[" + this.args[0] as String + ".xml , " + mode + "]"
                }
            }
        }

        XposedHelpers.findAndHookConstructor("android.app.SharedPreferencesImpl", appClassLoader, FileClass, Int, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                val mFile = param!!.args[0] as File
                var text = ""
                if (mFile.exists() && mFile.canRead()) {
                    val f = FileInputStream(mFile)
                    val ch = f.channel
                    val mbb = ch.map(FileChannel.MapMode.READ_ONLY, 0L, ch.size())
                    while (mbb.hasRemaining()) {
                        val charsetName = "UTF-8"
                        val cb = Charset.forName(charsetName).decode(mbb)
                        text = cb.toString()
                    }
                }

                Log.d(TAG, "afterHookedMethod: text is: $text, file name is: ${mFile.name}")
//                FileUtil.writeToFile(sPrefs, text, FileType.PREFS_BKP, mFile.name)
            }
        })

        XposedHelpers.findAndHookMethod("android.app.SharedPreferencesImpl.EditorImpl", appClassLoader, "commit", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                if (sb.toString().isNotEmpty()) XposedBridge.log(
                    TAG + "" + sb.toString().substring(0, sb!!.length - 1) + ""
                )

                sb = StringBuffer()
            }
        })

        XposedHelpers.findAndHookMethod("android.app.SharedPreferencesImpl.EditorImpl", appClassLoader, "apply", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                if (sb.toString().isNotEmpty()) XposedBridge.log(
                    TAG + "" + sb.toString().substring(0, sb!!.length - 1) + ""
                )

                sb = StringBuffer()
            }
        })


        XposedHelpers.findAndHookMethod("android.app.SharedPreferencesImpl", appClassLoader, "getString", StringClass, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                val f = getObjectField(param!!.thisObject, "mFile") as File

                XposedBridge.log(TAG + "GET[" + f.name + "] String(" + param!!.args[0] as String + " , " + param!!.result as String + ")")
            }
        })

        XposedHelpers.findAndHookMethod("android.app.SharedPreferencesImpl", appClassLoader, "getStringSet", StringClass, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                val set: Set<String> = param!!.result as Set<String>
                val sb = StringBuffer()
                if (set != null && set.isNotEmpty()) for (x in set) {
                    sb.append(
                        """
            $x
            
            """.trimIndent()
                    )
                }
                val f = getObjectField(param.thisObject, "mFile") as File
                XposedBridge.log(TAG + "GET[" + f.name + "] StringSet(" + param!!.args[0] as String + ")= " + sb.toString() + ")")
            }
        })

        XposedHelpers.findAndHookMethod("android.app.SharedPreferencesImpl", appClassLoader, "getBoolean", StringClass, BooleanClass, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                val f = getObjectField(param!!.thisObject, "mFile") as File

                MainHook.sPrefs?.reload()
                val strReplace: List<String>? =
                    MainHook.sPrefs?.getString("prefs_replace", "")?.split(",")
                val key = param!!.args[0] as String

                if (key == strReplace!![0]) {
                    param!!.result = strReplace[1]
                }

                XposedBridge.log(
                    TAG + "GET[" + f.name + "] Boolean(" + param!!.args[0] + " , " + java.lang.String.valueOf(
                        param!!.result
                    ) + ")"
                )
            }
        })

        XposedHelpers.findAndHookMethod("android.app.SharedPreferencesImpl", appClassLoader, "getFloat", StringClass, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                val f = getObjectField(param!!.thisObject, "mFile") as File
                XposedBridge.log(
                    TAG + "GET[" + f.name + "] Float(" + param!!.args[0] as String + " , " + java.lang.Float.toString(
                        param!!.result as Float
                    ) + ")"
                )
            }
        })

        XposedHelpers.findAndHookMethod("android.app.SharedPreferencesImpl", appClassLoader, "getInt", StringClass, IntType, object :XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                val f = getObjectField(param!!.thisObject, "mFile") as File
                XposedBridge.log(
                    TAG + "GET[" + f.name + "] Int(" + param!!.args[0] as String + " , " + Integer.toString(
                        param!!.result as Int
                    ) + ")"
                )
            }
        })

        XposedHelpers.findAndHookMethod("android.app.SharedPreferencesImpl", appClassLoader, "getLong", StringClass, LongType, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                val f = getObjectField(param!!.thisObject, "mFile") as File
                XposedBridge.log(
                    TAG + "GET[" + f.name + "] Long(" + param!!.args[0] as String + " , " + java.lang.Long.toString(
                        (param!!.result as Long)!!
                    ) + ")"
                )
            }
        })

        XposedHelpers.findAndHookMethod("android.app.SharedPreferencesImpl", appClassLoader, "contains", StringClass, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                val f = getObjectField(param!!.thisObject, "mFile") as File
                XposedBridge.log(
                    TAG + "CONTAINS[" + f.name + "](" + param!!.args[0] as String + " , " + java.lang.Boolean.toString(
                        param!!.result as Boolean
                    ) + ")"
                )
            }
        })

         XposedHelpers.findAndHookMethod("android.app.SharedPreferencesImpl.EditorImpl", appClassLoader, "putString", StringClass, StringClass, object : XC_MethodHook() {
             override fun afterHookedMethod(param: MethodHookParam?) {
                 super.afterHookedMethod(param)
                 sb!!.append(putFileName + " String(" + param!!.args[0] as String + "," + param!!.args[1] as String + "),")
                 Log.d(TAG, "afterHookedMethod: android.app.SharedPreferencesImpl.EditorImpl: putString")
             }
         })

        XposedHelpers.findAndHookMethod("android.app.SharedPreferencesImpl.EditorImpl", appClassLoader, "putBoolean", StringClass, BooleanType, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                sb!!.append(
                    putFileName + " Boolean(" + param!!.args[0] as String + "," + java.lang.String.valueOf(
                        param!!.args[1]
                    ) + "),"
                )
                Log.d(TAG, "afterHookedMethod: android.app.SharedPreferencesImpl.EditorImpl: putString")
            }
        })

    }
}