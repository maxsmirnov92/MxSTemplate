package net.maxsmr.mxstemplate.di.network

import android.content.Context
import android.util.Log
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import dagger.Module
import dagger.Provides
import net.maxsmr.core_network.interceptor.FailRetryInterceptor
import net.maxsmr.core_network.interceptor.RemoteLoggingInterceptor
import net.maxsmr.core_network.interceptor.ServiceInterceptor
import net.maxsmr.core_network.model.request.api.IApiMapper
import net.maxsmr.mxstemplate.BuildConfig
import net.maxsmr.mxstemplate.di.PerApplication
import net.maxsmr.mxstemplate.di.app.DI_NAME_VERSION_NAME
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Named

const val DI_NAME_INTERCEPTOR_SERVICE = "interceptor_service"
const val DI_NAME_INTERCEPTOR_REMOTE_LOGGING = "interceptor_remote_logging"
const val DI_NAME_INTERCEPTOR_FORCE_CACHE = "interceptor_force_cache"
const val DI_NAME_INTERCEPTOR_FAIL_RETRY = "interceptor_fail_retry"
const val DI_NAME_INTERCEPTOR_FAIL_RETRY_LOG = "interceptor_fail_retry_log"
const val DI_NAME_API_MAPPER = "api_mapper"
const val DI_NAME_OK_HTTP_MAIN = "ok_http_main"
const val DI_NAME_OK_HTTP_DOWNLOADER = "ok_http_downloader"
const val DI_NAME_OK_HTTP_LOGGING = "ok_http_logging"
const val DI_NAME_OK_HTTP_PICASSO = "ok_http_picasso"

private const val NETWORK_TIMEOUT = 30L //sec
private const val MAX_RETRIES_ANY_REQUEST = 10
private const val MAX_RETRIES_LOG_REQUEST = 1

private const val HTTP_LOG_TAG = "OkHttp"

// Размер дискового кеша пикассо = 250 Мб
private const val PICASSO_DISK_CACHE_SIZE = (1024 * 1024 * 250).toLong()
private const val PICASSO_CACHE = "picasso-cache"

@Module
class OkHttpModule {

    @Provides
    @PerApplication
    @Named(DI_NAME_INTERCEPTOR_SERVICE)
    fun provideServiceInterceptor(
        @Named(DI_NAME_VERSION_NAME)
        versionName: String,
        @Named(DI_NAME_API_MAPPER)
        apiMap: IApiMapper
    ): Interceptor =
        ServiceInterceptor(versionName, apiMap.map, false)

    @Provides
    @Named(DI_NAME_INTERCEPTOR_REMOTE_LOGGING)
    @PerApplication
    fun provideRemoteLoggingInterceptor(@Named(DI_NAME_API_MAPPER) apiMapper: IApiMapper): Interceptor =
        RemoteLoggingInterceptor(apiMapper.map, true)

    @Provides
    @PerApplication
    @Named(DI_NAME_INTERCEPTOR_FAIL_RETRY)
    fun provideFailRetryInterceptor(): Interceptor = FailRetryInterceptor(MAX_RETRIES_ANY_REQUEST)

    @Provides
    @PerApplication
    @Named(DI_NAME_INTERCEPTOR_FAIL_RETRY_LOG)
    fun provideFailRetryLogInterceptor(): Interceptor =
        FailRetryInterceptor(MAX_RETRIES_LOG_REQUEST)

    @Provides
    @PerApplication
    @Named(DI_NAME_INTERCEPTOR_FORCE_CACHE)
    fun provideForceCacheInterceptor(context: Context): Interceptor {
        return Interceptor { chain ->
            val builder = chain.request().newBuilder()
            builder.cacheControl(CacheControl.FORCE_CACHE)
            chain.proceed(builder.build())
        }
    }

    @Provides
    @PerApplication
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            Log.d("HttpLoggingInterceptor", "$HTTP_LOG_TAG $message")
        }.apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.BASIC
        }
    }

    @Provides
    @PerApplication
    @Named(DI_NAME_OK_HTTP_MAIN)
    fun provideMainOkHttpClient(
        @Named(DI_NAME_INTERCEPTOR_SERVICE) serviceInterceptor: Interceptor,
        @Named(DI_NAME_INTERCEPTOR_REMOTE_LOGGING) remoteLoggingInterceptor: Interceptor,
        @Named(DI_NAME_INTERCEPTOR_FAIL_RETRY) failRetryInterceptor: Interceptor,
        httpLoggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder().apply {

            connectTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
            readTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
            writeTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)

            addInterceptor(serviceInterceptor)
            addInterceptor(remoteLoggingInterceptor)
            addInterceptor(httpLoggingInterceptor)
            addInterceptor(failRetryInterceptor)
        }.build()
    }

    /**
     * @return [OkHttpClient] для [LogSendIntentService]
     */
    @Provides
    @PerApplication
    @Named(DI_NAME_OK_HTTP_DOWNLOADER)
    fun provideDownloaderOkHttpClient(
        httpLoggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder().apply {
            connectTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
            readTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
            writeTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
            addInterceptor(httpLoggingInterceptor)
        }.build()
    }

    /**
     * @return [OkHttpClient] для [LogSendIntentService]
     */
    @Provides
    @PerApplication
    @Named(DI_NAME_OK_HTTP_LOGGING)
    fun provideLoggingOkHttpClient(@Named(DI_NAME_INTERCEPTOR_FAIL_RETRY_LOG) failRetryInterceptor: Interceptor): OkHttpClient {
        return OkHttpClient.Builder().apply {
            connectTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
            readTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
            writeTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
            addInterceptor(failRetryInterceptor)
        }.build()
    }

    @Provides
    @PerApplication
    @Named(DI_NAME_OK_HTTP_PICASSO)
    fun providePicassoOkHttpClient(
        context: Context,
        @Named(DI_NAME_INTERCEPTOR_FORCE_CACHE) forceCacheInterceptor: Interceptor,
        httpLoggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        // Каталог кэша Picasso
        val cacheDir = File(context.cacheDir, PICASSO_CACHE)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return OkHttpClient.Builder().apply {
            connectTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
            readTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
            writeTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)

            cache(Cache(cacheDir, PICASSO_DISK_CACHE_SIZE))
            addInterceptor(forceCacheInterceptor)
            addInterceptor(httpLoggingInterceptor)
        }.build()
    }

    @Provides
    @PerApplication
    fun providePicasso(
        context: Context,
        @Named(DI_NAME_OK_HTTP_PICASSO) okHttpClient: OkHttpClient
    ): Picasso = Picasso.Builder(context)
        .downloader(OkHttp3Downloader(okHttpClient))
        .build()
}
