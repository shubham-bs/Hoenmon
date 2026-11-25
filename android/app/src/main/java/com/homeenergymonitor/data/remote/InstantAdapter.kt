package com.homeenergymonitor.data.remote

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.Instant

object InstantAdapter : JsonSerializer<Instant>, JsonDeserializer<Instant> {
    override fun serialize(
        src: Instant,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement = JsonPrimitive(src.toString())

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Instant {
        val value = json.asString
        return try {
            Instant.parse(value)
        } catch (error: Exception) {
            throw JsonParseException("Invalid ISO-8601 instant: $value", error)
        }
    }
}

fun buildGson(): Gson = GsonBuilder()
    .registerTypeAdapter(Instant::class.java, InstantAdapter)
    .create()
