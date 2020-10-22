package net.maxsmr.mxstemplate.ui

import android.content.res.ColorStateList
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import java.io.Serializable

data class NavigationMenuItem(
    val itemId: IMenuItemId,
    @StringRes
        val titleResId: Int,
    @DrawableRes
        val iconResId: Int,
    val textColorStateList: ColorStateList? = null
) : Serializable {

    interface IMenuItemId {

        val id: Int
    }
}
