package wtf.imba.inspector.hooks

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.type.java.IntArrayType
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringArrayClass
import com.highcapable.yukihookapi.hook.type.java.StringClass
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.Error
import java.lang.String
import kotlin.Int


object ProcessHook: YukiBaseHooker() {

    private const val TAG = "ProcessHook"
    override fun onHook() {
        try {
            XposedHelpers.findAndHookMethod("android.os.Process", appClassLoader, "start", StringClass, StringClass, IntType, IntType, IntArrayType, IntType,
                 IntType, IntType, StringClass, StringClass, StringClass, StringClass, StringArrayClass, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam?) {
                        super.afterHookedMethod(param)
                        val uid = param!!.args[2] as Int
                        if (uid == 10066) {
                            val debugFlags = param!!.args[5] as Int
                            param!!.args[5] = debugFlags or 0x1
                            XposedBridge.log(TAG + "debugFlags: " + String.valueOf(param!!.args[5]))
                        }
                    }
                 })
        } catch (e: Error) {
            XposedBridge.log("ERROR_PROCESS: "+e.message);
        }
    }
}