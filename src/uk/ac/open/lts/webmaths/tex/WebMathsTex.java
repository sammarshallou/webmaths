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

Copyright 2011 The Open University
*/
package uk.ac.open.lts.webmaths.tex;

import javax.jws.WebService;

import org.w3c.dom.Document;

import uk.ac.open.lts.webmaths.*;

@WebService(endpointInterface="uk.ac.open.lts.webmaths.tex.MathsTexPort",
	targetNamespace="http://ns.open.ac.uk/lts/vle/filter_maths/",
	serviceName="MathsTex", portName="MathsTexPort")
public class WebMathsTex extends WebMathsService implements MathsTexPort
{
	private MathmlToLatex texConverter;

	@Override
	public MathsTexReturn getMathml(MathsTexParams params)
	{
		// Set up default return values
		MathsTexReturn result = new MathsTexReturn();
		result.setOk(false);
		result.setError("");
		result.setMathml("");
		
		try
		{
			// Convert TeX to MathML
			TokenInput input = new TokenInput(params.getTex());
			String mathml = input.toMathml(params.isDisplay());
			
			result.setMathml(mathml);
			result.setOk(true);
		}
		catch(Throwable t)
		{
			t.printStackTrace(); // TODO Get rid of this or log somehow
			result.setError(t.getMessage());
		}
		
		return result;
	}

	@Override
	public GetTexReturn getTex(GetTexParams params)
	{
		// Set up default return values
		GetTexReturn result = new GetTexReturn();
		result.setOk(false);
		result.setError("");
		result.setTex("");

		// Set up the converter if we didn't already
		synchronized(this)
		{
			if(texConverter == null)
			{
				texConverter = new MathmlToLatex(getFixer());
			}
		}

		try
		{
			// Parse MathML
			Document doc = parseMathml(params.getMathml());
			// Convert MathML to TeX
			result.setTex(texConverter.convert(doc, params.isLenient()));
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
