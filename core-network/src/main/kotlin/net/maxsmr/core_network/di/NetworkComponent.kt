package net.maxsmr.core_network.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.maxsmr.core_network.HostManager

lateinit var networkComponent: NetworkComponent

interface NetworkComponent {

    fun gson(): Gson

    fun gsonBuilder(): GsonBuilder

    fun hostManager(): HostManager
}