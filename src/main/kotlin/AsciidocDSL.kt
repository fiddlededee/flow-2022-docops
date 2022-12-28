import org.asciidoctor.Asciidoctor
import org.asciidoctor.Options
import org.languagetool.JLanguageTool
import org.languagetool.language.Russian
import org.languagetool.rules.spelling.SpellingCheckRule
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.StringReader
import java.util.SortedMap
import java.util.SortedSet
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

enum class NodeType { Any, Para, List }
enum class ParaType { Normal, FirstList, OtherList }

object LangTools {
    private val langTool = JLanguageTool(Russian())
    var ruleTokenExceptions: Map<String, Set<String>> = mapOf()
    var ruleExceptions: Set<String> = setOf("")

    fun setSpellTokens(ignoreTokens: ArrayList<String>, acceptPhrases: ArrayList<String>) {
        langTool.allActiveRules.forEach { rule ->
            if (rule is SpellingCheckRule) {
                rule.addIgnoreTokens(ignoreTokens)
                rule.acceptPhrases(acceptPhrases)
            }
        }
    }

    init {
    }

    fun check(para: AdocDSLParagraph) {
        val text = para.toText()
        val errs = langTool.check(text).filterNot {
            (this.ruleTokenExceptions[it.rule.id]?.contains(text.substring(it.fromPos, it.toPos)) ?: false) or
                    ((this.ruleExceptions + para.ruleExeptions).contains(it.rule.id))
        }

        if (errs.isNotEmpty()) {
            var errorMessage = "Spell failed for:\n$text\n"
            errs.forEachIndexed { index, it ->
                errorMessage += "[${index + 1}] ${it.message}, ${it.rule.id} (${it.fromPos}:${it.toPos} " +
                        "- ${text.substring(it.fromPos, it.toPos)})\n"
            }
            throw Exception(
                errorMessage.split("\n").map { it.chunked(120) }.flatten().joinToString("\n")
            )
        }
    }
}

object AsciidocValidatorFactory {
    private val factory: Asciidoctor = Asciidoctor.Factory.create()

    init {
        factory.requireLibrary(this::class.java.getResource("/asciiml.rb") !!.toString())
    }

    fun getXML(string: String): String {
        return factory.convert(string, Options.builder().backend("asciiml").sourcemap(true).build())
    }
}

object DBFactory {
    private val factory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
    fun parseDocument(string: String): Document {
        return factory.newDocumentBuilder().parse(InputSource(StringReader(string)))
    }
}

object XPFactory {
    private val factory: XPathFactory = XPathFactory.newInstance()
    fun eval(xpath: String, document: Document): NodeList {
        return factory.newXPath().evaluate(xpath, document, XPathConstants.NODESET) as NodeList
    }
}

open class AdocDSLInline(val text: String) : Comparable<AdocDSLInline> {
    init {
        this.checkValid()
    }

    open fun checkValid() {
    }

    override fun compareTo(other: AdocDSLInline): Int {
        return if (this.text > other.text) 1 else - 1
    }

    override fun toString(): String {
        return text
    }

    open fun toHabrMd(): String {
        return toString()
    }

    open fun toText(): String {
        return toString()
    }
}

class IdString(string: String) : AdocDSLInline(string) {
    override fun checkValid() {
        val pattern = "[a-zA-Zа-яА-Я_][a-zA-Z-аяА-Я0-9_-]*"
        if (! (Regex(pattern) matches text)) {
            throw Exception("String [$text] should match [$pattern]")
        }
    }
}

class RoleString(string: String) : AdocDSLInline(string) {
    override fun checkValid() {
        val pattern = "[a-zA-Z_][a-zA-Z0-9_-]*"
        if (! (Regex(pattern) matches text)) {
            throw Exception("String [$text] should match [$pattern]")
        }
    }
}

class AttrNameString(string: String) : AdocDSLInline(string) {
    override fun checkValid() {
        val pattern = "[a-z_][a-z0-9_-]*"
        if (! (Regex(pattern) matches text)) {
            throw Exception("String [$text] should match [$pattern]")
        }
    }
}

class TitleString(string: String) : AdocDSLInline(string) {
    override fun toString(): String {
        return text
    }

