package uk.ac.open.lts.webmaths;

import java.util.*;

public abstract class MapUtil
{

	public static Map<String, String> makeMap(String[] data)
	{
		Map<String, String> result = new HashMap<String, String>();
		for(int i=0; i<data.length; i+=2)
		{
			String previous = result.put(data[i], data[i+1]);
			if(previous != null)
			{
				Error e = new Error("Duplicate map entry: " + data[i] + "=" + data[i+1]
					+ " (previously " + previous + ")");
				System.err.println(e.getMessage());
				throw e;
			}
		}
		return result;
	}

}
