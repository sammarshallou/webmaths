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
 * MathML equations
 */
public class InputMathmlEquation extends InputEquation
{
	/**
	 * @param content MathML as string
	 */
	public InputMathmlEquation(String content)
	{
		super(content);
	}

	/**
	 * Gets the format code used by the MathJax systems for this type of equation.
	 * @return Format code e.g. "TeX"
	 */
	@Override
	public String getFormat()
	{
		return "MathML";
	}
}