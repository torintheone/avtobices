package app.steamgarage.domain

/** Поля синхронизации, общие для всех сущностей (раздел «Данные» SPEC.md). */
interface Syncable {
    val id: String
    val updatedAt: Long
    val updatedBy: String
    val deleted: Boolean
}

/**
 * Правило слияния: побеждает версия с бо́льшим updatedAt, при равенстве — с бо́льшим updatedBy.
 * Удалённая запись («надгробие») сливается так же, поэтому удаление не воскресает.
 */
object Merge {

    /** true — входящая версия должна заменить локальную. */
    fun incomingWins(local: Syncable, incoming: Syncable): Boolean =
        incoming.updatedAt > local.updatedAt ||
            (incoming.updatedAt == local.updatedAt && incoming.updatedBy > local.updatedBy)

    fun <T : Syncable> winner(local: T?, incoming: T): T =
        if (local == null || incomingWins(local, incoming)) incoming else local

    /** Слить записи, пришедшие в любом порядке: результат от порядка не зависит. */
    fun <T : Syncable> mergeAll(records: Iterable<T>): Map<String, T> {
        val result = LinkedHashMap<String, T>()
        for (record in records) result[record.id] = winner(result[record.id], record)
        return result
    }
}
