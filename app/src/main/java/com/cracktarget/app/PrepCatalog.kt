package com.cracktarget.app

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

internal data class PrepLink(val title: String, val detail: String, val path: String) {
    val url: String get() = if (path.startsWith("https://")) path else SITE + path.trimStart('/')
}

internal object PrepCatalog {
    const val AFFAIRS = "current-affairs/"
    const val VACANCIES = "vacancies/"
    const val FREE = "free-study-materials-by-cracktarget-com/"
    const val TESTS = "mocktest/"
    const val PAPERS = "previous-years-questions/"

    val exams = listOf(
        PrepLink("UPSC EPFO", "APFC · EO/AO · Labour laws", "study-directory/epfo/"),
        PrepLink("DRDO SAO", "Administration · Management", "study-directory/administration/"),
        PrepLink("Banking and regulators", "RBI · NABARD · Regulatory exams", "study-directory/banking/"),
        PrepLink("Law and prosecution", "Judiciary and recruitment exams", "judiciary/"),
    )

    val practice = listOf(
        PrepLink("UPSC EPFO APFC", "Free diagnostic · 36 questions", "upsc-epfo-apfc-2026-free-diagnostic-test-36-mcqs/"),
        PrepLink("DRDO Senior Administrative Officer", "Free full-length practice", "free-mock-test-upsc-senior-administrative-officer/"),
        PrepLink("UPPCS General Studies and CSAT", "Free Hindi mock", "uppcs-2026-mock-test-hindi/"),
        PrepLink("NABARD Grade A", "Free Phase I demo", "nabard-grade-a-2026-online-mock-tests/"),
    )

    data class Book(val title: String, val detail: String, val cover: String, val product: String)

    val books = listOf(
        Book("Industrial Relations, Labour Codes & Social Security", "UPSC EPFO APFC", "book_labour.webp", "product/upsc-epfo-apfc-2026-labour-laws/"),
        Book("Human Resource Management", "DRDO SAO & allied exams", "book_hrm.webp", "product/human-resource-management-drdo-sao-guide/"),
        Book("Principles of Accounting & Financial Management", "Recruitment exam study guide", "book_accounting.webp", "product/principles-of-accounting-financial-management-drdo/"),
    )

    data class Notice(val title: String, val organisation: String, val timing: String, val officialUrl: String?)

    fun load(path: String): Document {
        val url = SITE + path
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "text/html")
            check(connection.responseCode == 200) { "Unable to load the CrackTarget feed" }
            return connection.inputStream.use { input ->
                // Hub pages are public and contain their lists in the initial HTML.
                val body = java.io.ByteArrayOutputStream().use { bytes ->
                    val block = ByteArray(8192)
                    var read = input.read(block)
                    while (read != -1) {
                        bytes.write(block, 0, read)
                        check(bytes.size() <= 1_500_000) { "Feed exceeded the expected size" }
                        read = input.read(block)
                    }
                    bytes.toByteArray()
                }
                Jsoup.parse(body.toString(Charsets.UTF_8), url)
            }
        } finally {
            connection.disconnect()
        }
    }

    fun currentAffairs(document: Document): List<PrepLink> = document.select("a.dchip[href][title]")
        .mapNotNull { link ->
            val url = link.absUrl("href")
            if (!firstParty(url) || !dailyPath.matches(URI(url).path.orEmpty())) return@mapNotNull null
            val title = link.attr("title").removePrefix("Current Affairs ").trim()
            title.takeIf(String::isNotEmpty)?.let { PrepLink(it, "Daily current affairs", url) }
        }.distinctBy(PrepLink::url).take(10)

    fun vacancies(document: Document, section: String): List<Notice> = document
        .select("#ctv-$section details.ctv-it").take(14).mapNotNull { item ->
            val title = item.selectFirst("summary .ctv-t")?.text().orEmpty()
            if (title.isEmpty()) return@mapNotNull null
            val organisation = item.selectFirst("summary .ctv-o")?.text().orEmpty()
            val timing = item.select("div, li, tr").firstOrNull { row ->
                val children = row.children()
                children.size == 2 && children.first()?.text()?.let {
                    it.startsWith("Apply Dates", true) || it.startsWith("Exam /", true) || it.startsWith("Result /", true)
                } == true
            }?.children()?.last()?.text().orEmpty()
            val official = item.select("a[href]").firstOrNull { link -> official(link.absUrl("href")) }
                ?.absUrl("href")
            Notice(title, organisation, timing, official)
        }

    fun firstParty(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        return uri.scheme == "https" && uri.host == "cracktarget.com" && uri.userInfo == null &&
            (uri.port == -1 || uri.port == 443)
    }

    fun official(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (uri.scheme != "https" || uri.userInfo != null || uri.port !in listOf(-1, 443)) return false
        val host = uri.host?.lowercase() ?: return false
        return host.endsWith(".gov.in") || host.endsWith(".nic.in") ||
            host in setOf("www.nabard.org", "nabard.org", "www.eximbankindia.in", "eximbankindia.in",
                "www.rbi.org.in", "rbi.org.in", "www.sebi.gov.in", "www.ibps.in", "ibps.in")
    }

    private val dailyPath = Regex("/current-affairs-[0-9]{1,2}-[a-z]+-20[0-9]{2}/?")
}
