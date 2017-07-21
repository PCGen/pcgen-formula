/*
 * Copyright 2017 (C) Tom Parker <thpr@users.sourceforge.net>
 * 
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with
 * this library; if not, write to the Free Software Foundation, Inc., 59 Temple Place,
 * Suite 330, Boston, MA 02111-1307 USA
 */
package pcgen.base.solver;

import java.util.function.Function;
import java.util.function.Predicate;

import pcgen.base.formula.base.VariableID;
import pcgen.base.graph.inst.DefaultDirectionalGraphEdge;
import pcgen.base.graph.inst.DefaultGraphEdge;

/**
 * This class contains methods designed to support implementations of pcgen.base.solver.Solver
 */
public final class SolverUtilities
{

	private SolverUtilities()
	{
		//Don't instantiate utility class
	}

	//TODO Move to Base library into EdgeUtilities...
	public static <T> Predicate<? super DefaultDirectionalGraphEdge<VariableID<?>>> sourceNodeIs(
		Object obj)
	{
		return edge -> (edge.getNodeAt(0).equals(obj));
	}

	//TODO Move to Base library into EdgeUtilities...
	public static <T> Predicate<? super DefaultDirectionalGraphEdge<VariableID<?>>> sinkNodeIs(
		Object obj)
	{
		return edge -> (edge.getNodeAt(1).equals(obj));
	}

	//TODO Move to Base library into EdgeUtilities...
	public static <T> Function<? super DefaultGraphEdge<T>, T> getNode(int i)
	{
		return edge -> edge.getNodeAt(i);
	}

	public static <T> Function<? super VariableID<?>, ? extends DefaultDirectionalGraphEdge<VariableID<?>>> createDependencyTo(
		VariableID<T> varID)
	{
		/*
		 * Better to use depID here rather than Solver: (1) No order of operations risk
		 * (2) Process can still write to cache knowing ID
		 */
		return depID -> new DefaultDirectionalGraphEdge<VariableID<?>>(depID, varID);
	}

}