    override fun checkValid() {
        val pattern = "[^\n]+"
        if (! (Regex(pattern) matches text)) {
            throw Exception("String [$text] should match [$pattern]")
        }
    }
}

open class AdocDSLText(text: String) : AdocDSLInline(text) {

    override fun checkValid() {
        checkHasNoAsciidocMarkup()
    }

    private fun checkHasNoAsciidocMarkup() {
        val xmlRepresentation = DBFactory.parseDocument(AsciidocValidatorFactory.getXML(text))
        if (XPFactory.eval("//*[@block = 'true']", xmlRepresentation).length != 2) {
            throw Exception("String has asciidoc block elements: \n--------\n$text\n--------\n")
        }
        if (XPFactory.eval("/embedded/paragraph/*", xmlRepresentation).length != 0) {
            throw Exception("String has asciidoc inline elements: \n--------\n$text\n--------\n")
        }
    }
}

class AdocDSLLink(text: String, val url: String) : AdocDSLText(text) {
    override fun toString(): String {
        return "$url[$text]"
    }

    override fun toHabrMd(): String {
        return "[$text]($url)"
    }

    override fun toText(): String {
        return text
    }

    init {
        super.checkValid()
        val urlPattern = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"
        if (! (Regex(urlPattern) matches url)) {
            throw Exception("URL [$url] should match [$urlPattern]")
        }
    }
}

class AdocDSLVideo(text: String) : AdocDSLInline(text) {
    override fun toString(): String {
        return """Ссылка на видео: $text"""
    }

    override fun toHabrMd(): String {
        return "<video>$text</video>"
    }

    override fun toText(): String {
        return "Link to video: $text"
    }

}

open class AdocDSLInlineContent : ArrayList<AdocDSLInline>() {
    override fun toString(): String {
        var returnString = ""
        this.forEach { inlineMarkup ->
            returnString += "$inlineMarkup"
        }
        return returnString
    }

    fun toHabrMd(): String {
        var returnString = ""
        this.forEach { inlineMarkup ->
            returnString += inlineMarkup.toHabrMd()
        }
        return returnString
    }

    fun toText(): String {
        var returnString = ""
        this.forEach { inlineMarkup ->
            returnString += inlineMarkup.toText()
        }
        return returnString
    }
}

@DslMarker
annotation class AsciidocTagMarker

@AsciidocTagMarker
open class AdocDSLStructuralNode {
    var sectionLevel: Int? = null
    open val type = NodeType.Any
    val blocks = arrayListOf<AdocDSLStructuralNode>()
    private val roles: SortedSet<RoleString> = sortedSetOf()
    private var id: IdString? = null
    var title: TitleString? = null
    val attrs: SortedMap<AttrNameString, String> = sortedMapOf()

    fun attr(name: String, value: String) {
        attrs[AttrNameString(name)] = value
    }


    open fun toHabrMd(): String {
        return this.toString()
    }

    open fun toText(): String {
        return this.toString()
    }

    protected var inlineContent: AdocDSLInlineContent = AdocDSLInlineContent()

    protected open fun text(string: String) {
        inlineContent.add(AdocDSLText(string))
    }

    fun link(text: String, url: String): AdocDSLLink {
        return AdocDSLLink(text, url)
    }

    fun video(text: String): AdocDSLVideo {
        return AdocDSLVideo(text)
    }

    fun id(id: String) {
        this.id = IdString(id)
    }

    fun title(title: String) {
        this.title = TitleString(title)
    }

    fun roles(vararg roles: String) {
        roles.forEach { role ->
            this.roles.add(RoleString(role))
        }
    }

    protected open fun ol(init: AdocDSLOList.() -> Unit): AdocDSLOList {
        val list = AdocDSLOList()
        list.apply(init)
        this.blocks.add(list)
        return list
    }

    protected open fun section(init: AdocDSLSection.() -> Unit): AdocDSLSection {
        val section = AdocDSLSection()
        section.apply(init)
        section.sectionLevel = this.sectionLevel !! + 1
        this.blocks.add(section)
        return section
    }


