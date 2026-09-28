package app.sprout.habits

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** [Strings] backed by the English `values/strings.xml`, so tests check the real text. */
object TestStrings : Strings {
    private val strings = mutableMapOf<String, String>()
    private val plurals = mutableMapOf<String, Map<String, String>>()

    init {
        val file = listOf("src/main/res/values/strings.xml", "app/src/main/res/values/strings.xml").map(::File).first { it.exists() }
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
        val nodes = root.childNodes
        for (i in 0 until nodes.length) {
            val e = nodes.item(i) as? Element ?: continue
            when (e.tagName) {
                "string" -> strings[e.getAttribute("name")] = unescape(e.textContent)
                "plurals" -> {
                    val items = e.getElementsByTagName("item")
                    plurals[e.getAttribute("name")] = (0 until items.length).associate {
                        val item = items.item(it) as Element
                        item.getAttribute("quantity") to unescape(item.textContent)
                    }
                }
            }
        }
    }

    private fun unescape(s: String) = s.replace("\\'", "'").replace("\\\"", "\"").replace("\\n", "\n").replace("\\u00A0", "\u00A0").removeSurrounding("\"")

    private fun name(id: Int, type: Class<*>) = type.fields.first { it.getInt(null) == id }.name

    override fun invoke(id: Int, vararg args: Any): String = strings.getValue(name(id, R.string::class.java)).format(*args)

    override fun plural(id: Int, count: Int, vararg args: Any): String {
        val forms = plurals.getValue(name(id, R.plurals::class.java))
        return (if (count == 1) forms["one"] else forms["other"])!!.format(*args)
    }
}
