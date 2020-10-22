/*
  Copyright (c) 2018-present, SurfStudio LLC.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package net.maxsmr.core_network.gson.converter.factory

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import net.maxsmr.core_network.gson.converter.factory.TypeAdapterHelper.parseElement
import java.io.IOException

/**
 * Адаптер для [Gson] конвертирующий пустые объекты в null
 */
class EmptyToNullTypeAdapterFactory(shouldConsumeDocument: Boolean) : BaseTypeAdapterFactory(shouldConsumeDocument) {

    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T> {
        val delegate = gson.getDelegateAdapter(this, type)
        val elementAdapter = gson.getAdapter(JsonElement::class.java)
        return EmptyCheckTypeAdapter(delegate, elementAdapter, type, shouldConsumeDocument).nullSafe()
    }

    class EmptyCheckTypeAdapter<T>(
            private val delegate: TypeAdapter<T>,
            private val elementAdapter: TypeAdapter<JsonElement>,
            private val type: TypeToken<T>,
            private val shouldConsumeDocument: Boolean
    ) : TypeAdapter<T>() {

        @Throws(IOException::class)
        override fun write(out: JsonWriter, value: T) {
            this.delegate.write(out, value)
        }

        @Throws(IOException::class)
        override fun read(`in`: JsonReader): T? {
            val element = elementAdapter.read(`in`)

            if (element is JsonObject) {
                if (element.entrySet().isEmpty())
                    return null
            }
            return parseElement(`in`, element, type, delegate, shouldConsumeDocument)
        }
    }
}