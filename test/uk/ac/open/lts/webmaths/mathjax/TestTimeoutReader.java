package uk.ac.open.lts.webmaths.mathjax;

import java.io.*;

import org.junit.Test;

import junit.framework.TestCase;

public class TestTimeoutReader extends TestCase
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
			"Line A\nLine B\nLast line".getBytes(), 5);
		TimeoutReader reader = new TimeoutReader(input);

		assertEquals("Line A", reader.getNextLine(100));
		assertEquals("Line B", reader.getNextLine(100));
		try
		{
			reader.getNextLine(10);
			fail();
		}
		catch(IOException e)
		{
			assertEquals("Timeout reading line from process", e.getMessage());
		}
	}
}
