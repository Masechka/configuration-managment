package nuget

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Проверяет протокол NuGet на настоящем локальном HTTP-сервере без интернета. */
class NugetRepositoryTest {
    /** Service index, выбор стабильной версии и группы framework разбираются вручную. */
    @Test fun discoversDependencies() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val base = "http://127.0.0.1:${server.address.port}"
        val paths = mutableListOf<String>()
        val replies = mapOf(
            "/index.json" to """{"resources":[
                {"@type":"PackageBaseAddress/3.0.0","@id":"$base/flat/"}]}""",
            "/flat/demo/index.json" to """{"versions":["2.0.0","10.0.0","11.0.0-beta"]}""",
            "/flat/demo/10.0.0/demo.nuspec" to """<package xmlns="urn:nuget"><metadata>
                <id>Demo</id><version>10.0.0</version><dependencies>
                <group targetFramework="net8.0"><dependency id="Child" version="[1.0,2.0)"/>
                </group><group targetFramework="net9.0"><dependency id="Other" version="2.0"/>
                </group></dependencies></metadata></package>"""
        )
        server.createContext("/") { exchange ->
            paths += exchange.requestURI.path
            val body = replies[exchange.requestURI.path]?.toByteArray()
            exchange.sendResponseHeaders(if (body == null) 404 else 200, (body?.size ?: 0).toLong())
            exchange.responseBody.use { if (body != null) it.write(body) }
        }
        server.start()
        try {
            val repository = NugetRepository("$base/index.json")
            assertEquals(listOf("Child", "Other"), repository.dependencies("Demo").map { it.name })
            assertEquals("[1.0,2.0)", repository.dependencies("DEMO").first().versionRange)
            assertEquals("net8.0", repository.dependencies("Demo").first().framework)
            assertEquals(3, paths.size)
            assertFailsWith<IllegalStateException> {
                HttpTransport().get(URI("$base/missing"))
            }
        } finally { server.stop(0) }
    }

    /** Манифест без зависимостей и плоская форма корректны, XXE запрещён. */
    @Test fun parsesManifestsSafely() {
        assertEquals(emptyList(), Nuspec.dependencies("<package><metadata/></package>"))
        val xml = """<package><metadata><dependencies>
            <dependency id="Child" version="1.0"/>
            </dependencies></metadata></package>"""
        assertEquals(listOf(Dependency("Child", "1.0", "")), Nuspec.dependencies(xml))
        assertFailsWith<IllegalArgumentException> { Nuspec.dependencies("<broken>") }
        assertFailsWith<IllegalArgumentException> {
            Nuspec.dependencies("""<!DOCTYPE package [<!ENTITY x SYSTEM "file:///etc/passwd">]>
                <package><metadata>&x;</metadata></package>""")
        }
        assertFailsWith<IllegalArgumentException> { Nuspec.dependencies("<html/>") }
    }

    /** NuGet service index может находиться по URL без расширения .json. */
    @Test fun discoversIndexWithoutExtension() {
        val repository = NugetRepository("https://example.com/v3") { uri ->
            when (uri.path) {
                "/v3" -> """{"resources":[{"@type":"PackageBaseAddress/3.0.0",
                    "@id":"https://example.com/flat/"}]}"""
                "/flat/demo/index.json" -> """{"versions":["1.0.0"]}"""
                "/flat/demo/1.0.0/demo.nuspec" -> "<package><metadata/></package>"
                else -> throw IllegalArgumentException("Unexpected URI: $uri")
            }
        }
        assertEquals(emptyList(), repository.dependencies("Demo"))
    }

    /** Ошибки протокола не превращаются в отсутствие зависимостей. */
    @Test fun rejectsInvalidMetadata() {
        assertFailsWith<IllegalArgumentException> {
            NugetRepository("https://example.com/index.json") { "{}" }.dependencies("Demo")
        }
        val source = """{"resources":[
            {"@type":"PackageBaseAddress/3.0.0","@id":"https://example.com/flat/"}]}"""
        val repository = NugetRepository("https://example.com/index.json") { uri: URI ->
            if (uri.path == "/index.json") source else """{"versions":["1.0.0-beta"]}"""
        }
        assertFailsWith<IllegalArgumentException> { repository.dependencies("Demo") }
    }
}
