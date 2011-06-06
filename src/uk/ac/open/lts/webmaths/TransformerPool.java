package uk.ac.open.lts.webmaths;

import java.io.*;
import java.util.*;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamSource;

/**
 * Class that manages a pool of Transformer objects so we don't have to
 * construct one for every call. (Transformers are not thread-safe.)
 */
public class TransformerPool
{
	private List<Transformer> transformerPool = new LinkedList<Transformer>();
	private Class<?> refClass;
	private String fileName, typeParam;
	private MathmlEntityFixer fixer;
	
	/**
	 * Constructs a transformer pool for returning instances of a certain
	 * XSL file.
	 * @param fixer Fixer used to replace entities in the XSL file
	 * @param refClass Class that file is stored relative to
	 * @param fileName Filename
	 * @param typeParam Value of TYPE parameter in xsl
	 */
	public TransformerPool(MathmlEntityFixer fixer,
		Class<?> refClass, String fileName, String typeParam)
	{
		this.refClass = refClass;
		this.fileName = fileName;
		this.typeParam = typeParam;
		this.fixer = fixer;
	}
	
	/**
	 * Constructs a transformer pool for returning instances of a certain
	 * XSL file.
	 * @param fixer Fixer used to replace entities in the XSL file
	 * @param refClass Class that file is stored relative to
	 * @param fileName Filename
	 */
	public TransformerPool(MathmlEntityFixer fixer,
		Class<?> refClass, String fileName)
	{
		this(fixer, refClass, fileName, null);
	}
	
	/**
	 * Reserves a Transformer object. After this returns you must call 
	 * {@link #release(Transformer)}.
	 * @return A Transformer object that you can use
	 * @throws IOException Any error
	 * @throws TransformerConfigurationException Error with transformer
	 */
	public synchronized Transformer reserve() throws IOException,
		TransformerConfigurationException
	{
		if (transformerPool.isEmpty())
		{
			// Load to string
			BufferedReader reader = new BufferedReader(new InputStreamReader(
				refClass.getResourceAsStream(fileName), "UTF-8"));
			StringWriter writer = new StringWriter();
			while(true)
			{
				String line = reader.readLine();
				if(line == null)
				{
					break;
				}
				writer.write(line);
				writer.write('\n');
			}
			reader.close();

			// Get string and fix entities
			String xml = fixer.fix(writer.toString());
			
			// Create transformer
			Transformer t = TransformerFactory.newInstance().newTransformer(
				new StreamSource(new StringReader(xml)));
			if(typeParam != null)
			{
				t.setParameter("TYPE", typeParam);
			}
			return t;
		}
		else
		{
			return transformerPool.remove(0);
		}
	}
	
	/**
	 * Releases the transformer object, adding it back to the queue.
	 * @param t Transformer to release
	 */
	public synchronized void release(Transformer t)
	{
		transformerPool.add(t);
	}
}
