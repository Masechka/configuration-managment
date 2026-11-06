package nuget

/** Разбирает JSON-ответы NuGet без внешних библиотек. */
object Json {
    /** Требует ровно одно значение и отсутствие постороннего текста. */
    fun parse(text: String): Any? = JsonReader(text).document()
}

/** Проверяет ожидаемый объект JSON. */
fun Any?.jsonObject(): Map<*, *> =
    requireNotNull(this as? Map<*, *>) { "Ожидался JSON-объект." }

/** Проверяет ожидаемый массив JSON. */
fun Any?.jsonArray(): List<*> =
    requireNotNull(this as? List<*>) { "Ожидался JSON-массив." }

/** Проверяет обязательное строковое поле JSON. */
fun Map<*, *>.string(key: String): String =
    requireNotNull(this[key] as? String) { "Ожидалась строка JSON: $key" }
