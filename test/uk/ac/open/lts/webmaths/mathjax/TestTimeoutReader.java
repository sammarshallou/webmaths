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

import java.io.*;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestTimeoutReader
{
	@Test
	public void testImmediate() throws IOException
	{
		ByteArrayInputStream input = new ByteArrayInputStream(
			"Line\nAnother line\nLast line".getBytes());
		TimeoutReader reader = new TimeoutReader(input);

		assertEquals("Line", reader.getNextLine(100));
		assertEquals("Another line", reader.getNextLine(100));
		assertEquals("Last line", reader.getNextLine(100));

		reader.requestExit();
		reader.waitForExit();
	}

	@Test
	public void testSlow() throws IOException
	{
		DelayedInputStream input = new DelayedInputStream(
			"Line A\nLine B\nLast line".getBytes(), 50);
		TimeoutReader reader = new TimeoutReader(input);

		assertEquals("Line A", reader.getNextLine(500));
		assertEquals("Line B", reader.getNextLine(500));
		try
		{
			reader.getNextLine(100);
			fail();
		}
		catch(IOException e)
		{
			assertEquals("Timeout reading line from process", e.getMessage());
		}
	}
}
