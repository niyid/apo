package com.techducat.apo.utils

import java.util.UUID

fun generateMockSubaddress(): String {
    return "8" + UUID.randomUUID().toString().replace("-", "").take(94)
}
