package zied.harzallah.controlandroid

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService

class MyJobIntentService : JobIntentService() {

    companion object {
        private const val JOB_ID = 1

        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, MyJobIntentService::class.java, JOB_ID, work)
        }
    }

    override fun onHandleWork(intent: Intent) {
        val serviceIntent = Intent(this, RunningService.ACTION_START.toString()::class.java)
        startService(serviceIntent)
    }

}