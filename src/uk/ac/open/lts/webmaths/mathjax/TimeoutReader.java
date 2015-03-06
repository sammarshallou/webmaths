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
import java.nio.charset.Charset;

/**
 * Line reader that includes support for timeouts.
 */
class TimeoutReader implements Runnable
{
	private final static int MAXIMUM_LINE_LENGTH = 256 * 1024;

	private InputStream in;
	private boolean threadExit = false, exitRequested = false;
	private String currentLine = null;

	/**
	 * @param in Input stream to read
	 */
	TimeoutReader(InputStream in)
	{
		this.in = in;
		(new Thread(this, "stdout reader")).start();
	}

	@Override
	public void run()
	{
		byte[] buffer = new byte[MAXIMUM_LINE_LENGTH];
		try
		{
			int pos = 0;
			while(true)
			{
				int read = in.read();
				if(read == -1)
				{
					// At EOF, use everything that's left as a string.
					synchronized(this)
					{
						currentLine = new String(buffer, 0, pos, Charset.forName("UTF-8"));
						pos = 0;
						notifyAll();
						while(currentLine != null && !exitRequested)
						{
							try
							{
								wait();
							}
							catch(InterruptedException e)
							{
							}
						}
						return;
					}
				}
				else if(read == 13)
				{
					// Ignore CR in CRLF.
					continue;
				}
				else if(read == 10)
				{
					// At LF, we have a string, so stop and wait.
					synchronized(this)
					{
						currentLine = new String(buffer, 0, pos, Charset.forName("UTF-8"));
						pos = 0;
						notifyAll();
						while(currentLine != null && !exitRequested)
						{
							try
							{
								wait();
							}
							catch(InterruptedException e)
							{
							}
						}
					}
				}
				else
				{
					if(pos == MAXIMUM_LINE_LENGTH)
					{
						throw new IOException("Exceeded maximum line length");
					}
					buffer[pos++] = (byte)(read & 0xff);
				}
			}
		}
		catch(IOException e)
		{
			// Ignore.
		}
		finally
		{
			synchronized(this)
			{
				threadExit = true;
				notifyAll();
			}
		}
	}

	/**
	 * Gets next line, waiting for a given timeout.
	 * @param timeout Timeout in milliseconds
	 * @return Line
	 * @throws IOException If there is no line within the timeout period
	 */
	public String getNextLine(long timeout) throws IOException
	{
		synchronized(this)
		{
			if(currentLine != null)
			{
				String result = currentLine;
				currentLine = null;
				notifyAll();
				return result;
			}
			try
			{
				wait(timeout);
			}
			catch(InterruptedException e)
			{
			}
			if(currentLine != null)
			{
				String result = currentLine;
				currentLine = null;
				notifyAll();
				return result;
			}
			throw new IOException("Timeout reading line from process");
		}
	}

	/**
	 * Requests exist. (Should also close the process after this.)
	 */
	public void requestExit()
	{
		synchronized(this)
		{
			exitRequested = true;
			notifyAll();
		}
	}

	/**
	 * Waits for the thread to exit.
	 */
	public void waitForExit()
	{
		try
		{
			synchronized(this)
			{
				if(!exitRequested)
				{
					throw new IllegalStateException("Must call requestExit first");
				}
				exitRequested = true;
				notifyAll();
				while(!threadExit)
				{
					wait();
				}
			}
		}
		catch(InterruptedException e)
		{
			// Ignore.
		}
	}
}