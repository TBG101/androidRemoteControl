package zied.harzallah.controlandroid

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import io.socket.engineio.parser.Base64
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

import kotlin.concurrent.thread


class RunningService : Service() {
    private var serviceRunning = false
    private var running = false
    private lateinit var webSocketManager: WebSocketManager
    private var screenHeight: Int = 0
    private var screenWidth: Int = 0
    private var mySid = ""
    private var targetSid = ""

    companion object {
        private const val CHANNEL_ID = "RemoteControl"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent?.action.toString() == ACTION_START && !serviceRunning) {
            Log.i("REMOTE CONTROL", "started service remote control")
            serviceRunning = true
            val (width, height) = getScreenResolution()
            screenWidth = width
            screenHeight = height
            startForegroundService()

        } else {
            try {
                stopSelf()
            } catch (e: Exception) {
                Log.e("REMOTE CONTROL", "Error on closing socket  $e")
            }
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
                    Log.e("REMOTE CONTROL", "capture error")
                }
            } else if (map["data"] == "stop capture") {
                running = false
            } else if (map["data"] == "tap") {
                tap(map["x"], map["y"])
            } else if (map["data"] == "swipe") {
                val cor = Json.decodeFromString<List<List<Double>>>(map["coordinates"]!!)
                thread(start = true) {
                    swipe(cor)
                }
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
                println("my sid is $mySid")
            }
        }
        webSocketManager.on("createconnection") { args ->
            if (args.isNotEmpty()) {
                targetSid = args[0].toString()
                println("my sid is $targetSid")
            }
            if (targetSid.isEmpty()) {
                running = false
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
            Runtime.getRuntime().exec(cmd)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun swipe(cor: List<List<Double>>) {
        if (cor.size == 2) {
            val realX = (cor[0][0] / 100) * screenWidth
            val realY = (cor[0][1] / 100) * screenHeight
            val realX1 = (cor[1][0] / 100) * screenWidth
            val realY1 = (cor[1][1] / 100) * screenHeight
            simpleSwipe(realX, realY, realX1, realY1)
            return
        }

        for (i in 0 until cor.size) {
            println(cor[i])
            val realX = (cor[i][0] / 100) * screenWidth
            val readY = (cor[i][1] / 100) * screenHeight
            when (i) {
                (0) -> {
                    fingerDown(realX, readY)
                }

                (cor.size - 1) -> {
                    fingerUp(realX, readY)
                }

                else -> {
                    fingerMove(realX, readY)
                }
            }
        }


    }

    private fun fingerDown(x: Double, y: Double) {
        val cmd = arrayOf(
            "su", "-c", "input motionevent DOWN $x $y"
        )
        Runtime.getRuntime().exec(cmd).waitFor()

        Runtime.getRuntime().exec(
            arrayOf(
                "su", "-c", "input motionevent MOVE ${x + 1} ${y + 1}"
            )
        ).waitFor()
    }

    private fun fingerMove(x: Double, y: Double) {
        val cmd = arrayOf(
            "su", "-c", "input motionevent MOVE $x $y"
        )
        Runtime.getRuntime().exec(cmd).waitFor()
    }

    private fun fingerUp(x: Double, y: Double) {

        Runtime.getRuntime().exec(
            arrayOf(
                "su", "-c", "input motionevent MOVE $x $y"
            )
        ).waitFor()
        val cmd = arrayOf(
            "su", "-c", "input motionevent UP $x $y"
        )
        Runtime.getRuntime().exec(cmd).waitFor()
    }

    private fun simpleSwipe(x: Double, y: Double, x1: Double, x2: Double) {
        val cmd = arrayOf(
            "su", "-c", "input swipe $x $y $x1 $x2"
        )
        Runtime.getRuntime().exec(cmd).waitFor()
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
            @Suppress("DEPRECATION") display?.getRealMetrics(displayMetrics)
        } else {
            @Suppress("DEPRECATION") windowManager.defaultDisplay.getRealMetrics(displayMetrics)
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
        val sh = Runtime.getRuntime().exec("su", null, null)
        val os = sh.outputStream
        os.write("/system/bin/screencap -p\n".toByteArray(charset("ASCII")))
        os.flush()
        return BitmapFactory.decodeStream(sh.inputStream)
    }

    private fun sendScreenshot() {
        Log.i("REMOTE CONTROL", "saving")
        thread(start = true) {
            while (running && targetSid.isNotEmpty()) {
                val bitmap = takeScreenshot()
                if (bitmap != null && running) {
                    val baos = ByteArrayOutputStream()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 50, baos)
                    } else {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
                    }
                    val imageBytes = baos.toByteArray()
                    val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                    webSocketManager.emit(
                        "image_event", Gson().toJson(
                            mapOf(
                                "target" to targetSid, "image" to base64Image
                            )
                        )
                    )
                } else {
                    Log.i("REMOTE CONTROL", "Stopped running")
                }
            }
        }
    }

    override fun onDestroy() {
        running = false
        serviceRunning = false
        try {
            webSocketManager.disconnect(mySid)
        } catch (e: Exception) {
            Log.e("REMOTE CONTROL", "Socket disconnect error: $e")
        }

        super.onDestroy()
    }


}