package net.maxsmr.core_network.session

import android.text.TextUtils
import androidx.lifecycle.LiveData

interface ISessionStorage {
    /**
     * Метод для сохранения сессии при стандартной авторизации (вход по логину и паролю)
     * @param session
     */
    var session: String?

    /**
     * Метод для сохранения сессии при покупке без авторизации
     * @param session сессия
     * @param signUpByCode флаг покупки без авторизации: true - покупка без авторизации (вход по e-mail и телефону), false - обычная авторизация (вход по логину и паролю)
     */
    fun set(session: String?, signUpByCode: Boolean)

    fun has(): Boolean = !TextUtils.isEmpty(session)

    /**
     * Метод для получения информации о способе авторизации
     * @return способ авторизации: true - была покупка без авторизации, сессия получена путем ввода телефона и e-email,
     * false - была обычная авторизация по логину и паролю
     */
    fun wasSignedUpByCode(): Boolean

    fun clear()

    fun observe(): LiveData<String>
}