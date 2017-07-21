/*
 * Copyright 2014 (C) Tom Parker <thpr@users.sourceforge.net>
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

import static pcgen.base.solver.SolverUtilities.createDependencyTo;
import static pcgen.base.solver.SolverUtilities.getNode;
import static pcgen.base.solver.SolverUtilities.sinkNodeIs;
import static pcgen.base.solver.SolverUtilities.sourceNodeIs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Stream;

import pcgen.base.formula.base.DependencyManager;
import pcgen.base.formula.base.EvaluationManager;
import pcgen.base.formula.base.FormulaManager;
import pcgen.base.formula.base.ManagerFactory;
import pcgen.base.formula.base.ScopeInstance;
import pcgen.base.formula.base.VariableID;
import pcgen.base.formula.base.WriteableVariableStore;
import pcgen.base.graph.inst.DefaultDirectionalGraphEdge;
import pcgen.base.graph.inst.DirectionalSetMapGraph;
import pcgen.base.util.FormatManager;

/**
 * An AggressiveSolverManager manages a series of Solver objects in order to manage
 * dependencies between those Solver objects and ensure that any Solver which needs to be
 * processed to update a value is processed "aggressively" (as soon as a dependency has
 * calculated a new value).
 * 
 * One of the primary characteristic of the AggressiveSolverManager is also that callers
 * will consider items as represented by a given "VariableID", whereas the
 * AggressiveSolverManager will build and manage the associated Solver for that
 * VariableID.
 */
@SuppressWarnings("PMD.TooManyMethods")
public class AggressiveSolverManager implements SolverManager
{

	/**
	 * The FormulaManager used by the Solver members of this AggressiveSolverManager.
	 */
	private final FormulaManager formulaManager;

	/**
	 * The ManagerFactory to be used to generate visitor managers in this
	 * AggressiveSolverManager.
	 */
	private final ManagerFactory managerFactory;

	/**
	 * The relationship from each VariableID to the Solver calculating the value of the
	 * VariableID.
	 */
	private final Map<VariableID<?>, Solver<?>> scopedChannels =
			new HashMap<VariableID<?>, Solver<?>>();

	/**
	 * The "summarized" results of the calculation of each Solver.
	 */
	private final WriteableVariableStore resultStore;

	/**
	 * A mathematical graph used to store dependencies between VariableIDs. Since there is
	 * a 1:1 relationship with the Solver used for a VariableID, this implicitly stores
	 * the dependencies between the Solvers that are part of this AggressiveSolverManager.
	 */
	private final DirectionalSetMapGraph<VariableID<?>, DefaultDirectionalGraphEdge<VariableID<?>>> dependencies =
			new DirectionalSetMapGraph<>();

	/**
	 * The SolverFactory to be used to construct the Solver objects that are members of
	 * this AggressiveSolverFactory.
	 */
	private final SolverFactory solverFactory;

	/**
	 * Constructs a new AggressiveSolverManager which will use the given FormulaMananger
	 * and store results in the given VariableStore.
	 * 
	 * It is assumed that the WriteableVariableStore provided to this
	 * AggressiveSolverManager will not be shared as a Writeable object to any other
	 * Object. (So for purposes of ownership, the ownership of that WriteableVariableStore
	 * transfers to this AggressiveSolverManager. It can be shared to other locations as a
	 * (readable) VariableStore, as necessary.)
	 * 
	 * @param manager
	 *            The FormulaManager to be used by any Solver in this
	 *            AggressiveSolverManager
	 * @param managerFactory
	 *            The ManagerFactory to be used to generate visitor managers in this
	 *            AggressiveSolverManager
	 * @param solverFactory
	 *            The SolverFactory used to store Defaults and build Solver objects
	 * @param resultStore
	 *            The WriteableVariableStore used to store results of the calculations of
	 *            the Solver objects within this AggressiveSolverManager.
	 */
	public AggressiveSolverManager(FormulaManager manager, ManagerFactory managerFactory,
		SolverFactory solverFactory, WriteableVariableStore resultStore)
	{
		this.formulaManager = Objects.requireNonNull(manager);
		this.managerFactory = Objects.requireNonNull(managerFactory);
		this.solverFactory = Objects.requireNonNull(solverFactory);
		this.resultStore = Objects.requireNonNull(resultStore);
	}

