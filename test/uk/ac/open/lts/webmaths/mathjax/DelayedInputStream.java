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

/**
 * Special input stream that behaves like a slow connection.
 */
class DelayedInputStream extends InputStream
{
	private boolean gotData;
	private byte current;

	DelayedInputStream(final byte[] data, final int msPerByte)
	{
		new Thread(new Runnable()
		{
			public void run()
			{
				long start = System.currentTimeMillis();
				for(int i = 0; i < data.length; i++)
				{
					while(true)
					{
						long now = System.currentTimeMillis();
						long wait = (start + (i + 1) * msPerByte) - now;
						if(wait <= 0)
						{
							add(data[i]);
							break;
						}
						else
						{
							try
							{
								Thread.sleep(wait);
							}
							catch(InterruptedException e)
							{
								e.printStackTrace();
							}
						}
					}
				}
			}
		}).start();
	}

	private synchronized void add(byte b)
	{
		while(gotData)
		{
			try
			{
				wait();
			}
			catch(InterruptedException e)
			{
			}
		}
		current = b;
		gotData = true;
		notifyAll();
	}

	@Override
	public synchronized int read() throws IOException
	{
		while(!gotData)
		{
			try
			{
				wait();
			}
			catch(InterruptedException e)
			{
			}
		}
		byte result = current;
		gotData = false;
		notifyAll();
		return result;
	}
}