package wtf.imba.inspector.hooks

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers
import wtf.imba.inspector.MainHook
import wtf.imba.inspector.hooks.entity.FingerprintItem
import wtf.imba.inspector.hooks.entity.FingerprintList


object FingerprintHook : YukiBaseHooker() {

    private const val TAG = "FingerprintHook"
    private var sPrefs: XSharedPreferences? = null
    private val gson = GsonBuilder().disableHtmlEscaping().create()

    fun loadPrefs() {
        sPrefs = XSharedPreferences(
            MainHook::class.java.getPackage()?.getName() ?: "",
            MainHook.PREFS
        )
        sPrefs!!.makeWorldReadable()
    }
    
    override fun onHook() {
        loadPrefs()
        try {
            loadPrefs()
            val json = sPrefs!!.getString("fingerprint_hooks", "")
            val classBuild: Class<*> = XposedHelpers.findClass("android.os.Build", appClassLoader)
            val classBuildVersion: Class<*> = XposedHelpers.findClass("android.os.Build.VERSION", appClassLoader)

            try {
                val fingerprintList : FingerprintList? = gson.fromJson(json, FingerprintList::class.java) //as FingerprintList
                if (fingerprintList?.fingerprintItems != null && fingerprintList.fingerprintItems.isNotEmpty()) {
                    for (fingerprintItem: FingerprintItem in fingerprintList.fingerprintItems) {
                        if (fingerprintItem.enable) {
                            if (fingerprintItem.type.equals("BUILD")) {
                                XposedHelpers.setStaticObjectField(classBuild, fingerprintItem.name, fingerprintItem.newValue)
                            }else if (fingerprintItem.type == "VERSION") {
                                XposedHelpers.setStaticObjectField(classBuildVersion, fingerprintItem.name, fingerprintItem.newValue)
                            } else if (fingerprintItem.type == "TelephonyManager") {

                                try {
                                    when (fingerprintItem.name) {
                                        "IMEI" -> {
                                            HookFingerprintItem("android.telephony.TelephonyManager", "getDeviceId")
                                            HookFingerprintItem("com.android.internal.telephony.PhoneSubInfo", "getDeviceId")
                                            HookFingerprintItem("com.android.internal.telephony.PhoneProxy", "getDeviceId")
                                        }
                                        "IMSI" -> {
                                            HookFingerprintItem("android.telephony.TelephonyManager", "getSubscriberId")
                                        }
                                        "PhoneNumber" -> {
                                            HookFingerprintItem("android.telephony.TelephonyManager", "getLine1Number")
                                        }
                                        "SimSerial" -> HookFingerprintItem("android.telephony.TelephonyManager","getSimSerialNumber")
                                        "CarrierCode" -> HookFingerprintItem("android.telephony.TelephonyManager","getNetworkOperator")
                                        "Carrier" -> HookFingerprintItem("android.telephony.TelephonyManager","getNetworkOperatorName")
                                        "SimCountry" -> HookFingerprintItem("android.telephony.TelephonyManager","getSimCountryIso")
                                        "NetworkCountry" -> HookFingerprintItem("android.telephony.TelephonyManager","getNetworkCountryIso")
                                        "SimSerialNumber" -> HookFingerprintItem("android.telephony.TelephonyManager","getSimSerialNumber")
                                    }
                                }catch (e: Error) {
                                    Log.d(TAG, "onHook: fingerprintItem.name is: ${fingerprintItem.name}  error is: ${e.message}")
                                }
                            } else if (fingerprintItem.type == "Advertising") {
                                try {
                                    HookFingerprintItem("com.google.android.gms.ads.identifier.AdvertisingIdClient","getId")
                                }catch (e: Error){

                                }
                            }else if (fingerprintItem.type.equals("Wi-Fi")) {
                                try {
                                    when(fingerprintItem.name) {
                                        "BSSID" -> HookFingerprintItem("android.net.wifi.WifiInfo","getBSSID")
                                        "SSID" -> HookFingerprintItem("android.net.wifi.WifiInfo", "getSSID")
                                        "IP" -> HookFingerprintItem("android.net.wifi.WifiInfo", "getIpAddress")
                                        "Android" -> HookFingerprintItem("java.net.NetworkInterface", "getHardwareAddress")
                                    }
                                }catch (e: Error) {}
                            }
                        }
                    }
                }
            }catch (e: JsonSyntaxException) {
                Log.d(TAG, "onHook: ${e.message}")
            }
        }catch (e: NoSuchMethodError) {
            Log.d(TAG, "onHook: ${e.message}")}
    }

    private fun HookFingerprintItem(
        hookClass: String,
        methodName: String
    ) {
        try {
            hookClass.hook {
                injectMember {
                    method {
                        name = methodName
                    }
                    afterHook {
                        Log.d(TAG, "HookFingerprintItem: thisParap is: ${this.result}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "HookFingerprintItem: $methodName, Error is: ${e.message}")
        }
    }
}