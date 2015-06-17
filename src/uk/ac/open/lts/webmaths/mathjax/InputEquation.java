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
	/** Default font used for equations */
	public final static String DEFAULT_FONT = "TeX";

	private String content, font;

	/**
	 * Constructs with content.
	 * @param content Content
	 * @param font Font or null for default
	 */
	protected InputEquation(String content, String font)
	{
		this.content = content;
		if (font == null)
		{
			font = DEFAULT_FONT;
		}
		this.font = font;
	}

	public boolean isFontValid()
	{
		// Font list from http://docs.mathjax.org/en/latest/options/SVG.html#configure-svg
		return font.equals("TeX") || font.equals("STIX-Web") || font.equals("Asana-Math") ||
			font.equals("Neo-Euler") || font.equals("Gyre-Pagella") || font.equals("Gyre-Termes") ||
			font.equals("Latin-Modern");
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

	/**
	 * @return Font name (default is "TeX")
	 */
	public String getFont()
	{
		return font;
	}

	@Override
	public int hashCode()
	{
		return (getFormat() + "\n" + content + "\n" + font).hashCode();
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
	 * @param font Font or null for default
	 * @return Equation
	 */
	public static InputEquation getFromMathml(String mathml, String font)
	{
		InputTexEquation eq = InputTexEquation.getFromMathml(mathml, font);
		if(eq != null)
		{
			return eq;
		}
		return new InputMathmlEquation(mathml, font);
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
			return InputMathmlEquation.getFromMathml(eq.getMathml(), eq.getFont());
		}
		else
		{
			if(eq.isDisplay())
			{
				return new InputTexDisplayEquation(eq.getTex(), eq.getFont());
			}
			else
			{
				return new InputTexInlineEquation(eq.getTex(), eq.getFont());
			}
		}
	}
}