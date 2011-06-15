<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:m="http://www.w3.org/1998/Math/MathML">

<!--
  Root template 
  -->
<xsl:template match="/m:math">
<result>
<xsl:apply-templates/>
</result>
</xsl:template>

<!--
  TOKEN ELEMENTS (3.1.6.1)
  ***************************************************************************
 -->

<!--
  <mglyph> 
  http://www.w3.org/TR/MathML2/chapter3.html#presm.mglyph
  Uses alt attribute.
  -->
<xsl:template match="m:mglyph">
  <xsl:text> </xsl:text><xsl:value-of select="@alt"/><xsl:text> </xsl:text>
</xsl:template>

<!--
  <ms> 
  http://www.w3.org/TR/MathML2/chapter3.html#presm.ms
  Puts quotes around content.
  -->
<xsl:template match="m:ms">
  <xsl:text>"</xsl:text><xsl:apply-templates/><xsl:text>"</xsl:text>
</xsl:template>

<!--
  <mspace> 
  http://www.w3.org/TR/MathML2/chapter3.html#presm.mspace
  -->
<xsl:template match="m:mspace">
  <xsl:text> </xsl:text>
</xsl:template>

<!--
  <mo> 
  http://www.w3.org/TR/MathML2/chapter3.html#presm.mo
  -->
<xsl:template match="m:mo" priority="+10">
  <xsl:text> </xsl:text><xsl:value-of select="."/><xsl:text> </xsl:text>
</xsl:template>

<!--
  <mi>, <mn>, <mtext> no special handling 
  -->

<!--
  GENERAL LAYOUT SCHEMATA (3.1.6.2)
  ***************************************************************************
 -->

<!-- 
  <mrow> (with more than one item, and not the one directly inside <math>)
  gets brackets added
  http://www.w3.org/TR/MathML2/chapter3.html#presm.mrow
  -->
<xsl:template match="m:mrow">
    <xsl:choose>
        <!-- Root one doesn't get brackets -->
        <xsl:when test="parent::m:math">
            <xsl:apply-templates/>
        </xsl:when>
        <!-- Only one child, no brackets -->
        <xsl:when test="count(*) = 1">
            <xsl:apply-templates/>
        </xsl:when>
        <!-- Children already include displayed brackets, no brackets -->
        <xsl:when test="*[1][self::m:mo[string(.)='(']] and 
            *[position()=last()][self::m:mo[string(.)=')']]">
            <xsl:apply-templates/>
        </xsl:when>
        <!-- Siblings are displayed brackets, no brackets -->
        <xsl:when test="preceding-sibling::*[1][self::m:mo[string(.)='(']] and 
            following-sibling::*[1][self::m:mo[string(.)=')']]">
            <xsl:apply-templates/>
        </xsl:when>
        <xsl:otherwise>
            <!-- Add brackets! -->
            <xsl:text> ( </xsl:text>
            <xsl:apply-templates/>
            <xsl:text> ) </xsl:text>
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<!--
  <mfrac>
  http://www.w3.org/TR/MathML2/chapter3.html#presm.mfrac 
  -->
<xsl:template match="m:mfrac">
    (
    <xsl:apply-templates select="*[1]"/>
    over
    <xsl:apply-templates select="*[2]"/>
    )
</xsl:template>

<!--
  <msqrt>
  http://www.w3.org/TR/MathML2/chapter3.html#presm.mroot 
  -->
<xsl:template match="m:msqrt">
    square root of
    <xsl:apply-templates/>
</xsl:template>

<!--
  <mroot> - numeric root 
  -->
<xsl:template match="m:mroot[child::*[position()=2 and self::m:mn]]">
    <xsl:value-of select="child::*[position()=2 and self::m:mn]"/>th root of
    <xsl:apply-templates select="*[1]"/>
</xsl:template>

<!--
  <mroot> - square root
  http://www.w3.org/TR/MathML2/chapter3.html#presm.mroot 
  -->
<xsl:template match="m:mroot[child::*[position()=2 and self::m:mn and normalize-space(.) = '2']]">
    square root of
    <xsl:apply-templates select="*[1]"/>
</xsl:template>

<!--
  <mroot> - cube root
  http://www.w3.org/TR/MathML2/chapter3.html#presm.mroot 
  -->
<xsl:template match="m:mroot[child::*[position()=2 and self::m:mn and normalize-space(.) = '3']]">
    cube root of
    <xsl:apply-templates select="*[1]"/>
</xsl:template>

