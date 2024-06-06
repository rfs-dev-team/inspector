package wtf.imba.inspector.hooks

import android.app.Activity
import android.util.Log
import androidx.fragment.app.Fragment
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.type.android.BundleClass

object UIHook: YukiBaseHooker() {

    private const val TAG = "UIHook"
    override fun onHook() {
        Activity::class.java.hook {
            injectMember {
                method {
                    name = "onCreate"
                    returnType = BundleClass
                }
                afterHook {
                    Log.d(TAG, "onHook: afterHook on onCreate")
                }
            }
        }

        Fragment::class.java.hook {
            injectMember {
                method {
                    name = "onCreate"
                    returnType = BundleClass
                }
                afterHook {
                    Log.d(TAG, "onHook: afterhook with Fragment")
                }
            }
        }
    }
}