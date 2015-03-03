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
import java.util.EnumSet;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

import static uk.ac.open.lts.webmaths.mathjax.ConversionType.*;

import uk.ac.open.lts.webmaths.WebMathsService;

@WebService(endpointInterface="uk.ac.open.lts.webmaths.mathjax.MathsMathJaxPort",
	targetNamespace="http://ns.open.ac.uk/lts/vle/filter_maths/",
	serviceName="MathsMathJax", portName="MathsMathJaxPort")
public class WebMathsMathJax extends WebMathsService implements MathsMathJaxPort
{
	@Resource
	private WebServiceContext context;

	@Override
	public ConvertEquationsReturn convertEquations(ConvertEquationsParams params)
	{
		ConvertEquationsReturn result = new ConvertEquationsReturn();
		MathJax mathJax = MathJax.get(context);

		EnumSet<ConversionType> types = EnumSet.noneOf(ConversionType.class);
		types.addAll(params.getOutputs());

		// Process all the input equations in order.
		for(SourceEquation equation : params.getEquations())
		{
			OutputData out = new OutputData();
			out.setOk(false);

			InputEquation eq = InputEquation.getFromSourceEquation(equation);

			try
			{
				// We need the pixel SVG for lots of things.
				String pixelSvg = null, exSvg = null;
				if(types.contains(SVG_PX) || types.contains(PNG) ||
					types.contains(PNG_BASELINE) || types.contains(SVG_PX_BASELINE) ||
					types.contains(TEXT))
				{
					pixelSvg = mathJax.getSvg(eq, true, params.getExSize(), params.getRgb());
				}
				if(types.contains(SVG_EX) || types.contains(SVG_EX_BASELINE))
				{
					exSvg = mathJax.getSvg(eq, true, MathJax.SIZE_IN_EX, params.getRgb());
				}

				// If SVG was turned on, store it.
				if(types.contains(SVG_EX))
				{
					out.setSvg(exSvg);
				}
				else if(types.contains(SVG_PX))
				{
					out.setSvg(pixelSvg);
				}

				if(types.contains(PNG))
				{
					out.setPng(mathJax.getPngFromSvg(pixelSvg));
				}

				if(types.contains(TEXT))
				{
					out.setText(mathJax.getEnglishFromSvg(pixelSvg));
				}

				if(types.contains(SVG_PX_BASELINE))
				{
					out.setSvgPxBaseline((float)mathJax.getBaselineFromSvg(pixelSvg));
				}
				if(types.contains(SVG_EX_BASELINE))
				{
					out.setSvgExBaseline((float)mathJax.getExBaselineFromSvg(exSvg));
				}
				if(types.contains(PNG_BASELINE))
				{
					out.setPngBaseline((float)mathJax.getBaselineFromSvg(
						MathJax.offsetSvg(pixelSvg, MathJax.PNG_OFFSET)));
				}

				out.setOk(true);
			}
			catch(MathJaxException e)
			{
				out.setError("MathJax failure: " + e.getMessage());
			}
			catch(IOException e)
			{
				out.setError("Unexpected error: " + e.getMessage());
			}

			result.getOutput().add(out);
		}

		return result;
	}

}
