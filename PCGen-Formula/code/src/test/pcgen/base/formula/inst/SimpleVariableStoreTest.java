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

import junit.framework.TestCase;
import pcgen.base.format.NumberManager;
import pcgen.base.formula.base.LegalScope;
import pcgen.base.formula.base.ScopeInstance;
import pcgen.base.formula.base.VariableID;

public class SimpleVariableStoreTest extends TestCase
{

	public void testNulls()
	{
		SimpleVariableStore varStore = new SimpleVariableStore();
		NumberManager numberManager = new NumberManager();
		LegalScope varScope = new SimpleLegalScope(null, "Global");
		ScopeInstance globalInst = new SimpleScopeInstance(null, varScope, null);
		VariableID<Number> vid = new VariableID<>(globalInst, numberManager, "test");
		try
		{
			varStore.put(null, Integer.valueOf(4));
			fail();
		}
		catch (IllegalArgumentException e)
		{
			//yep
		}
		try
		{
			varStore.put(vid, null);
		}
		catch (IllegalArgumentException e)
		{
			//yep
		}
		try
		{
			//Intentionally break generics
			varStore.put((VariableID) vid, "NotANumber!");
			fail();
		}
		catch (IllegalArgumentException e)
		{
			//yep
		}
	}

	public void testGlobal()
	{
		SimpleVariableStore varStore = new SimpleVariableStore();
		NumberManager numberManager = new NumberManager();
		LegalScope varScope = new SimpleLegalScope(null, "Global");
		ScopeInstance globalInst = new SimpleScopeInstance(null, varScope, null);
		VariableID vid = new VariableID(globalInst, numberManager, "test");
		assertFalse(varStore.containsKey(vid));
		assertNull(varStore.put(vid, Integer.valueOf(9)));
		assertTrue(varStore.containsKey(vid));
		assertEquals(Integer.valueOf(9), varStore.get(vid));
		assertEquals(Integer.valueOf(9), varStore.put(vid, Integer.valueOf(4)));
		assertTrue(varStore.containsKey(vid));
		assertEquals(Integer.valueOf(4), varStore.get(vid));
	}

	public void testIndependence()
	{
		SimpleVariableStore varStore = new SimpleVariableStore();
		NumberManager numberManager = new NumberManager();
		LegalScope varScope = new SimpleLegalScope(null, "Global");
		ScopeInstance globalInst = new SimpleScopeInstance(null, varScope, null);
		VariableID vid1 = new VariableID(globalInst, numberManager, "test");
		VariableID vid2 = new VariableID(globalInst, numberManager, "test");
		VariableID vid3 = new VariableID(globalInst, numberManager, "test2");
		ScopeInstance globalInst2 = new SimpleScopeInstance(null, varScope, null);
		VariableID vid4 = new VariableID(globalInst2, numberManager, "test");
		assertNull(varStore.put(vid1, Integer.valueOf(9)));
		assertTrue(varStore.containsKey(vid1));
		assertTrue(varStore.containsKey(vid2));
		assertFalse(varStore.containsKey(vid3));
		assertFalse(varStore.containsKey(vid4));
		assertEquals(Integer.valueOf(9), varStore.put(vid2, Integer.valueOf(4)));
		assertTrue(varStore.containsKey(vid1));
		assertTrue(varStore.containsKey(vid2));
		assertFalse(varStore.containsKey(vid3));
		assertFalse(varStore.containsKey(vid4));
		assertEquals(Integer.valueOf(4), varStore.get(vid1));
		assertNull(varStore.put(vid4, Integer.valueOf(3)));
		assertTrue(varStore.containsKey(vid1));
		assertTrue(varStore.containsKey(vid2));
		assertFalse(varStore.containsKey(vid3));
		assertTrue(varStore.containsKey(vid4));
		assertEquals(Integer.valueOf(4), varStore.get(vid1));
		assertEquals(Integer.valueOf(3), varStore.get(vid4));
	}

}