<!--
  <mroot> - textual root 
  -->
<xsl:template match="m:mroot">
    <xsl:apply-templates select="*[2]"/>
    -th root of
    <xsl:apply-templates select="*[1]"/>
</xsl:template>

<!--
  <merror>
  http://www.w3.org/TR/MathML2/chapter3.html#presm.merror 
  -->
<xsl:template match="m:merror">
    ERROR [
    <xsl:apply-templates/>
    ]
</xsl:template>

<!--
  <mphantom>
  http://www.w3.org/TR/MathML2/chapter3.html#presm.mphantom 
  -->
<xsl:template match="m:mphantom">
    <!-- mphantom is not rendered -->
</xsl:template>

<!--
  <menclose> 
  http://www.w3.org/TR/MathML2/chapter3.html#presm.menclose
 -->
<xsl:template match="m:menclose">
    <xsl:call-template name="menclose-to-text">
        <xsl:with-param name="NOTATION">
            <xsl:call-template name="tidy-whitespace">
                <xsl:with-param name="TEXT" select="@notation"/>
            </xsl:call-template>
        </xsl:with-param>
    </xsl:call-template>

    <xsl:apply-templates/>
</xsl:template>

<!-- 
  Outputs text based on the notation attribute of the menclose tag. Supports
  all attributes listed in MathML 2 spec.
  $NOTATION - value of notation attribute after it has been passed through
    tidy-whitespace
  Returns text (or none if no recognised attributes)
 -->
<xsl:template name="menclose-to-text">
    <xsl:param name="NOTATION"/>
    <xsl:variable name="NOTATIONSP" select="concat(' ', $NOTATION, ' ')"/>

    <xsl:variable name="LEFT">
        <xsl:if test="contains($NOTATIONSP, ' left ') or contains($NOTATIONSP, ' box ')">y</xsl:if>
    </xsl:variable> 
    <xsl:variable name="RIGHT">
        <xsl:if test="contains($NOTATIONSP, ' right ') or contains($NOTATIONSP, ' box ') or contains($NOTATIONSP, ' actuarial ')">y</xsl:if>
    </xsl:variable> 
    <xsl:variable name="TOP">
        <xsl:if test="contains($NOTATIONSP, ' top ') or contains($NOTATIONSP, ' box ') or contains($NOTATIONSP, ' actuarial ')">y</xsl:if>
    </xsl:variable> 
    <xsl:variable name="BOTTOM">
        <xsl:if test="contains($NOTATIONSP, ' bottom ') or contains($NOTATIONSP, ' box ')">y</xsl:if>
    </xsl:variable> 
    
    <!-- Box or lines -->
    <xsl:choose>
        <xsl:when test="$LEFT='y' and $RIGHT='y' and $TOP='y' and $BOTTOM='y'">
            <xsl:text>box around </xsl:text>
        </xsl:when>
        <xsl:when test="$LEFT='y' and $RIGHT='y' and $TOP='y'">
            <xsl:text>line around left, right and top </xsl:text>
        </xsl:when>
        <xsl:when test="$LEFT='y' and $RIGHT='y' and $BOTTOM='y'">
            <xsl:text>line around left, right and bottom </xsl:text>
        </xsl:when>
        <xsl:when test="$LEFT='y' and $TOP='y' and $BOTTOM='y'">
            <xsl:text>line around left, top and bottom </xsl:text>
        </xsl:when>
        <xsl:when test="$RIGHT='y' and $TOP='y' and $BOTTOM='y'">
            <xsl:text>line around right, top and bottom </xsl:text>
        </xsl:when>
        <xsl:when test="$LEFT='y' and $TOP='y'">
            <xsl:text>line around left and top </xsl:text>
        </xsl:when>
        <xsl:when test="$LEFT='y' and $BOTTOM='y'">
            <xsl:text>line around left and bottom </xsl:text>
        </xsl:when>
        <xsl:when test="$RIGHT='y' and $TOP='y'">
            <xsl:text>line around right and top </xsl:text>
        </xsl:when>
        <xsl:when test="$RIGHT='y' and $BOTTOM='y'">
            <xsl:text>line around right and bottom </xsl:text>
        </xsl:when>
        <xsl:when test="$LEFT='y' and $RIGHT='y'">
            <xsl:text>vertical lines to left and right </xsl:text>
        </xsl:when>
        <xsl:when test="$TOP='y' and $BOTTOM='y'">
            <xsl:text>horizontal lines to top and bottom </xsl:text>
        </xsl:when>
        <xsl:when test="$LEFT='y'">
            <xsl:text>line to left  </xsl:text>
        </xsl:when>
        <xsl:when test="$RIGHT='y'">
            <xsl:text>line to right  </xsl:text>
        </xsl:when>
        <xsl:when test="$TOP='y'">
            <xsl:text>line over  </xsl:text>
        </xsl:when>
        <xsl:when test="$BOTTOM='y'">
            <xsl:text>line under  </xsl:text>
        </xsl:when>
    </xsl:choose>
    
    <!-- Basic enclosures -->
    <xsl:if test="contains($NOTATIONSP, ' longdiv ')">
        <xsl:text>long division sign around </xsl:text>    
    </xsl:if>
    <xsl:if test="contains($NOTATIONSP, ' radical ')">
        <xsl:text>square root sign around </xsl:text>
    </xsl:if>
    <xsl:if test="contains($NOTATIONSP, ' roundedbox ')">
        <xsl:text>rounded box around </xsl:text>
    </xsl:if>
    <xsl:if test="contains($NOTATIONSP, ' circle ')">
        <xsl:text>circle around </xsl:text>
    </xsl:if>
    <xsl:if test="contains($NOTATIONSP, ' updiagonalstrike ')">
        <xsl:text>upward diagonal strike through </xsl:text>
    </xsl:if>
    <xsl:if test="contains($NOTATIONSP, ' downdiagonalstrike ')">
        <xsl:text>downward diagonal strike through </xsl:text>
    </xsl:if>
    <xsl:if test="contains($NOTATIONSP, ' verticalstrike ')">
        <xsl:text>vertical strike through </xsl:text>
    </xsl:if>
    <xsl:if test="contains($NOTATIONSP, ' horizontalstrike ')">
        <xsl:text>horizontal strike through </xsl:text>
    </xsl:if>
