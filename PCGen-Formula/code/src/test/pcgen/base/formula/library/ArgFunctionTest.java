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
package pcgen.base.formula.library;

import org.junit.Test;

import pcgen.base.formula.analysis.ArgumentDependencyManager;
import pcgen.base.formula.analysis.DependencyKeyUtilities;
import pcgen.base.formula.base.DependencyManager;
import pcgen.base.formula.base.FunctionLibrary;
import pcgen.base.formula.base.OperatorLibrary;
import pcgen.base.formula.function.AbsFunction;
import pcgen.base.formula.library.ArgFunction;
import pcgen.base.formula.operator.number.NumberEquals;
import pcgen.base.formula.operator.number.NumberGreaterThan;
import pcgen.base.formula.operator.number.NumberLessThan;
import pcgen.base.formula.operator.number.NumberMinus;
import pcgen.base.formula.parse.ASTNum;
import pcgen.base.formula.parse.Node;
import pcgen.base.formula.parse.SimpleNode;
import pcgen.base.formula.visitor.DependencyVisitor;
import pcgen.base.formula.visitor.ReconstructionVisitor;
import pcgen.base.testsupport.AbstractFormulaTestCase;
import pcgen.base.testsupport.TestUtilities;

public class ArgFunctionTest extends AbstractFormulaTestCase
{

	private ArgumentDependencyManager argManager;
	private DependencyManager depManager;
	private DependencyVisitor varCapture;

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		ASTNum four = new ASTNum(0);
		four.setToken("4");
		ASTNum five = new ASTNum(1);
		five.setToken("5");
		String formula = "abs(-4.5)";
		SimpleNode node = TestUtilities.doParse(formula);
		Node[] array = new Node[]{four, five, node};
		FunctionLibrary functionLibrary = getFunctionLibrary();
		functionLibrary.addFunction(new ArgFunction(array));
		functionLibrary.addFunction(new AbsFunction());
		OperatorLibrary operatorLibrary = getOperatorLibrary();
		operatorLibrary.addAction(new NumberEquals());
		operatorLibrary.addAction(new NumberLessThan());
		operatorLibrary.addAction(new NumberGreaterThan());
		operatorLibrary.addAction(new NumberMinus());
		resetManager();
		varCapture =
				new DependencyVisitor(getFormulaManager(),
					getGlobalScopeInst(), depManager);
	}

	private void resetManager()
	{
		depManager = new DependencyManager();
		argManager = new ArgumentDependencyManager();
		depManager.addDependency(DependencyKeyUtilities.DEP_ARGUMENT, argManager);
	}

	@Test
	public void testInvalidWrongArg()
	{
		String formula = "arg()";
		SimpleNode node = TestUtilities.doParse(formula);
		isNotValid(formula, node, numberManager, null);
		formula = "arg(2, 3)";
		node = TestUtilities.doParse(formula);
		isNotValid(formula, node, numberManager, null);
	}

	@Test
	public void testNoArg()
	{
		String formula = "4";
		SimpleNode node = TestUtilities.doParse(formula);
		isValid(formula, node, numberManager, null);
		isStatic(formula, node, true);
		varCapture.visit(node, null);
		assertEquals(-1, argManager.getMaximumArgument());
		evaluatesTo(formula, node, Integer.valueOf(4));
		Object rv =
				new ReconstructionVisitor().visit(node, new StringBuilder());
		assertTrue(rv.toString().equals(formula));
	}

	@Test
	public void testInvalidTooHigh()
	{
		String formula = "arg(4)";
		SimpleNode node = TestUtilities.doParse(formula);
		isNotValid(formula, node, numberManager, null);
	}

	@Test
	public void testInvalidTooLow()
	{
		String formula = "arg(-1)";
		SimpleNode node = TestUtilities.doParse(formula);
		isNotValid(formula, node, numberManager, null);
	}

	@Test
	public void testInvalidDouble()
	{
		String formula = "arg(1.5)";
		SimpleNode node = TestUtilities.doParse(formula);
		isNotValid(formula, node, numberManager, null);
	}

	@Test
	public void testInvalidNaN()
	{
		String formula = "arg(\"string\")";
		SimpleNode node = TestUtilities.doParse(formula);
		isNotValid(formula, node, numberManager, null);
	}

	@Test
	public void testArgZero()
	{
		String formula = "arg(0)";
		SimpleNode node = TestUtilities.doParse(formula);
		isValid(formula, node, numberManager, null);
		isStatic(formula, node, true);
		varCapture.visit(node, null);
		assertEquals(0, argManager.getMaximumArgument());
		evaluatesTo(formula, node, Integer.valueOf(4));
		Object rv =
				new ReconstructionVisitor().visit(node, new StringBuilder());
		assertTrue(rv.toString().equals(formula));
	}

	@Test
	public void testArgOne()
	{
		String formula = "arg(1)";
		SimpleNode node = TestUtilities.doParse(formula);
		isValid(formula, node, numberManager, null);
		isStatic(formula, node, true);
		varCapture.visit(node, null);
		assertEquals(1, argManager.getMaximumArgument());
		evaluatesTo(formula, node, Integer.valueOf(5));
		Object rv =
				new ReconstructionVisitor().visit(node, new StringBuilder());
		assertTrue(rv.toString().equals(formula));
		DependencyManager fdm = new DependencyManager();
		/*
		 * Safe and "ignored" - if this test fails, need to change what FDM is
		 * passed in - it should NOT contain an ArgumentDependencyManager
		 */
		assertTrue(null == fdm.getDependency(DependencyKeyUtilities.DEP_ARGUMENT));
		DependencyVisitor dv =
				new DependencyVisitor(varCapture.getFormulaManager(),
					varCapture.getScopeInstance(), fdm);
		dv.visit(node, null);
	}

	@Test
	public void testComplex()
	{
		String formula = "arg(2)";
		SimpleNode node = TestUtilities.doParse(formula);
		isValid(formula, node, numberManager, null);
		isStatic(formula, node, true);
		varCapture.visit(node, null);
		assertEquals(2, argManager.getMaximumArgument());
		evaluatesTo(formula, node, Double.valueOf(4.5));
		Object rv =
				new ReconstructionVisitor().visit(node, new StringBuilder());
		assertTrue(rv.toString().equals(formula));
	}

}
