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
		String hackedFont = null;

		protected MathJaxNodeInstanceMock(long started)
		{
			super(started, InputEquation.DEFAULT_FONT);
		}

		public void hackFont(String font)
		{
			hackedFont = font;
		}

		@Override
		public String getFont()
		{
			return hackedFont != null ? hackedFont : super.getFont();
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
		private LinkedList<MathJaxNodeInstanceMock> instances = new LinkedList<MathJaxNodeInstanceMock>();

		@Override
		protected synchronized MathJaxNodeInstance createInstance(String font)
		{
			assertTrue(!instances.isEmpty());
			MathJaxNodeInstanceMock instance = instances.removeFirst();
			instance.hackFont(font);
			return instance;
		}

		synchronized void addInstance(MathJaxNodeInstanceMock instance)
		{
			instances.addLast(instance);
		}

		void makeSparesDue()
		{
			for(int i = 0; i < lastSimultaneousUsed.length; i++)
			{
				lastSimultaneousUsed[i] = System.currentTimeMillis() - 100 * 60 * 1000;
			}
			synchronized(checker)
			{
				checker.notifyAll();
				try 
				{
					checker.wait();
				} 
				catch (InterruptedException e) 
				{
					e.printStackTrace();
				}
			}
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
			new InputTexDisplayEquation("x", null));
		assertEquals(
			"*sendLine:TeX\n"
			+ "*sendLine:x\n"
			+ "*sendLine:\n"
			+ "*flush\n", instance.getActions());
		assertEquals(TestMathJax.MATHML_X, results.getMathml());
		assertEquals(TestMathJax.SVG_X, results.getSvg());

		// Check that repeats use the cache (nothing required in instance).
		results = executable.convertEquation(
			new InputTexDisplayEquation("x", null));
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
				new InputTexDisplayEquation("x", null));
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
			executable.convertEquation(new InputTexDisplayEquation("x", null));
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
		executable.convertEquation(new InputTexDisplayEquation("x", null));
		assertEquals(
			"*sendLine:TeX\n"
			+ "*sendLine:x\n"
			+ "*sendLine:\n"
			+ "*flush\n", instance2.getActions());
	}

	@Test
	public void testMultiThreaded() throws Exception
	{
		// Create first instance with 200ms delays.
		MathJaxNodeInstanceMock instance1 = new MathJaxNodeInstanceMock(1);
		executable.addInstance(instance1);
		instance1.addLine("delay");
		instance1.addLines(RESULT_SUCCESS);
		instance1.addLine("delay");
		instance1.addLines(RESULT_SUCCESS);
		instance1.addLine("delay");
		instance1.addLines(RESULT_SUCCESS);

		// And second instance.
		MathJaxNodeInstanceMock instance2 = new MathJaxNodeInstanceMock(2);
		executable.addInstance(instance2);

		// Spin up a couple of threads.
		final LinkedList<Boolean> list = new LinkedList<Boolean>();
		final LinkedList<String> suffix = new LinkedList<String>();
		suffix.add("1");

		Runnable task1 = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					// Convert 2 equations with 100ms gap.
					executable.convertEquation(new InputTexDisplayEquation("a" + suffix.getFirst(), null));
					Thread.sleep(100);
					executable.convertEquation(new InputTexDisplayEquation("c" + suffix.getFirst(), null));
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
		Runnable task2 = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					// Convert 1 equation after 100ms.
					Thread.sleep(100);
					executable.convertEquation(new InputTexDisplayEquation("b" + suffix.getFirst(), null));
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
		new Thread(task1, "Task1-1").start();
		new Thread(task2, "Task2-1").start();

		synchronized(list)
		{
			while(list.size() < 2)
			{
				list.wait();
			}
		}

		// Check all succeeded.
		assertArrayEquals(new Boolean[] { true, true }, list.toArray(new Boolean[2]));

		// Check instance 1 includes requests for a, b, and c.
		assertEquals(
			"*sendLine:TeX\n"
			+ "*sendLine:a1\n"
			+ "*sendLine:\n"
			+ "*flush\n"
			+ "*sendLine:TeX\n"
			+ "*sendLine:b1\n"
			+ "*sendLine:\n"
			+ "*flush\n"
			+ "*sendLine:TeX\n"
			+ "*sendLine:c1\n"
			+ "*sendLine:\n"
			+ "*flush\n", instance1.getActions());

		// Check instance 2 hasn't done anything yet.
		assertEquals("", instance2.getActions());

		// Repeat, but this time make instance 1 delay long enough that instance 2
		// spins up.
		instance1.addLine("delay");
		instance1.addLine("delay");
		instance1.addLine("delay");
		instance1.addLine("delay");
		instance1.addLines(RESULT_SUCCESS); // At 800ms.
		instance1.addLine("delay");
		instance1.addLines(RESULT_SUCCESS); // At 1000ms.

		instance2.addLine("delay"); // Starts at approx 600ms.
		instance2.addLine("delay");
		instance2.addLines(RESULT_SUCCESS); // At 1000ms.

		suffix.addFirst("2");

		list.clear();
		new Thread(task1, "Task1-2").start();
		new Thread(task2, "Task2-2").start();

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
			+ "*sendLine:a2\n"
			+ "*sendLine:\n"
			+ "*flush\n"
			+ "*sendLine:TeX\n"
			+ "*sendLine:c2\n"
			+ "*sendLine:\n"
			+ "*flush\n", instance1.getActions());

		// Check instance 2 has request b.
		assertEquals(
			"*sendLine:TeX\n"
			+ "*sendLine:b2\n"
			+ "*sendLine:\n"
			+ "*flush\n", instance2.getActions());

		// After some time, check that the oldest instance is closed, and a
		// single equation uses the other one.
		executable.makeSparesDue();
		instance2.addLines(RESULT_SUCCESS);
		executable.convertEquation(new InputTexDisplayEquation("d", null));
		assertEquals("*closeInstance\n", instance1.getActions());
		assertEquals(
			"*sendLine:TeX\n"
			+ "*sendLine:d\n"
			+ "*sendLine:\n"
			+ "*flush\n", instance2.getActions());
	}

	@Test
	public void testMultiThreadedFonts() throws Exception
	{
		// Try similar to the above but with different fonts so that it has to create
		// 2 even though they're quite quick.
		// Create first instance with 200ms delays.
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

		// Spin up a couple of threads.
		final LinkedList<Boolean> list = new LinkedList<Boolean>();
		final LinkedList<String> suffix = new LinkedList<String>();
		suffix.add("1");

		Runnable task1 = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					// Convert 2 equations with 100ms gap.
					executable.convertEquation(new InputTexDisplayEquation(
						"a" + suffix.getFirst(), null));
					Thread.sleep(100);
					executable.convertEquation(new InputTexDisplayEquation(
						"c" + suffix.getFirst(), null));
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
		Runnable task2 = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					// Convert 1 equation after 100ms.
					Thread.sleep(100);
					executable.convertEquation(new InputTexDisplayEquation(
						"b" + suffix.getFirst(), "STIX-Web"));
					synchronized(list)
					{
						list.add(true);
						list.notifyAll();
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
					synchronized(list)
					{
						list.add(false);
						list.notifyAll();
					}
				}
			}
		};
		new Thread(task1, "Task1-1").start();
		new Thread(task2, "Task2-1").start();

		synchronized(list)
		{
			while(list.size() < 2)
			{
				list.wait();
			}
		}

		// Check all succeeded.
		assertArrayEquals(new Boolean[] { true, true }, list.toArray(new Boolean[2]));

		// Check the instances have been set up with the right fonts.
		assertEquals("TeX", instance1.getFont());
		assertEquals("STIX-Web", instance2.getFont());

		// Check instance 1 includes requests for a and c.
		assertEquals(
			"*sendLine:TeX\n"
			+ "*sendLine:a1\n"
			+ "*sendLine:\n"
			+ "*flush\n"
			+ "*sendLine:TeX\n"
			+ "*sendLine:c1\n"
			+ "*sendLine:\n"
			+ "*flush\n", instance1.getActions());

		// Check instance 2 has done b.
		assertEquals(
			"*sendLine:TeX\n"
			+ "*sendLine:b1\n"
			+ "*sendLine:\n"
			+ "*flush\n", instance2.getActions());

		// Also check that the one with default font is left when flushing spares,
		// even though it's oldest.
		executable.makeSparesDue();
		instance1.addLines(RESULT_SUCCESS);
		executable.convertEquation(new InputTexDisplayEquation("d", null));
		assertEquals("*closeInstance\n", instance2.getActions());
		assertEquals(
			"*sendLine:TeX\n"
			+ "*sendLine:d\n"
			+ "*sendLine:\n"
			+ "*flush\n", instance1.getActions());
	}

}
