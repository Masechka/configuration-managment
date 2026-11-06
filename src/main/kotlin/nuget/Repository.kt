package nuget

/** Прямая зависимость, объявленный диапазон версий и исходная группа framework. */
data class Dependency(val name: String, val versionRange: String = "", val framework: String = "")

/** Источник прямых зависимостей; имя служит идентификатором узла графа. */
fun interface Repository {
    /** Возвращает прямые зависимости либо сообщает об ошибке получения данных. */
    fun dependencies(packageName: String): List<Dependency>
}
