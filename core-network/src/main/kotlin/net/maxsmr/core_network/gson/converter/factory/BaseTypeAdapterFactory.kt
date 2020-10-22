package net.maxsmr.core_network.gson.converter.factory

import com.google.gson.TypeAdapterFactory

abstract class BaseTypeAdapterFactory(
        protected val shouldConsumeDocument: Boolean
): TypeAdapterFactory