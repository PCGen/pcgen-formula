/*
 * Copyright 2014 (C) Tom Parker <thpr@users.sourceforge.net>
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package pcgen.base.formula.inst;

import pcgen.base.formula.base.DependencyManager;
import pcgen.base.formula.base.FormulaManager;
import pcgen.base.formula.base.FormulaSemantics;
import pcgen.base.formula.base.LegalScope;
import pcgen.base.util.FormatManager;

/**
 * A NEPFormula is a formula that is part of the "Native Equation Parser" for
 * PCGen.
 * 
 * @param <T>
 *            The Class of object returned by this NEPFormula
 */
public interface NEPFormula<T>
{

	/**
	 * Resolves the NEPFormula in the context of the given ScopeInformation. The
	 * given ScopeInformation must contain information about variable values,
	 * available functions, and other characteristics required for the formula
	 * to produce a value.
	 * 
	 * If variables and formulas required by the NEPFormula are not available in
	 * the given ScopeInformation, behavior is not guaranteed and NEPFormula or
	 * other methods called within this method reserve the right to throw an
	 * Exception or otherwise not fail gracefully. (The precise behavior is
	 * likely defined by the ScopeInformation).
	 * 
	 * Note in the case of a valid formula that the format (but not the exact
	 * class) of the return value is guaranteed by the NEPFormula. The Class may
	 * extend the format contained by the NEPFormula. The exact class returned
	 * is defined by the ScopeInformation, which can therefore implement the
	 * appropriate processing (precision in the case of numbers) desired for the
	 * given calculation.
	 * 
	 * @param scopeInfo
	 *            The ScopeInformation providing the context in which the
	 *            NEPFormula is to be resolved
	 * @param assertedFormat
	 *            The Class indicating the asserted Format for the formula. This
	 *            parameter is optional - null can indicate that there is no
	 *            format asserted by the context of the formula
	 * @param source
	 *            The source of the resolution being performed, so it can be
	 *            referred back to if necessary
	 * @return The value calculated for the NEPFormula
	 * @throws IllegalArgumentException
	 *             if the given ScopeInformation is null.
	 */
	public T resolve(ScopeInformation scopeInfo, Class<T> assertedFormat,
		Object source);

	/**
	 * Returns the FormulaSemantics for the NEPFormula.
	 * 
	 * The given FormulaManager must contain information about variable values,
	 * available functions, and other characteristics required for the formula
	 * to establish the list of variables contained within the NEPFormula. These
	 * must be valid within the context of the given FormatManager and
	 * LegalScope.
	 * 
	 * @param fm
	 *            The FormulaManager providing the context in which the
	 *            NEPFormula is to be resolved
	 * @param legalScope
	 *            The LegalScope in which the NEPFormula should be checked to
	 *            ensure it is valid
	 * @param formatManager
	 *            The FormatManager in which the NEPFormula should be checked to
	 *            ensure it is valid
	 * @param assertedFormat
	 *            The Class indicating the asserted Format for the formula. This
	 *            parameter is optional - null can indicate that there is no
	 *            format asserted by the context of the Formula
	 * @return The FormulaSemantics for the NEPFormula
	 */
	public FormulaSemantics isValid(FormulaManager fm, LegalScope legalScope,
		FormatManager<T> formatManager, Class<?> assertedFormat);

	/**
	 * Determines the dependencies for this formula, including the VariableID
	 * objects representing the variables within the NEPFormula.
	 * 
	 * The given ScopeInformation must contain information about variable
	 * values, available functions, and other characteristics required for the
	 * formula to establish the list of variables contained within the
	 * NEPFormula.
	 * 
	 * The given DependencyManager will be loaded with the dependency
	 * information.
	 * 
	 * @param scopeInfo
	 *            The ScopeInformation providing the context in which the
	 *            NEPFormula variables are to be determined
	 * @param depManager
	 *            The DependencyManager to be used to capture the dependencies
	 * @param assertedFormat
	 *            The Class indicating the asserted Format for the NEPFormula.
	 *            This parameter is optional - null can indicate that there is
	 *            no format asserted by the context of the formula
	 * @throws IllegalArgumentException
	 *             if the given ScopeInformation is null
	 */
	public void getDependencies(ScopeInformation scopeInfo,
		DependencyManager depManager, Class<?> assertedFormat);
}
