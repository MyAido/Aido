package com.rr.aido.data.models

data class BackupData(
    val settings: Settings,
    val preprompts: List<Preprompt>,
    val textShortcuts: List<TextShortcut>? = emptyList(),
    val disabledApps: Set<String>? = emptySet()
)
