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

import java.io.IOException;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

import uk.ac.open.lts.webmaths.WebMathsService;
import uk.ac.open.lts.webmaths.english.*;
import uk.ac.open.lts.webmaths.mathjax.*;

@WebService(endpointInterface="uk.ac.open.lts.webmaths.english.MathsEnglishPort",
	targetNamespace="http://ns.open.ac.uk/lts/vle/filter_maths/",
	serviceName="MathsEnglish", portName="MathsEnglishPort")
public class WebMathsMjEnglish extends WebMathsService implements MathsEnglishPort
{
	@Resource
	private WebServiceContext context;

	@Override
	public MathsEnglishReturn getEnglish(MathsEnglishParams params)
	{
		MathsEnglishReturn result = new MathsEnglishReturn();
		result.setOk(false);
		result.setError("");
		result.setEnglish("");

		try
		{
			result.setEnglish(MathJax.get(context).getEnglish(
				new InputMathmlEquation(params.getMathml())));
			result.setOk(true);
		}
		catch(MathJaxException e)
		{
			result.setError("MathJax failure: " + e.getMessage());
		}
		catch(IOException e)
		{
			result.setError("Unexpected error: " + e.getMessage());
		}

		return result;
	}
}
