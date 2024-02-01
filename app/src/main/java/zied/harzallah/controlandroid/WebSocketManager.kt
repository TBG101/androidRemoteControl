package zied.harzallah.controlandroid

import android.content.Context
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject


class WebSocketManager(private var context: Context) {

    private var mSocket: Socket? = null
    private var tokenManager = SharedPreferencesHelper()

    init {
        val jwtToken = tokenManager.getToken(context)
        try {
            val options = IO.Options.builder().setExtraHeaders(
                mapOf(
                    "Authorization" to listOf("Bearer $jwtToken"), "hardware" to listOf("phone")
                )
            ).setTransports(arrayOf("websocket")).build()

            mSocket = IO.socket("wss://testiingdeploy.onrender.com/", options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun connect() {
        try {
            if (mSocket?.connected() != true) {
                mSocket?.connect()
                println("connection establised")
            }
        } catch (e: Exception) {
            println("exception")
        }
    }

    fun disconnect(target: String) {
        try {
            if (target.isNotEmpty()) mSocket?.emit(
                "message", JSONObject(
                    mapOf(
                        "target" to target, "data" to "disconnected",
                    )
                ).toString()

            )
            mSocket?.disconnect()
            mSocket?.close()
        } catch (e: Exception) {
            Log.e("REMOTE CONTROL", " exception on disconnect $e")
        }
    }

    fun emit(event: String, arg: Any) {
        mSocket?.emit(event, arg)
    }

    fun on(event: String, listener: Emitter.Listener) {
        mSocket?.on(event, listener)
    }

    fun setIncomingMessageListener(listener: (String) -> Unit) {
        on("message", Emitter.Listener { args ->
            if (args.isNotEmpty()) {
                val message = args[0].toString()
                listener.invoke(message)
            }
        })
    }


}