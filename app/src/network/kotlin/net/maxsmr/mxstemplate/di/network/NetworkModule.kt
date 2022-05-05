package net.maxsmr.mxstemplate.di.network

import android.app.DownloadManager
import android.content.Context
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import net.maxsmr.core_common.arch.StringsProvider
import net.maxsmr.core_network.HostManager
import net.maxsmr.core_network.calladapter.CallAdapterFactory
import net.maxsmr.core_network.connection.ConnectionProvider
import net.maxsmr.core_network.error.handler.error.http.BaseHttpErrorHandler
import net.maxsmr.core_network.error.handler.error.http.LogHttpErrorHandler
import net.maxsmr.core_network.model.request.api.IApiMapper
import net.maxsmr.mxstemplate.di.PerApplication
import net.maxsmr.mxstemplate.di.DI_NAME_HOST_MANAGER
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import javax.inject.Named

const val DI_NAME_ERROR_HANDLER_CALL_ADAPTER = "error_handler_call_adapter"

// не должно быть internal у тех зависимостей, которые используются в других gradle-модулях!
@Module
class NetworkModule {

    @Provides
    @PerApplication
    fun provideRetrofit(
        @Named(DI_NAME_OK_HTTP_MAIN)
            okHttpClient: OkHttpClient,
        callAdapterFactory: CallAdapterFactory,
        gson: Gson,
        @Named(DI_NAME_HOST_MANAGER)
            hostManager: HostManager
    ): Retrofit {
        return Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(hostManager.getUrl())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(callAdapterFactory)
                .build()
    }

    @Provides
    @PerApplication
    fun provideDownloadManager(context: Context): DownloadManager {
        return context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }

    @Provides
    @PerApplication
    @Named(DI_NAME_ERROR_HANDLER_CALL_ADAPTER)
    fun provideErrorHandler(@Named(DI_NAME_API_MAPPER) apiMapper: IApiMapper): BaseHttpErrorHandler =
            LogHttpErrorHandler(apiMapper.map)

    @Provides
    @PerApplication
    fun provideCallAdapterFactory(
        @Named(DI_NAME_ERROR_HANDLER_CALL_ADAPTER)
            errorHandler: BaseHttpErrorHandler?,
        gson: Gson
    ): CallAdapterFactory {
        return CallAdapterFactory(errorHandler, gson)
    }

    @Provides
    @PerApplication
    fun provideConnectionProvider(context: Context): ConnectionProvider = ConnectionProvider(context)
}