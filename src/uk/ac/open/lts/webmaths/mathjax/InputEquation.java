/*
This file is part of OU webmaths

OU webmaths is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

OU webmaths is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with OU webmaths. If not, see <http://www.gnu.org/licenses/>.

Copyright 2015 The Open University
*/
package uk.ac.open.lts.webmaths.mathjax;


/**
 * Base class for equations used as input to MathJax.
 */
public abstract class InputEquation
{
	private String content;

	/**
	 * Constructs with content.
	 * @param content Content
	 */
	protected InputEquation(String content)
	{
		this.content = content;
	}

	/**
	 * Gets the format code used by the MathJax systems for this type of equation.
	 * @return Format code e.g. "TeX"
	 */
	public abstract String getFormat();

	/**
	 * Gets the content of the equation.
	 * @return Actual content as string
	 */
	public String getContent()
	{
		return content;
	}

	@Override
	public int hashCode()
	{
		return (getFormat() + "\n" + content).hashCode();
	}

	@Override
	public String toString()
	{
		return getFormat() + ":" + content;
	}

	@Override
	public boolean equals(Object obj)
	{
		if(!(obj instanceof InputEquation))
		{
			return false;
		}
		InputEquation other = (InputEquation)obj;
		return content.equals(other.content) && getFormat().equals(other.getFormat());
	}

	/**
	 * Creates an InputEquation from MathML text. If the MathML appears to
	 * actually include a TeX alternative, then a TeX equation will be created
	 * instead of a MathML one.
	 * @param mathml MathML string
	 * @return Equation
	 */
	public static InputEquation getFromMathml(String mathml)
	{
		InputTexEquation eq = InputTexEquation.getFromMathml(mathml);
		if(eq != null)
		{
			return eq;
		}
		return new InputMathmlEquation(mathml);
	}

	/**
	 * Converts from SourceEquation, which is the web service class.
	 * @param eq Source equation
	 * @return Suitable InputEquation
	 */
	public static InputEquation getFromSourceEquation(SourceEquation eq)
	{
		if(eq.getMathml() != null)
		{
			return InputMathmlEquation.getFromMathml(eq.getMathml());
		}
		else
		{
			if(eq.isDisplay())
			{
				return new InputTexDisplayEquation(eq.getTex());
			}
			else
			{
				return new InputTexInlineEquation(eq.getTex());
			}
		}
	}
}