package net.maxsmr.mxstemplate.di.app

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import net.maxsmr.mxstemplate.db.AppDataBase
import net.maxsmr.mxstemplate.db.migrations
import net.maxsmr.mxstemplate.di.PerApplication

@Module
class DatabaseModule {

    @Provides
    @PerApplication
    fun database(context: Context): AppDataBase =
            Room.databaseBuilder(context, AppDataBase::class.java, "appRoomDataBase")
                    .addMigrations(*migrations)
                    .allowMainThreadQueries()
                    .build()

}