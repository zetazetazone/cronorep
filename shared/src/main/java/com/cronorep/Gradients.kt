package com.cronorep

/** A still background. [top] and [bottom] are ARGB colours, for example 0xFFF97316. */
data class Gradient(val name: String, val top: Long, val bottom: Long)

object Gradients {
    val ALL = listOf(
        Gradient("Ember", 0xFFF97316, 0xFF7F1D1D),
        Gradient("Ocean", 0xFF06B6D4, 0xFF1E3A8A),
        Gradient("Forest", 0xFF4ADE80, 0xFF064E3B),
        Gradient("Violet", 0xFFA855F7, 0xFF312E81),
        Gradient("Steel", 0xFF94A3B8, 0xFF0F172A),
        Gradient("Sunset", 0xFFF472B6, 0xFF9A3412),
    )
}
