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
					long now = System.currentTimeMillis();
					if (now - start < (i + 1) * msPerByte)
					{
						add(data[i]);
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