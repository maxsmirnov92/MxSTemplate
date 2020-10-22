package net.maxsmr.core_network.session

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import net.maxsmr.core_common.BaseApplication
import net.maxsmr.core_common.SharedPreferenceLiveData.SharedPreferenceStringLiveData

object SessionStorage : ISessionStorage {

    private const val PREFS = "session_storage_prefs"
    private const val SESSION = "session"
    private const val SIGN_UP_BY_CODE = "sign_up_by_code"

    private val context: Context get() = BaseApplication.context
    private val prefs by lazy {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    override var session: String?
        get() {
            return prefs.getString(SESSION, "")
        }
        set(value) {
            set(value, false)
        }

    @SuppressLint("ApplySharedPref")
    override fun set(session: String?, signUpByCode: Boolean) {
        prefs.edit().putString(SESSION, session).putBoolean(SIGN_UP_BY_CODE, signUpByCode).commit()
    }

    override fun wasSignedUpByCode(): Boolean {
        return prefs.getBoolean(SIGN_UP_BY_CODE, false)
    }

    @SuppressLint("CommitPrefEdits", "ApplySharedPref")
    override fun clear() {
        prefs.edit().clear().commit()
    }

    override fun observe(): LiveData<String> {
        return SharedPreferenceStringLiveData(prefs, SESSION, "")
    }
}