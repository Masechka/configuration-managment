package nuget

import java.net.URI
import java.util.Locale

private const val EQUAL_VERSIONS = 0

/** Читает NuGet V3 service index, список версий и nuspec через заданный URL. */
class NugetRepository(
    private val source: String,
    private val fetch: (URI) -> String = HttpTransport()::get
) : Repository {
    private val cache = mutableMapOf<String, List<Dependency>>()
    private val base: URI by lazy { discover() }

    /** Кеширует результат по имени без учёта регистра NuGet. */
    override fun dependencies(packageName: String): List<Dependency> {
        val id = packageName.lowercase(Locale.ROOT)
        require(Regex("[a-z0-9_][a-z0-9_.-]*").matches(id)) { "Некорректное имя NuGet." }
        return cache.getOrPut(id) { load(id) }
    }

    /** Находит ресурс PackageBaseAddress в service index либо использует flat-container URL. */
    private fun discover(): URI {
        val uri = URI(source)
        if (uri.path.endsWith('/')) return directory(source)
        val resources = Json.parse(fetch(uri)).jsonObject()["resources"].jsonArray()
        val resource = resources.map { it.jsonObject() }.firstOrNull {
            it["@type"] == "PackageBaseAddress/3.0.0"
        }
        requireNotNull(resource) { "В service index нет PackageBaseAddress/3.0.0." }
        return directory(resource.string("@id"))
    }

    /** Получает последнюю стабильную версию и объявленные в ней прямые зависимости. */
    private fun load(id: String): List<Dependency> {
        val versions = Json.parse(fetch(base.resolve("$id/index.json")))
            .jsonObject()["versions"].jsonArray().map {
                requireNotNull(it as? String) { "Некорректная версия NuGet." }
            }
        val stable = versions.filter { Regex("[0-9]+(\\.[0-9]+){1,3}").matches(it) }
        val version = requireNotNull(stable.maxWithOrNull(::compareVersions)) {
            "У пакета $id нет стабильной версии."
        }
        return Nuspec.dependencies(fetch(base.resolve("$id/$version/$id.nuspec")))
    }

    /** Сравнивает числовые компоненты, а не лексикографический порядок строк версий. */
    private fun compareVersions(left: String, right: String): Int {
        val a = left.split('.').map { it.toBigInteger() }
        val b = right.split('.').map { it.toBigInteger() }
        for (index in 0 until maxOf(a.size, b.size)) {
            val comparison = (a.getOrNull(index) ?: java.math.BigInteger.ZERO)
                .compareTo(b.getOrNull(index) ?: java.math.BigInteger.ZERO)
            if (comparison != EQUAL_VERSIONS) return comparison
        }
        return 0
    }

    /** Нормализует адрес каталога, не допуская непригодный протокол. */
    private fun directory(value: String): URI {
        val uri = URI(if (value.endsWith('/')) value else "$value/")
        require(uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()) {
            "Некорректный PackageBaseAddress."
        }
        require(uri.query == null && uri.fragment == null && uri.userInfo == null) {
            "PackageBaseAddress должен быть URL каталога."
        }
        return uri
    }
}
