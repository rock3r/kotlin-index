package com.kotlincodeindex.application.selectioncontext.model

data class SelectionContainerInfo(val file: String, val line: Int, val function: String)

data class DisableSelectionInfo(val file: String, val line: Int, val function: String)

data class SelectionContext(
    val callee: String,
    val inSelectionContainer: Boolean,
    val selectionContainerCount: Int,
    val excludedByDisableSelection: Boolean,
    val selectionContainers: List<SelectionContainerInfo>,
    val disableSelection: DisableSelectionInfo? = null,
    val confidence: String = "lexical",
)
