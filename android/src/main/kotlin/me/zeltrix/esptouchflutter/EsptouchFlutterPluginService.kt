package me.zeltrix.esptouchflutter

import android.annotation.SuppressLint
import android.app.IntentService
import android.content.Intent
import android.util.Log

import com.espressif.iot.esptouch.EsptouchTask
import com.espressif.iot.esptouch.IEsptouchResult
import com.espressif.iot.esptouch.IEsptouchListener


@SuppressLint("NewApi")
class EsptouchFlutterPluginService: IntentService("EsptouchFlutterPluginService") {

    private lateinit var mEsptouchTask: EsptouchTask

    override fun onHandleIntent(intent: Intent?) {
        Log.d(TAG, "onHandleIntent()")
        val action = intent?.action
        when(action) {
            ACTION_ESP_TOUCH_BROADCAST -> {
                val apSsid = intent.getByteArrayExtra(PARAMS_SSID)
                val apBssid = intent.getByteArrayExtra(PARAMS_BSSID)
                val apPassword = intent.getByteArrayExtra(PARAMS_PASSWORD)
                val deviceCountData = intent.getByteArrayExtra(PARAMS_DEVICE_COUNT_DATA)
                val broadcastData = intent.getByteArrayExtra(PARAMS_BROADCAST_DATA)
                Log.d(TAG, "initiating Esp Touch")
                initTouch(apSsid, apBssid, apPassword, deviceCountData, broadcastData)
                Log.d(TAG, "initiated Esp Touch")
            }
            else -> {
                Log.d(TAG, "Action not implemented.")
            }
        }

    }

    private fun initTouch(vararg params: ByteArray): List<IEsptouchResult> {

        Log.d(TAG, "initTouch")

        val taskResultCount: Int
        val apSsid = params[0]
        val apBssid = params[1]
        val apPassword = params[2]
        val deviceCountData = params[3]
        val broadcastData = params[4]
        taskResultCount = if (deviceCountData.isEmpty()) -1 else Integer.parseInt(String(deviceCountData))

        mEsptouchTask = EsptouchTask(apSsid, apBssid, apPassword, applicationContext)
        mEsptouchTask.setPackageBroadcast(broadcastData[0].toInt() == 1)
        mEsptouchTask.setEsptouchListener(myListener)
        return mEsptouchTask.executeForResults(taskResultCount)

    }

    private val myListener = IEsptouchListener { result -> onEsptouchResultAddedPerform(result) }

    private fun onEsptouchResultAddedPerform(result: IEsptouchResult) {

        Log.d(TAG, "onEsptouchResultAddedPerform" + result.bssid)

        val intent = Intent()
        intent.action = NOTIFICATION_ESP_TOUCH_DEVICE_ADDED
        intent.putExtra(FIELD_BSSID, result.bssid)
        intent.putExtra(FIELD_INET_ADDRESS, result.inetAddress)
        intent.putExtra(FIELD_ISCANCELLED, result.isCancelled)
        intent.putExtra(FIELD_ISSUC, result.isSuc)

        sendBroadcast(intent)

    }


    companion object {
        const val TAG = "EsptouchPluginService"

        const val ACTION_ESP_TOUCH_BROADCAST = "esptouch.broadcast"
        const val NOTIFICATION_ESP_TOUCH_DEVICE_ADDED = "esptouch.notify.device_added"

        const val PARAMS_SSID = "ssid"
        const val PARAMS_BSSID = "bssid"
        const val PARAMS_PASSWORD = "password"
        const val PARAMS_DEVICE_COUNT_DATA = "deviceCountData"
        const val PARAMS_BROADCAST_DATA = "broadcastData"

        const val FIELD_BSSID = "bssid"
        const val FIELD_INET_ADDRESS = "inetaddr"
        const val FIELD_ISCANCELLED = "isCancelled"
        const val FIELD_ISSUC = "isSuc"

    }
}