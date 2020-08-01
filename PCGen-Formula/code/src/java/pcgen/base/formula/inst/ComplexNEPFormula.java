/*
 * Copyright 2014-15 (C) Tom Parker <thpr@users.sourceforge.net>
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

import java.io.StringReader;
import java.util.Objects;
import java.util.Optional;

import pcgen.base.formula.base.DependencyManager;
import pcgen.base.formula.base.EvaluationManager;
import pcgen.base.formula.base.FormulaSemantics;
import pcgen.base.formula.exception.SemanticsException;
import pcgen.base.formula.exception.SemanticsFailureException;
import pcgen.base.formula.parse.FormulaParser;
import pcgen.base.formula.parse.ParseException;
import pcgen.base.formula.parse.SimpleNode;
import pcgen.base.formula.visitor.DependencyVisitor;
import pcgen.base.formula.visitor.EvaluateVisitor;
import pcgen.base.formula.visitor.ReconstructionVisitor;
import pcgen.base.formula.visitor.SemanticsVisitor;
import pcgen.base.util.FormatManager;

/**
 * A ComplexNEPFormula is a formula that is part of the "Native Equation Parser"
 * for PCGen. A ComplexNEPFormula is a binary representation of a formula stored
 * in a parsed tree of objects. The tree of nodes is generated by the parser
 * present in pcgen.base.formula.parse.
 * 
 * The tree within a ComplexNEPFormula is designed to visited in order to
 * evaluate or otherwise process a ComplexNEPFormula.
 * 
 * @param <T>
 *            The Format (Class) of object returned by this ComplexNEPFormula
 */
public class ComplexNEPFormula<T> implements NEPFormula<T>
{

	private static final SemanticsVisitor SEMANTICS_VISITOR =
			new SemanticsVisitor();

	private static final DependencyVisitor DEPENDENCY_VISITOR =
			new DependencyVisitor();

	private static final ReconstructionVisitor RECONSTRUCTION_VISITOR =
			new ReconstructionVisitor();

	private static final EvaluateVisitor EVALUATE_VISITOR =
			new EvaluateVisitor();

	/**
	 * The root node of the tree representing the calculation of this
	 * ComplexNEPFormula.
	 * 
	 * Note that while this object is private, it is intended that this object
	 * will escape from the ComplexNEPFormula instance (This is because the
	 * method of evaluating or processing a ComplexNEPFormula uses a visitor
	 * pattern on the tree of objects). Given that this root object and the
	 * resulting tree is shared, a ComplexNEPFormula is not immutable; it is up
	 * to the behavior of the visitor to ensure that it treats the
	 * ComplexNEPFormula in an appropriate fashion.
	 */
	private final SimpleNode root;

	/**
	 * The FormatManager indicating the format of the result of calculating this
	 * ComplexNEPFormula.
	 */
	private final FormatManager<T> formatManager;
	
	/**
	 * The (cached) hashCode.
	 */
	private int hash = 0;

	/**
	 * Construct a new ComplexNEPFormula from the given String. This calculates the tree
	 * of objects representing the calculation to be performed by the ComplexNEPFormula,
	 * and loads the root of that tree into the root field.
	 * 
	 * @param expression
	 *            The String representation of the formula used to construct the
	 *            ComplexNEPFormula.
	 * @param formatManager
	 *            The FormatManager indicating the format of the result of calculating
	 *            this ComplexNEPFormula.
	 * @throws IllegalArgumentException
	 *             if the given String does not represent a well-structured Formula. (For
	 *             example, if parenthesis are not matched, an exception will be thrown)
	 */
	public ComplexNEPFormula(String expression, FormatManager<T> formatManager)
	{
		this.formatManager = Objects.requireNonNull(formatManager);
		try
		{
			StringReader reader = new StringReader(Objects.requireNonNull(expression));
			root = new FormulaParser(reader).query();
		}
		catch (ParseException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Resolves the ComplexNEPFormula in the context of the given
	 * EvaluationManager. The given EvaluationManager must contain information
	 * about variable values, available functions, and other characteristics
	 * required for the formula to produce a value.
	 * 
	 * If variables and formulas required by the ComplexNEPFormula are not
	 * available in the given EvaluationManager, behavior is not guaranteed and
	 * ComplexNEPFormula or other methods called within this method reserve the
	 * right to throw an Exception or otherwise not fail gracefully. (The
	 * precise behavior is likely defined by the FormulaManager).
	 * 
	 * Note in the case of a valid formula that the format (but not the exact
	 * class) of the return value is guaranteed by the ComplexNEPFormula. The
	 * Class may extend the format contained by the ComplexNEPFormula. The exact
	 * class returned is defined by the EvaluationManager, which can therefore
	 * implement the appropriate processing (precision in the case of numbers)
	 * desired for the given calculation.
	 * 
	 * @param manager
	 *            The EvaluationManager for the context of the formula
	 * @return The value calculated for the ComplexNEPFormula.
	 */
	@Override
	public T resolve(EvaluationManager manager)
	{
		EvaluationManager evalManager =
				manager.getWith(EvaluationManager.ASSERTED, Optional.of(formatManager));
		@SuppressWarnings("unchecked")
		T result = (T) EVALUATE_VISITOR.visit(root, evalManager);
		return result;
	}

	/**
	 * Determines the dependencies for this formula, including the VariableID
	 * objects representing the variables within the ComplexNEPFormula.
	 * 
	 * The given DependencyManager must contain information about variable
	 * values, available functions, and other characteristics required for the
	 * formula to establish the list of variables contained within the
	 * ComplexNEPFormula.
	 * 
	 * The given DependencyManager will be loaded with the dependency
	 * information.
	 * 
	 * @param depManager
	 *            The DependencyManager to be used to capture the dependencies
	 */
	@Override
	public void captureDependencies(DependencyManager depManager)
	{
		DEPENDENCY_VISITOR.visit(root, Objects.requireNonNull(depManager));
	}

	@Override
	public void isValid(FormulaSemantics semantics) throws SemanticsException
	{
		try
		{
			FormatManager<?> formulaFormat =
					(FormatManager<?>) SEMANTICS_VISITOR.visit(root, semantics);
			if (!formatManager.equals(formulaFormat))
			{
				throw new SemanticsException("Parse Error: Invalid Value Format: "
						+ formulaFormat + " found in " + root.getClass().getName()
						+ " found in location requiring a "
						+ formatManager.getManagedClass()
						+ " (class cannot be evaluated)");
			}
		}
		catch (SemanticsFailureException e)
		{
			throw new SemanticsException(e);
		}
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		RECONSTRUCTION_VISITOR.visit(root, sb);
		return sb.toString();
	}

	@Override
	public FormatManager<T> getFormatManager()
	{
		return formatManager;
	}

	@Override
	public int hashCode()
	{
		if (hash == 0)
		{
			hash = toString().hashCode();
		}
		return hash;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof ComplexNEPFormula)
		{
			ComplexNEPFormula<?> other = (ComplexNEPFormula<?>) obj;
			return formatManager.equals(other.formatManager)
				&& toString().equals(other.toString());
		}
		return false;
	}
}
