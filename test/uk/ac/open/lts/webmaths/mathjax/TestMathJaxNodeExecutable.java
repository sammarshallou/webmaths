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

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.*;

import org.junit.*;

import uk.ac.open.lts.webmaths.mathjax.MathJaxNodeExecutable.ConversionResults;

public class TestMathJaxNodeExecutable
{
	/** A successful response from the Node.js executable. */
	private final static String[] RESULT_SUCCESS =
	{
		"<<BEGIN:RESULT",
		"<<BEGIN:SVG",
		TestMathJax.SVG_X,
		"<<END:SVG",
		"<<BEGIN:MATHML",
		TestMathJax.MATHML_X,
		"<<END:MATHML",
		"<<END:RESULT"
	};

	/**
	 * Mock of the MathJax.Node Instance.
	 */
	private class MathJaxNodeInstanceMock extends MathJaxNodeInstance
	{
		StringBuilder out = new StringBuilder();
		LinkedList<String> lines = new LinkedList<String>();
		String stderr = null;

		protected MathJaxNodeInstanceMock(long started)
		{
			super(started);
		}

		@Override
		synchronized void closeInstance()
		{
			out.append("*closeInstance\n");
		}

		@Override
		synchronized void sendLine(String text) throws IOException
		{
			out.append("*sendLine:" + text + "\n");
		}

		@Override
		synchronized void flush() throws IOException
		{
			out.append("*flush\n");
		}

		@Override
		synchronized String readLine() throws IOException
		{
			assertTrue(!lines.isEmpty());
			String first = lines.removeFirst();
			if(first.equals("crash"))
			{
				throw new IOException("Failed during readLine");
			}
			if(first.equals("delay"))
			{
				try
				{
					Thread.sleep(200);
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
				return readLine();
			}
			return first;
		}

		@Override
		synchronized String closeWithStderr() throws IOException
		{
			out.append("*closeWithStderr\n");
			assertTrue(stderr != null);
			String result = stderr;
			stderr = null;
			return result;
		}

		synchronized void addLine(String newLine)
		{
			addLines(new String[] { newLine });
		}

		synchronized void addLines(String[] newLines)
		{
			for(String line : newLines)
			{
				lines.addLast(line);
			}
		}

		synchronized void setStderr(String stderr)
		{
			this.stderr = stderr;
		}

		synchronized String getActions()
		{
			String actions = out.toString();
			out.setLength(0);
			return actions;
		}
	}

	/**
	 * Test version of the MathJaxNodeExecutable class. Only change is to use the
	 * mock instance.
	 */
	private class MathJaxNodeExecutableTester extends MathJaxNodeExecutable
	{
		private LinkedList<MathJaxNodeInstance> instances = new LinkedList<MathJaxNodeInstance>();

		@Override
		protected synchronized MathJaxNodeInstance createInstance()
		{
			assertTrue(!instances.isEmpty());
			return instances.removeFirst();
		}

		synchronized void addInstance(MathJaxNodeInstanceMock instance)
		{
			instances.addLast(instance);
		}

		void makeSparesDue()
		{
			sparesSince = System.currentTimeMillis() - 100 * 60 * 1000;
		}
	}

	private MathJaxNodeExecutableTester executable;

	@Before
	public void before()
	{
		executable = new MathJaxNodeExecutableTester();
	}

	@Test
	public void testConvertEquationSuccess() throws Exception
	{
		MathJaxNodeInstanceMock instance = new MathJaxNodeInstanceMock(1);
		executable.addInstance(instance);

		// Mock up result that would come from real executable.
		instance.addLines(RESULT_SUCCESS);
		ConversionResults results = executable.convertEquation(
			new InputTexDisplayEquation("x"));
		assertEquals(
			"*sendLine:TeX\n"
			+ "*sendLine:x\n"
			+ "*sendLine:\n"
			+ "*flush\n", instance.getActions());
		assertEquals(TestMathJax.MATHML_X, results.getMathml());
		assertEquals(TestMathJax.SVG_X, results.getSvg());

		// Check that repeats use the cache (nothing required in instance).
		results = executable.convertEquation(
			new InputTexDisplayEquation("x"));
		assertEquals(TestMathJax.MATHML_X, results.getMathml());
		assertEquals(TestMathJax.SVG_X, results.getSvg());
	}

	@Test
	public void testConvertEquationFailure() throws Exception
	{
		MathJaxNodeInstanceMock instance = new MathJaxNodeInstanceMock(1);
		executable.addInstance(instance);

		// Mock up result that would come from real executable.
		instance.addLines(new String[]
		{
			"<<BEGIN:RESULT",
			"<<BEGIN:ERRORS",
			"This is an error!",
			"<<END:ERRORS",
			"<<END:RESULT"
		});
		try
		{
			executable.convertEquation(
				new InputTexDisplayEquation("x"));
			fail();
		}
		catch(MathJaxException e)
		{
			assertEquals("This is an error!", e.getMessage());
		}
	}

