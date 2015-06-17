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
 * TeX display equations.
 */
public class InputTexDisplayEquation extends InputTexEquation
{
	/**
	 * @param content TeX string
	 * @param font Font or null for default
	 */
	public InputTexDisplayEquation(String content, String font)
	{
		super(content, font);
	}

	@Override
	public String getFormat()
	{
		return "TeX";
	}
}