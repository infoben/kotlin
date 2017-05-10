/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

class DeepCopySymbolsRemapper : IrElementVisitorVoid {
    private val classes = hashMapOf<IrClassSymbol, IrClassSymbol>()
    private val constructors = hashMapOf<IrConstructorSymbol, IrConstructorSymbol>()
    private val enumEntries = hashMapOf<IrEnumEntrySymbol, IrEnumEntrySymbol>()
    private val externalPackageFragments = hashMapOf<IrExternalPackageFragmentSymbol, IrExternalPackageFragmentSymbol>()
    private val fields = hashMapOf<IrFieldSymbol, IrFieldSymbol>()
    private val files = hashMapOf<IrFileSymbol, IrFileSymbol>()
    private val functions = hashMapOf<IrSimpleFunctionSymbol, IrSimpleFunctionSymbol>()
    private val typeParameters = hashMapOf<IrTypeParameterSymbol, IrTypeParameterSymbol>()
    private val valueParameters = hashMapOf<IrValueParameterSymbol, IrValueParameterSymbol>()
    private val variables = hashMapOf<IrVariableSymbol, IrVariableSymbol>()

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    private inline fun <D : DeclarationDescriptor, B : IrSymbolOwner, reified S : IrBindableSymbol<D, B>>
            remapSymbol(map: MutableMap<S, S>, owner: B, createNewSymbol: (S) -> S) {
        val symbol = owner.symbol as S
        map[symbol] = createNewSymbol(symbol)
    }

    override fun visitClass(declaration: IrClass) {
        remapSymbol(classes, declaration) { IrClassSymbolImpl(it.descriptor) }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitConstructor(declaration: IrConstructor) {
        remapSymbol(constructors, declaration) { IrConstructorSymbolImpl(it.descriptor) }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitEnumEntry(declaration: IrEnumEntry) {
        remapSymbol(enumEntries, declaration) { IrEnumEntrySymbolImpl(it.descriptor) }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment) {
        remapSymbol(externalPackageFragments, declaration) { IrExternalPackageFragmentSymbolImpl(it.descriptor) }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitField(declaration: IrField) {
        remapSymbol(fields, declaration) { IrFieldSymbolImpl(it.descriptor) }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitFile(declaration: IrFile) {
        remapSymbol(files, declaration) { IrFileSymbolImpl(it.descriptor) }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        remapSymbol(functions, declaration) { IrSimpleFunctionSymbolImpl(it.descriptor) }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitTypeParameter(declaration: IrTypeParameter) {
        remapSymbol(typeParameters, declaration) { IrTypeParameterSymbolImpl(it.descriptor) }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitValueParameter(declaration: IrValueParameter) {
        remapSymbol(valueParameters, declaration) { IrValueParameterSymbolImpl(it.descriptor) }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitVariable(declaration: IrVariable) {
        remapSymbol(variables, declaration) { IrVariableSymbolImpl(it.descriptor) }
        declaration.acceptChildrenVoid(this)
    }

    private fun <T : IrSymbol> Map<T, T>.getDeclared(symbol: T) =
            getOrElse(symbol) {
                throw IllegalArgumentException("Non-remapped symbol $symbol ${symbol.descriptor}")
            }

    private fun <T : IrSymbol> Map<T, T>.getReferenced(symbol: T) =
            getOrElse(symbol) { symbol }

    fun getDeclaredClass(symbol: IrClassSymbol): IrClassSymbol = classes.getDeclared(symbol)
    fun getDeclaredFunction(symbol: IrSimpleFunctionSymbol): IrSimpleFunctionSymbol = functions.getDeclared(symbol)
    fun getDeclaredField(symbol: IrFieldSymbol): IrFieldSymbol = fields.getDeclared(symbol)
    fun getDeclaredFile(symbol: IrFileSymbol): IrFileSymbol = files.getDeclared(symbol)
    fun getDeclaredConstructor(symbol: IrConstructorSymbol): IrConstructorSymbol = constructors.getDeclared(symbol)
    fun getDeclaredEnumEntry(symbol: IrEnumEntrySymbol): IrEnumEntrySymbol = enumEntries.getDeclared(symbol)
    fun getDeclaredExternalPackageFragment(symbol: IrExternalPackageFragmentSymbol): IrExternalPackageFragmentSymbol = externalPackageFragments.getDeclared(symbol)
    fun getDeclaredVariable(symbol: IrVariableSymbol): IrVariableSymbol = variables.getDeclared(symbol)
    fun getDeclaredTypeParameter(symbol: IrTypeParameterSymbol): IrTypeParameterSymbol = typeParameters.getDeclared(symbol)
    fun getDeclaredValueParameter(symbol: IrValueParameterSymbol): IrValueParameterSymbol = valueParameters.getDeclared(symbol)

    fun getReferencedClass(symbol: IrClassSymbol): IrClassSymbol = classes.getReferenced(symbol)
    fun getReferencedClassOrNull(symbol: IrClassSymbol?): IrClassSymbol? = symbol?.let { classes.getReferenced(it) }
    fun getReferencedEnumEntry(symbol: IrEnumEntrySymbol): IrEnumEntrySymbol = enumEntries.getReferenced(symbol)
    fun getReferencedVariable(symbol: IrVariableSymbol): IrVariableSymbol = variables.getReferenced(symbol)
    fun getReferencedField(symbol: IrFieldSymbol): IrFieldSymbol = fields.getReferenced(symbol)
    fun getReferencedConstructor(symbol: IrConstructorSymbol): IrConstructorSymbol = constructors.getReferenced(symbol)

    fun getReferencedValue(symbol: IrValueSymbol): IrValueSymbol =
            when (symbol) {
                is IrValueParameterSymbol -> valueParameters.getReferenced(symbol)
                is IrVariableSymbol -> variables.getReferenced(symbol)
                else -> throw IllegalArgumentException("Unexpected symbol $symbol ${symbol.descriptor}")
            }

    fun getReferencedFunction(symbol: IrFunctionSymbol): IrFunctionSymbol =
            when (symbol) {
                is IrSimpleFunctionSymbol -> functions.getReferenced(symbol)
                is IrConstructorSymbol -> constructors.getReferenced(symbol)
                else -> throw IllegalArgumentException("Unexpected symbol $symbol ${symbol.descriptor}")
            }

    fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol =
            when (symbol) {
                is IrClassSymbol -> classes.getReferenced(symbol)
                is IrTypeParameterSymbol -> typeParameters.getReferenced(symbol)
                else -> throw IllegalArgumentException("Unexpected symbol $symbol ${symbol.descriptor}")
            }
}