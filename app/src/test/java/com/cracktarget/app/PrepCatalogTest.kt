package com.cracktarget.app

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PrepCatalogTest {
    @Test fun dailyFeedKeepsUniqueFirstPartyEditions() {
        val document = Jsoup.parse("""
            <a class="dchip" title="Current Affairs 19 November 2026" href="/current-affairs-19-november-2026/">19</a>
            <a class="dchip" title="Current Affairs 19 November 2026" href="/current-affairs-19-november-2026/">Repeated</a>
            <a class="dchip" title="Current Affairs 18 November 2026" href="/current-affairs-18-november-2026/">18</a>
            <a class="dchip" title="Impersonator" href="https://example.org/current-affairs-19-november-2026/">External</a>
            <a class="dchip" title="Archive" href="/current-affairs/">Archive</a>
        """.trimIndent(), "https://cracktarget.com/current-affairs/")
        assertEquals(listOf("19 November 2026", "18 November 2026"), PrepCatalog.currentAffairs(document).map { it.title })
    }

    @Test fun vacancyFeedRespectsCategoryAndOfficialHosts() {
        val document = Jsoup.parse("""
            <section id="ctv-open"><details class="ctv-it">
              <summary><span class="ctv-t">Recruitment notice</span><span class="ctv-o">Public Service Commission</span></summary>
              <div><span>Apply Dates / आवेदन तिथि</span><strong>10–25 November</strong></div>
              <a href="https://buy.example.org/">External</a><a href="https://uppsc.up.nic.in/Notice">Official Link</a>
            </details></section>
            <section id="ctv-results"><details class="ctv-it">
              <summary><span class="ctv-t">Results notice</span></summary>
              <a href="https://example.org/gov.in">Unrelated link</a>
            </details></section>
        """.trimIndent(), "https://cracktarget.com/vacancies/")
        val notice = PrepCatalog.vacancies(document, "open").single()
        assertEquals("Recruitment notice", notice.title)
        assertEquals("10–25 November", notice.timing)
        assertEquals("https://uppsc.up.nic.in/Notice", notice.officialUrl)
        assertNull(PrepCatalog.vacancies(document, "results").single().officialUrl)
        assertFalse(PrepCatalog.official("https://uppsc.up.nic.in@example.org/"))
    }
}
