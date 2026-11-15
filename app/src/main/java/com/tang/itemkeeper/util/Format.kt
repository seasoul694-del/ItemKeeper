package com.tang.itemkeeper.util

import java.util.Locale

/** 统一金额格式：千分位 + 两位小数 */
fun money(value: Double): String = String.format(Locale.CHINA, "%,.2f", value)