    protected open fun p(init: AdocDSLParagraph.() -> Unit): AdocDSLParagraph {

        val para = AdocDSLParagraph()
        para.apply(init)
        this.blocks.add(para)
        LangTools.check(para)
        return para
    }

    fun getIdRoleSyntax(): String {
        if (id == null && roles.isEmpty()) return ""
        val strings = mutableListOf<String>()
        if (id != null) {
            strings += mutableListOf("#$id")
        }
        strings += roles.map { role -> ".$role" }.toMutableList()
        return strings.joinToString("", "[", "]\n")
    }

    private fun getLocalTitleSyntax(): String {
        if (title != null) {
            return ".$title\n"
        }
        return ""
    }

    fun getAttributesSyntax(): String {
        if (attrs.isNotEmpty()) {
            return attrs.map { "${it.key}=\"${it.value}\"" }.joinToString(", ", "[", "]\n")
        }
        return ""
    }

    fun getBlockMetaSyntax(): String {
        return getIdRoleSyntax() + getLocalTitleSyntax() + getAttributesSyntax()
    }

}

open class AdocDSLList : AdocDSLStructuralNode() {
    override val type = NodeType.List
    fun li(init: AdocDSLListItem.() -> Unit): AdocDSLListItem {
        val listItem = AdocDSLListItem()
        listItem.apply(init)
        this.blocks.add(listItem)
        return listItem
    }
}

class AdocDSLOList : AdocDSLList() {
    override fun toString(): String {
        var returnString = ""
        returnString += "\n${getBlockMetaSyntax()}"
        blocks.forEach { listItem ->
            returnString += listItem.toString()
        }
        return returnString
    }

    override fun toHabrMd(): String {
        var returnString = ""
        blocks.forEach { listItem ->
            returnString += listItem.toHabrMd()
        }
        return returnString
    }

    override fun toText(): String {
        var returnString = ""
        blocks.forEach { listItem ->
            returnString += listItem.toText()
        }
        return returnString
    }
}

class AdocDSLListItem : AdocDSLStructuralNode() {
    override fun toString(): String {
        var returnString = ""
        blocks.forEachIndexed { index, adocDSLStructuralNode ->
            returnString += if ((index == 0) and (adocDSLStructuralNode.type == NodeType.Para)) {
                val para = adocDSLStructuralNode as AdocDSLParagraph
                "\n. $para"
            } else {
                "\n+\n$adocDSLStructuralNode"
            }
        }
        return returnString
    }

    override fun toHabrMd(): String {
        var returnString = ""
        blocks.forEachIndexed { index, adocDSLStructuralNode ->
            returnString += if ((index == 0) and (adocDSLStructuralNode.type == NodeType.Para)) {
                val para = adocDSLStructuralNode as AdocDSLParagraph
                "\n1. ${para.toHabrMd()}"
            } else if ((index == 1) and (adocDSLStructuralNode.type == NodeType.Para)) {
                "\n\n  ${adocDSLStructuralNode.toHabrMd()}\n"
            } else {
                "\n  ${adocDSLStructuralNode.toHabrMd()}\n"
            }
        }
        return returnString
    }

    override fun toText(): String {
        var returnString = ""
        blocks.forEachIndexed { index, adocDSLStructuralNode ->
            returnString += if ((index == 0) and (adocDSLStructuralNode.type == NodeType.Para)) {
                val para = adocDSLStructuralNode as AdocDSLParagraph
                "\n\n- ${para.toText()}"
            } else if ((index == 1) and (adocDSLStructuralNode.type == NodeType.Para)) {
                "\n\n${adocDSLStructuralNode.toText()}\n"
            } else {
                "\n${adocDSLStructuralNode.toText()}\n"
            }
        }
        return returnString
    }

    public override fun p(init: AdocDSLParagraph.() -> Unit): AdocDSLParagraph {
        val newPara = super.p(init)
        newPara.paraType = if (blocks.size == 1) ParaType.FirstList else ParaType.OtherList
        return newPara
    }

}

class AdocDSLSection : AdocDSLStructuralNode() {
    override fun toString(): String {
        var returnString =
            "\n\n${getIdRoleSyntax()}${getAttributesSyntax()}${"=".repeat(sectionLevel !!.plus(1))} $title"
        blocks.forEach { returnString += it.toString() }
        return returnString
    }

