// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
fun asCall() {
    1 <!USELESS_CAST!>as Int<!>
    1 <!CAST_NEVER_SUCCEEDS_ERROR!>as<!> Byte
    1 <!CAST_NEVER_SUCCEEDS_ERROR!>as<!> Short
    1 <!CAST_NEVER_SUCCEEDS_ERROR!>as<!> Long
    1 <!CAST_NEVER_SUCCEEDS_ERROR!>as<!> Char
    1 <!CAST_NEVER_SUCCEEDS_ERROR!>as<!> Double
    1 <!CAST_NEVER_SUCCEEDS_ERROR!>as<!> Float

    1.0 <!CAST_NEVER_SUCCEEDS_ERROR!>as<!> Int
    1.0 <!CAST_NEVER_SUCCEEDS_ERROR!>as<!> Byte
    1.0 <!CAST_NEVER_SUCCEEDS_ERROR!>as<!> Short
    1.0 <!CAST_NEVER_SUCCEEDS_ERROR!>as<!> Long
    1.0 <!CAST_NEVER_SUCCEEDS_ERROR!>as<!> Char
    1.0 <!USELESS_CAST!>as Double<!>
    1.0 <!CAST_NEVER_SUCCEEDS_ERROR!>as<!> Float

    1f <!CAST_NEVER_SUCCEEDS_ERROR!>as<!> Int
    1f <!CAST_NEVER_SUCCEEDS_ERROR!>as<!> Byte
    1f <!CAST_NEVER_SUCCEEDS_ERROR!>as<!> Short
    1f <!CAST_NEVER_SUCCEEDS_ERROR!>as<!> Long
    1f <!CAST_NEVER_SUCCEEDS_ERROR!>as<!> Char
    1f <!CAST_NEVER_SUCCEEDS_ERROR!>as<!> Double
    1f <!USELESS_CAST!>as Float<!>
}

fun asSafe() {
    1 <!USELESS_CAST!>as? Int<!>
    1 <!CAST_NEVER_SUCCEEDS_ERROR!>as?<!> Byte
    1 <!CAST_NEVER_SUCCEEDS_ERROR!>as?<!> Short
    1 <!CAST_NEVER_SUCCEEDS_ERROR!>as?<!> Long
    1 <!CAST_NEVER_SUCCEEDS_ERROR!>as?<!> Char
    1 <!CAST_NEVER_SUCCEEDS_ERROR!>as?<!> Double
    1 <!CAST_NEVER_SUCCEEDS_ERROR!>as?<!> Float

    1.0 <!CAST_NEVER_SUCCEEDS_ERROR!>as?<!> Int
    1.0 <!CAST_NEVER_SUCCEEDS_ERROR!>as?<!> Byte
    1.0 <!CAST_NEVER_SUCCEEDS_ERROR!>as?<!> Short
    1.0 <!CAST_NEVER_SUCCEEDS_ERROR!>as?<!> Long
    1.0 <!CAST_NEVER_SUCCEEDS_ERROR!>as?<!> Char
    1.0 <!USELESS_CAST!>as? Double<!>
    1.0 <!CAST_NEVER_SUCCEEDS_ERROR!>as?<!> Float

    1f <!CAST_NEVER_SUCCEEDS_ERROR!>as?<!> Int
    1f <!CAST_NEVER_SUCCEEDS_ERROR!>as?<!> Byte
    1f <!CAST_NEVER_SUCCEEDS_ERROR!>as?<!> Short
    1f <!CAST_NEVER_SUCCEEDS_ERROR!>as?<!> Long
    1f <!CAST_NEVER_SUCCEEDS_ERROR!>as?<!> Char
    1f <!CAST_NEVER_SUCCEEDS_ERROR!>as?<!> Double
    1f <!USELESS_CAST!>as? Float<!>
}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, integerLiteral */
