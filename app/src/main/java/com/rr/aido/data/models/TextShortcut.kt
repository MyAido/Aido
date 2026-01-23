package com.rr.aido.data.models

import java.util.UUID

data class TextShortcut(
    val trigger: String,
    val replacement: String,
    val id: String = UUID.randomUUID().toString()
)
