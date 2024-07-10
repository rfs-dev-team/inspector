package wtf.imba.inspector

import android.graphics.Path.FillType
import android.util.Log
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.type.java.StringClass
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import de.robv.android.xposed.XSharedPreferences
import wtf.imba.inspector.hooks.ClipboardHook
import wtf.imba.inspector.hooks.FileSystemHook
import wtf.imba.inspector.hooks.FingerprintHook
import wtf.imba.inspector.hooks.SQLiteHook
import wtf.imba.inspector.hooks.SerializationHook
import wtf.imba.inspector.hooks.SharedPrefsHook
import wtf.imba.inspector.hooks.UIHook
import wtf.imba.inspector.hooks.UserHook
import wtf.imba.inspector.utils.Config.SP_DATA_DIR
import wtf.imba.inspector.utils.FileType
import java.io.File

@InjectYukiHookWithXposed
class MainHook : IYukiHookXposedInit {

    private val TAG = "INSPECT_MainHook"
    override fun onHook() =encase {
        loadZygote { initZygote() }

        if (this.packageName == "wtf.imba.inspector") { return@encase }
        if (this.packageName != sPrefs?.getString("package", "")) { return@encase }

        val folder: File = File(sPrefs?.getString(SP_DATA_DIR, null))
        folder.setExecutable(true, false)

        "android.util.Log".hook {
            injectMember {
                method {
                    name = "i"
                    returnType = StringClass
                    param(StringClass)
                }
                afterHook {
                    if (this.args[0] == "Xposed") {
                        val log = this.args[1] as String
                        var ft: FileType? = null
                        if (log.contains(SharedPrefsHook.TAG)) {
                            ft = FileType.PREFS
                        } else if (log.contains(SQLiteHook.TAG)) {
                            ft = FileType.SQLITE
                        } else if (log.contains(ClipboardHook.TAG)) {
                            ft = FileType.CLIPB
                        } else if (log.contains(FileSystemHook.TAG)) {
                            ft = FileType.FILESYSTEM
                        } else if (log.contains(SerializationHook.TAG)) {
                            ft = FileType.SERIALIZATION
                        } else if (log.contains(UserHook.TAG)) {
                            ft = FileType.USERHOOKS
                        }
                        
                        if (ft != null) {
                            Log.d(TAG, "onHook: ft is: $ft")
                        }

                        loadHooker(UIHook)
                        loadHooker(ClipboardHook)
                        loadHooker(FileSystemHook)
                        loadHooker(SharedPrefsHook)
                        loadHooker(SQLiteHook)
                        loadHooker(SerializationHook)
                        loadHooker(UserHook)
                        loadHooker(FingerprintHook)
                    }
                }
            }
        }
    }


    override fun onInit()  = configs{
        super.onInit()
        isDebug = true
        isEnableHookSharedPreferences = true
    }

    private fun initZygote() {
        sPrefs = XSharedPreferences(MY_PACKAGE_NAME, PREFS)
        sPrefs?.makeWorldReadable()
    }

    companion object {
        const val PREFS = "InspectorPrefs"
        var sPrefs: XSharedPreferences? = null
        val MY_PACKAGE_NAME = MainHook::class.java.`package`?.name
    }
}