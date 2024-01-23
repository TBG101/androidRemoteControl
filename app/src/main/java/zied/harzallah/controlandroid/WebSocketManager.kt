package zied.harzallah.controlandroid

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import android.util.Base64
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter


class WebSocketManager {

    private var mSocket: Socket? = null
    private val jwtToken = "q5xntmuiWMv9grUz00848W7f4la5SPQjJ37gKqYH"


    init {
        try {
            val options = IO.Options.builder().setExtraHeaders(
                    mapOf("Authorization" to listOf("Bearer $jwtToken"))
                ).build()

            mSocket = IO.socket("ws://192.168.1.13:5000/", options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun connect() {
        try {

            mSocket?.connect()
        } catch (e: Exception) {
            println("execptiçon")
        }
        mSocket?.on(Socket.EVENT_CONNECT) {
            emit("message", "Phone Connected")
        }

    }

    fun disconnect() {
        mSocket?.disconnect()
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