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

Copyright 2011 The Open University
*/
package uk.ac.open.lts.webmaths;

import java.io.IOException;

import javax.xml.ws.Endpoint;

import uk.ac.open.lts.webmaths.english.WebMathsEnglish;
import uk.ac.open.lts.webmaths.image.WebMathsImage;
import uk.ac.open.lts.webmaths.tex.WebMathsTex;

/**
 * Manually publishes service endpoints for testing on developer machine.
 */
public class DevServer
{
	/**
	 * @param args Ignored
	 */
	public static void main(String[] args) throws IOException
	{
		MathmlEntityFixer fixer = new MathmlEntityFixer();
		Endpoint.publish("http://pclt1507.open.ac.uk:9997/", new WebMathsTex(fixer));
		Endpoint.publish("http://pclt1507.open.ac.uk:9998/", new WebMathsEnglish(fixer));
		Endpoint.publish("http://pclt1507.open.ac.uk:9999/", new WebMathsImage(fixer));
		System.out.println("Services ready");
	}
}
