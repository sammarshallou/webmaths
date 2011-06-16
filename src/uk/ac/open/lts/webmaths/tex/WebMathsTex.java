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

import uk.ac.open.lts.webmaths.*;

@WebService(endpointInterface="uk.ac.open.lts.webmaths.tex.MathsTexPort",
	targetNamespace="http://ns.open.ac.uk/lts/vle/filter_maths/",
	serviceName="MathsTex", portName="MathsTexPort")
public class WebMathsTex extends WebMathsService implements MathsTexPort
{
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