    override fun toHabrMd(): String {
        var returnString =
            "\n\n${"#".repeat(sectionLevel !!.plus(1))} $title"
        blocks.forEach { returnString += it.toString() }
        return returnString
    }

    override fun toText(): String {
        var returnString =
            "\n\n$title"
        blocks.forEach { returnString += it.toText() }
        return returnString
    }

    public override fun section(init: AdocDSLSection.() -> Unit): AdocDSLSection {
        return super.section(init)
    }
}

class AdocDSLDocument : AdocDSLStructuralNode() {
    init {
        sectionLevel = 0
    }

    override fun toString(): String {
        var returnString = getIdRoleSyntax()
        returnString += if (title == null) "" else "= $title"
        attrs.forEach { attr ->
            returnString += "\n:${attr.key}: ${attr.value}"
        }
        blocks.forEach { block ->
            returnString += block.toString()
        }
        return returnString
    }

    override fun toHabrMd(): String {
        var returnString = ""
        blocks.forEach { block ->
            returnString += block.toHabrMd()
        }
        return returnString
    }

    override fun toText(): String {
        var returnString = ""
        returnString += if (title == null) "" else "$title"
        blocks.forEach { block ->
            returnString += block.toText()
        }
        return returnString
    }

    public override fun p(init: AdocDSLParagraph.() -> Unit): AdocDSLParagraph {
        return super.p(init)
    }

    public override fun ol(init: AdocDSLOList.() -> Unit): AdocDSLOList {
        return super.ol(init)
    }

    public override fun section(init: AdocDSLSection.() -> Unit): AdocDSLSection {
        return super.section(init)
    }
}

class AdocDSLParagraph : AdocDSLStructuralNode() {
    var ruleExeptions: Set<String> = setOf()
        private set
    private val thisPara = this
    override val type = NodeType.Para
    var paraType = ParaType.Normal

    fun ignoreRules(vararg rules: String) {
        this.ruleExeptions = rules.toSet()
    }

    override fun toString(): String {
        if (paraType == ParaType.FirstList) {
            return "$inlineContent"
        } else if (paraType == ParaType.OtherList) {
            return "${getBlockMetaSyntax()}$inlineContent"
        }
        return "\n\n${getBlockMetaSyntax()}$inlineContent"
    }

    override fun toHabrMd(): String {
        if (paraType == ParaType.FirstList) {
            return inlineContent.toHabrMd()
        } else if (paraType == ParaType.OtherList) {
            return "${getBlockMetaSyntax()}${inlineContent.toHabrMd()}"
        }
        return "\n\n${getBlockMetaSyntax()}${inlineContent.toHabrMd()}\n\n<cut/>"
    }

    override fun toText(): String {
        val inlineText = inlineContent.toText()
        if (paraType == ParaType.FirstList) {
            return inlineText
        } else if (paraType == ParaType.OtherList) {
            return "${getBlockMetaSyntax()}$inlineText"
        }
        return "\n\n${getBlockMetaSyntax()}$inlineText"
    }

    operator fun String.unaryPlus(): AdocDSLInlineContent {
        thisPara.text(this)
        return inlineContent
    }

    operator fun AdocDSLVideo.unaryPlus(): AdocDSLInlineContent {
        thisPara.inlineContent.add(video(this.text))
        return inlineContent
    }
    operator fun AdocDSLLink.unaryPlus(): AdocDSLInlineContent {
        thisPara.inlineContent.add(link(this.text, this.url))
        return inlineContent
    }


    operator fun AdocDSLInlineContent.plus(text: String): AdocDSLInlineContent {
        thisPara.text(text)
        return inlineContent
    }

    operator fun AdocDSLInlineContent.plus(link: AdocDSLLink): AdocDSLInlineContent {
        thisPara.inlineContent.add(link(link.text, link.url))
        return inlineContent
    }

    public override fun text(string: String) {
        return super.text(string)
    }

}

fun markupDocument(init: AdocDSLDocument.() -> Unit): AdocDSLDocument {
    return AdocDSLDocument().apply(init)
}

