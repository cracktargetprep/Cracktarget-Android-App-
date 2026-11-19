package com.cracktarget.app

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import java.util.concurrent.Executors
import org.jsoup.nodes.Document

internal enum class PrepScreen(val label: String, val icon: PrepIcon) {
    HOME("Home", PrepIcon.HOME), STUDY("Study", PrepIcon.STUDY), TESTS("Tests", PrepIcon.TESTS),
    UPDATES("Updates", PrepIcon.UPDATES), ACCOUNT("Account", PrepIcon.ACCOUNT),
    AFFAIRS("Current affairs", PrepIcon.NEWS), VACANCIES("Vacancy notices", PrepIcon.VACANCY),
    BOOKS("Exclusive books", PrepIcon.BOOKS);

    val tab: PrepScreen get() = when (this) {
        AFFAIRS, VACANCIES -> UPDATES
        BOOKS -> STUDY
        else -> this
    }
}

internal class NativePrep(
    private val context: Context,
    private val navigate: (PrepScreen) -> Unit,
    private val open: (PrepLink) -> Unit,
    private val signIn: () -> Unit,
    private val official: (String) -> Unit,
) {
    private val feedCache = mutableMapOf<String, Pair<Long, Document>>()
    private val coverCache = mutableMapOf<String, android.graphics.Bitmap>()
    private val callbacks = android.os.Handler(android.os.Looper.getMainLooper())
    private var destroyed = false
    private var vacancySection = "open"

    fun destroy() {
        destroyed = true
        callbacks.removeCallbacksAndMessages(null)
    }

    fun toolbar(screen: PrepScreen): View {
        val bar = LinearLayout(context).apply {
            setPadding(dp(19), dp(8), dp(19), dp(8))
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(surface)
        }
        if (screen == PrepScreen.HOME) {
            bar.addView(ImageView(context).apply {
                setImageResource(R.drawable.launcher_foreground)
                setBackground(round(dark, 13))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                scaleType = ImageView.ScaleType.FIT_CENTER
            }, LinearLayout.LayoutParams(dp(43), dp(43)))
            val titles = column().apply {
                addView(text("CrackTarget", 19, ink, "sans-serif-black"))
            }
            bar.addView(titles, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(10) })
            bar.addView(iconButton(PrepIcon.ACCOUNT, "My account") { navigate(PrepScreen.ACCOUNT) })
        } else {
            if (screen != screen.tab) {
                bar.addView(iconButton(PrepIcon.BACK, "Back to ${screen.tab.label}") { navigate(screen.tab) })
            }
            bar.addView(text(screen.label, 24, ink, "sans-serif-black"),
                LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = if (screen != screen.tab) dp(6) else 0 })
        }
        return bar
    }

    fun content(screen: PrepScreen): ScrollView {
        val body = column().apply { setPadding(dp(19), dp(12), dp(19), dp(24)) }
        when (screen) {
            PrepScreen.HOME -> home(body)
            PrepScreen.STUDY -> study(body)
            PrepScreen.TESTS -> tests(body)
            PrepScreen.UPDATES -> updates(body)
            PrepScreen.ACCOUNT -> account(body)
            PrepScreen.AFFAIRS -> affairs(body)
            PrepScreen.VACANCIES -> vacancies(body)
            PrepScreen.BOOKS -> books(body)
        }
        return ScrollView(context).apply {
            isFillViewport = true
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            setBackgroundColor(surface)
            addView(body)
        }
    }

    private fun home(body: LinearLayout) {
        body.item(panel().apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(dark, 0xFF1C5150.toInt())).apply {
                cornerRadius = dp(24).toFloat()
            }
            setPadding(dp(19), dp(17), dp(19), dp(16))
            addView(text("Exam preparation", 27, Color.WHITE, "sans-serif-black"))
            item(text("Pick a subject, practise a mock, or catch up on the latest updates.", 14, 0xFFCBDEDA.toInt()), 5)
            item(pill("Browse mock tests", PrepIcon.ARROW, pale, dark) { navigate(PrepScreen.TESTS) }, 12, -2)
        })
        section(body, "Quick access", 19)
        val tiles = listOf(
            tile("Current affairs", "Daily editions", PrepIcon.NEWS, 0xFFE5F2E8.toInt(), green) { navigate(PrepScreen.AFFAIRS) },
            tile("Mock tests", "Free mocks & practice", PrepIcon.TESTS, 0xFFE9EDF8.toInt(), 0xFF405584.toInt()) { navigate(PrepScreen.TESTS) },
            tile("Vacancy notices", "Openings & exam dates", PrepIcon.VACANCY, 0xFFFBEBDD.toInt(), 0xFF8A512B.toInt()) { navigate(PrepScreen.VACANCIES) },
            tile("Free study material", "Notes by exam or subject", PrepIcon.STUDY, 0xFFE6F2EE.toInt(), green) { navigate(PrepScreen.STUDY) },
            tile("Exclusive books", "CrackTarget editions", PrepIcon.BOOKS, 0xFFF3E9F4.toInt(), 0xFF785083.toInt()) { navigate(PrepScreen.BOOKS) },
            tile("Previous papers", "Past questions & solutions", PrepIcon.PAPERS, 0xFFF5F0DC.toInt(), 0xFF746122.toInt()) {
                open(PrepLink("Previous papers", "", PrepCatalog.PAPERS))
            },
        )
        tiles.chunked(2).forEach { group ->
            body.item(LinearLayout(context).apply {
                group.forEachIndexed { index, card ->
                    addView(card, LinearLayout.LayoutParams(0, -1, 1f).apply { if (index > 0) marginStart = dp(10) })
                }
            }, 9)
        }
    }

    private fun study(body: LinearLayout) {
        body.item(intro("Free study material", "Revision notes, subject guides and previous-year questions.", PrepIcon.STUDY))
        body.item(linkRow(PrepLink("All free study material", "Open the full notes collection", PrepCatalog.FREE), PrepIcon.STUDY), 11)
        body.item(linkRow(PrepLink("Previous-year papers", "Find questions and solutions", PrepCatalog.PAPERS), PrepIcon.PAPERS), 8)
        section(body, "Study by exam")
        PrepCatalog.exams.forEach { body.item(linkRow(it, PrepIcon.STUDY), 8) }
        section(body, "Books")
        body.item(row("Exclusive books", "See CrackTarget editions and open your downloads", PrepIcon.BOOKS) {
            navigate(PrepScreen.BOOKS)
        }, 8)
    }

    private fun tests(body: LinearLayout) {
        body.item(intro("Mock tests", "Choose a free mock or open a test series you already have access to.", PrepIcon.TESTS))
        section(body, "Free practice")
        PrepCatalog.practice.forEach { body.item(linkRow(it, PrepIcon.TESTS), 8) }
        body.item(linkRow(PrepLink("All mock tests", "Browse the full test directory", PrepCatalog.TESTS), PrepIcon.TESTS), 10)
        body.item(linkRow(PrepLink("My orders & tests", "Sign in to open eligible test series", "my-account/orders/"), PrepIcon.ACCOUNT), 8)
    }

    private fun updates(body: LinearLayout) {
        body.item(intro("Exam updates", "Check daily current affairs, recruitment notices and exam dates.", PrepIcon.UPDATES))
        body.item(row("Current affairs", "Read the most recent daily editions", PrepIcon.NEWS) { navigate(PrepScreen.AFFAIRS) }, 12)
        body.item(row("Vacancy notices", "Openings, exam dates and results", PrepIcon.VACANCY) { navigate(PrepScreen.VACANCIES) }, 8)
    }

    private fun account(body: LinearLayout) {
        body.item(intro("My account", "Open your CrackTarget tests, orders and book downloads.", PrepIcon.ACCOUNT))
        body.item(pill("Sign in with Google", PrepIcon.ACCOUNT, dark, Color.WHITE, signIn), 12)
        body.item(linkRow(PrepLink("Account dashboard", "Sign in with email or manage your account", "my-account/"), PrepIcon.ACCOUNT), 8)
        body.item(linkRow(PrepLink("My orders & tests", "Open your eligible courses and tests", "my-account/orders/"), PrepIcon.TESTS), 8)
        body.item(linkRow(PrepLink("My downloads", "Open the books and PDFs in your account", "my-account/downloads/"), PrepIcon.DOWNLOAD), 8)
        section(body, "Help & privacy")
        body.item(linkRow(PrepLink("Contact support", "Get help with your account or materials", "contact/"), PrepIcon.ACCOUNT), 8)
        body.item(linkRow(PrepLink("Privacy policy", "", PlayWebPolicy.privacyUrl), PrepIcon.PAPERS), 8)
        body.item(linkRow(PrepLink("Delete account", "Request deletion of your account and data", PlayWebPolicy.deletionUrl), PrepIcon.ACCOUNT), 8)
    }

    private fun affairs(body: LinearLayout) {
        body.item(intro("Daily current affairs", "Read recent editions from the CrackTarget newsroom.", PrepIcon.NEWS))
        section(body, "Recent editions")
        val list = column()
        body.item(list, 8)
        feed(list, PrepCatalog.AFFAIRS) { document ->
            val dates = PrepCatalog.currentAffairs(document)
            if (dates.isEmpty()) list.item(text("New editions will appear here when available.", 14, muted))
            dates.forEachIndexed { index, date ->
                list.item(linkRow(if (index == 0) date.copy(detail = "Latest daily edition") else date, PrepIcon.NEWS), if (index == 0) 0 else 8)
            }
        }
        body.item(linkRow(PrepLink("Current affairs archive", "Find older editions and compilations", PrepCatalog.AFFAIRS), PrepIcon.PAPERS), 12)
    }

    private fun vacancies(body: LinearLayout) {
        body.item(intro("Recruitment notices", "Check the official notice for eligibility and deadlines.", PrepIcon.VACANCY))
        val choices = listOf("Openings" to "open", "Exam dates" to "upcoming", "Results" to "results")
        val filters = LinearLayout(context)
        val list = column()
        fun populate() {
            filters.removeAllViews()
            choices.forEachIndexed { index, (title, key) ->
                val selected = key == vacancySection
                filters.addView(pill(title, null, if (selected) dark else Color.WHITE, if (selected) Color.WHITE else ink) {
                    vacancySection = key
                    populate()
                }, LinearLayout.LayoutParams(0, -2, 1f).apply { if (index > 0) marginStart = dp(6) })
            }
            val requestedSection = vacancySection
            feed(list, PrepCatalog.VACANCIES) { document ->
                if (requestedSection != vacancySection) return@feed
                val notices = PrepCatalog.vacancies(document, requestedSection)
                if (notices.isEmpty()) list.item(text("No listings are available in this section.", 14, muted))
                notices.forEachIndexed { index, notice ->
                    list.item(row(notice.title, notice.organisation.ifBlank { "Recruitment notice" }, PrepIcon.VACANCY) {
                        showNotice(notice)
                    }, if (index == 0) 0 else 8)
                }
            }
        }
        body.item(filters, 12)
        body.item(list, 10)
        body.item(linkRow(PrepLink("All vacancy notices", "Open the complete notice board", PrepCatalog.VACANCIES), PrepIcon.PAPERS), 12)
        populate()
    }

    private fun showNotice(notice: PrepCatalog.Notice) {
        val details = listOf(notice.organisation, notice.timing).filter(String::isNotBlank).joinToString("\n\n")
        AlertDialog.Builder(context).setTitle(notice.title)
            .setMessage(details.ifBlank { "See the CrackTarget notice board for the published details." })
            .setNeutralButton("Notice board") { _, _ -> open(PrepLink("Vacancy notices", "", PrepCatalog.VACANCIES)) }
            .setNegativeButton("Close", null)
            .apply { notice.officialUrl?.let { url -> setPositiveButton("Official notice") { _, _ -> official(url) } } }
            .show()
    }

    private fun books(body: LinearLayout) {
        body.item(intro("Exclusive books", "Explore CrackTarget editions for specialised recruitment exams.", PrepIcon.BOOKS))
        body.item(linkRow(PrepLink("My book downloads", "Open the books already in your account", "my-account/downloads/"), PrepIcon.DOWNLOAD), 10)
        section(body, "CrackTarget editions")
        PrepCatalog.books.forEach { book ->
            body.item(LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = round(Color.WHITE, 16, outline)
                val bitmap = coverCache[book.cover] ?: runCatching {
                    context.assets.open("covers/${book.cover}").use(BitmapFactory::decodeStream)
                }.getOrNull()?.also { coverCache[book.cover] = it }
                addView(ImageView(context).apply {
                    setImageBitmap(bitmap)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LinearLayout.LayoutParams(dp(59), dp(82)))
                val bookText = column().apply {
                    addView(text(book.title, 15, ink, "sans-serif-medium"))
                    item(text(book.detail, 13, muted), 4)
                }
                addView(bookText, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) })
                if (!BuildConfig.PLAY_DISTRIBUTION) {
                    background = touch(Color.WHITE, 16, outline)
                    isFocusable = true
                    setOnClickListener { open(PrepLink(book.title, book.detail, book.product)) }
                    addView(symbol(PrepIcon.ARROW, green, 19), LinearLayout.LayoutParams(dp(31), dp(36)))
                }
            }, 8)
        }
        if (!BuildConfig.PLAY_DISTRIBUTION) {
            body.item(linkRow(PrepLink("All books", "Open the CrackTarget book store", "shop/"), PrepIcon.BOOKS), 10)
        }
    }

    private fun feed(holder: LinearLayout, path: String, show: (Document) -> Unit) {
        holder.removeAllViews()
        val request = Any()
        holder.tag = request
        val cached = feedCache[path]
        if (cached != null && System.currentTimeMillis() - cached.first < 300_000) {
            show(cached.second)
            return
        }
        holder.item(LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            val progress = ProgressBar(context).apply {
                indeterminateTintList = ColorStateList.valueOf(green)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            addView(progress, LinearLayout.LayoutParams(dp(20), dp(20)))
            addView(text("Loading updates…", 13, muted), LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(10) })
        })
        executor.execute {
            val result = runCatching { PrepCatalog.load(path) }
            callbacks.post {
                if (destroyed || holder.tag !== request) return@post
                holder.removeAllViews()
                result.onSuccess { document ->
                    feedCache[path] = System.currentTimeMillis() to document
                    show(document)
                }.onFailure {
                    if (cached != null) {
                        holder.item(text("Showing the last loaded updates.", 13, muted))
                        show(cached.second)
                    } else {
                        holder.item(text("Could not load updates. Check your connection and try again.", 14, muted))
                    }
                    holder.item(pill("Retry updates", null, Color.WHITE, green) { feed(holder, path, show) }, 5, -2)
                }
            }
        }
    }

    private fun intro(title: String, description: String, icon: PrepIcon) = panel().apply {
        background = round(pale, 20)
        setPadding(dp(16), dp(15), dp(16), dp(16))
        addView(symbol(icon, green, 23), LinearLayout.LayoutParams(dp(34), dp(32)))
        item(text(title, 23, ink, "sans-serif-black"), 5)
        item(text(description, 14, muted), 4)
    }

    private fun tile(title: String, detail: String, icon: PrepIcon, tint: Int, iconColor: Int, click: () -> Unit) = panel().apply {
        minimumHeight = dp(99)
        setPadding(dp(12), dp(10), dp(12), dp(10))
        background = touch(tint, 17)
        isFocusable = true
        contentDescription = "$title, $detail"
        setOnClickListener { click() }
        addView(LinearLayout(context).apply {
            addView(symbol(icon, iconColor, 20), LinearLayout.LayoutParams(dp(24), dp(24)))
            addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))
            addView(symbol(PrepIcon.ARROW, iconColor, 16), LinearLayout.LayoutParams(dp(21), dp(22)))
        })
        item(text(title, 15, ink, "sans-serif-medium"), 5)
        item(text(detail, 12, muted), 2)
    }

    private fun linkRow(link: PrepLink, icon: PrepIcon) = row(link.title, link.detail, icon) { open(link) }

    private fun row(title: String, detail: String, icon: PrepIcon, click: () -> Unit) = LinearLayout(context).apply {
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(58)
        setPadding(dp(10), dp(9), dp(9), dp(9))
        background = touch(Color.WHITE, 16, outline)
        isFocusable = true
        setOnClickListener { click() }
        addView(symbol(icon, green, 20).apply { background = round(pale, 12) },
            LinearLayout.LayoutParams(dp(37), dp(37)))
        addView(column().apply {
            addView(text(title, 15, ink, "sans-serif-medium"))
            if (detail.isNotBlank()) item(text(detail, 13, muted), 3)
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(10); marginEnd = dp(5) })
        addView(symbol(PrepIcon.ARROW, green, 18), LinearLayout.LayoutParams(dp(29), dp(30)))
    }

    private fun pill(label: String, icon: PrepIcon?, fill: Int, foreground: Int, click: () -> Unit) = Button(context).apply {
        text = label
        isAllCaps = false
        textSize = 14f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setTextColor(foreground)
        minimumHeight = dp(46)
        minHeight = dp(46)
        minimumWidth = 0
        minWidth = 0
        stateListAnimator = null
        background = touch(fill, 12)
        setPadding(dp(12), dp(8), dp(12), dp(8))
        icon?.let {
            setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, PrepIconDrawable(it, foreground, dp(19)), null)
            compoundDrawablePadding = dp(7)
        }
        setOnClickListener { click() }
    }

    private fun iconButton(icon: PrepIcon, label: String, click: () -> Unit) = symbol(icon, green, 22).apply {
        layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        background = touch(pale, 14)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = label
        isFocusable = true
        setOnClickListener { click() }
    }

    private fun symbol(icon: PrepIcon, color: Int, size: Int) = ImageView(context).apply {
        setImageDrawable(PrepIconDrawable(icon, color, dp(size)))
        scaleType = ImageView.ScaleType.CENTER
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun section(body: LinearLayout, title: String, margin: Int = 16) {
        body.item(text(title, 18, ink, "sans-serif-black").apply { ViewCompat.setAccessibilityHeading(this, true) }, margin)
    }

    private fun text(value: String, size: Int, color: Int, family: String = "sans-serif"): TextView = TextView(context).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        typeface = Typeface.create(family, Typeface.NORMAL)
        includeFontPadding = false
        setLineSpacing(dp(2).toFloat(), 1f)
    }

    private fun panel() = column()
    private fun column() = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    private fun LinearLayout.item(view: View, top: Int = 0, width: Int = -1) {
        addView(view, LinearLayout.LayoutParams(width, -2).apply { topMargin = dp(top) })
    }

    private fun round(color: Int, radius: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        stroke?.let { setStroke(dp(1), it) }
    }

    private fun touch(color: Int, radius: Int, stroke: Int? = null) =
        RippleDrawable(ColorStateList.valueOf(0x22136758), round(color, radius, stroke), round(Color.WHITE, radius))

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density + .5f).toInt()

    companion object {
        val surface = 0xFFF5F7F2.toInt()
        val dark = 0xFF123736.toInt()
        val ink = 0xFF132E2D.toInt()
        val muted = 0xFF526662.toInt()
        val green = 0xFF176C56.toInt()
        val pale = 0xFFDFEEE5.toInt()
        val outline = 0xFFE0E7DF.toInt()
        private val executor = Executors.newFixedThreadPool(2)
    }
}
