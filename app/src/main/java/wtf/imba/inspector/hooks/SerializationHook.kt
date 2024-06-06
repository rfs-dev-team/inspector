package wtf.imba.inspector.hooks

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.type.java.FileClass
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import wtf.imba.inspector.utils.Util
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutput
import java.io.ObjectOutputStream


object SerializationHook : YukiBaseHooker() {

    const val TAG = "SerializationHook"
    var f = ""

    override fun onHook() {
        XposedHelpers.findAndHookConstructor(FileInputStream::class.java, FileClass, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                val file = param!!.args[0] as File
                if (file != null) {
                    ///
                    if (!file.path.contains("inspeckage") && (file.path.contains("data/data/")
                                || file.path.contains("storage/emulated/") || file.path.contains("data/media/"))
                    ) {
                        f = file.path
                    }
                }
            }
        })

        XposedHelpers.findAndHookMethod(ObjectInputStream::class.java, "readObject", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                val paramObject = param!!.result
                val sb = StringBuilder()

                if (paramObject != null) {
                    val name = paramObject.javaClass.canonicalName
                    if (name != null) {
                        if (name.length > 5 && name.substring(0, 5)
                                .contains("java.") || name.substring(0, 5).contains("byte")
                        ) {
                            //do nothing
                        } else {
                            sb.append("Read Object[$name] HEX = ")
                            val bos = ByteArrayOutputStream()
                            try {
                                val out: ObjectOutput = ObjectOutputStream(bos)
                                out.writeObject(paramObject)
                                val yourBytes = bos.toByteArray()
                                val hex = Util.toHexString(yourBytes)
                                sb.append(hex)
                                XposedBridge.log(TAG + "Possible Path [" + f + "] " + sb.toString())
                            } catch (e: NullPointerException) {
                                //
                            } catch (i: IOException) {
                                i.printStackTrace()
                            }
                        }
                    }
                }
            }
        })
    }
}