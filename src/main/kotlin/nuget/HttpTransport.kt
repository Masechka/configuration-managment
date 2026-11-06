package nuget

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private const val TIMEOUT_SECONDS = 30L
private const val HTTP_OK = 200

/** Получает текст по HTTP средствами JDK с ограниченным временем ожидания. */
class HttpTransport {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
        .followRedirects(HttpClient.Redirect.NORMAL).build()

    /** Возвращает UTF-8 тело успешного ответа; статус ошибки не скрывается. */
    fun get(uri: URI): String {
        val request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .header("User-Agent", "nuget-graph/1.0").GET().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() == HTTP_OK) {
            "HTTP ${response.statusCode()}: $uri"
        }
        return response.body()
    }
}
