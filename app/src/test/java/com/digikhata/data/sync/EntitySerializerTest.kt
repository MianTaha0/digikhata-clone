package com.digikhata.data.sync

import com.digikhata.data.entity.Client
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitySerializerTest {

    @Test
    fun `client entity round-trips to map with expected keys`() {
        val client = Client(
            id = 12L,
            businessId = 3L,
            type = 0,
            name = "Ali",
            phone = "03001234567",
            creditLimit = 5000.0,
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_000_000L
        )
        val map = EntitySerializer.toMap(client)

        // Gson decodes numeric JSON into Double by default, so assert via numeric casts.
        assertEquals(12.0, (map["id"] as Number).toDouble(), 0.0)
        assertEquals(3.0, (map["businessId"] as Number).toDouble(), 0.0)
        assertEquals(0.0, (map["type"] as Number).toDouble(), 0.0)
        assertEquals("Ali", map["name"])
        assertEquals("03001234567", map["phone"])
        assertEquals(5000.0, (map["creditLimit"] as Number).toDouble(), 0.0)
        assertTrue(map.containsKey("isArchived"))
        assertTrue(map.containsKey("isPinned"))
    }

    @Test
    fun `empty json blob decodes to empty map`() {
        assertEquals(emptyMap<String, Any?>(), EntitySerializer.toMap(""))
    }

    @Test
    fun `json string survives round-trip`() {
        val payload = mapOf("a" to 1, "b" to "two")
        val json = EntitySerializer.toJson(payload)
        val back = EntitySerializer.toMap(json)
        assertEquals("two", back["b"])
    }
}
