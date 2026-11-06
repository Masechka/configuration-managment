package nuget

import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import org.xml.sax.helpers.DefaultHandler

/** Извлекает зависимости из nuspec, объединяя объявленные framework-группы. */
object Nuspec {
    /** Разбирает XML без DTD, внешних сущностей, XInclude и внешних схем. */
    fun dependencies(xml: String): List<Dependency> {
        val document = try {
            val builder = factory().newDocumentBuilder()
            builder.setErrorHandler(object : DefaultHandler() {
                /** Прерывает разбор вместо печати XML-ошибки в stderr. */
                override fun fatalError(error: SAXParseException) { throw error }
            })
            builder.parse(InputSource(StringReader(xml)))
        } catch (error: Exception) {
            throw IllegalArgumentException("Некорректный nuspec: ${error.message}", error)
        }
        val root = document.documentElement
        require(root.localName == "package") { "В nuspec отсутствует package." }
        val metadata = root.children("metadata").singleOrNull()
        requireNotNull(metadata) { "В nuspec отсутствует единственный metadata." }
        return metadata.children("dependencies").flatMap(::readDependencies).distinct()
    }

    /** Создаёт защищённую конфигурацию стандартного XML-парсера. */
    private fun factory(): DocumentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        isXIncludeAware = false
        setExpandEntityReferences(false)
        setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
        setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
    }

    /** Поддерживает плоский список и группировку зависимостей по framework. */
    private fun readDependencies(element: Element): List<Dependency> {
        val flat = element.children("dependency").map { dependency(it, "") }
        val groups = element.children("group").flatMap { group ->
            val framework = group.getAttribute("targetFramework")
            group.children("dependency").map { dependency(it, framework) }
        }
        return flat + groups
    }

    /** Читает обязательное имя и необязательный диапазон версий зависимости. */
    private fun dependency(element: Element, framework: String): Dependency {
        val name = element.getAttribute("id")
        require(Regex("[A-Za-z0-9_][A-Za-z0-9_.-]*").matches(name)) {
            "Некорректный id зависимости в nuspec."
        }
        return Dependency(name, element.getAttribute("version"), framework)
    }

    /** Возвращает непосредственных дочерних XML-элементов с данным локальным именем. */
    private fun Element.children(name: String): List<Element> =
        (0 until childNodes.length).mapNotNull { childNodes.item(it) as? Element }
            .filter { it.localName == name }
}