	/*
	 * Note: This creates a "local" scoped channel that only exists for the item in
	 * question (item is "in" the VariableID). The key here being that there is the
	 * ability to have a local variable (e.g. Equipment variable).
	 */
	@Override
	public <T> void createChannel(VariableID<T> varID)
	{
		Solver<?> currentSolver = scopedChannels.get(Objects.requireNonNull(varID));
		if (currentSolver != null)
		{
			throw new IllegalArgumentException(
				"Attempt to recreate local channel: " + varID);
		}
		unconditionallyBuildSolver(varID);
		solveFromNode(varID);
	}

	@Override
	public <T> void addModifier(VariableID<T> varID, Modifier<T> modifier,
		ScopeInstance source)
	{
		addModifierAndSolve(varID, modifier, source);
	}

	@Override
	public <T> boolean addModifierAndSolve(VariableID<T> varID, Modifier<T> modifier,
		ScopeInstance source)
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

		if (!formulaManager.getFactory()
			.isLegalVariableID(varID.getScope().getLegalScope(), varID.getName()))
		{
			/*
			 * The above check allows the implicit create below for only items within the
			 * VariableLibrary
			 */
			throw new IllegalArgumentException("Request to add Modifier to Solver for "
				+ varID + " but that channel was never defined");
		}
		//Note: This cast is enforced by the solver during addModifier
		@SuppressWarnings("unchecked")
		Solver<T> solver = (Solver<T>) scopedChannels.get(varID);
		if (solver == null)
		{
			//CONSIDER This build is implicit - do we want explicit or implicit?
			solver = unconditionallyBuildSolver(varID);
		}
		/*
		 * Now build new edges of things this solver will be dependent upon...
		 */
		DependencyManager fdm = managerFactory.generateDependencyManager(formulaManager,
			source, varID.getFormatManager().getManagedClass());
		modifier.getDependencies(fdm);
		fdm.getVariables().stream()
						  .peek(this::ensureSolverExists)
						  .map(createDependencyTo(varID))
						  .forEach(dependencies::addEdge);
		//Cast of Solver<T> above effectively enforced here
		solver.addModifier(modifier, source);
		/*
		 * Solve this solver and anything that requires it (recursively)
		 */
		return solveFromNode(varID);
	}

	private void ensureSolverExists(VariableID<?> varID)
	{
		if (scopedChannels.get(varID) == null)
		{
			unconditionallyBuildSolver(varID);
			solveFromNode(varID);
		}
	}

	private <T> Solver<T> unconditionallyBuildSolver(VariableID<T> varID)
	{
		FormatManager<T> formatManager = varID.getFormatManager();
		Solver<T> solver = solverFactory.getSolver(formatManager);
		scopedChannels.put(varID, solver);
		dependencies.addNode(varID);
		return solver;
	}

	@Override
	public <T> void removeModifier(VariableID<T> varID, Modifier<T> modifier,
		ScopeInstance source)
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
			throw new IllegalArgumentException("Request to remove Modifier to Solver for "
				+ varID + " but that channel was never defined");
		}
		DependencyManager fdm = managerFactory.generateDependencyManager(formulaManager,
			source, varID.getFormatManager().getManagedClass());
		modifier.getDependencies(fdm);
		processDependencies(varID, fdm);
		//Cast above effectively enforced here
		solver.removeModifier(modifier, source);
		solveFromNode(varID);
	}

	/**
	 * Process Dependencies to be removed for the given VariableID stored in the given
	 * DependencyManager.
	 * 
	 * @param <T>
	 *            The format (class) of object contained by the given VariableID
	 * @param varID
	 *            The VariableID for which dependencies will be removed
	 * @param dm
	 *            The DependencyManager containing the dependencies of the given
	 *            VariableID
	 */
	private <T> void processDependencies(VariableID<T> varID, DependencyManager dm)
	{
		List<VariableID<?>> deps = dm.getVariables();
		if (deps == null)
		{
			return;
		}
		//DO NOT INLINE - bug in Oracle Java 8 R 141
		Stream<VariableID<?>> str = 
			dependencies.getAdjacentEdges(varID).stream()
												.filter(sinkNodeIs(varID))
												.filter(edge -> deps.contains(edge.getNodeAt(0)))
												.peek(dependencies::removeEdge)
												.map(getNode(0));
		str.forEach(deps::remove);
		if (!deps.isEmpty())
		{
			/*
			 * TODO Some form of error here since couldn't find matching edges for all
			 * dependencies...
			 */
		}
	}

	/**
	 * Triggers Solvers to be called, recursively through the dependencies, from the given
	 * VariableID.
	 * 
	 * @param varID
	 *            The VariableID as a starting point for triggering Solvers to be
	 *            processed
	 */
	public boolean solveFromNode(VariableID<?> varID)
	{
		boolean changed = false;
		boolean warning = varStack.contains(varID);
		try
		{
			varStack.push(varID);
			changed = processSolver(varID);
			if (changed)
			{
				if (warning)
				{
					throw new IllegalStateException(
						"Infinite Loop in Variable Processing: " + varStack);
				}
				/*
				 * Only necessary if the answer changes. The problem is that this is not
				 * doing them in order of a topological sort - it is completely random...
				 * so things may be processed twice :/
				 */
				solveChildren(varID);
			}
		}
		finally
		{
			varStack.pop();
		}
		return changed;
	}

	@Override
	public void solveChildren(VariableID<?> varID)
	{
		Set<DefaultDirectionalGraphEdge<VariableID<?>>> adjacentEdges =
				dependencies.getAdjacentEdges(varID);
		if (adjacentEdges != null)
		{
			//DO NOT INLINE - bug in Java 8 R 141
			Stream<VariableID<?>> str = 
					adjacentEdges.stream()
						 		 .filter(sourceNodeIs(varID))
						 		 .map(getNode(1));
			str.forEach(this::solveFromNode);
		}
	}

	private Stack<VariableID<?>> varStack = new Stack<>();

	/**
	 * Processes a single Solver represented by the given VariableID. Returns true if the
	 * value of the Variable calculated by the Solver has changed due to this processing.
	 * 
	 * @param <T>
	 *            The format (class) of object contained by the given VariableID
	 * @param varID
	 *            The VariableID for which the given Solver should be processed.
	 * 
	 * @return true if the value of the Variable calculated by the Solver has changed due
	 *         to this processing; false otherwise
	 */
	private <T> boolean processSolver(VariableID<T> varID)
	{
		@SuppressWarnings("unchecked")
		Solver<T> solver = (Solver<T>) scopedChannels.get(varID);
		/*
		 * Solver should "never" be null here, so we accept risk of NPE, since it's always
		 * a code bug
		 */
		EvaluationManager evalManager = managerFactory
			.generateEvaluationManager(formulaManager, varID.getVariableFormat());
		T newValue = solver.process(evalManager);
		Object oldValue = resultStore.put(varID, newValue);
		return !newValue.equals(oldValue);
	}

	@Override
	public <T> List<ProcessStep<T>> diagnose(VariableID<T> varID)
	{
		@SuppressWarnings("unchecked")
		Solver<T> solver = (Solver<T>) scopedChannels.get(varID);
		if (solver == null)
		{
			throw new IllegalArgumentException("Request to diagnose VariableID " + varID
				+ " but that channel was never defined");
		}
		EvaluationManager evalManager = managerFactory
			.generateEvaluationManager(formulaManager, varID.getVariableFormat());
		return solver.diagnose(evalManager);
	}

	@Override
	public <T> T getDefaultValue(Class<T> varFormat)
	{
		return solverFactory.getDefault(varFormat);
	}

}
