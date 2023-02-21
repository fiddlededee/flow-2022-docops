import org.approvaltests.Approvals
import org.junit.jupiter.api.Test

internal class Title {

    private val document = markupDocument {
        title("Some title")
        id("id")
        attr("attr1", "attr1 value")
    }

    @Test
    fun asciidoc() {
        Approvals.verify(document)
    }

    @Test
    fun habrMd() {
        Approvals.verify(document.toHabrMd())
    }
}

internal class Link {

    private val document = markupDocument {
        title("Links")

        ol {
            li { p { + link("Here is some link", "http://abc.ru") } }
        }
    }

    @Test
    fun asciidoc() {
        Approvals.verify(document)
    }

    @Test
    fun habrMd() {
        Approvals.verify(document.toHabrMd())
    }
}

internal class Para {

    private val document = markupDocument {
        title("Title")
        p { id("id"); roles("r1", "r2"); + "Some paragraph" }
        p { + "Some another paragraph" }
    }

    @Test
    fun asciidoc() {
        Approvals.verify(document)
    }

    @Test
    fun habrMd() {
        Approvals.verify(document.toHabrMd())
    }
}

internal class UList {

    private val document = markupDocument {
        title("Title")
        ul {
            li {
                p { + "Some paragraph" }
                p { + "Some another paragraph" }
            }
        }
    }

    @Test
    fun asciidoc() {
        Approvals.verify(document)
    }
    @Test
    fun habrMd() {
        Approvals.verify(document.toHabrMd())
    }
}

internal class Table {
    private val document = markupDocument {
        title("Title")
        table {
            title("Table Name")
            attr("cols", "1,1")
            attr("options", "header")
            td { p { +"ABC" } }
            td { p { +"DEF" }; p { +"xyz"} }
            td { p { +"GHI" } }
            td { p { +"JKL" } }
            td { p { +"MNO" } }
            td { p { +"PQR" } }
        }
    }

    @Test
    fun asciidoc() {
        Approvals.verify(document)
    }
}

internal class OList {

    private val document = markupDocument {
        title("Title")
        ol {
            li {
                p { + "Some paragraph with " + link("link", "http://ya.ru") }
                p { + "Second paragraph" }
            }
            li {
                p { + "Some another paragraph in list" }
            }
        }
        p { + "Paragraph out of list" }
        ol {
            li {
                p { + "Some paragraph" }
                p { + "Second paragraph" }
                p { + "Third paragraph" }
            }
            li { p { + "Some another paragraph" } }
        }
    }

    @Test
    fun asciidoc() {
        Approvals.verify(document)
    }

    @Test
    fun habrMd() {
        Approvals.verify(document.toHabrMd())
    }
}
