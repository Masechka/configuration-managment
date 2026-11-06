package nuget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Проверяет JSON service index без стороннего JSON-пакета. */
class JsonTest {
    /** Поддерживаются структуры JSON, escape-последовательности и Unicode. */
    @Test fun readsJson() {
        val value = Json.parse("""{"name":"A\u0042\n\"","values":[true,false,null,1.5e2]}""")
        assertEquals("AB\n\"", value.jsonObject()["name"])
        assertEquals(4, value.jsonObject()["values"].jsonArray().size)
    }

    /** Повреждённый JSON, дубликаты ключей и хвост документа отклоняются. */
    @Test fun rejectsInvalidJson() {
        listOf("\"\\u+001\"", "", "{", "[1,]", "{\"a\":1,\"a\":2}", "{}x", "\"\\x\"").forEach {
            assertFailsWith<IllegalArgumentException>(it) { Json.parse(it) }
        }
    }
}