</xsl:template>

<!-- 
  The following elements have no special handling:
  <mstyle>
  <mpadded>
  
  The <mfenced> element will not be present as it is removed by the normalise
  XSL.
 -->

<!--
  SCRIPT AND LIMIT SCHEMATA (3.1.6.3)
  ***************************************************************************
 -->

<!--
  <msub>
  http://www.w3.org/TR/MathML2/chapter3.html#presm.msub 
  -->
<xsl:template match="m:msub">
    <xsl:apply-templates select="*[1]"/>
    subscript
    <xsl:apply-templates select="*[2]"/>
</xsl:template>

<!--
  <msup>
  http://www.w3.org/TR/MathML2/chapter3.html#presm.msup 
  -->
<xsl:template match="m:msup">
    <xsl:apply-templates select="*[1]"/>
    to the power
    <xsl:apply-templates select="*[2]"/>
</xsl:template>

<!--
  <msup> - squared
  -->
<xsl:template match="m:msup[child::*[position()=2 and self::m:mn and normalize-space(.) = '2']]">
    <xsl:apply-templates select="*[1]"/>
    squared
</xsl:template> 

<!--
  <msup> - cubed
  -->
<xsl:template match="m:msup[child::*[position()=2 and self::m:mn and normalize-space(.) = '3']]">
    <xsl:apply-templates select="*[1]"/>
    cubed
</xsl:template> 

<!-- 
  <msubsup> - limits on operator
  http://www.w3.org/TR/MathML2/chapter3.html#presm.msubsup
  When not applied to an operator, this is normalised to <msup><msub> in
  normalise.xsl so that the usual 'squared' etc can work.
  -->
<xsl:template match="m:msubsup">
  <xsl:apply-templates select="*[1]"/>
  subscript <xsl:apply-templates select="*[2]"/>
  superscript <xsl:apply-templates select="*[3]"/> 
</xsl:template>

<!--
  <msubsup> - integral, etc
  -->
<xsl:template match="m:msubsup[*[1][m:mo]]">
  <xsl:apply-templates select="*[1]"/>
  from <xsl:apply-templates select="*[2]"/>
  to <xsl:apply-templates select="*[3]"/> 
</xsl:template>

<!--
  <munder>
  http://www.w3.org/TR/MathML2/chapter3.html#presm.munder 
  -->
<xsl:template match="m:munder">
  <xsl:apply-templates select="*[2]"/>
  below <xsl:apply-templates select="*[1]"/>
</xsl:template>

<!--
  <mover>
  http://www.w3.org/TR/MathML2/chapter3.html#presm.mover 
  -->
