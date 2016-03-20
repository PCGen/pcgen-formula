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
package pcgen.base.solver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import pcgen.base.calculation.Modifier;
import pcgen.base.formula.analysis.DependencyKeyUtilities;
import pcgen.base.formula.analysis.VariableDependencyManager;
import pcgen.base.formula.base.DependencyManager;
import pcgen.base.formula.base.FormulaManager;
import pcgen.base.formula.base.ScopeInstance;
import pcgen.base.formula.base.VariableID;
import pcgen.base.formula.base.VariableStore;
import pcgen.base.formula.base.WriteableVariableStore;
import pcgen.base.formula.inst.ScopeInformation;
import pcgen.base.graph.inst.DefaultDirectionalGraphEdge;
import pcgen.base.graph.inst.DirectionalSetMapGraph;
import pcgen.base.util.FormatManager;

/**
 * An AggressiveSolverManager manages a series of Solver objects in order to
 * manage dependencies between those Solver objects and ensure that any Solver
 * which needs to be processed to update a value is processed "aggressively" (as
 * soon as a dependency has calculated a new value).
 * 
 * One of the primary characteristic of the AggressiveSolverManager is also that
 * callers will consider items as represented by a given "VariableID", whereas
 * the AggressiveSolverManager will build and manage the associated Solver for
 * that VariableID.
 */
public class AggressiveSolverManager
{

	/**
	 * The FormulaManager used by the Solver members of this
	 * AggressiveSolverManager.
	 */
	private final FormulaManager formulaManager;

	/**
	 * The relationship from each VariableID to the Solver calculating the value
	 * of the VariableID.
	 */
	private final Map<VariableID<?>, Solver<?>> scopedChannels =
			new HashMap<VariableID<?>, Solver<?>>();

	/**
	 * The "summarized" results of the calculation of each Solver.
	 */
	private final WriteableVariableStore resultsCache;

	/**
	 * A mathematical graph used to store dependencies between VariableIDs.
	 * Since there is a 1:1 relationship with the Solver used for a VariableID,
	 * this implicitly stores the dependencies between the Solvers that are part
	 * of this AggressiveSolverManager.
	 */
	private final DirectionalSetMapGraph<VariableID<?>, DefaultDirectionalGraphEdge<VariableID<?>>> graph =
			new DirectionalSetMapGraph<>();

	/**
	 * Cache for ScopeInformation objects.
	 */
	private final ScopeDatabase scopeCache = new ScopeDatabase();

	/**
	 * The SolverFactory to be used to construct the Solver objects that are
	 * members of this AggressiveSolverFactory.
	 */
	private final SolverFactory solverFactory;

	/**
	 * Constructs a new AggressiveSolverManager which will use the given
	 * FormulaMananger and store results in the given VariableStore.
	 * 
	 * It is assumed that the WriteableVariableStore provided to this
	 * AggressiveSolverManager will not be shared as a Writeable object to any
	 * other Object. (So for purposes of ownership, the ownership of that
	 * WriteableVariableStore transfers to this AggressiveSolverManager. It can
	 * be shared to other locations as a (readable) VariableStore, as
	 * necessary.)
	 * 
	 * @param manager
	 *            The FormulaManager to be used by any Solver in this
	 *            AggressiveSolverManager
	 * @param solverFactory
	 *            The SolverFactory used to store Defaults and build Solver
	 *            objects
	 * @param resultStore
	 *            The WriteableVariableStore used to store results of the
	 *            calculations of the Solver objects within this
	 *            AggressiveSolverManager.
	 * @throws IllegalArgumentException
	 *             if any of the parameters is null
	 */
	public AggressiveSolverManager(FormulaManager manager,
		SolverFactory solverFactory, WriteableVariableStore resultStore)
	{
		if (manager == null)
		{
			throw new IllegalArgumentException("FormulaManager cannot be null");
		}
		if (solverFactory == null)
		{
			throw new IllegalArgumentException("SolverFactory cannot be null");
		}
		if (resultStore == null)
		{
			throw new IllegalArgumentException(
				"WriteableVariableStore cannot be null");
		}
		this.formulaManager = manager;
		this.solverFactory = solverFactory;
		/*
		 * CONSIDER should ownership transfer of this be complete? We can do
		 * getValue for any "reader" that is interested... as long as they have
		 * this SolverManager...?
		 */
		resultsCache = resultStore;
	}

