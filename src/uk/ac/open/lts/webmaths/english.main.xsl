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
  <mrow> (with more than one item, and not the one directly inside <math>)
  http://www.w3.org/TR/MathML2/chapter3.html#presm.mrow
  -->
<xsl:template match="m:mrow[count(*) > 1 and not(parent::m:math)]">
    (
    <xsl:apply-templates/>
    )
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
<xsl:template match="m:msqrt or m:mroot[child::*[position()=2 and self::m:mn and normalize-space(.) = '2']]">
    square root of
    <xsl:apply-templates/>
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
  <mroot> - numeric root 
  -->
<xsl:template match="m:mroot[child::*[position()=2 and self::m:mn]]">
    <xsl:value-of select="child::*[position()=2 and self::m:mn]"/>th root of
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

<!-- Skipped <mstyle>: style only -->

<!--
  <merror>
  http://www.w3.org/TR/MathML2/chapter3.html#presm.merror 
  -->
<xsl:template match="m:merror">
    ERROR [
    <xsl:apply-templates/>
    ]
</xsl:template>

<!-- Skipped <mpadded>: style only -->

<!--
  <mphantom>
  http://www.w3.org/TR/MathML2/chapter3.html#presm.mphantom 
  -->
<xsl:template match="m:mphantom">
    <!-- mphantom is not rendered -->
</xsl:template>

<!-- Skipped mfenced: removed by normalise script -->

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
  <mo>
  http://www.w3.org/TR/MathML2/chapter3.html#presm.mo
  TODO I am probably going to change this to handle all the character names
  separately outside the xsl as postprocess.
  -->
<xsl:template match="m:mo[normalize-space() = '&InvisibleTimes;']">
    <xsl:variable name="O" select="normalize-space(.)"/>
    <xsl:choose>
        <xsl:when test="$O = '&CapitalDifferentialD;'">
            <xsl:text> D </xsl:text>
        </xsl:when>
        <xsl:when test="$O = '&InvisibleTimes;'">
            <xsl:text> times </xsl:text>
        </xsl:when>
        <xsl:when test="$O = '&InvisibleTimes;'">
            <xsl:text> times </xsl:text>
        </xsl:when>
        <xsl:when test="$O = '&InvisibleTimes;'">
            <xsl:text> times </xsl:text>
        </xsl:when>


        <xsl:when test="$O = '&InvisibleTimes;'">
            <xsl:text> times </xsl:text>
        </xsl:when>
        <xsl:otherwise>
            <!-- Output characters directly -->
            <xsl:text> </xsl:text><xsl:value-of select="$O"/><xsl:text> </xsl:text>
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>




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




</xsl:stylesheet>
