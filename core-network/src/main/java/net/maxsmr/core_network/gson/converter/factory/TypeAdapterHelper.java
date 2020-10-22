package net.maxsmr.core_network.gson.converter.factory;

import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;

import io.reactivex.Completable;

public class TypeAdapterHelper {

    private static final String LOG_TAG = "TypeAdapterHelper";

    /**
     * Парсинг элементов с учётом указания Completable в кач-ве результата;
     * применяется на первом зарегистрированном {@link TypeAdapterFactory}
     * @param shouldConsumeDocument дочитать документ до конца, если подставленный тип оказался Completable
     * @param reader из изначального TypeAdapterFactory
     */
    @SuppressWarnings("unchecked")
    public static <T> T parseElement(
            JsonReader reader,
            JsonElement jsonElement,
            TypeToken<T> type,
            TypeAdapter<T> delegate,
            boolean shouldConsumeDocument
    ) {
        if (Completable.class.isAssignableFrom(type.getRawType())) {
            if (shouldConsumeDocument) {
                consumeDocument(reader);
            }
            return (T) Completable.complete();
        } else {
            return delegate.fromJsonTree(jsonElement);
        }
    }

    /**
     * Дочитать документ до конца
     * @param reader из изначального TypeAdapterFactory
     */
    private static void consumeDocument(JsonReader reader) {
        JsonToken token = null;
        while (token != JsonToken.END_DOCUMENT) {
            try {
                token = reader.peek();
                if (reader.hasNext()) {
                    reader.skipValue();
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "An IOException occurred during read from reader " + reader + ": " + e);
            }
        }
    }
}