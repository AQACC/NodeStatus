package com.aqa.cc.nodestatus.adapter.testkit

data class FixtureDescriptor(
    val path: String,
    val description: String,
    val sanitized: Boolean,
)

fun FixtureDescriptor.isUsable(): Boolean = sanitized && path.isNotBlank()
