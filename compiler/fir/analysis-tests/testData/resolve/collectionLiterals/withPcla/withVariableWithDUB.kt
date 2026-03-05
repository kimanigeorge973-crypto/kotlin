// LANGUAGE: +CollectionLiterals
// WITH_STDLIB
// RUN_PIPELINE_TILL: FRONTEND

interface Box<T> {
    var x: T
}

fun <Z: Set<Int>> buildBox(block: Box<Z>.() -> Unit): Box<Z> = TODO()

fun test() {
    buildBox {
        x = []
    }

    buildBox {
        x = [<!ARGUMENT_TYPE_MISMATCH!>"!"<!>]
    }

    buildBox {
        x = [42]
    }
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral,
nullableType, propertyDeclaration, stringLiteral, typeConstraint, typeParameter, typeWithExtension */
