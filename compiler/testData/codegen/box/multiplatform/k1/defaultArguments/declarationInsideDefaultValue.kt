// LANGUAGE: +MultiPlatformProjects
// OPT_IN: kotlin.ExperimentalMultiplatform

// FILE: common.kt

expect class Foo {
    fun foo(
        x: () -> String = {
            object {
                fun bar(d: String) = d
            }.bar("OK")
        },
    ): String
}

// FILE: actual.kt

actual class Foo {
    actual fun foo(x: () -> String) = x()
}

fun box(): String {
    return Foo().foo()
}