<xsl:template match="m:mover">
  <xsl:apply-templates select="*[2]"/>
  above <xsl:apply-templates select="*[1]"/>
</xsl:template>

<!--
  <mover> for 'hat'
  -->
<xsl:template match="m:mover[@accent='true' and 
    string-length(normalize-space(*[1])) = 1 and
    *[2][self::m:mo and string(.)='&Hat;']]">
  <xsl:apply-templates select="*[1]"/>-hat
</xsl:template>


<!--
  <munderover>
  http://www.w3.org/TR/MathML2/chapter3.html#presm.munderover 
  -->
<xsl:template match="m:munderover">
  <xsl:apply-templates select="*[1]"/>
  (<xsl:apply-templates select="*[2]"/> below,
  <xsl:apply-templates select="*[3]"/> above) 
</xsl:template>

<!--
  <munderover> - sum, etc   
  -->
<xsl:template match="m:munderover[*[1][self::mo]]">
  <xsl:apply-templates select="*[1]"/>
  from <xsl:apply-templates select="*[2]"/>
  to <xsl:apply-templates select="*[3]"/> 
</xsl:template>

<!--
  <mmultiscripts> 
  http://www.w3.org/TR/MathML2/chapter3.html#presm.mmultiscripts
  -->
<xsl:template match="m:mmultiscripts">
  <!-- Base -->
  <xsl:apply-templates select="*[1]"/>
  
  (  
  
  <!-- Output all the super/subscript pairs that go BEFORE the base -->
  <xsl:if test="mprescripts">
    preceded by
    <xsl:for-each select="*[preceding-sibling::mprescripts]">
      <xsl:if test="not(self::none)">
        <xsl:choose>
          <xsl:when test="(count(preceding-sibling::*[preceding-sibling::mprescripts]) mod 2) = 0">
            subscript
          </xsl:when>
          <xsl:otherwise>
            superscript
          </xsl:otherwise> 
        </xsl:choose>
        <xsl:apply-templates select="."/>
      </xsl:if>
    </xsl:for-each>
  </xsl:if>

  <!-- Output all the super/subscript pairs that go AFTER the base -->  
  <xsl:for-each select="*[not(self::mprescripts) and not(preceding-sibling::mprescripts)]">
    <xsl:if test="not(self::none)">
      <xsl:choose>
        <xsl:when test="(count(preceding-sibling::*) mod 2) = 1">
          subscript
        </xsl:when>
        <xsl:otherwise>
          superscript
        </xsl:otherwise> 
      </xsl:choose>
      <xsl:apply-templates select="."/>
    </xsl:if>
  </xsl:for-each>
  
  }

</xsl:template>

<!--
  TABLES AND MATRICES (3.1.6.4)
  ***************************************************************************
 -->

<!--
  <mtable> 
  http://www.w3.org/TR/MathML2/chapter3.html#presm.mtable
  Relies on additional x_* attributes added in WebMathsEnglish class to
  resolve column and row indices.
  -->
<xsl:template match="m:mtable">
  <xsl:value-of select="@x_cols"/>
  by
  <xsl:value-of select="@x_rows"/>
  grid.
  
  <xsl:for-each select="*">
    <xsl:call-template name="output-mtr">
      <xsl:with-param name="COLS" select="../@x_cols"/>
    </xsl:call-template>  
  </xsl:for-each>

</xsl:template>

<xsl:template name="output-mtr">
  <xsl:param name="COLS"/>
  
  Row
  <xsl:value-of select="@x_row"/>
  <xsl:if test="self::m:mlabeledtr">
    <xsl:text> </xsl:text>
    <xsl:apply-templates select="*[1]"/>
  </xsl:if>
  :
  
  <xsl:for-each select="m:mtd">
    <!-- Do extra blanks if there were missing columns before this one -->
    <xsl:choose>
      <xsl:when test="position()=1">
        <!-- For first one, do all blanks before 1 -->
        <xsl:call-template name="output-mtr-extra-blanks">
          <xsl:with-param name="NUM" select="number(@x_col) - 1"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <!-- For other columns, do all blanks between columns -->
        <xsl:call-template name="output-mtr-extra-blanks">
          <xsl:with-param name="NUM" select="number(@x_col) - 
            number(preceding-sibling::m:mtd[1]/@x_col) - 1"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
    
    <!-- Do column itself -->
    <xsl:apply-templates/>
  </xsl:for-each>

  <!-- Do extra blanks if there weren't enough columns -->
  <xsl:variable name="LASTCOL" select="m:mtd[last()]/@x_col"/>
  <xsl:call-template name="output-mtr-extra-blanks">
    <xsl:with-param name="NUM" select="number($COLS) - number($LASTCOL)"/>
  </xsl:call-template>
