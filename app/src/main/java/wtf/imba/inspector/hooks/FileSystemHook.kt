package wtf.imba.inspector.hooks

import android.content.Context
import android.content.ContextWrapper
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.type.java.FileClass
import com.highcapable.yukihookapi.hook.type.java.StringClass
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.findAndHookConstructor
import java.io.File
import java.net.URI


object FileSystemHook : YukiBaseHooker() {

    const val TAG = "FileSystemHook"
    override fun onHook() {
        ContextWrapper::class.java.hook {
            injectMember {
                method {
                    name = "openFileOutput"
                    returnType = StringClass
                }
                afterHook {
                    val name = this.args[0] as String
                    val mode = this.args[1] as Int

                    if (name.contains("Inspector")) {
                        XposedBridge.invokeOriginalMethod(
                            this.method,
                            this.instance,
                            this.args
                        )
                    } else {
                        val m: String = when (mode) {
                            Context.MODE_PRIVATE -> "MODE_PRIVATE"
                            Context.MODE_APPEND -> "MODE_APPEND"
                            else -> "?"
                        }
                        XposedBridge.log(TAG + "openFileOutput(" + name + ", " + m + ")")
                    }
                }
            }
        }

        findAndHookConstructor(FileClass, StringClass, object  : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                val str = param!!.args[0] as String
                if (str.contains("Inspector")) {
                    XposedBridge.invokeOriginalMethod(
                        param!!.method,
                        param!!.thisObject,
                        param!!.args
                    )
                } else {
                    XposedBridge.log(TAG + "R/W [new File(String)]: " + str)
                }
            }
        })

        findAndHookConstructor(
            File::class.java,
            String::class.java,
            String::class.java, object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    val dir = param.args[0] as String
                    val fileName = param.args[1] as String
                    if (dir != null) {
                        if (dir.contains("Inspector") || fileName.contains("Inspeckage")) {
                            XposedBridge.invokeOriginalMethod(
                                param.method,
                                param.thisObject,
                                param.args
                            )
                        } else {
                            XposedBridge.log(TAG + "R/W Dir: " + dir + " File: " + fileName)
                        }
                    }
                }
            })

        findAndHookConstructor(
            File::class.java,
            File::class.java,
            String::class.java, object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    val fileDir = param.args[0] as File
                    val fileName = param.args[1] as String
                    if (fileDir != null) {
                        if (fileDir.absolutePath.contains("Inspector") || fileName.contains("Inspeckage")) {
                            XposedBridge.invokeOriginalMethod(
                                param.method,
                                param.thisObject,
                                param.args
                            )
                        } else {
                            XposedBridge.log(TAG + "R/W Dir: " + fileDir.absolutePath + " File: " + fileName)
                        }
                    }
                }
            })

        findAndHookConstructor(File::class.java, URI::class.java, object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun afterHookedMethod(param: MethodHookParam) {
                val uri = param.args[0] as URI
                if (uri != null) {
                    if (uri.toString().contains("Inspector")) {
                        XposedBridge.invokeOriginalMethod(
                            param.method,
                            param.thisObject,
                            param.args
                        )
                    } else {
                        XposedBridge.log(TAG + "R/W [new File(URI)]: " + uri.toString())
                    }
                }
            }
        })
    }
}