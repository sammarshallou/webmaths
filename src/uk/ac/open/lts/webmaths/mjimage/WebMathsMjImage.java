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
package uk.ac.open.lts.webmaths.mjimage;

import java.io.IOException;
import java.math.BigInteger;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

import uk.ac.open.lts.webmaths.WebMathsService;
import uk.ac.open.lts.webmaths.image.*;
import uk.ac.open.lts.webmaths.mathjax.*;
import uk.ac.open.lts.webmaths.mathjax.MathJax.PngResult;

@WebService(endpointInterface="uk.ac.open.lts.webmaths.image.MathsImagePort",
	targetNamespace="http://ns.open.ac.uk/lts/vle/filter_maths/",
	serviceName="MathsImage", portName="MathsImagePort")
public class WebMathsMjImage extends WebMathsService implements MathsImagePort
{
	@Resource
	private WebServiceContext context;

	private final static byte[] EMPTY = new byte[0];

	@Override
	public MathsImageReturn getImage(MathsImageParams params)
	{
		MathsImageReturn result = new MathsImageReturn();
		result.setOk(false);
		result.setImage(EMPTY);
		result.setError("");
		result.setBaseline(BigInteger.valueOf(0));

		try
		{
			PngResult png = MathJax.get(context).getPng(
				InputEquation.getFromMathml(params.getMathml()),
				params.getRgb(), params.getSize());

			result.setImage(png.getPng());
			result.setBaseline(BigInteger.valueOf(png.getBaseline()));
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

	@Override
	public MathsEpsReturn getEps(MathsEpsParams params)
	{
		MathsEpsReturn result = new MathsEpsReturn();
		result.setOk(false);
		result.setEps(EMPTY);
		result.setError("");

		return result;
	}
}
