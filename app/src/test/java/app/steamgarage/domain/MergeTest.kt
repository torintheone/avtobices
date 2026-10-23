package app.steamgarage.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MergeTest {

    private data class Rec(
        override val id: String,
        override val updatedAt: Long,
        override val updatedBy: String,
        override val deleted: Boolean = false,
        val value: String = "",
    ) : Syncable

    @Test
    fun newerVersionWins() {
        val local = Rec("a", 100, "dev-1", value = "старое")
        val incoming = Rec("a", 200, "dev-2", value = "новое")
        assertEquals("новое", Merge.winner(local, incoming).value)
        assertEquals("новое", Merge.winner(incoming, local).value)
    }

    @Test
    fun tieIsResolvedByUpdatedBy() {
        val a = Rec("a", 100, "dev-a", value = "A")
        val b = Rec("a", 100, "dev-b", value = "B")
        assertEquals("B", Merge.winner(a, b).value)
        assertEquals("B", Merge.winner(b, a).value)
    }

    @Test
    fun tombstoneDoesNotResurrect() {
        val alive = Rec("a", 100, "dev-1", value = "жива")
        val tombstone = Rec("a", 150, "dev-2", deleted = true)
        // Устройство, не знавшее об удалении, присылает старую живую версию — надгробие остаётся.
        val merged = Merge.winner(tombstone, alive)
        assertTrue(merged.deleted)
        assertTrue(Merge.mergeAll(listOf(tombstone, alive)).getValue("a").deleted)
        assertTrue(Merge.mergeAll(listOf(alive, tombstone)).getValue("a").deleted)
    }

    @Test
    fun arrivalOrderDoesNotMatter() {
        val records = listOf(
            Rec("a", 100, "dev-1", value = "a1"),
            Rec("a", 300, "dev-2", value = "a3"),
            Rec("a", 200, "dev-3", value = "a2"),
            Rec("b", 50, "dev-1", value = "b1"),
            Rec("b", 50, "dev-9", value = "b9"),
            Rec("c", 10, "dev-1", deleted = true),
            Rec("c", 5, "dev-2", value = "c-old"),
        )
        val expected = Merge.mergeAll(records)
        val random = java.util.Random(42)
        repeat(200) {
            val shuffled = records.shuffled(random)
            assertEquals(expected, Merge.mergeAll(shuffled))
        }
        assertEquals("a3", expected.getValue("a").value)
        assertEquals("b9", expected.getValue("b").value)
        assertTrue(expected.getValue("c").deleted)
    }
}
