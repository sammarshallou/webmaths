package uk.ac.open.lts.webmaths.tex;

import javax.jws.WebService;

import uk.ac.open.lts.webmaths.*;

@WebService(endpointInterface="uk.ac.open.lts.webmaths.tex.MathsTexPort")
public class WebMathsTex extends WebMathsService implements MathsTexPort
{
	/**
	 * @param fixer Entity fixer (not used)
	 */
	public WebMathsTex(MathmlEntityFixer fixer)
	{
		super(fixer);
	}

	@Override
	public MathsTexReturn getMathml(MathsTexParams params)
	{
		// Set up default return values
		MathsTexReturn result = new MathsTexReturn();
		result.setOk(false);
		result.setError("");
		result.setMathml("");
		
		// TODO The display parameter should be used somehow
		// boolean display = params.isDisplay();
		
		try
		{
			// Convert TeX to MathML
			TokenInput input = new TokenInput(params.getTex());
			result.setMathml(input.toMathml());
			result.setOk(true);
		}
		catch(Throwable t)
		{
			t.printStackTrace(); // TODO Get rid of this or log somehow
			result.setError(t.getMessage());
		}
		
		return result;
	}
}
