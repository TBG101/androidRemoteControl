package zied.harzallah.controlandroid

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import java.io.ByteArrayOutputStream


class WebSocketManager(private val context: Context) {

    private var mSocket: Socket? = null
    private var tokenManager = SharedPreferencesHelper()


    init {
        var jwtToken = tokenManager.getToken(context)
        try {
            val options = IO.Options.builder().setExtraHeaders(
                mapOf(
                    "Authorization" to listOf("Bearer $jwtToken"), "hardware" to listOf("phone")
                )
            ).setTransports(arrayOf("websocket")).build()

            mSocket = IO.socket("ws://192.168.1.13:5000/", options)
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

    fun disconnect() {
        try {
            mSocket?.disconnect()
            mSocket?.close()


        } catch (e: Exception) {
            Log.e("REMOTE CONTROL", " exception on disconnect $e")
        }
    }

    fun sendImage(bitmap: Bitmap) {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageBytes = baos.toByteArray()
        val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)

        emit("image_event", base64Image)
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