	/*
	 * Note: This creates a "local" scoped channel that only exists for the item
	 * in question (item is "in" the VariableID). The key here being that there
	 * is the ability to have a local variable (e.g. Equipment variable).
	 */
	/**
	 * Defines a new Variable that requires solving in this
	 * AggressiveSolverManager. The Variable, identified by the given
	 * VariableID, will be of the format of the given Class.
	 * 
	 * @param <T>
	 *            The format (class) of object contained by the given VariableID
	 * @param varID
	 *            The VariableID used to identify the Solver to be built
	 * @throws IllegalArgumentException
	 *             if any of the parameters is null
	 */
	public <T> void createChannel(VariableID<T> varID)
	{
		if (varID == null)
		{
			throw new IllegalArgumentException("VariableID cannot be null");
		}
		Solver<?> currentSolver = scopedChannels.get(varID);
		if (currentSolver != null)
		{
			throw new IllegalArgumentException(
				"Attempt to recreate local channel: " + varID);
		}
		ScopeInstance scope = varID.getScope();
		FormatManager<T> formatManager = varID.getFormatManager();
		ScopeInformation scopeInfo =
				scopeCache.getScopeInformation(formulaManager, scope);
		Solver<T> solver = solverFactory.getSolver(formatManager, scopeInfo);
		scopedChannels.put(varID, solver);
		graph.addNode(varID);
		solveFromNode(varID);
	}

	/**
	 * Adds a Modifier (with the given source object) to the Solver identified
	 * by the given VariableID.
	 * 
	 * @param <T>
	 *            The format (class) of object contained by the given VariableID
	 * @param varID
	 *            The VariableID for which a Modifier should be added to the
	 *            responsible Solver
	 * @param modifier
	 *            The Modifier to be added to the Solver for the given
	 *            VariableID
	 * @param source
	 *            The source of the Modifier to be added to the Solver
	 * @throws IllegalArgumentException
	 *             if any of the parameters is null
	 */
	public <T> void addModifier(VariableID<T> varID, Modifier<T> modifier,
		Object source)
	{
		if (varID == null)
		{
			throw new IllegalArgumentException("VariableID cannot be null");
		}
		if (modifier == null)
		{
			throw new IllegalArgumentException("Modifier cannot be null");
		}
		if (source == null)
		{
			throw new IllegalArgumentException("Source cannot be null");
		}
		if (!formulaManager.getFactory().isLegalVariableID(
			varID.getScope().getLegalScope(), varID.getName()))
		{
			/*
			 * The above check allows the implicit create below for only items
			 * within the VariableLibrary
			 */
			throw new IllegalArgumentException(
				"Request to add Modifier to Solver for " + varID
					+ " but that channel was never defined");
		}
		ScopeInstance scope = varID.getScope();
		FormatManager<T> formatManager = varID.getFormatManager();
		ScopeInformation scopeInfo =
				scopeCache.getScopeInformation(formulaManager, scope);

		//Note: This cast is enforced by the solver during addModifier
		@SuppressWarnings("unchecked")
		Solver<T> solver = (Solver<T>) scopedChannels.get(varID);
		if (solver == null)
		{
			//CONSIDER This is create implicit - what we want to do?
			solver = solverFactory.getSolver(formatManager, scopeInfo);
			scopedChannels.put(varID, solver);
			graph.addNode(varID);
		}
		/*
		 * Now build new edges of things this solver will be dependent upon...
		 */
		DependencyManager fdm = new DependencyManager();
		VariableDependencyManager vdm = new VariableDependencyManager();
		fdm.addDependency(DependencyKeyUtilities.DEP_VARIABLE, vdm);
		modifier.getDependencies(scopeInfo, fdm,
			formatManager.getManagedClass());
		if (!vdm.isEmpty())
		{
			for (VariableID<?> depID : vdm.getVariables())
			{
				ensureSolverExists(depID);
				/*
				 * Better to use depID here rather than Solver: (1) No order of
				 * operations risk (2) Process can still write to cache knowing
				 * ID
				 */
				@SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
				DefaultDirectionalGraphEdge<VariableID<?>> edge =
						new DefaultDirectionalGraphEdge<VariableID<?>>(depID,
							varID);
				graph.addEdge(edge);
			}
		}
		//Cast above effectively enforced here
		solver.addModifier(modifier, source);
		/*
		 * Solve this solver and anything that requires it (recursively)
		 */
		solveFromNode(varID);
	}

