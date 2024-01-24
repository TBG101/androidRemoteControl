package zied.harzallah.controlandroid

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.os.PowerManager.WakeLock
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.core.app.NotificationCompat
import io.socket.engineio.parser.Base64
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.net.DatagramSocket
import kotlin.concurrent.thread


class RunningService : Service() {
    private var socket: DatagramSocket? = null
    private var running = false
    private lateinit var wakeLock: WakeLock
    private lateinit var webSocketManager: WebSocketManager
    private var screenHeight: Int = 0
    private var screenWidth: Int = 0
    private var serviceRunning = false
    private var mySid = ""

    companion object {
        private const val CHANNEL_ID = "RemoteControl"
        const val ACTION_START = "com.example.ACTION_START"
        const val ACTION_STOP = "com.example.ACTION_STOP"
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("REMOTE CONTROL", "started service remote control")
        if (intent?.action.toString() == ACTION_START && !serviceRunning) {
            serviceRunning = true
            val (width, height) = getScreenResolution()
            screenWidth = width
            screenHeight = height
            println(screenHeight)
            startForegroundService()
        } else {
            stopSelf()
            serviceRunning = false
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        webSocketManager = WebSocketManager(this)
        webSocketManager.connect()

        webSocketManager.setIncomingMessageListener { message ->
            Log.i("REMOTE CONTROL", "Received message: $message")

            val map = Json.decodeFromString<Map<String, String>>(message)

            if (map["data"] == "capture") {
                Log.i("REMOTE CONTROL", "capturing")
                try {
                    running = true

                    sendScreenshot()

                } catch (e: Exception) {
                    Log.e("REMOTE CONTROL", "capture errpr")
                }
            } else if (map["data"] == "stop capture") {
                running = false;
            } else if (map["data"] == "tap") {
                tap(map["x"], map["y"])
            } else if (map["data"] == "swipe") {
                swipe(map["x1"], map["y1"], map["x2"], map["y2"])
            } else if (map["data"] == "lock") {
                lockBTN()
            } else if (map["data"] == "volumeUp") {
                volumeUp()
            } else if (map["data"] == "volumeDown") {
                volumeDown()
            }
        }

        webSocketManager.on("getsid") { args ->
            if (args.isNotEmpty()) {
                mySid = args[1].toString()
            }
        }

        val notification = createNotification()
        startForeground(1, notification)
    }

    private fun volumeDown() {
        Runtime.getRuntime().exec(arrayOf("su", "-c", "input keyevent 25"))
    }

    private fun volumeUp() {
        Runtime.getRuntime().exec(arrayOf("su", "-c", "input keyevent 24"))
    }

    private fun lockBTN() {
        try {
            Runtime.getRuntime().exec(arrayOf("su", "-c", "input keyevent 26"))
        } catch (e: Exception) {
            Log.e("REMOTE CONTROL", "LOCK BTN ERROR: $e")
        }
    }

    private fun tap(x: String?, y: String?) {
        if (x == null || y == null) return
        try {
            println("Performing tap function")
            val realX = (x.toDouble() / 100) * screenWidth
            val readY = (y.toDouble() / 100) * screenHeight
            println("tap x $realX")
            println("tap Y $readY")
            val cmd = arrayOf(
                "su", "-c", "input tap $realX $readY"
            )
            val p = Runtime.getRuntime().exec(cmd)
            p.waitFor()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun swipe(x1: String?, y1: String?, x2: String?, y2: String?) {
        if (x1 == null || y1 == null || x2 == null || y2 == null) return

        try {
            println("Performing swipe function")
            val realX = (x1.toDouble() / 100) * screenWidth
            val readY = (y1.toDouble() / 100) * screenHeight
            val realX2 = (x2.toDouble() / 100) * screenWidth
            val readY2 = (y2.toDouble() / 100) * screenHeight

            Log.i("REMOTE CONTROL", "$realX $readY $realX2 $readY2")
            val cmd = arrayOf(
                "su", "-c", "input swipe $realX $readY $realX2 $readY2"
            )

            val p = Runtime.getRuntime().exec(cmd)
            p.waitFor()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun getScreenResolution(): Pair<Int, Int> {

        val windowManager = this.getSystemService(WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            Check if this reutrn the right heught and width
            val metrics: WindowMetrics =
                this.getSystemService(WindowManager::class.java).currentWindowMetrics
            return Pair(metrics.bounds.height(), metrics.bounds.width())

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = this.display
            display?.getRealMetrics(displayMetrics)
        } else {
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        }
        return Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }

    private fun createNotification(): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground).setContentTitle("RemoteControl")
            .setContentText("Phone being remote controlled").setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        return builder.build()
    }

    private fun takeScreenshot(): Bitmap? {
        var screenshot: Bitmap? = null
        val sh = Runtime.getRuntime().exec("su", null, null)
        val os = sh.outputStream
        os.write("/system/bin/screencap -p\n".toByteArray(charset("ASCII")))
        os.flush()

        val `is` = sh.inputStream
        screenshot = BitmapFactory.decodeStream(`is`)
        return screenshot
    }

    private fun sendScreenshot() {
        Log.i("REMOTE CONTROL", "saving")
        // check if this works without the thread
        thread(start = true) {
            while (running) {
                var bitmap = takeScreenshot()
                if (bitmap != null && running) {
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                    val imageBytes = baos.toByteArray()
                    val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                    webSocketManager.emit("image_event", base64Image)
                } else {
                    Log.e("REMOTE CONTROL", "ERROR")

                }
            }
        }
    }

    override fun onDestroy() {
        running = false

        try {
            socket?.disconnect()

        } catch (e: Exception) {
            Log.e("REMOTE CONTROL", "Socket disconnect error: $e")
        }
        try {
            socket?.close()

        } catch (e: Exception) {
            Log.e("REMOTE CONTROL", "Socket close error: $e")
        }

        try {
            wakeLock?.release()
        } catch (e: Exception) {
            Log.e("REMOTE CONTROL", "wakelock close error: $e")
        }
        super.onDestroy()
    }

}