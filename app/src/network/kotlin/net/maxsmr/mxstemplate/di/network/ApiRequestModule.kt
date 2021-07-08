package net.maxsmr.mxstemplate.di.network

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import net.maxsmr.commonutils.model.gson.exclusion.FieldsAnnotationExclusionStrategy
import net.maxsmr.core_common.arch.ErrorHandler
import net.maxsmr.core_network.error.handler.response.BaseResponseHandler
import net.maxsmr.core_network.gson.converter.factory.ApiRequestTypeAdapterFactory
import net.maxsmr.core_network.gson.converter.factory.EmptyToNullTypeAdapterFactory
import net.maxsmr.core_network.gson.converter.factory.ResponseTypeAdapterFactory
import net.maxsmr.core_network.gson.converter.factory.SafeConverterFactory
import net.maxsmr.core_network.model.request.api.IApiMapper
import net.maxsmr.mxstemplate.api.ApiMapper
import net.maxsmr.mxstemplate.api.handler.StandardResponseHandler
import net.maxsmr.mxstemplate.di.PerApplication
import javax.inject.Named

const val DI_NAME_RESPONSE_HANDLER_STANDARD = "response_handler_standard"

@Module
class ApiRequestModule {

    @Provides
    @PerApplication
    @Named(DI_NAME_API_MAPPER)
    fun provideApiMapper(): IApiMapper = ApiMapper

    @Provides
    @PerApplication
    @Named(DI_NAME_RESPONSE_HANDLER_STANDARD)
    fun provideStandardResponseHandler(context: Context, errorHandler: ErrorHandler): BaseResponseHandler = StandardResponseHandler(context, errorHandler)

    @Provides
    @PerApplication
    fun provideGsonBuilder(@Named(DI_NAME_API_MAPPER) apiMapper: IApiMapper): GsonBuilder {
        with (GsonBuilder()) {
            // TODO регистрировать остальные адаптеры
            registerTypeAdapterFactory(ResponseTypeAdapterFactory(SafeConverterFactory(emptyMap()), true))
            registerTypeAdapterFactory(EmptyToNullTypeAdapterFactory(false))
            registerTypeAdapterFactory(ApiRequestTypeAdapterFactory(apiMapper, false))
//            registerTypeAdapter(GetResultRequest::class.java, GetResultRequestSerializer())
//            registerTypeAdapter(Profile::class.java, ProfileDeserializer())
            setExclusionStrategies(FieldsAnnotationExclusionStrategy())
            setLenient()
            serializeNulls()
            return this
        }
    }

    @Provides
    @PerApplication
    fun provideGson(builder: GsonBuilder): Gson {
        return builder.create()
    }

//    @Provides
//    @PerApplication
//    fun provideAsyncApi(retrofit: Retrofit) : AsyncApi {
//        return retrofit.create(AsyncApi::class.java)
//    }
}
