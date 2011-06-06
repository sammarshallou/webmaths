package uk.ac.open.lts.webmaths;

import javax.jws.WebService;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;

import uk.ac.open.lts.webmaths.english.*;


@WebService(endpointInterface="uk.ac.open.lts.webmaths.english.MathsEnglishPort")
public class WebMathsEnglish extends WebMathsService implements MathsEnglishPort
{
	private TransformerPool normaliseXsl, mainXsl;
	
	/**
	 * @param fixer Entity fixer
	 */
	public WebMathsEnglish(MathmlEntityFixer fixer)
	{
		super(fixer);
		normaliseXsl = new TransformerPool(fixer,
			WebMathsEnglish.class, "normalise.xsl", "english");
		mainXsl = new TransformerPool(fixer, 
			WebMathsEnglish.class, "english.main.xsl"); 
	}
	
	@Override
	public MathsEnglishReturn getEnglish(MathsEnglishParams params)
	{
		MathsEnglishReturn result = new MathsEnglishReturn();
		result.setOk(false);
		result.setError("");
		result.setEnglish("");
		try
		{
			// Parse input
			Document doc = parseMathml(params.getMathml());
			
			// Normalise using XSL
			DOMSource in = new DOMSource(doc);
			DOMResult out = new DOMResult(); 
			Transformer t = normaliseXsl.reserve();
			try
			{
				t.transform(in, out);
			}
			finally
			{
				normaliseXsl.release(t);
			}

			// Add table column attributes
			Document intermediate = (Document)out.getNode();
			addTableAttributes(intermediate);
			display(intermediate);
			
			// Do conversion to English
			in = new DOMSource(intermediate);
			out = new DOMResult();
			t = mainXsl.reserve();
			try
			{
				t.transform(in, out);
				display(out.getNode());
			}
			finally
			{
				mainXsl.release(t);
			}
			Document after = (Document)out.getNode();
			StringBuilder text = new StringBuilder();
			for(Node child = after.getDocumentElement().getFirstChild();
				child != null; child = child.getNextSibling())
			{
				if(child instanceof Text)
				{
					text.append(child.getNodeValue());
				}
			}
			
			// Fix entities and whitespace
			String speech = text.toString();
			speech = getFixer().toSpeech(speech);
			
			// Set the result 
			System.out.println("<<\n" + speech + "\n>>");
			result.setEnglish(speech);
			result.setOk(true);
			return result;
		}
		catch(Throwable t)
		{
			result.setError("MathML unexpected error - " + t.getMessage());
			t.printStackTrace();
			return result;
		}
	}

	/**
	 * Adds information about table columns, as x_* attributes, to all tables in
	 * given document.
	 * @param doc Document to modify
	 */
	private void addTableAttributes(Document doc)
	{
		NodeList tables = doc.getElementsByTagNameNS(
			"http://www.w3.org/1998/Math/MathML", "mtable");
		for(int i=0; i<tables.getLength(); i++)
		{
			addTableAttributes((Element)tables.item(i));
		}
	}
	
	/**
	 * Adds information about table columns, as x_* attributes, to the specified
	 * element (which should be an <mtable>).
	 * <p>
	 * This function will not behave correctly if colspan and rowspan are
	 * specified in a manner that would overlap.
	 * @param mtable Table element
	 */
	private void addTableAttributes(Element mtable)
	{
		int maxRows=0, maxCols=0;
		
		// Lists reserved (rowspan) columns. Indexed by column index, 0-based.
		// Data values are the number of rows that the column is reserved for
		// (reduced each time around the loop) or 0 if not reserved.
		int[] reserved = new int[0];
		
		// Note: We assume that all child elements are mtr or mlabeledtr, and
		// all child elements of those are mtd, which should be true because of 
		// the fix in normalise.xsl
		int row=0;
		for(Node child = mtable.getFirstChild(); child!=null;
			child = child.getNextSibling())
		{
			if(!(child instanceof Element))
			{
				continue;
			}
			Element mtr = (Element)child;
			boolean hasLabel = mtr.getLocalName().equals("mlabeledtr");

			mtr.setAttribute("x_row", "" + (row+1));
			
			// Work out following row's reserved array
			int[] nextReserved = new int[reserved.length];
			for(int i=0; i<reserved.length; i++)
			{
				nextReserved[i] = reserved[i] == 0 ? 0 : reserved[i] - 1;
			}
			
			int col = findFreeCol(reserved, 0);
			for(Node grandchild = mtr.getFirstChild(); grandchild!=null;
				grandchild = grandchild.getNextSibling())
			{
				if(!(grandchild instanceof Element))
				{
					continue;
				}
				if(hasLabel)
				{
					// Skip label
					hasLabel = false;
					continue;
				}
				Element mtd = (Element)grandchild;
				
				// Set this entry's position (1-based)
				mtd.setAttribute("x_col", "" + (col+1));
				
				// Get colspan and rowspan
				int colspan = mtd.hasAttribute("colspan")
					? Integer.parseInt(mtd.getAttribute("colspan")) : 1;
				int rowspan = mtd.hasAttribute("rowspan")
					? Integer.parseInt(mtd.getAttribute("rowspan")) : 1;

			  // Update maximum counts
			  maxRows = Math.max(maxRows, row + rowspan);
			  maxCols = Math.max(maxCols, col + colspan);
			  
			  // If rowspan, add to reserved list for next row
			  if(rowspan > 1)
			  {
			  	// May need to make array bigger
			  	int extent = row + rowspan;
			  	if(nextReserved.length < extent)
			  	{
			  		int[] bigger = new int[extent];
			  		System.arraycopy(nextReserved, 0, bigger, 0, nextReserved.length);
			  		nextReserved = bigger;
			  	}
			  	
			  	// Put it in the array
			  	for(int reserveCol=col; reserveCol<extent; reserveCol++)
			  	{
			  		// Take into account that we already did this row
			  		nextReserved[reserveCol] = rowspan-1;
			  	}
			  }
					
				// Go to next column
				col += colspan;
				col = findFreeCol(reserved, col);
			}
			
			row++;
			reserved = nextReserved;
		}
	
		// Store the max count
		mtable.setAttribute("x_cols", "" + maxCols);
		mtable.setAttribute("x_rows", "" + maxRows);
	}
	
	/**
	 * Finds the next free column based on the reserved array.
	 * @param reserved Array indicating that certain columns (with >0 in the
	 *   value) are reserved
	 * @param start First position to look in (may be past the end of array)
	 * @return First valid position
	 */
	private static int findFreeCol(int[] reserved, int start)
	{
		int col;
		for(col = start; col < reserved.length && reserved[col] > 0; col++) ;
		return col;
	}
	
	private void display(Node node) throws Exception
	{
		DOMSource in = new DOMSource(node);
		System.out.println();
		TransformerFactory.newInstance().newTransformer().transform(in, 
			new StreamResult(System.out));
		System.out.println();
	}

}
