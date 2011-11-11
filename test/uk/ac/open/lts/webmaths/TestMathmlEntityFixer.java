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
package uk.ac.open.lts.webmaths;

import static uk.ac.open.lts.webmaths.MathmlEntityFixer.getWithHexEntities;
import junit.framework.TestCase;

import org.junit.Test;

public class TestMathmlEntityFixer extends TestCase
{
	@Test
	public void testHexNothing()
	{
		assertEquals("hello world", getWithHexEntities("hello world"));
	}
	
	@Test
	public void testHexExisting()
	{
		assertEquals("hey&#x1234a;you", getWithHexEntities("hey&#x1234a;you"));
	}
	
	@Test
	public void testHexExistingZeros()
	{
		assertEquals("hey&#x12;you", getWithHexEntities("hey&#x00000012;you"));
	}
	
	@Test
	public void testHexExistingCase()
	{
		assertEquals("hey&#xabcde;you", getWithHexEntities("hey&#xABCDE;you"));
	}
	
	@Test
	public void testHexHighChar()
	{
		assertEquals("caf&#xe9;", getWithHexEntities("caf\u00e9"));
	}
	
	@Test
	public void testHexReallyHighChar()
	{
		assertEquals("&#x2a6d6;", getWithHexEntities("\ud869\uded6"));
	}
	
	@Test
	public void testCombined()
	{
		assertEquals("hey&#x1234a;you&#xabcde;&#xe9;&#x2a6d6;",
			getWithHexEntities("hey&#x1234a;you&#xABCDE;\u00e9\ud869\uded6"));
	}

	@Test
	public void testXmlEntities() throws Exception
	{
		MathmlEntityFixer fixer = new MathmlEntityFixer();
		String testString = "&amp;&lt;&gt;&apos;&quot;";
		assertEquals(testString, fixer.fix(testString));
	}

	@Test
	public void testToSpeech() throws Exception
	{
		MathmlEntityFixer fixer = new MathmlEntityFixer();
		assertEquals("and therefore", fixer.toSpeech("and \u2234"));
	}
}
