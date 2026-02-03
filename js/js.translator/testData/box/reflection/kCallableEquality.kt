// WITH_STDLIB

fun checkEqual(x: Any, y: Any) {
    if (x != y || y != x) throw AssertionError("$x and $y should be equal")
    if (x.hashCode() != y.hashCode()) throw AssertionError("$x and $y should have the same hash code")
}

fun checkNotEqual(x: Any, y: Any) {
    if (x == y || y == x) throw AssertionError("$x and $y should NOT be equal")
}

fun topLevelFun1() = 123

fun topLevelOverloaded(x: Int) = x
fun topLevelOverloaded(x: String) = x

fun captureInt(f: (Int) -> Int): Any = f
fun captureString(f: (String) -> String): Any = f

suspend fun topLevelSuspendFun1(p: String) {
    println(p)
    topLevelSuspendFun2(p)
    println(p)
}

suspend fun topLevelSuspendFun2(p: String) {
    println(p)
}

class Foo {
    fun memberFun() {
        println("Hello, world!")
    }

    suspend fun memberSuspendFun(p: String) {
        println(p)
        topLevelSuspendFun2(p)
        println(p)
    }
}

fun Foo.topLevelExtension(): Unit {}

val refInVariable = ::topLevelFun1

fun box(): String {
    val foo = Foo()
    val bar = Foo()

    // Reference identity
    assertFalse(::topLevelFun1 === ::topLevelFun1)

    // Saved reference in variable
    checkEqual(refInVariable, ::topLevelFun1)

    // Top-level suspend function
    checkEqual(::topLevelSuspendFun1, ::topLevelSuspendFun1)
    checkNotEqual(::topLevelSuspendFun1, ::topLevelSuspendFun2)

    // Overloaded functions
    checkNotEqual(captureInt(::topLevelOverloaded), captureString(::topLevelOverloaded))
    checkEqual(captureInt(::topLevelOverloaded), captureInt(::topLevelOverloaded))

    // Top-level extension function equality
    checkEqual(Foo::topLevelExtension, Foo::topLevelExtension)
    checkEqual(foo::topLevelExtension, foo::topLevelExtension)
    checkNotEqual(foo::topLevelExtension, bar::topLevelExtension)
    checkNotEqual(foo::topLevelExtension, Foo::topLevelExtension)

    // Bound suspend member function equality/inequality
    checkEqual(foo::memberSuspendFun, foo::memberSuspendFun)
    checkNotEqual(foo::memberSuspendFun, bar::memberSuspendFun)
    checkNotEqual(foo::memberSuspendFun, Foo::memberSuspendFun)

    // Lambda non-equality
    checkNotEqual({ 1 }, { 1 })
    assertFalse({ 1 } === { 1 })

    // Null and non-callable equality
    assertFalse(::topLevelFun1.equals(null))
    assertFalse(::topLevelFun1.equals("not a callable"))
    assertFalse(::topLevelFun1.equals(42))
    assertFalse(Foo::memberFun.equals(null))

    // hashCode stability
    val ref = ::topLevelFun1
    assertTrue(ref.hashCode() == ref.hashCode())

    return "OK"
}
