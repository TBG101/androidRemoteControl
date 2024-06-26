package zied.harzallah.controlandroid

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val channel = NotificationChannel(
            "RemoteControl", "Phone being remote controlled", NotificationManager.IMPORTANCE_HIGH
        )

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)


        val serviceIntent = Intent(this, RunningService::class.java)
        serviceIntent.action = RunningService.ACTION_START
        this.startService(serviceIntent)

        setContentView(
            R.layout.activity_main
        )

        val button1: Button = findViewById(R.id.button1)
        val button2: Button = findViewById(R.id.button2)
        val button3: Button = findViewById(R.id.button3)


        button1.setOnClickListener {
            val serviceIntent = Intent(this, RunningService::class.java)
            serviceIntent.action = RunningService.ACTION_START
            this.startService(serviceIntent)
        }
        button2.setOnClickListener {
            val serviceIntent = Intent(this, RunningService::class.java)
            serviceIntent.action = RunningService.ACTION_STOP
            this.startService(serviceIntent)
        }

        button3.setOnClickListener {

                var cmd = arrayOf(
                    "su", "-c", "input motionevent DOWN 500 600"
                )
                var p = Runtime.getRuntime().exec(cmd)
                p.waitFor()
                p.outputStream.flush()
                cmd = arrayOf(
                    "su", "-c", "input motionevent MOVE 800 900"
                )
                p = Runtime.getRuntime().exec(cmd)
                p.waitFor()
                p.outputStream.flush()
                cmd = arrayOf(
                    "su", "-c", "input motionevent MOVE 100 200"
                )
                p = Runtime.getRuntime().exec(cmd)
                p.waitFor()
                p.outputStream.flush()
                cmd = arrayOf(
                    "su", "-c", "input motionevent UP 200 240"
                )
                p = Runtime.getRuntime().exec(cmd)
                p.waitFor()
                p.outputStream.flush()


        }

    }
}


