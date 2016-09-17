/* Generated By:JJTree: Do not edit this line. SimpleNode.java */

/*
 * Copyright (c) Andrew Wilson, 2010.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package pcgen.base.formula.parse;

public class SimpleNode implements Node
{
	private Node parent;
	private Node[] children;
	private int id;
	private FormulaParser parser;

	public SimpleNode(int i)
	{
		id = i;
	}

	public SimpleNode(FormulaParser p, int i)
	{
		this(i);
		parser = p;
	}

	@Override
	public void jjtOpen()
	{
	}

	@Override
	public void jjtClose()
	{
	}

	@Override
	public void jjtSetParent(Node n)
	{
		parent = n;
	}

	@Override
	public Node jjtGetParent()
	{
		return parent;
	}

	@Override
	public void jjtAddChild(Node n, int i)
	{
		if (children == null)
		{
			children = new Node[i + 1];
		}
		else if (i >= children.length)
		{
			Node[] c = new Node[i + 1];
			System.arraycopy(children, 0, c, 0, children.length);
			children = c;
		}
		children[i] = n;
	}

	@Override
	public Node jjtGetChild(int i)
	{
		return children[i];
	}

	@Override
	public int jjtGetNumChildren()
	{
		return (children == null) ? 0 : children.length;
	}

	@Override
	public Object jjtAccept(FormulaParserVisitor visitor, Object data)
	{
		return visitor.visit(this, data);
	}

	public Object childrenAccept(FormulaParserVisitor visitor, Object data)
	{
		if (children != null)
		{
			for (Node aChildren : children)
			{
				aChildren.jjtAccept(visitor, data);
			}
		}
		return data;
	}

	/*
	 * Items below this point are not auto-generated by javacc, but content
	 * unique to this implementation.
	 */
	/**
	 * The Operator for this node, if any. This is only loaded for relations,
	 * arithmetic calculations and other "operations" in a formula.
	 * 
	 * Generally nodes that have a text String will not have an operator.
	 */
	private Operator operator;

	/**
	 * The String containing the text for the node, if any. This is only loaded
	 * for text-related nodes (variables, formula names, etc.). This does
	 * include numerical nodes, as they are stored as a String until the formula
	 * is visited.
	 */
	private String text;

	/**
	 * Sets the Operator for this Node. Under normal circumstances, this method
	 * should only be called by the parser, not by any method at runtime.
	 * 
	 * @param operator
	 *            The Operator for this Node
	 */
	public void setOperator(Operator operator)
	{
		this.operator = operator;
	}

	/**
	 * Sets the text String contained by the node. Under normal circumstances,
	 * this method should only be called by the parser, not by any method at
	 * runtime.
	 * 
	 * @param s
	 *            The text String contained by the node
	 */
	public void setToken(String s)
	{
		text = s;
	}

	/**
	 * Returns the ID of this node. The ID is a numerical representation of the
	 * type of node in the tree. It is arguably redundant information to the
	 * class of the node, but is nonetheless useful for accessing the array in
	 * pcgen.base.formula.parse.FormulaParserTreeConstants in order to get a
	 * String representation of the node type.
	 * 
	 * @return The ID of this node.
	 */
	@Override
	public int getId()
	{
		return id;
	}

	/**
	 * Returns the Operator for this node, if any. Null may be returned if no
	 * operator has been set.
	 * 
	 * @return The Operator for this node, if any. Null is a legal return value
	 *         if no operator is set.
	 */
	public Operator getOperator()
	{
		return operator;
	}

	/**
	 * Returns the text representation of this node, if any. Null may be
	 * returned if no text representation has been set.
	 * 
	 * @return The text representation of this node, if any. Null is a legal
	 *         return value if text representation has been set.
	 */
	public String getText()
	{
		return text;
	}
}
