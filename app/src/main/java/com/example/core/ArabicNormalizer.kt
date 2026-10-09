package com.example.core

object ArabicNormalizer {
    private val ALEF_VARIANTS_REGEX = Regex("[أإآٱ]")
    private val TASHKEEL_REGEX = Regex("[\\u064B-\\u065F\\u06D6-\\u06ED]")
    private val TATWEEL_AND_SYMBOLS_REGEX = Regex("[ـ\\u0610-\\u061A\\uFD3E\\uFD3F\\u200E\\u200F]")
    private val PUNCTUATION_REGEX = Regex("[،؟!.«»\"'()\\[\\]:;\\-_/\\\\<>~*#\\{\\}]+")
    private val WHITESPACE_REGEX = Regex("\\s+")

    fun normalize(input: String): String {
        val preprocessed = input
            .replace("\ufeff", "")
            // Convert Quranic dagger alif to real alif so words like العالمين and مالك and الصراط match
            .replace("\u0670", "ا")
            .replace(TASHKEEL_REGEX, "")
            .replace(ALEF_VARIANTS_REGEX, "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
            .replace("ئ", "ي")
            .replace("ؤ", "و")
            .replace("ء", "")
            .replace(TATWEEL_AND_SYMBOLS_REGEX, "")
            .replace(PUNCTUATION_REGEX, " ")
            .replace(WHITESPACE_REGEX, " ")
            .trim()

        val words = preprocessed.split(" ")
        val normalizedWords = words.map { w ->
            when (w) {
                "هاذا" -> "هذا"
                "هاذه" -> "هذه"
                "هاولاء" -> "هولاء"
                "ذالك" -> "ذلك"
                "الرحمان" -> "الرحمن"
                "الاه" -> "اله"
                "صلوة" -> "صلاه"
                "الصلوة" -> "الصلاه"
                "زكوة" -> "زكاه"
                "الزكوة" -> "الزكاه"
                "حيوة" -> "حياه"
                "الحيوة" -> "الحياه"
                "ربوا" -> "ربا"
                "الربوا" -> "الربا"
                else -> {
                    if (w.startsWith("والاه")) "واله"
                    else if (w.startsWith("فالاه")) "فاله"
                    else w
                }
            }
        }

        return normalizedWords.joinToString(" ")
    }

    /**
     * Converts integer to Arabic-Indic digits (e.g. 1 -> ١, 255 -> ٢٥٥)
     */
    fun toArabicDigits(number: Int): String {
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        val sb = StringBuilder()
        val str = number.toString()
        for (ch in str) {
            if (ch in '0'..'9') {
                sb.append(arabicDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}