	private void ensureSolverExists(VariableID<?> varID)
	{
		if (scopedChannels.get(varID) == null)
		{
			ScopeInstance scope = varID.getScope();
			FormatManager<?> formatManager = varID.getFormatManager();
			ScopeInformation scopeInfo =
					scopeCache.getScopeInformation(formulaManager, scope);
			Solver<?> solver =
					solverFactory.getSolver(formatManager, scopeInfo);
			scopedChannels.put(varID, solver);
			graph.addNode(varID);
			solveFromNode(varID);
		}
	}

	/**
	 * Removes a Modifier (with the given source object) from the Solver
	 * identified by the given VariableID.
	 * 
	 * For this to have any effect, the combination of Modifier and source must
	 * be the same (as defined by .equals() equality) as a combination provided
	 * to the addModifier method for the given VariableID.
	 * 
	 * @param <T>
	 *            The format (class) of object contained by the given VariableID
	 * @param varID
	 *            The VariableID for which a Modifier should be removed from the
	 *            responsible Solver
	 * @param modifier
	 *            The Modifier to be removed from the Solver identified by the
	 *            given VariableID
	 * @param source
	 *            The source object for the Modifier to be removed from the
	 *            Solver identified by the given VariableID
	 * @throws IllegalArgumentException
	 *             if any of the parameters is null
	 */
	public <T> void removeModifier(VariableID<T> varID, Modifier<T> modifier,
		Object source)
	{
		if (varID == null)
		{
			throw new IllegalArgumentException("VariableID cannot be null");
		}
		if (modifier == null)
		{
			throw new IllegalArgumentException("Modifier cannot be null");
		}
		if (source == null)
		{
			throw new IllegalArgumentException("Source cannot be null");
		}
		//Note: This cast is enforced by the solver during addModifier
		@SuppressWarnings("unchecked")
		Solver<T> solver = (Solver<T>) scopedChannels.get(varID);
		if (solver == null)
		{
			throw new IllegalArgumentException(
				"Request to remove Modifier to Solver for " + varID
					+ " but that channel was never defined");
		}
		DependencyManager fdm = new DependencyManager();
		VariableDependencyManager vdm = new VariableDependencyManager();
		fdm.addDependency(DependencyKeyUtilities.DEP_VARIABLE, vdm);
		ScopeInstance scope = varID.getScope();
		ScopeInformation scopeInfo =
				scopeCache.getScopeInformation(formulaManager, scope);
		modifier.getDependencies(scopeInfo, fdm,
			varID.getFormatManager().getManagedClass());
		processDependencies(varID, vdm);
		//Cast above effectively enforced here
		solver.removeModifier(modifier, source);
		solveFromNode(varID);
	}

