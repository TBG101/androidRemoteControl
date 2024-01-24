package zied.harzallah.controlandroid

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL


class LoginActivity : ComponentActivity() {
    private lateinit var email: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private var tokenManager = SharedPreferencesHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)


        email = findViewById(R.id.editTextUsername)
        passwordEditText = findViewById(R.id.editTextPassword)
        loginButton = findViewById(R.id.buttonLogin)

        loginButton.setOnClickListener {
            val url = "http://192.168.1.13:5000/login"
            val jsonData =
                "{ \"email\": \"${email.text}\", \"password\": \"${passwordEditText.text}\" }"
            if (email.text.isEmpty() && passwordEditText.text.isEmpty()) {
                Toast.makeText(this, "email or password is empty", Toast.LENGTH_SHORT)
                return@setOnClickListener
            }


            lifecycleScope.launch {
                try {
                    val result = makePostRequest(url, jsonData)
                    if (result[1] == "200") {
                        val json: Map<String, JsonElement> =
                            Json.parseToJsonElement(result[0]).jsonObject
                        println(json["access_token"].toString().removeSurrounding("\""))
                        tokenManager.saveToken(
                            this@LoginActivity,
                            json["access_token"].toString().removeSurrounding("\"")
                        )
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        println("CODE 400")

                    }


                    // Handle the response here
                } catch (e: Exception) {
                    // Handle exceptions or errors
                    println("error connecting to server")
                }
            }
        }


        lifecycleScope.launch {
            checkForToken()
        }
    }

    private suspend fun makePostRequest(urlString: String, jsonData: String): List<String> {
        return withContext(Dispatchers.IO) {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
                connection.doOutput = true

                // Write JSON data to the request body
                val outputStream: OutputStream = connection.outputStream
                outputStream.write(jsonData.toByteArray())

                val responseCode = connection.responseCode

                // Get response from the server
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()

                var line: String? = reader.readLine()
                while (line != null) {
                    response.append(line)
                    line = reader.readLine()
                }
                println(responseCode.toString())
                arrayListOf<String>(response.toString(), responseCode.toString())

            } finally {
                connection.disconnect()
            }
        }
    }


    private suspend fun checkForToken() {
        val t = tokenManager.getToken(this)
        if (t.isNullOrEmpty()) return

        println(t)
        val url = "http://192.168.1.13:5000/protected"


        val r = withContext(Dispatchers.IO) {
            val url = URL(url)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET";
            connection.setRequestProperty(
                "Authorization", "Bearer ${t!!.removeSurrounding("\"")}"
            )
            connection.setRequestProperty(
                "Content-Type", "application/json"
            )

            try {

                val responseCode = connection.responseCode

                if (responseCode == 200) {

                    return@withContext responseCode
                } else {
                    // Handle other HTTP response codes as needed
                    print("different than 200")
                    return@withContext null
                }
            } finally {
                connection.disconnect()
            }

        }

        if (r != null) {
            println("Permitted to enter: $r")
            val intent = Intent(this@LoginActivity, MainActivity::class.java)
            startActivity(intent)
            finish()

        } else {
            // Handle the case where there was an error or the response was null
            println("Error or null response,$r")
        }
    }
}