</xsl:template>

<xsl:template name="output-mtr-extra-blanks">
  <xsl:param name="NUM"/>
  <xsl:choose>
    <xsl:when test="number($NUM) &lt;= 0">
      <!-- Already done enough columns -->
    </xsl:when>
    <xsl:otherwise>
      <!-- Do extra column -->
      <xsl:text> blank </xsl:text>
      <!-- Recurse -->
      <xsl:call-template name="output-mtr-extra-blanks">
        <xsl:with-param name="NUM" select="number($NUM) - 1"/>
      </xsl:call-template>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!--
  ENLIVENING EXPRESSIONS (3.1.6.5)
  ***************************************************************************
 -->

<!--
  <maction> 
  http://www.w3.org/TR/MathML2/chapter3.html#presm.maction
  Applies default behaviour to render only selected element.
  -->
<template match="m:maction">
  <xsl:variable name="SELECTION">
    <xsl:choose>
      <xsl:when test="@selection"><xsl:value-of select="@selection"/></xsl:when>
      <xsl:otherwise>1</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:apply-templates select="*[position()=number($SELECTION)]"/>
</template>

<!--
  UTILITY TEMPLATES 
  ***************************************************************************
 -->

<!--
  Tidies whitespace to standard form.
  $TEXT - Text to be tidied
  Returns text with whitespace trimmed from ends and each run of internal
    whitespace converted to a single space character
 -->
<xsl:template name="tidy-whitespace">
    <xsl:param name="TEXT"/>
    <xsl:call-template name="inner-tidy-whitespace">
        <xsl:with-param name="TEXT" select="$TEXT"/>
        <xsl:with-param name="MODE">text</xsl:with-param>
    </xsl:call-template>
</xsl:template>

<xsl:template name="inner-tidy-whitespace">
    <xsl:param name="TEXT"/>
    <xsl:param name="MODE"/>

    <xsl:choose>
        <xsl:when test="normalize-space(substring($TEXT, 1, 1)) = ''">
            <!-- First character is whitespace. -->
            <xsl:if test="$MODE != 'white'">
                <!-- Output a single space -->
                <xsl:text> </xsl:text>
            </xsl:if>
            <!-- Continue in whitespace mode -->
            <xsl:call-template name="inner-tidy-whitespace">
                <xsl:with-param name="TEXT" select="substring($TEXT, 2)"/>
                <xsl:with-param name="MODE">white</xsl:with-param>
            </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
            <!-- First chasracter is not whitespace. Output it. -->
            <xsl:value-of select="substring($TEXT, 1, 1)"/>
            
            <!-- Continue in text mode -->
            <xsl:call-template name="inner-tidy-whitespace">
                <xsl:with-param name="TEXT" select="substring($TEXT, 2)"/>
                <xsl:with-param name="MODE">text</xsl:with-param>
            </xsl:call-template>
        </xsl:otherwise>
    </xsl:choose>
    
</xsl:template>

<!--
  Outputs an 'x' character a specified number of times.
  COUNT - number of x characters to output
  -->
<xsl:template name="output-x">
  <xsl:param name="COUNT"/>
  <xsl:text>x</xsl:text>
  <xsl:if test="$COUNT &gt; 1">
    <xsl:call-template name="output-x">
      <xsl:with-param name="COUNT" select="$COUNT - 1"/>
    </xsl:call-template>
  </xsl:if>
</xsl:template>

<!--
  Outputs the length of the largest string in the input.
  STRINGS - a comma-separated list of strings
  -->
<xsl:template name="largest-string">
  <xsl:param name="STRINGS"/>
  <xsl:choose>
    <xsl:when test="contains($STRINGS, ',')">
      <xsl:variable name="FIRST">
        <xsl:value-of select="string-length(substring-before($STRINGS, ','))"/>
      </xsl:variable>
      <xsl:variable name="OTHER">
        <xsl:call-template name="largest-string">
          <xsl:with-param name="STRINGS" select="substring-after($STRINGS, ',')"/>
        </xsl:call-template>
      </xsl:variable>
      <xsl:choose>
        <xsl:when test="number($FIRST) >= number($OTHER)">
          <xsl:value-of select="$FIRST"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$OTHER"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="string-length($STRINGS)"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

</xsl:stylesheet>
