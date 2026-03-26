package pg.geobingo.one.data

import kotlin.test.*

class PhotoCacheTest {

    @Test
    fun putAndGet_returnsBytes() {
        val cache = PhotoCache(maxEntries = 5)
        val bytes = byteArrayOf(1, 2, 3)
        cache.put("p1", "cat1", bytes)

        val result = cache.get("p1", "cat1")
        assertNotNull(result)
        assertContentEquals(bytes, result)
    }

    @Test
    fun get_nonExisting_returnsNull() {
        val cache = PhotoCache(maxEntries = 5)
        assertNull(cache.get("p1", "cat1"))
    }

    @Test
    fun contains_worksCorrectly() {
        val cache = PhotoCache(maxEntries = 5)
        assertFalse(cache.contains("p1", "cat1"))

        cache.put("p1", "cat1", byteArrayOf(1))
        assertTrue(cache.contains("p1", "cat1"))
        assertFalse(cache.contains("p1", "cat2"))
        assertFalse(cache.contains("p2", "cat1"))
    }

    @Test
    fun eviction_removesOldest() {
        val cache = PhotoCache(maxEntries = 3)
        cache.put("p1", "a", byteArrayOf(1))
        cache.put("p1", "b", byteArrayOf(2))
        cache.put("p1", "c", byteArrayOf(3))

        assertEquals(3, cache.size)

        cache.put("p1", "d", byteArrayOf(4))
        assertEquals(3, cache.size)
        assertFalse(cache.contains("p1", "a")) // oldest evicted
        assertTrue(cache.contains("p1", "b"))
        assertTrue(cache.contains("p1", "c"))
        assertTrue(cache.contains("p1", "d"))
    }

    @Test
    fun put_overwrite_doesNotIncreaseSizeOrEvict() {
        val cache = PhotoCache(maxEntries = 3)
        cache.put("p1", "a", byteArrayOf(1))
        cache.put("p1", "b", byteArrayOf(2))
        cache.put("p1", "c", byteArrayOf(3))

        // Overwrite existing entry
        cache.put("p1", "b", byteArrayOf(20))
        assertEquals(3, cache.size)
        assertContentEquals(byteArrayOf(20), cache.get("p1", "b"))
        // All entries should still exist
        assertTrue(cache.contains("p1", "a"))
        assertTrue(cache.contains("p1", "c"))
    }

    @Test
    fun clear_removesEverything() {
        val cache = PhotoCache(maxEntries = 5)
        cache.put("p1", "a", byteArrayOf(1))
        cache.put("p1", "b", byteArrayOf(2))

        cache.clear()

        assertEquals(0, cache.size)
        assertNull(cache.get("p1", "a"))
        assertNull(cache.get("p1", "b"))
    }

    @Test
    fun multiplePlayersAndCategories() {
        val cache = PhotoCache(maxEntries = 10)
        cache.put("p1", "cat1", byteArrayOf(1))
        cache.put("p2", "cat1", byteArrayOf(2))
        cache.put("p1", "cat2", byteArrayOf(3))

        assertEquals(3, cache.size)
        assertContentEquals(byteArrayOf(1), cache.get("p1", "cat1"))
        assertContentEquals(byteArrayOf(2), cache.get("p2", "cat1"))
        assertContentEquals(byteArrayOf(3), cache.get("p1", "cat2"))
    }
}
