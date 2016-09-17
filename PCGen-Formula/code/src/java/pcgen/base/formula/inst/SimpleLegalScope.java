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

import pcgen.base.formula.base.LegalScope;

/**
 * A SimpleLegalScope is a simple implementation of LegalScope.
 */
public class SimpleLegalScope implements LegalScope
{

	/**
	 * The LegalScope that is a parent of this LegalScope.
	 */
	private final LegalScope parent;

	/**
	 * The name of this LegalScope.
	 */
	private final String name;

	/**
	 * Constructs a new LegalScope with the given parent LegalScope and name.
	 * 
	 * @param parentScope
	 *            The LegalScope that is a parent of this LegalScope. May be
	 *            null to represent global
	 * @param name
	 *            The name of this SimpleLegalScope
	 * @throws IllegalArgumentException
	 *             if the given name is null
	 */
	public SimpleLegalScope(LegalScope parentScope, String name)
	{
		if (name == null)
		{
			throw new IllegalArgumentException(
				"LegalScope must have a non-null name");
		}
		this.parent = parentScope;
		this.name = name;
	}

	/**
	 * Returns the LegalScope that serves as a "parent" for this LegalScope.
	 * 
	 * Null is a legal return value for a "global" scope.
	 * 
	 * @return The LegalScope that serves as a "parent" for this LegalScope
	 */
	@Override
	public LegalScope getParentScope()
	{
		return parent;
	}

	/**
	 * Returns the name of this LegalScope.
	 * 
	 * @return the name of the LegalScope
	 */
	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
