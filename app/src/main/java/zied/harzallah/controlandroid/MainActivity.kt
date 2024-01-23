package zied.harzallah.controlandroid

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.WindowMetrics
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)


        val channel = NotificationChannel(
            "RemoteControl",
            "Phone being remote controlled",
            NotificationManager.IMPORTANCE_HIGH
        )

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)



        setContentView(
            R.layout.activity_main
        )

        val button1: Button = findViewById(R.id.button1)
        val button2: Button = findViewById(R.id.button2)
        val button3: Button = findViewById(R.id.button3)

        button1.setOnClickListener {
            val serviceIntent = Intent(applicationContext, RunningService::class.java)
            serviceIntent.action = RunningService.ACTION_START
            applicationContext.startService(serviceIntent)
        }
        button2.setOnClickListener {
            val serviceIntent = Intent(applicationContext, RunningService::class.java)
            serviceIntent.action = RunningService.ACTION_STOP
            applicationContext.startService(serviceIntent)
        }
        button3.setOnClickListener {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            println(displayMetrics.heightPixels)


        }


    }
}


