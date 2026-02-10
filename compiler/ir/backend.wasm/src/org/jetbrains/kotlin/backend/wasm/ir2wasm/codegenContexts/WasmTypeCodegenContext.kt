/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.utils.redefinitionError
import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.wasm.ir.WasmFunctionType
import org.jetbrains.kotlin.wasm.ir.WasmStructDeclaration
import org.jetbrains.kotlin.wasm.ir.WasmTypeDeclaration

open class WasmTypeCodegenContext(
    private val wasmFileFragment: WasmCompiledTypesFileFragment,
    idSignatureRetriever: IdSignatureRetriever,
) : WasmCodegenContext(idSignatureRetriever) {

    fun getDeclarationTag(declaration: IrDeclaration): String =
        declaration.symbol.getReferenceKey().toString()

    private fun getFunctionTypeSignature(wasmFunctionType: WasmFunctionType): IdSignature {
        val params = wasmFunctionType.parameterTypes.joinToString("_")
        val results = wasmFunctionType.resultTypes.joinToString("_")
        return IdSignature.CommonSignature("__SYNTHETIC__", "wasm_func_type_\$${params}_\$${results}", null, 0, null)
    }

    fun defineGcType(irClass: IrClassSymbol, wasmType: WasmTypeDeclaration) {
        if (wasmFileFragment.definedGcTypes.put(irClass.getReferenceKey(), wasmType) != null) {
            redefinitionError(irClass.getReferenceKey(), "GcTypes")
        }
    }

    fun defineVTableGcType(irClass: IrClassSymbol, wasmType: WasmStructDeclaration) {
        if (wasmFileFragment.definedVTableGcTypes.put(irClass.getReferenceKey(), wasmType) != null) {
            redefinitionError(irClass.getReferenceKey(), "VTableGcTypes")
        }
    }

    fun defineFunctionType(irFunction: IrFunctionSymbol, wasmFunctionType: WasmFunctionType) {
        if (wasmFileFragment.definedFunctionTypes.put(irFunction.getReferenceKey(), wasmFunctionType) != null) {
            redefinitionError(irFunction.getReferenceKey(), "FunctionTypes")
        }
    }

    fun referenceWasmFunctionType(wasmFunctionType: WasmFunctionType): FunctionTypeSymbol {
        val signature = getFunctionTypeSignature(wasmFunctionType)
        wasmFileFragment.definedFunctionTypes.putIfAbsent(signature, wasmFunctionType)
        return FunctionTypeSymbol(signature)
    }

    fun referenceWasmFunctionHeapType(wasmFunctionType: WasmFunctionType): FunctionHeapTypeSymbol {
        val signature = getFunctionTypeSignature(wasmFunctionType)
        wasmFileFragment.definedFunctionTypes.putIfAbsent(signature, wasmFunctionType)
        return FunctionHeapTypeSymbol(signature)
    }

    open fun referenceGcType(irClass: IrClassSymbol): GcTypeSymbol =
        GcTypeSymbol(irClass.getReferenceKey())

    open fun referenceHeapType(irClass: IrClassSymbol): GcHeapTypeSymbol =
        GcHeapTypeSymbol(irClass.getReferenceKey())

    open fun referenceVTableGcType(irClass: IrClassSymbol): VTableTypeSymbol =
        VTableTypeSymbol(irClass.getReferenceKey())

    open fun referenceVTableHeapType(irClass: IrClassSymbol): VTableHeapTypeSymbol =
        VTableHeapTypeSymbol(irClass.getReferenceKey())

    open fun referenceFunctionType(irClass: IrFunctionSymbol): FunctionTypeSymbol =
        FunctionTypeSymbol(irClass.getReferenceKey())

    open fun referenceFunctionHeapType(irClass: IrFunctionSymbol): FunctionHeapTypeSymbol =
        FunctionHeapTypeSymbol(irClass.getReferenceKey())
}
