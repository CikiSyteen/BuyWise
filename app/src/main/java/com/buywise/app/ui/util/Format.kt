package com.buywise.app.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 金额格式化：1234.5 -> 1,234.50 */
fun formatMoney(value: Double): String =
    String.format(Locale.CHINA, "%,.2f", value)

/** 时间戳格式化：1728000000000 -> 2026-09-04 16:21 */
fun formatDateTime(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(millis))

/** 数字格式化：整数不带小数点 */
fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString()
    else String.format(Locale.CHINA, "%.2f", value)

/** 时长格式化：2.75 -> 2.8 */
fun formatHours(value: Double): String =
    String.format(Locale.CHINA, "%.1f", value)

/** 评分格式化：7.0 -> 7 */
fun formatScore(value: Double): String =
    String.format(Locale.CHINA, "%.1f", value)

/**
 * 过滤输入框内容，只保留数字与最多一个小数点；
 * 以小数点开头时自动补 0，避免出现 ".5" 这种无法解析的文本。
 */
fun sanitizeDecimal(input: String): String {
    var result = input.filter { it.isDigit() || it == '.' }
    val firstDot = result.indexOf('.')
    if (firstDot >= 0) {
        result = result.substring(0, firstDot + 1) +
            result.substring(firstDot + 1).replace(".", "")
    }
    if (result.startsWith(".")) result = "0$result"
    return result
}
