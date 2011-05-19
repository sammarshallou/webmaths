package uk.ac.open.lts.webmaths;

import java.util.regex.Pattern;

import javax.jws.WebService;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;

import uk.ac.open.lts.webmaths.english.*;


@WebService(endpointInterface="uk.ac.open.lts.webmaths.english.MathsEnglishPort")
public class WebMathsEnglish extends WebMathsService implements MathsEnglishPort
{
	private final static Pattern REGEX_WHITESPACE = Pattern.compile("\\s+");
	private TransformerPool normaliseXsl, mainXsl;
	
	/**
	 * @param fixer Entity fixer
	 */
	public WebMathsEnglish(MathmlEntityFixer fixer)
	{
		super(fixer);
		normaliseXsl = new TransformerPool(fixer,
			WebMathsEnglish.class, "normalise.xsl");
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
			
			// Convert using XSL
			DOMSource in = new DOMSource(doc);
			DOMResult out = new DOMResult(); 
			Transformer t = normaliseXsl.reserve();
			try
			{
				t.transform(in, out);
				display(out);
			}
			finally
			{
				normaliseXsl.release(t);
			}

			in = new DOMSource((Document)out.getNode());
			out = new DOMResult();
			t = mainXsl.reserve();
			try
			{
				t.transform(in, out);
				display(out);
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
			
			// Fix up whitespace
			String speech = text.toString();
			speech = REGEX_WHITESPACE.matcher(speech).replaceAll(" ").trim();
			
			// Set the result 
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
	
	private void display(DOMResult result) throws Exception
	{
		DOMSource in = new DOMSource(result.getNode());
		System.out.println();
		TransformerFactory.newInstance().newTransformer().transform(in, 
			new StreamResult(System.out));
		System.out.println();
	}

}
