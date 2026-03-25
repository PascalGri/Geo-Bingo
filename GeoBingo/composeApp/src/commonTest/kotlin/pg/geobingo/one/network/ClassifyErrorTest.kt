package pg.geobingo.one.network

import kotlin.test.Test
import kotlin.test.assertEquals

class ClassifyErrorTest {

    @Test
    fun duplicateByPostgresCode() {
        val e = Exception("ERROR: duplicate key value violates unique constraint (23505)")
        assertEquals(ErrorType.DUPLICATE, classifyError(e))
    }

    @Test
    fun duplicateByMessage() {
        val e = Exception("A unique constraint was violated")
        assertEquals(ErrorType.DUPLICATE, classifyError(e))
    }

    @Test
    fun networkByTimeout() {
        val e = Exception("Connection timeout after 30s")
        assertEquals(ErrorType.NETWORK, classifyError(e))
    }

    @Test
    fun networkByConnect() {
        val e = Exception("Failed to connect to host")
        assertEquals(ErrorType.NETWORK, classifyError(e))
    }

    @Test
    fun authBy401() {
        val e = Exception("HTTP 401 Unauthorized")
        assertEquals(ErrorType.AUTH, classifyError(e))
    }

    @Test
    fun notFoundBy404() {
        val e = Exception("404 not found")
        assertEquals(ErrorType.NOT_FOUND, classifyError(e))
    }

    @Test
    fun unknownForGenericError() {
        val e = Exception("Something unexpected happened")
        assertEquals(ErrorType.UNKNOWN, classifyError(e))
    }

    @Test
    fun nullMessageDefaultsToUnknown() {
        val e = Exception(null as String?)
        assertEquals(ErrorType.UNKNOWN, classifyError(e))
    }

    @Test
    fun caseInsensitiveDuplicate() {
        val e = Exception("DUPLICATE key violation")
        assertEquals(ErrorType.DUPLICATE, classifyError(e))
    }
}
