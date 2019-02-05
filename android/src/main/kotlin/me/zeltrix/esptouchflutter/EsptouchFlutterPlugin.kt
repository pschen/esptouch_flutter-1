package me.zeltrix.esptouchflutter

import java.net.InetAddress

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

import com.espressif.iot.esptouch.EsptouchResult
import com.espressif.iot.esptouch.util.ByteUtil
import com.espressif.iot.esptouch.util.EspNetUtil

import me.zeltrix.esptouchflutter.EsptouchFlutterPluginService.Companion.PARAMS_BROADCAST_DATA
import me.zeltrix.esptouchflutter.EsptouchFlutterPluginService.Companion.PARAMS_BSSID
import me.zeltrix.esptouchflutter.EsptouchFlutterPluginService.Companion.PARAMS_DEVICE_COUNT_DATA
import me.zeltrix.esptouchflutter.EsptouchFlutterPluginService.Companion.PARAMS_PASSWORD
import me.zeltrix.esptouchflutter.EsptouchFlutterPluginService.Companion.PARAMS_SSID
import me.zeltrix.esptouchflutter.EsptouchFlutterPluginService.Companion.FIELD_BSSID
import me.zeltrix.esptouchflutter.EsptouchFlutterPluginService.Companion.FIELD_INET_ADDRESS
import me.zeltrix.esptouchflutter.EsptouchFlutterPluginService.Companion.FIELD_ISSUC
import me.zeltrix.esptouchflutter.EsptouchFlutterPluginService.Companion.NOTIFICATION_ESP_TOUCH_DEVICE_ADDED

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

class EsptouchFlutterPlugin(val activity: Activity): MethodCallHandler {

  companion object {
    val TAG = "EsptouchFlutterPlugin"
    val METHOD_CHANNEL = "me.zeltrix.esptouch_flutter/method_channel"
    val EVENT_STREAM = "me.zeltrix.esptouch_flutter/event_stream"

    @JvmStatic
    fun registerWith(registrar: Registrar) {

      Log.d(TAG, "registerWith")

      val espPlugin = EsptouchFlutterPlugin(registrar.activity())
      val pluginMessenger = registrar.messenger()
      val methodchannel = MethodChannel(pluginMessenger, METHOD_CHANNEL)
      methodchannel.setMethodCallHandler(espPlugin)
      val eventChannel = EventChannel(pluginMessenger, EVENT_STREAM)
      eventChannel.setStreamHandler(espPlugin.EspTouchResultStream())

    }
  }

  override fun onMethodCall(call: MethodCall, result: Result) {

    when(call.method) {
      "getPlatformVersion" -> result.success("Android ${android.os.Build.VERSION.CODENAME}")
      "initEspTouch" -> {
        Log.d(TAG, "onMethodCall: initEspTouch")

        val ssidArg: String? = call.argument("ssid")
        val passwordArg: String? = call.argument("pass")
        val bssidArg: String? = call.argument("bssid")
        val deviceCountArg: String? = call.argument("deviceCount")
        val broadcastArg: String? = call.argument("broadcast")

        val ssid = ByteUtil.getBytesByString(ssidArg)
        val password = ByteUtil.getBytesByString(passwordArg)
        val bssid = EspNetUtil.parseBssid2bytes(bssidArg)
        val deviceCount = deviceCountArg!!.toByteArray()
        val broadcast = byteArrayOf(broadcastArg?.toInt()!!.toByte())
        val isInit = initEspTouch(ssid, bssid, password, deviceCount, broadcast)

        result.success(isInit)
      }
      else -> {
        result.notImplemented()
      }
    }

  }

  private fun initEspTouch(vararg params: ByteArray): Boolean {

    Log.d(TAG, "initEspTouch")

    val mServiceIntent = Intent(activity.applicationContext, EsptouchFlutterPluginService::class.java)
    mServiceIntent.action = EsptouchFlutterPluginService.ACTION_ESP_TOUCH_BROADCAST

    mServiceIntent.putExtra(PARAMS_SSID, params[0])
    mServiceIntent.putExtra(PARAMS_BSSID, params[1])
    mServiceIntent.putExtra(PARAMS_PASSWORD, params[2])
    mServiceIntent.putExtra(PARAMS_DEVICE_COUNT_DATA, params[3])
    mServiceIntent.putExtra(PARAMS_BROADCAST_DATA, params[4])

    val permissions = arrayOf(Manifest.permission.CHANGE_WIFI_MULTICAST_STATE)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (this.activity.applicationContext.checkSelfPermission(permissions[0]) != PackageManager.PERMISSION_GRANTED) {
        Log.w(TAG, "not granted: " + permissions[0])
        //Permission not granted
        this.activity.requestPermissions(permissions, 128)
        return false
      }
    }

    activity.startService(mServiceIntent)

    return true
  }

  inner class EspTouchResultStream: EventChannel.StreamHandler {

    private var eventSink: EventChannel.EventSink? = null

      private val receiver = object:BroadcastReceiver() {
          override fun onReceive(context: Context?, intent: Intent?) {
              Log.d(TAG, "EspTouchResultListener: onReceive")
              if(intent?.action == NOTIFICATION_ESP_TOUCH_DEVICE_ADDED) {
                  val espResult = EsptouchResult(
                          intent.getBooleanExtra(FIELD_ISSUC, false),
                          intent.getStringExtra(FIELD_BSSID),
                          intent.getSerializableExtra(FIELD_INET_ADDRESS) as InetAddress?
                  )

                  val response = EsptouchResultMap.getAsMap(espResult)

                  Log.d(TAG, espResult.bssid)
                  eventSink?.success(response)
              }
          }

      }

    override fun onListen(args: Any?, events: EventChannel.EventSink) {
      Log.d(TAG, "EspTouchResultStream: onListen")
      eventSink = events
      val intentFilter = IntentFilter()
      intentFilter.addAction(NOTIFICATION_ESP_TOUCH_DEVICE_ADDED)
      activity.registerReceiver(receiver, intentFilter)
    }

    override fun onCancel(args: Any?) {
      Log.d(TAG, "EspTouchResultStream: onCancel")
        activity.unregisterReceiver(receiver)
        if(eventSink != null) {
            eventSink = null
        }
    }

  }

}

private class EsptouchResultMap {
    companion object {
        fun getAsMap(espTouchResult: EsptouchResult):HashMap<String, String> {
            val hashMap = HashMap<String, String>()
            hashMap.set("bssid", espTouchResult.bssid)
            hashMap.set("inetAddress", espTouchResult.inetAddress.toString())
            hashMap.set("isSuc", espTouchResult.isSuc.toString())
            return hashMap
        }
    }
}
