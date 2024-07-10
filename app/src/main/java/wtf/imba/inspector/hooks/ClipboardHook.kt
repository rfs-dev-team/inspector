package wtf.imba.inspector.hooks

import android.content.ClipData
import android.content.ClipboardManager
import android.util.Log
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import de.robv.android.xposed.XposedBridge


object ClipboardHook: YukiBaseHooker() {

    const val TAG = "INSPECT_ClipboardHook"

    override fun onHook() {
        ClipboardManager::class.java.hook {
            injectMember {
                method {
                    name = "setPrimaryClip"
                    returnType = ClipData::class.java
                }
                beforeHook {
                    val cd = this.args[0] as ClipData?
                    val sb = StringBuilder()
                    if (cd != null && cd.itemCount > 0) {
                        for (i in 0 until cd.itemCount) {
                            val item = cd.getItemAt(i)
                            sb.append(item.text)
                        }
                    }
                    Log.d(TAG, "onHook: sb is: $sb")
                    XposedBridge.log(TAG + "Copied to the clipboard: " + sb.toString() + "")
                }
            }
        }
    }
}