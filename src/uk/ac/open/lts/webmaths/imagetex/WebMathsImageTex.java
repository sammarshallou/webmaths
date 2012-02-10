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
package uk.ac.open.lts.webmaths.imagetex;

import javax.jws.WebService;

import uk.ac.open.lts.webmaths.WebMathsService;
import uk.ac.open.lts.webmaths.image.*;

@WebService(endpointInterface="uk.ac.open.lts.webmaths.image.MathsImagePort",
	targetNamespace="http://ns.open.ac.uk/lts/vle/filter_maths/",
	serviceName="MathsImageTex", portName="MathsImagePort")
public class WebMathsImageTex extends WebMathsService implements MathsImagePort
{
	private static boolean SHOWPERFORMANCE = false;
	private final static byte[] EMPTY = new byte[0];

	private MathmlToLatex converter;

	@Override
	public MathsImageReturn getImage(MathsImageParams params)
	{
		long start = System.currentTimeMillis();
		MathsImageReturn result = new MathsImageReturn();
		result.setOk(false);
		result.setError("");
		result.setImage(EMPTY);

		try
		{
			result.setError("Not yet implemented");
			if(SHOWPERFORMANCE)
			{
				System.err.println("Setup: " + (System.currentTimeMillis() - start));
			}
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
	 * @return Converter used to change MathML to LaTeX
	 */
	synchronized protected MathmlToLatex getMathmlToLatex()
	{
		if(converter == null)
		{
			converter = new MathmlToLatex(getFixer());
		}
		return converter;
	}
}