	@Test
	public void testConvertEquationCrash() throws Exception
	{
		// Create first instance, set to crash.
		MathJaxNodeInstanceMock instance = new MathJaxNodeInstanceMock(1);
		executable.addInstance(instance);
		instance.addLines(new String[] { "crash" });
		instance.setStderr("This\nis\nstderr");

		try
		{
			executable.convertEquation(new InputTexDisplayEquation("x"));
			fail();
		}
		catch(IOException e)
		{
			// Message includes normal exception message with stderr appended.
			assertEquals("Failed during readLine\nThis\nis\nstderr", e.getMessage());
		}

		// Check it closes the first instance.
		assertEquals(
			"*sendLine:TeX\n"
			+ "*sendLine:x\n"
			+ "*sendLine:\n"
			+ "*flush\n"
			+ "*closeWithStderr\n", instance.getActions());

		// Check it spins up a new instance for next equation.
		MathJaxNodeInstanceMock instance2 = new MathJaxNodeInstanceMock(2);
		executable.addInstance(instance2);
		instance2.addLines(RESULT_SUCCESS);
		executable.convertEquation(new InputTexDisplayEquation("x"));
		assertEquals(
			"*sendLine:TeX\n"
			+ "*sendLine:x\n"
			+ "*sendLine:\n"
			+ "*flush\n", instance2.getActions());
	}

	@Test
	public void testMultiThreaded() throws Exception
	{
		// Create first instance with 200ms delay.
		MathJaxNodeInstanceMock instance1 = new MathJaxNodeInstanceMock(1);
		executable.addInstance(instance1);
		instance1.addLine("delay");
		instance1.addLines(RESULT_SUCCESS);
		instance1.addLine("delay");
		instance1.addLines(RESULT_SUCCESS);

		// And second instance.
		MathJaxNodeInstanceMock instance2 = new MathJaxNodeInstanceMock(2);
		executable.addInstance(instance2);
		instance2.addLine("delay");
		instance2.addLines(RESULT_SUCCESS);
		instance2.addLine("delay");
		instance2.addLines(RESULT_SUCCESS);

		// Spin up a couple of threads.
		final LinkedList<Boolean> list = new LinkedList<Boolean>();
		Thread thread1 = new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					// Convert 2 equations.
					executable.convertEquation(new InputTexDisplayEquation("a"));
					executable.convertEquation(new InputTexDisplayEquation("c"));
					synchronized(list)
					{
						list.add(true);
						list.notifyAll();
					}
				}
				catch(Exception e)
				{
					synchronized(list)
					{
						list.add(false);
						list.notifyAll();
					}
				}
			}
		};
		Thread thread2 = new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					// Convert 1 equation after 100ms.
					Thread.sleep(100);
					executable.convertEquation(new InputTexDisplayEquation("b"));
					synchronized(list)
					{
						list.add(true);
						list.notifyAll();
					}
				}
				catch(Exception e)
				{
					synchronized(list)
					{
						list.add(false);
						list.notifyAll();
					}
				}
			}
		};
		thread1.start();
		thread2.start();

		synchronized(list)
		{
			while(list.size() < 2)
			{
				list.wait();
			}
		}

		// Check all succeeded.
		assertArrayEquals(new Boolean[] { true, true }, list.toArray(new Boolean[2]));

		// Check instance 1 includes requests for a and c.
		assertEquals(
			"*sendLine:TeX\n"
			+ "*sendLine:a\n"
			+ "*sendLine:\n"
			+ "*flush\n"
			+ "*sendLine:TeX\n"
			+ "*sendLine:c\n"
			+ "*sendLine:\n"
			+ "*flush\n", instance1.getActions());

		// Check instance 2 includes requests for b.
		assertEquals(
			"*sendLine:TeX\n"
			+ "*sendLine:b\n"
			+ "*sendLine:\n"
			+ "*flush\n", instance2.getActions());

		// After some time, check that the oldest instance is closed, and a
		// single equation uses the other one.
		executable.makeSparesDue();
		instance2.addLines(RESULT_SUCCESS);
		executable.convertEquation(new InputTexDisplayEquation("d"));
		assertEquals("*closeInstance\n", instance1.getActions());
		assertEquals(
			"*sendLine:TeX\n"
			+ "*sendLine:d\n"
			+ "*sendLine:\n"
			+ "*flush\n", instance2.getActions());
	}


}
