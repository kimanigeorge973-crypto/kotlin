// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/J.java
package test;

public class J {
    @Anno("sf")
    public static final String sf = "";
    @Anno("s")
    public static String s = "";
    @Anno("f")
    public final String f = "";
    @Anno("p")
    public String p = "";
}

// FILE: box.kt
package test

import kotlin.test.assertEquals

annotation class Anno(val value: String)

fun box(): String {
    assertEquals("[@test.Anno(value=sf)]", J::sf.annotations.toString())
    assertEquals("[@test.Anno(value=s)]", J::s.annotations.toString())
    assertEquals("[@test.Anno(value=f)]", J::f.annotations.toString())
    assertEquals("[@test.Anno(value=p)]", J::p.annotations.toString())

    assertEquals(emptyList(), J::sf.getter.annotations)
    assertEquals(emptyList(), J::s.getter.annotations)
    assertEquals(emptyList(), J::f.getter.annotations)
    assertEquals(emptyList(), J::p.getter.annotations)

    assertEquals(emptyList(), J::s.setter.annotations)
    assertEquals(emptyList(), J::p.setter.annotations)

    assertEquals(emptyList(), J::s.setter.parameters.last().annotations)
    assertEquals(emptyList(), J::p.setter.parameters.last().annotations)

    return "OK"
}