	/**
	 * Process Dependencies for the given VariableID stored in the given
	 * DependencyManager.
	 * 
	 * @param <T>
	 *            The format (class) of object contained by the given VariableID
	 * @param varID
	 *            The VariableID for which dependencies will be captured
	 * @param vdm
	 *            The VariableDependencyManager to be loaded with the
	 *            dependencies of the given VariableID
	 */
	private <T> void processDependencies(VariableID<T> varID,
		VariableDependencyManager vdm)
	{
		List<VariableID<?>> deps = vdm.getVariables();
		if (deps == null)
		{
			return;
		}
		Set<DefaultDirectionalGraphEdge<VariableID<?>>> edges =
				graph.getAdjacentEdges(varID);
		for (DefaultDirectionalGraphEdge<VariableID<?>> edge : edges)
		{
			if (edge.getNodeAt(1) == varID)
			{
				VariableID<?> depID = edge.getNodeAt(0);
				if (deps.contains(depID))
				{
					graph.removeEdge(edge);
					deps.remove(depID);
				}
			}
		}
		if (!deps.isEmpty())
		{
			/*
			 * TODO Some form of error here since couldn't find matching edges
			 * for all dependencies...
			 */
		}
	}

	/**
	 * Triggers Solvers to be called, recursively through the dependencies, from
	 * the given VariableID.
	 * 
	 * @param varID
	 *            The VariableID as a starting point for triggering Solvers to
	 *            be processed
	 */
	private void solveFromNode(VariableID<?> varID)
	{
		boolean warning = varStack.contains(varID);
		try
		{
			varStack.push(varID);
			if (processSolver(varID))
			{
				if (warning)
				{
					throw new IllegalStateException(
						"Infinite Loop in Variable Processing: " + varStack);
				}
				/*
				 * Only necessary if the answer changes. The problem is that
				 * this is not doing them in order of a topological sort - it is
				 * completely random... so things may be processed twice :/
				 */
				for (DefaultDirectionalGraphEdge<VariableID<?>> edge : graph
					.getAdjacentEdges(varID))
				{
					if (edge.getNodeAt(0).equals(varID))
					{
						solveFromNode(edge.getNodeAt(1));
					}
				}
			}
		}
		finally
		{
			varStack.pop();
		}
	}

	private Stack<VariableID<?>> varStack = new Stack<>();

	/**
	 * Processes a single Solver represented by the given VariableID. Returns
	 * true if the value of the Variable calculated by the Solver has changed
	 * due to this processing.
	 * 
	 * @param <T>
	 *            The format (class) of object contained by the given VariableID
	 * @param varID
	 *            The VariableID for which the given Solver should be processed.
	 * 
	 * @return true if the value of the Variable calculated by the Solver has
	 *         changed due to this processing; false otherwise
	 */
	private <T> boolean processSolver(VariableID<T> varID)
	{
		@SuppressWarnings("unchecked")
		Solver<T> solver = (Solver<T>) scopedChannels.get(varID);
		/*
		 * Solver should "never" be null here, so we accept risk of NPE, since
		 * it's always a code bug
		 */
		T newValue = solver.process();
		Object oldValue = resultsCache.put(varID, newValue);
		return !newValue.equals(oldValue);
	}

	/**
	 * Provides a List of ProcessStep objects identifying how the current value
	 * of the variable identified by the given VariableID has been calculated.
	 * 
	 * The ProcessStep objects are provided in the order of operations, with the
	 * first object in the list being the first step in the derivation.
	 * 
	 * @param <T>
	 *            The format (class) of object contained by the given VariableID
	 * @param varID
	 *            The VariableID for which the List of ProcessStep objects
	 *            should be returned.
	 * @return The List of ProcessStep objects identifying how the current value
	 *         of the variable identified by the given VariableID has been
	 *         calculated
	 */
	public <T> List<ProcessStep<T>> diagnose(VariableID<T> varID)
	{
		@SuppressWarnings("unchecked")
		Solver<T> solver = (Solver<T>) scopedChannels.get(varID);
		if (solver == null)
		{
			throw new IllegalArgumentException("Request to diagnoze VariableID "
				+ varID + " but that channel was never defined");
		}
		return solver.diagnose();
	}

	/**
	 * Returns the VariableStore used by this AggressiveSolverManager to store
	 * results of calculations.
	 * 
	 * @return The VariableStore used by this AggressiveSolverManager to store
	 *         results of calculations.
	 */
	public VariableStore getVariableStore()
	{
		return resultsCache;
	}

}
