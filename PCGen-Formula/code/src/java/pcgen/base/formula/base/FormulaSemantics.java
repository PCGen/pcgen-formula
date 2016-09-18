/*
 * Copyright 2015 (C) Tom Parker <thpr@users.sourceforge.net>
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
package pcgen.base.formula.base;

import pcgen.base.util.FormatManager;
import pcgen.base.util.MappedDeque;
import pcgen.base.util.TypedKey;

/**
 * A FormulaSemantics is a class to capture Formula semantics.
 * 
 * This is designed, among other things, to report on whether a formula is
 * valid, and if valid the semantics of the Formula (what format it will
 * return).
 * 
 * In order to capture specific dependencies, a specific set of semantics
 * information should be loaded into this FormulaSemantics.
 * 
 * If a formula is valid, then this should contain a FormulaValidity for which
 * the isValid() method will return true. In such a case, this should not
 * contain a FormulaInvalidReport. When valid, this should contain a
 * FormulaFormat.
 * 
 * If a formula is not valid, then a FormulaSemantics must contain a
 * FormulaInvalidReport. This value should indicate with some precision the
 * issue with the Formula. Note that if there is more than one issue, only one
 * issue needs to be returned (fast fail is acceptable).
 */
public class FormulaSemantics extends MappedDeque
{
	/**
	 * A TypedKey used for storing the FormulaManager contained in this
	 * FormulaSemantics.
	 */
	public static final TypedKey<FormulaManager> FMANAGER = new TypedKey<>();

	/**
	 * A TypedKey used for storing the LegalScope contained in this
	 * FormulaSemantics.
	 */
	public static final TypedKey<LegalScope> SCOPE = new TypedKey<>();

	/**
	 * A TypedKey used for storing the Format currently asserted for the formula
	 * served by this FormulaSemantics.
	 */
	public static final TypedKey<Class<?>> ASSERTED = new TypedKey<>();

	/**
	 * A TypedKey used for storing the Format of the input object for the
	 * formula served by this FormulaSemantics.
	 */
	public static final TypedKey<FormatManager<?>> INPUT_FORMAT = new TypedKey<>();

	/**
	 * A TypedKey used for storing if the formula served by this
	 * FormulaSemantics is valid.
	 */
	private static final TypedKey<Boolean> VALID = new TypedKey<>(Boolean.TRUE);

	/**
	 * A TypedKey used for storing a message indicating why the formula served
	 * by this FormulaSemantics is not valid.
	 */
	private static final TypedKey<String> REPORT = new TypedKey<>();

	/**
	 * Sets the FormulaSemantics to indicate a Formula is not valid, and
	 * provides the given String as the report indicating why it is invalid.
	 * 
	 * @param text
	 *            The report text, indicating why the Formula is invalid
	 */
	public void setInvalid(String text)
	{
		set(VALID, false);
		set(REPORT, text);
	}

	/**
	 * Returns the report indicating why the Formula is invalid.
	 * 
	 * Is guaranteed to return content only if isValid() returns false.
	 * 
	 * @return The report text, indicating why the Formula is invalid
	 */
	public String getReport()
	{
		return peek(REPORT);
	}

	/**
	 * Returns true if the recently processed Formula is valid; false otherwise.
	 * 
	 * @return true if the recently processed Formula is valid; false otherwise.
	 */
	public boolean isValid()
	{
		return peek(VALID);
	}

	/**
	 * Constructs and initializes a new FormulaSemantics object with the
	 * appropriate keys set to the given parameters.
	 * 
	 * @param manager
	 *            The FormulaManager referenced when a Formula is processed with
	 *            this FormulaSemantics
	 * @param legalScope
	 *            The LegalScope when a Formula is processed with this
	 *            FormulaSemantics
	 * @param assertedFormat
	 *            The asserted Format when a Formula is processed with this
	 *            FormulaSemantics (may be null)
	 * @return An initialized FormulaSemantics object with the appropriate keys
	 *         set to the given parameters
	 */
	public static FormulaSemantics generate(FormulaManager manager,
		LegalScope legalScope, Class<?> assertedFormat)
	{
		FormulaSemantics semantics = new FormulaSemantics();
		semantics.set(FMANAGER, manager);
		semantics.set(SCOPE, legalScope);
		semantics.set(ASSERTED, assertedFormat);
		return semantics;
	}
}
