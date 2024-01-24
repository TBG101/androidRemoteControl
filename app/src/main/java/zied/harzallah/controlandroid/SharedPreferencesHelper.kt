package zied.harzallah.controlandroid

import android.content.Context


class SharedPreferencesHelper {
    private val SHARED_PREF_NAME = "store"
    private val TOKEN_KEY = "token"

    fun saveToken(context: Context, token: String?) {
        val sharedPreferences = context.getSharedPreferences(
            SHARED_PREF_NAME, Context.MODE_PRIVATE
        )
        val editor = sharedPreferences.edit()
        editor.putString(TOKEN_KEY, token)
        editor.apply()
    }

    fun getToken(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences(
            SHARED_PREF_NAME, Context.MODE_PRIVATE
        )
        return sharedPreferences.getString(TOKEN_KEY, null)
    }
}