// RENDER_IR_DIAGNOSTICS_FULL_TEXT

// MODULE: lib
// FILE: A.kt

import kotlinx.powerassert.*

@PowerAssert
fun describe(value: Any): String? {
    return PowerAssert.explanation?.toDefaultMessage()
}

// MODULE: main(lib)
// DISABLE_RUNTIME
// FILE: B.kt

fun box(): String {
    val reallyLongList = listOf("a", "b")
    return describe(reallyLongList.reversed() == emptyList<String>()) ?: "FAIL"
}
