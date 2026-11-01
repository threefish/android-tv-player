package com.tvplayer.core.util

import com.tvplayer.core.model.FileInfo
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/**
 * 文件名智能排序：目录优先，数字段按数值比较，中文数字（第一集、第十二集）按数值参与比较，
 * 其余文本按中文拼音比较。
 */
object SmartFileNameComparator : Comparator<FileInfo> {

    override fun compare(a: FileInfo, b: FileInfo): Int {
        if (a.isDirectory != b.isDirectory) return if (a.isDirectory) -1 else 1
        val byName = compareNames(a.name, b.name)
        return if (byName != 0) byName else a.path.compareTo(b.path)
    }

    fun compareNames(a: String, b: String): Int {
        val left = tokenize(a)
        val right = tokenize(b)
        val common = minOf(left.size, right.size)
        for (i in 0 until common) {
            val result = compareToken(left[i], right[i])
            if (result != 0) return result
        }
        if (left.size != right.size) return left.size.compareTo(right.size)
        // 01 与 1 这类数值相同的文件名按原串兜底，保证顺序稳定
        return a.compareTo(b)
    }

    private fun compareToken(a: Token, b: Token): Int = when {
        a is Token.Number && b is Token.Number -> a.value.compareTo(b.value)
        a is Token.Number -> -1
        b is Token.Number -> 1
        else -> compareText((a as Token.Text).value, (b as Token.Text).value)
    }

    private fun compareText(a: String, b: String): Int {
        val comparator = collator.get() ?: return a.compareTo(b)
        return comparator.compare(a, b)
    }

    private fun tokenize(raw: String): List<Token> {
        val name = Normalizer.normalize(raw, Normalizer.Form.NFKC)
        val tokens = ArrayList<Token>()
        var i = 0
        while (i < name.length) {
            val c = name[i]
            when {
                c in '0'..'9' -> {
                    var value = 0L
                    while (i < name.length && name[i] in '0'..'9') {
                        value = (value * 10 + (name[i] - '0')).coerceAtMost(NUMBER_CAP)
                        i++
                    }
                    tokens.add(Token.Number(value))
                }
                isChineseNumeral(c) -> {
                    val start = i
                    while (i < name.length && isChineseNumeral(name[i])) i++
                    tokens.add(Token.Number(parseChineseNumeral(name.substring(start, i))))
                }
                else -> {
                    val start = i
                    while (i < name.length && name[i] !in '0'..'9' && !isChineseNumeral(name[i])) i++
                    tokens.add(Token.Text(name.substring(start, i)))
                }
            }
        }
        return tokens
    }

    private fun parseChineseNumeral(text: String): Long {
        if (text.none { it in UNIT_CHARS }) {
            var value = 0L
            for (c in text) {
                value = (value * 10 + (chineseDigit(c) ?: continue)).coerceAtMost(NUMBER_CAP)
            }
            return value
        }

        var total = 0L
        var section = 0L
        var digit = 0L
        for (c in text) {
            when (c) {
                '十' -> {
                    section += (if (digit == 0L) 1 else digit) * 10
                    digit = 0
                }
                '百' -> {
                    section += (if (digit == 0L) 1 else digit) * 100
                    digit = 0
                }
                '千' -> {
                    section += (if (digit == 0L) 1 else digit) * 1000
                    digit = 0
                }
                '万' -> {
                    total += (section + digit) * 10_000
                    section = 0
                    digit = 0
                }
                '亿' -> {
                    total = (total + section + digit) * 100_000_000
                    section = 0
                    digit = 0
                }
                else -> digit = chineseDigit(c) ?: digit
            }
        }
        return (total + section + digit).coerceAtMost(NUMBER_CAP)
    }

    private fun isChineseNumeral(c: Char): Boolean = c in CHINESE_NUMERAL_CHARS

    private fun chineseDigit(c: Char): Long? = when (c) {
        '零', '〇' -> 0
        '一' -> 1
        '二', '两' -> 2
        '三' -> 3
        '四' -> 4
        '五' -> 5
        '六' -> 6
        '七' -> 7
        '八' -> 8
        '九' -> 9
        else -> null
    }

    private sealed class Token {
        class Number(val value: Long) : Token()
        class Text(val value: String) : Token()
    }

    private val collator = object : ThreadLocal<Collator>() {
        override fun initialValue(): Collator = Collator.getInstance(Locale.CHINA).apply {
            strength = Collator.PRIMARY
        }
    }

    private const val NUMBER_CAP = Long.MAX_VALUE / 100
    private const val UNIT_CHARS = "十百千万亿"
    private const val CHINESE_NUMERAL_CHARS = "零〇一二三四五六七八九十百千万亿两"
}
