package net.maxsmr.mxstemplate

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.os.Build
import android.os.Build.VERSION_CODES.Q
import android.os.Environment
import net.maxsmr.commonutils.android.media.isExternalStorageMountedAndWritable
import net.maxsmr.commonutils.android.media.isExternalStorageMountedReadOnly
import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.core_common.BaseApplication
import java.io.File

object FilePaths {

    /**
     * Своя внутренняя директория - не требует [WRITE_EXTERNAL_STORAGE] / [READ_EXTERNAL_STORAGE]
    */
    fun privateInternalDirPath(dirType: String = EMPTY_STRING): File = if (dirType.isNotEmpty()) {
        File(BaseApplication.context.filesDir, dirType)
    } else {
        BaseApplication.context.filesDir
    }

    /**
     * Не требует [WRITE_EXTERNAL_STORAGE] / [READ_EXTERNAL_STORAGE]
     */
    fun privateExternalMediaDirPath(isReadOnly: Boolean = false): File? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && canUseExternal(isReadOnly)) {
            val dirs = BaseApplication.context.externalMediaDirs
            if (dirs != null && dirs.isNotEmpty()) {
                return dirs[0]
            }
        }
        return null
    }

    /**
     * @param isExternal true - требует [WRITE_EXTERNAL_STORAGE] / [READ_EXTERNAL_STORAGE]
     */
    fun privateDirPath(
        dirType: String = EMPTY_STRING,
        isExternal: Boolean = true,
        isReadOnly: Boolean = false
    ): File =
        if (isExternal && canUseExternal(isReadOnly)) {
            BaseApplication.context.getExternalFilesDir(dirType) ?: throw IllegalStateException("Relative path $dirType not found")
        } else {
            privateInternalDirPath(dirType)
        }

    /**
     * @param ignoreVersion проигнорировать версию, при наличии флага requestLegacyExternalStorage в манифесте на Q и пермишна MANAGE_EXTERNAL_STORAGE на > Q
     * @param dirType - поддиректория в основной публичной или приватной (/data/data или sdcard/Android/data) папке
     * @param isExternal true - по возможности на внешней карте
     */
    @Suppress("DEPRECATION")
    fun dirPath(
        dirType: String = EMPTY_STRING,
        ignoreVersion: Boolean = false,
        isExternal: Boolean = true,
        isReadOnly: Boolean = false
    ): File =
        if (!ignoreVersion && (Build.VERSION.SDK_INT == Q || Build.VERSION.SDK_INT > Q && !Environment.isExternalStorageManager())) {
            privateDirPath(dirType, isExternal)
        } else {
            if (isExternal && canUseExternal(isReadOnly)) {
                if (dirType.isNotEmpty()) {
                    Environment.getExternalStoragePublicDirectory(dirType)
                } else {
                    Environment.getExternalStorageDirectory()
                }
            } else {
                privateInternalDirPath(dirType)
            }
        }

    private fun canUseExternal(isReadOnly: Boolean): Boolean =
        isExternalStorageMountedAndWritable() || isReadOnly && isExternalStorageMountedReadOnly()
}