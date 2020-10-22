package net.maxsmr.core_network.gson.converter.factory

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.IOException

abstract class BaseCustomizedTypeAdapterFactory<C>(
        private val customizedClass: Class<C>,
        shouldConsumeDocument: Boolean
) : BaseTypeAdapterFactory(shouldConsumeDocument) {

    // we use a runtime check to guarantee that 'C' and 'T' are equal
    @Suppress("UNCHECKED_CAST")
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        return if (customizedClass.isAssignableFrom(type.rawType)) customizeMyClassAdapter(gson, type as TypeToken<C>) as TypeAdapter<T> else null
    }

    private fun customizeMyClassAdapter(gson: Gson, type: TypeToken<C>): TypeAdapter<C> {
        val delegate: TypeAdapter<C> = gson.getDelegateAdapter(this, type)
        val elementAdapter = gson.getAdapter(JsonElement::class.java)
        return object : TypeAdapter<C>() {
            @Throws(IOException::class)
            override fun write(out: JsonWriter?, value: C) {
                val tree = delegate.toJsonTree(value)
                elementAdapter.write(out, onWrite(value, tree))
            }

            @Throws(IOException::class)
            override fun read(`in`: JsonReader?): C {
                val tree = elementAdapter.read(`in`)
                afterRead(tree)
                return TypeAdapterHelper.parseElement(`in`, tree, type, delegate, shouldConsumeDocument)
            }
        }
    }

    /**
     * Override this to muck with `toSerialize` before it is written to
     * the outgoing JSON stream.
     */
    protected open fun onWrite(source: C, toSerialize: JsonElement): JsonElement {
        return toSerialize
    }

    /**
     * Override this to muck with `deserialized` before it parsed into
     * the application type.
     */
    protected open fun afterRead(deserialized: JsonElement) {}
}