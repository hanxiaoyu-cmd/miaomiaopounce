package com.miaomiaopounce

import android.app.*
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.*
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.miaomiaopounce.audio.GameAudio
import com.miaomiaopounce.data.SettingsStore
import com.miaomiaopounce.game.*
import com.miaomiaopounce.ui.*
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var store: SettingsStore
    private lateinit var settings: GameSettings
    private lateinit var audio: GameAudio
    private var page = Page.HOME
    private var game: GameView? = null
    private var preview: GameView? = null
    private var ownerDialog: AlertDialog? = null
    private var result: SessionResult? = null
    private var recorded = false
    private var pinStarted = false
    private var needsResume = false
    enum class Page { HOME, GAME, TOUCH, RESULT }
    private val density get() = resources.displayMetrics.density
    private fun dp(value: Int) = (value * density + 0.5f).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SettingsStore(this); settings = store.load(); audio = GameAudio(this)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = handleBack()
        })
        // An interrupted process returns to owner controls rather than unexpectedly starting sound.
        showHome()
    }

    private fun rounded(color: Int, radius: Int = 18, stroke: Int? = null): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat(); stroke?.let { setStroke(dp(1), it) }
    }
    private fun text(value: String, size: Float = 15f, color: Int = Palette.ink, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); includeFontPadding = false
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
    }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun space(parent: LinearLayout, height: Int = 12) { parent.addView(View(this), LinearLayout.LayoutParams(1, dp(height))) }
    private fun button(value: String, primary: Boolean = false, action: () -> Unit) = Button(this).apply {
        text = value; textSize = 15f; isAllCaps = false; minHeight = dp(48); minimumHeight = dp(48)
        setTextColor(if (primary) Palette.white else Palette.ink)
        background = RippleDrawable(android.content.res.ColorStateList.valueOf(0x224C6D5A), rounded(if (primary) Palette.orange else Palette.white, 14, if (primary) null else Palette.line), null)
        setPadding(dp(16), dp(9), dp(16), dp(9)); setOnClickListener { action() }
    }
    private fun lp(width: Int = LinearLayout.LayoutParams.MATCH_PARENT, height: Int = LinearLayout.LayoutParams.WRAP_CONTENT) = LinearLayout.LayoutParams(width, height)

    private fun ownerWindow(root: View) {
        // Install the decor before requesting its InsetsController (some Android 12 OEMs require this).
        window.decorView
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.show(WindowInsets.Type.systemBars())
            window.insetsController?.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        root.setBackgroundColor(Palette.cream)
        val originalLeft = root.paddingLeft; val originalTop = root.paddingTop
        val originalRight = root.paddingRight; val originalBottom = root.paddingBottom
        root.setOnApplyWindowInsetsListener { view, insets ->
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                val safe = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                view.setPadding(originalLeft + safe.left, originalTop + safe.top, originalRight + safe.right, originalBottom + safe.bottom)
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(originalLeft + insets.systemWindowInsetLeft, originalTop + insets.systemWindowInsetTop, originalRight + insets.systemWindowInsetRight, originalBottom + insets.systemWindowInsetBottom)
            }
            insets
        }
        setContentView(root); root.requestApplyInsets()
    }

    private fun showHome() {
        page = Page.HOME; preview?.pause(); game?.pause(); game = null; stopPin()
        settings = store.load()
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        val outer = column().apply { setPadding(dp(28), dp(20), dp(28), dp(104)) }
        scroll.addView(outer)
        val header = row()
        val logo = ImageView(this).apply { setImageResource(R.drawable.ic_launcher); contentDescription = "喵喵扑趣标志"; background = rounded(Palette.ink, 12); clipToOutline = true }
        header.addView(logo, lp(dp(40), dp(40)))
        val name = column().apply { setPadding(dp(12), 0, 0, 0) }
        name.addView(text("喵喵扑趣", 19f, bold = true)); space(name, 4)
        name.addView(text("MIAO MIAO POUNCE", 9f, Palette.muted, true).apply { letterSpacing = 0.16f })
        header.addView(name, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(text("●  离线小乐园", 12f, Palette.muted).apply { setPadding(dp(12), dp(8), dp(12), dp(8)); background = rounded(0xFFE9EBDD.toInt(), 20) })
        outer.addView(header); space(outer, 26)

        val availableDp = resources.configuration.screenWidthDp
        val wide = availableDp >= 760
        val content = LinearLayout(this).apply { orientation = if (wide) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL; gravity = Gravity.TOP }
        val left = column()
        left.addView(text("一点好奇，\n一场小小捕猎。", if (wide) 35f else 29f, bold = true).apply { setLineSpacing(dp(7).toFloat(), 1f) })
        space(left, 12)
        left.addView(text("给小爪子一个会回应的世界。", 15f, Palette.muted))
        space(left, 20)
        val scene = FrameLayout(this).apply { background = rounded(Palette.habitat(settings.theme), 24); clipToOutline = true }
        preview = GameView(this, GameEngine(settings.copy(minutes = 10)), true)
        scene.addView(preview, FrameLayout.LayoutParams(-1, -1))
        val habitat = text(settings.theme.habitat + "  /  LIVE", 11f, Palette.ink, true).apply {
            setPadding(dp(12), dp(7), dp(12), dp(7)); background = rounded(0xDDFBF9F0.toInt(), 20)
        }
        scene.addView(habitat, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START).apply { setMargins(dp(16), dp(16), 0, 0) })
        fun refreshPreview() {
            preview?.pause(); scene.removeView(preview)
            preview = GameView(this, GameEngine(settings.copy(minutes = 10)), true)
            scene.addView(preview, 0, FrameLayout.LayoutParams(-1, -1))
        }
        left.addView(scene, lp(-1, dp(if (wide) 220 else 210))); space(left, 14)
        left.addView(text("先看一看，再扑一下", 15f, bold = true)); space(left, 7)
        left.addView(text("短距离移动、停顿、转向。拍中后轻轻回应，\n每一次好奇，都有一个小小结果。", 12f, Palette.muted).apply { setLineSpacing(dp(4).toFloat(), 1f) })
        space(left, 16)
        val presets = row()
        presets.addView(button("☁  安静观察") {
            settings = settings.copy(count = 1, pace = Pace.GENTLE, size = PreySize.LARGE, sound = false); store.save(settings); showHome()
        }.apply { tag = "gentlePreset" }, LinearLayout.LayoutParams(0, dp(46), 1f))
        presets.addView(button("✦  好奇探索") {
            settings = settings.copy(count = 3, pace = Pace.NORMAL, size = PreySize.MEDIUM, sound = true, volume = 25); store.save(settings); showHome()
        }.apply { tag = "curiousPreset" }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginStart = dp(8) })
        left.addView(presets)
        if (store.sessions > 0) {
            space(left, 14)
            left.addView(text("上次 ${store.lastTheme} · 捕获 ${store.lastCaptures} 次\n共玩过 ${store.sessions} 场 · 累计捕获 ${store.totalCaptures} 次", 11f, Palette.muted))
        }
        val right = column().apply { setPadding(dp(20), dp(20), dp(20), dp(20)); background = rounded(Palette.white, 24, Palette.line) }
        right.addView(text("今天，想抓点什么？", 21f, bold = true)); space(right, 6)
        right.addView(text("挑一种玩法，再调成它喜欢的节奏", 12f, Palette.muted)); space(right, 16)
        val cards = mutableMapOf<PreyTheme, View>()
        for (pair in PreyTheme.entries.chunked(2)) {
            val cardRow = row()
            pair.forEachIndexed { i, theme ->
                val card = themeCard(theme, settings.theme == theme) {
                    settings = settings.copy(theme = theme); store.save(settings)
                    cards.forEach { (t, v) -> v.background = rounded(if (t == theme) tint(Palette.accent(t), 0.11f) else Palette.white, 16, if (t == theme) Palette.accent(t) else Palette.line) }
                    refreshPreview(); habitat.text = theme.habitat + "  /  LIVE"
                }
                cards[theme] = card
                cardRow.addView(card, LinearLayout.LayoutParams(0, dp(85), 1f).apply { if (i > 0) marginStart = dp(8) })
            }
            right.addView(cardRow); space(right, 8)
        }
        space(right, 8)
        val countRow = row()
        countRow.addView(text("屏幕上的小伙伴", 13f, bold = true), LinearLayout.LayoutParams(0, -2, 1f))
        val countText = text(settings.count.toString(), 18f, bold = true).apply { gravity = Gravity.CENTER; tag = "countValue" }
        countRow.addView(button("−") { settings = settings.copy(count = (settings.count - 1).coerceAtLeast(1)); countText.text = settings.count.toString(); store.save(settings); refreshPreview() }.apply { contentDescription = "减少目标"; tag = "countMinus" }, lp(dp(43), dp(40)))
        countRow.addView(countText, lp(dp(38), dp(40)))
        countRow.addView(button("＋") { settings = settings.copy(count = (settings.count + 1).coerceAtMost(8)); countText.text = settings.count.toString(); store.save(settings); refreshPreview() }.apply { contentDescription = "增加目标"; tag = "countPlus" }, lp(dp(43), dp(40)))
        right.addView(countRow); space(right, 14)
        right.addView(text("移动节奏", 13f, bold = true)); space(right, 8)
        right.addView(segments(Pace.entries.map { it.label }, settings.pace.ordinal, "pace") { settings = settings.copy(pace = Pace.entries[it]); store.save(settings); refreshPreview() })
        space(right, 12)
        right.addView(text("目标大小", 13f, bold = true)); space(right, 8)
        right.addView(segments(PreySize.entries.map { it.label }, settings.size.ordinal, "size") { settings = settings.copy(size = PreySize.entries[it]); store.save(settings); refreshPreview() })
        space(right, 12)
        right.addView(text("这次玩多久", 13f, bold = true)); space(right, 8)
        val durations = listOf(1, 3, 5)
        right.addView(segments(durations.map { "$it 分钟" }, durations.indexOf(settings.minutes).coerceAtLeast(0), "duration") { settings = settings.copy(minutes = durations[it]); store.save(settings) })
        space(right, 12)
        val soundSwitch = Switch(this).apply {
            text = "拍中时，轻轻响一下"; textSize = 13f; setTextColor(Palette.ink); isChecked = settings.sound; tag = "soundSwitch"
            setOnCheckedChangeListener { _, enabled -> settings = settings.copy(sound = enabled); store.save(settings) }
        }
        right.addView(soundSwitch, lp(-1, dp(40)))
        val volumeRow = row()
        volumeRow.addView(text("音量", 12f, Palette.muted), lp(dp(44), -2))
        val volumeText = text("${settings.volume}%", 11f, Palette.muted)
        val volumeBar = SeekBar(this).apply {
            max = 60; progress = settings.volume; tag = "volumeBar"; contentDescription = "游戏音量，最高百分之六十"
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) { settings = settings.copy(volume = value); volumeText.text = "$value%"; store.save(settings) }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) { if (settings.sound) audio.play(settings.theme, settings.volume) }
            })
        }
        volumeRow.addView(volumeBar, LinearLayout.LayoutParams(0, dp(38), 1f)); volumeRow.addView(volumeText, lp(dp(42), -2)); right.addView(volumeRow)
        val hideSwitch = Switch(this).apply {
            text = "偶尔躲一下，再出现"; textSize = 12f; setTextColor(Palette.muted); isChecked = settings.hiding; tag = "hidingSwitch"
            setOnCheckedChangeListener { _, enabled -> settings = settings.copy(hiding = enabled); store.save(settings); refreshPreview() }
        }
        right.addView(hideSwitch, lp(-1, dp(36)))
        space(right, 10)
        right.addView(text("结束后，让它抓一抓真正的玩具。", 11f, Palette.muted).apply { gravity = Gravity.CENTER })

        if (wide) {
            content.addView(left, LinearLayout.LayoutParams(0, -2, 0.9f).apply { marginEnd = dp(28) })
            content.addView(right, LinearLayout.LayoutParams(0, -2, 1.1f))
        } else {
            content.addView(left, lp()); space(content, 20); content.addView(right, lp())
        }
        outer.addView(content); space(outer, 20)
        val footer = row()
        footer.addView(button("猫爪触摸测试") { showTouchTest() }.apply { tag = "touchTestButton" }, LinearLayout.LayoutParams(0, dp(44), 1f))
        footer.addView(button("平板防误触") { pinHelp() }.apply { tag = "pinHelpButton" }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = dp(8) })
        footer.addView(button("使用小贴士") { tips() }.apply { tag = "tipsButton" }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = dp(8) })
        outer.addView(footer); space(outer, 12)
        outer.addView(text("为小爪子设计  ·  无广告  ·  不联网  ·  v1.0", 10f, Palette.muted).apply { gravity = Gravity.CENTER })
        val root = FrameLayout(this)
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        val dock = row().apply {
            setPadding(dp(28), dp(12), dp(28), dp(12)); setBackgroundColor(Palette.cream); elevation = dp(5).toFloat()
        }
        val dockText = column()
        dockText.addView(text("准备好了，就出爪。", 15f, bold = true)); space(dockText, 5)
        dockText.addView(text("平板平放 · 自愿玩耍 · 随时休息", 10f, Palette.muted))
        dock.addView(dockText, LinearLayout.LayoutParams(0, -2, 1f))
        dock.addView(button("开始小小捕猎   →", true) { showStartGuide() }.apply { tag = "startButton" }, lp(dp(if (wide) 260 else 190), dp(52)))
        root.addView(dock, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        ownerWindow(root)
    }

    private fun tint(color: Int, fraction: Float): Int = Color.rgb(
        (255 + (Color.red(color) - 255) * fraction).toInt(),
        (253 + (Color.green(color) - 253) * fraction).toInt(),
        (248 + (Color.blue(color) - 248) * fraction).toInt()
    )
    private fun themeCard(theme: PreyTheme, selected: Boolean, action: () -> Unit): View {
        val card = row().apply {
            tag = "theme_${theme.name}"; contentDescription = "选择${theme.label}"; isClickable = true; isFocusable = true
            setPadding(dp(10), dp(10), dp(8), dp(10))
            background = rounded(if (selected) tint(Palette.accent(theme), 0.11f) else Palette.white, 16, if (selected) Palette.accent(theme) else Palette.line)
            setOnClickListener { action() }
        }
        val icon = object : View(this) {
            val renderer = PreyRenderer()
            val prey = Prey(1, 0f, 0f, dp(48) * 0.27f, 0f, MotionPhase.RESTING, 1f, 0)
            override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) { prey.x = w * 0.5f; prey.y = h * 0.5f }
            override fun onDraw(canvas: Canvas) {
                renderer.prey(canvas, theme, prey)
            }
        }
        card.addView(icon, lp(dp(48), dp(48)))
        val copy = column().apply { setPadding(dp(6), 0, 0, 0) }
        copy.addView(text(theme.label, 13f, bold = true)); space(copy, 6)
        copy.addView(text(theme.description, 9f, Palette.muted))
        card.addView(copy, LinearLayout.LayoutParams(0, -2, 1f)); return card
    }
    private fun segments(labels: List<String>, selected: Int, prefix: String, onSelected: (Int) -> Unit): View {
        val group = row()
        val buttons = mutableListOf<Button>()
        labels.forEachIndexed { index, label ->
            val b = button(label) {
                onSelected(index)
                buttons.forEachIndexed { i, v -> v.background = rounded(if (i == index) Palette.ink else 0xFFF1F1E9.toInt(), 10); v.setTextColor(if (i == index) Palette.white else Palette.muted) }
            }.apply {
                tag = "${prefix}_$index"; textSize = 12f; setPadding(dp(4), 0, dp(4), 0)
                background = rounded(if (index == selected) Palette.ink else 0xFFF1F1E9.toInt(), 10)
                setTextColor(if (index == selected) Palette.white else Palette.muted)
            }
            buttons += b
            group.addView(b, LinearLayout.LayoutParams(0, dp(40), 1f).apply { if (index > 0) marginStart = dp(6) })
        }
        return group
    }

    private fun showStartGuide() {
        ownerDialog = AlertDialog.Builder(this).setTitle("准备好小乐园")
            .setMessage("把平板平放在稳定、防滑的位置，先用低音量。\n\n游戏中：长按左上角约 2 秒，再回答一个小问题，即可暂停或退出。\n\n这次玩 ${settings.minutes} 分钟；猫咪可以随时离开。")
            .setNegativeButton("再调一调") { _, _ -> ownerDialog = null }
            .setPositiveButton("准备好了，开始") { _, _ -> ownerDialog = null; startGame() }
            .setOnCancelListener { ownerDialog = null }.create()
        ownerDialog?.show()
    }

    private fun startGame() {
        store.save(settings); preview?.pause(); preview = null
        page = Page.GAME; recorded = false; needsResume = false
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val root = FrameLayout(this)
        game = GameView(this, GameEngine(settings)).apply {
            tag = "gameCanvas"
            onCapture = { if (settings.sound) audio.play(settings.theme, settings.volume) }
            onFinished = { finishGame() }
            onOwnerRequest = { ownerGate() }
        }
        root.addView(game, FrameLayout.LayoutParams(-1, -1))
        setContentView(root); immersive()
        if (settings.screenPinning) {
            try { startLockTask(); pinStarted = true }
            catch (_: SecurityException) { Toast.makeText(this, "这台设备未允许屏幕固定，请在系统设置中开启。", Toast.LENGTH_LONG).show() }
        }
    }
    private fun immersive() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    private fun ownerGate() {
        if (ownerDialog?.isShowing == true) return
        game?.pause(); audio.pause()
        ownerDialog = AlertDialog.Builder(this).setTitle("主人操作：3 + 4 = ?")
            .setItems(arrayOf("5", "7", "9")) { _, which ->
                ownerDialog = null
                if (which == 1) showPause() else { Toast.makeText(this, "继续陪小爪子玩吧", Toast.LENGTH_SHORT).show(); game?.resume(); immersive() }
            }
            .setOnCancelListener { ownerDialog = null; game?.resume(); immersive() }.create()
        ownerDialog?.show()
    }
    private fun showPause() {
        val engine = game?.engine ?: return
        ownerDialog = AlertDialog.Builder(this).setTitle("小乐园暂停中")
            .setMessage("已捕获 ${engine.captures} 次 · 还剩 ${formatTime(engine.remainingSeconds)}\n\n也可以现在结束，换一个真实的小玩具。")
            .setPositiveButton("继续玩") { _, _ -> ownerDialog = null; needsResume = false; game?.resume(); immersive() }
            .setNegativeButton("结束并查看记录") { _, _ -> ownerDialog = null; finishGame() }
            .setCancelable(false).create()
        ownerDialog?.show()
    }
    private fun finishGame() {
        val engine = game?.engine ?: return
        game?.pause(); audio.pause(); result = engine.result()
        if (!recorded) { store.record(result!!); recorded = true }
        stopPin(); showResult()
    }
    private fun showResult() {
        val r = result ?: return showHome()
        page = Page.RESULT; game?.pause()
        val root = FrameLayout(this)
        val card = column().apply { setPadding(dp(32), dp(28), dp(32), dp(28)); background = rounded(Palette.white, 24, Palette.line); gravity = Gravity.CENTER }
        card.addView(text("小爪子，辛苦啦。", 29f, bold = true)); space(card, 12)
        card.addView(text("${r.theme.habitat}的这一场小小捕猎", 13f, Palette.muted)); space(card, 26)
        card.addView(text(r.captures.toString(), 64f, Palette.orange, true))
        card.addView(text("次成功捕获", 15f, Palette.muted)); space(card, 20)
        card.addView(text("玩耍 ${formatTime(r.elapsedSeconds)}  ·  检测到 ${r.touches} 次落爪", 13f, Palette.muted)); space(card, 20)
        card.addView(text("拿出一个真实的小玩具，\n让它抓住、抱住，好好休息一下。", 15f).apply { gravity = Gravity.CENTER; setLineSpacing(dp(6).toFloat(), 1f) }); space(card, 24)
        card.addView(button("回到小乐园", true) { showHome() }.apply { tag = "resultHome" }, lp(-1, dp(52)))
        root.addView(card, FrameLayout.LayoutParams(dp(480).coerceAtMost(resources.displayMetrics.widthPixels - dp(48)), -2, Gravity.CENTER))
        ownerWindow(root)
    }

    private fun showTouchTest() {
        preview?.pause(); page = Page.TOUCH
        val root = column().apply { setPadding(dp(24), dp(16), dp(24), dp(16)) }
        val header = row()
        header.addView(text("猫爪触摸测试", 23f, bold = true), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(button("返回") { showHome() }.apply { tag = "touchBack" }, lp(dp(86), dp(44)))
        root.addView(header); space(root, 12)
        root.addView(text("先用手指确认，再让猫自愿拍拍。出现圆圈和轨迹，表示平板收到了触摸。", 13f, Palette.muted)); space(root, 12)
        val reading = text("落爪 0 次 · 当前触点 0 个 · 中心命中 0 次", 13f)
        val test = TouchTestView(this).apply { background = rounded(Palette.line); clipToOutline = true }
        test.onReading = { total, active -> reading.text = "落爪 $total 次 · 当前触点 $active 个 · 中心命中 ${test.targetHits} 次" }
        root.addView(test, LinearLayout.LayoutParams(-1, 0, 1f)); space(root, 12)
        val foot = row(); foot.addView(reading, LinearLayout.LayoutParams(0, -2, 1f))
        foot.addView(button("清空") { test.clear() }.apply { tag = "touchClear" }, lp(dp(80), dp(42))); root.addView(foot)
        ownerWindow(root)
    }
    private fun pinHelp() {
        val toggle = CheckBox(this).apply { text = "开始游戏时请求屏幕固定"; isChecked = settings.screenPinning; setPadding(dp(20), dp(10), dp(20), dp(10)) }
        AlertDialog.Builder(this).setTitle("平板防误触")
            .setMessage("全屏会隐藏系统栏；Android / HyperOS 的边缘手势仍可能唤出系统导航。\n\n开启“屏幕固定”后，开始游戏时按系统提示确认。部分机型需要先到设置中搜索“屏幕固定”或“固定应用”。\n\n系统解除方式以本机提示为准。游戏内退出：长按左上角约 2 秒，再回答小问题。")
            .setView(toggle).setPositiveButton("保存") { _, _ -> settings = settings.copy(screenPinning = toggle.isChecked); store.save(settings) }
            .setNegativeButton("取消", null).show()
    }
    private fun tips() {
        AlertDialog.Builder(this).setTitle("陪小爪子玩的小贴士")
            .setMessage("• 平板平放、防滑，使用保护壳和屏幕保护。\n• 先从 1 个大目标、慢速和静音开始。\n• 猫盯着看也可能在观察，不必催它出爪。\n• 猫咪可以自由离开，不要追着它播放。\n• 出现躲避、紧张或反复咬抓设备时，结束这一场。\n• 屏幕缺少真实触感，结束后接一小段实体玩具互动。\n\n本应用不联网、不收集照片和声音，记录只保存在这台设备上。")
            .setPositiveButton("知道啦", null).show()
    }
    private fun stopPin() {
        if (pinStarted) { try { stopLockTask() } catch (_: SecurityException) {} ; pinStarted = false }
    }
    private fun formatTime(seconds: Float): String { val s = seconds.toInt().coerceAtLeast(0); return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60) }

    private fun handleBack() {
        when (page) {
            Page.GAME -> ownerGate()
            Page.TOUCH, Page.RESULT -> showHome()
            Page.HOME -> finish()
        }
    }
    override fun onPause() {
        super.onPause(); game?.pause(); preview?.pause(); audio.pause()
        if (page == Page.GAME) needsResume = true
    }
    override fun onResume() {
        super.onResume()
        if (page == Page.GAME) {
            immersive()
            if (needsResume && ownerDialog?.isShowing != true) showPause()
            else if (ownerDialog?.isShowing != true) game?.resume()
        } else preview?.resume()
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        when (page) { Page.HOME -> showHome(); Page.TOUCH -> showTouchTest(); Page.RESULT -> showResult(); Page.GAME -> immersive() }
    }
    override fun onDestroy() {
        ownerDialog?.dismiss(); preview?.pause(); game?.pause(); audio.release(); stopPin(); super.onDestroy()
    }
}
