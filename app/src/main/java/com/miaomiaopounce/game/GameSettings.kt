package com.miaomiaopounce.game

enum class PreyTheme(val label: String, val description: String, val habitat: String) {
    BUG("追追小虫", "轻轻停下，忽然飞走", "午后花园"),
    FISH("碰碰小鱼", "摇摇尾巴，绕个小弯", "浅浅池塘"),
    MOUSE("扑扑小鼠", "探个脑袋，跑跑藏藏", "林间小径"),
    DOT("拍拍色块", "简单明亮，跳跃有趣", "彩色游乐场")
}

enum class Pace(val label: String, val multiplier: Float) {
    GENTLE("慢悠悠", 0.65f), NORMAL("刚刚好", 1f), LIVELY("活泼点", 1.5f)
}

enum class PreySize(val label: String, val fraction: Float) {
    SMALL("小", 0.038f), MEDIUM("中", 0.052f), LARGE("大", 0.070f)
}

data class GameSettings(
    val theme: PreyTheme = PreyTheme.BUG,
    val count: Int = 3,
    val pace: Pace = Pace.NORMAL,
    val size: PreySize = PreySize.MEDIUM,
    val sound: Boolean = true,
    val volume: Int = 25,
    val minutes: Int = 3,
    val hiding: Boolean = true,
    val screenPinning: Boolean = false
) {
    fun sanitized() = copy(count = count.coerceIn(1, 8), volume = volume.coerceIn(0, 60), minutes = minutes.coerceIn(1, 10))
}
