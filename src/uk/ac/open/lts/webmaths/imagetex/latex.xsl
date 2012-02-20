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
  Basic elements passed through
  -->
<xsl:template match="m:semantics|m:mn|m:mi|m:mo|m:mrow">
  <xsl:apply-templates select="@*|node()"/>
</xsl:template>

<!-- Detect constructs we do not support, and mark result equation. -->
<xsl:template match="*">
  <xsl:text>\UNSUPPORTED{element </xsl:text>
  <xsl:value-of select="local-name(.)"/>
  <xsl:text>}</xsl:text>
</xsl:template>
<xsl:template match="@*">
  <xsl:text>\UNSUPPORTED{attribute </xsl:text>
  <xsl:value-of select="local-name(..)"/>
  <xsl:text>/@</xsl:text>
  <xsl:value-of select="local-name(.)"/>
  <xsl:text>}</xsl:text>
</xsl:template>

<!--
  Include something without braces if it's a single character, otherwise
  use braces.
  -->
<xsl:template name="brace">
  <xsl:param name="VAL"/>
  <xsl:choose>
    <xsl:when test="string-length(string($VAL)) > 1">
      <xsl:text>{</xsl:text>
      <xsl:value-of select="$VAL"/>
      <xsl:text>}</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="$VAL"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- Skip elements -->
<xsl:template match="m:annotation"/>
<xsl:template match="m:annotation-xml"/>

<!-- mfrac -->
<xsl:template match="m:mfrac">
  <xsl:variable name="LOCALSTYLE">
    <xsl:for-each select="parent::m:mstyle">
      <xsl:call-template name="is-local-style"/>
    </xsl:for-each>
  </xsl:variable>
  <xsl:choose>
    <xsl:when test="parent::m:mstyle[@displaystyle='true'] and $LOCALSTYLE = 'y'">
      <xsl:text>\dfrac{</xsl:text>
    </xsl:when>
    <xsl:when test="parent::m:mstyle[@displaystyle='false'] and $LOCALSTYLE = 'y'">
      <xsl:text>\tfrac{</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>\frac{</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:apply-templates select="*[1]"/>
  <xsl:text>}{</xsl:text>
  <xsl:apply-templates select="*[2]"/>
  <xsl:text>}</xsl:text>
</xsl:template>

<!-- msup -->
<xsl:template match="m:msup">
  <xsl:call-template name="brace"><xsl:with-param name="VAL">
    <xsl:apply-templates select="*[1]"/>
  </xsl:with-param></xsl:call-template>
  <xsl:text>^</xsl:text>
  <xsl:call-template name="brace"><xsl:with-param name="VAL">
    <xsl:apply-templates select="*[2]"/>
  </xsl:with-param></xsl:call-template>
</xsl:template>

<!-- msub -->
<xsl:template match="m:msub">
  <xsl:call-template name="brace"><xsl:with-param name="VAL">
    <xsl:apply-templates select="*[1]"/>
  </xsl:with-param></xsl:call-template>
  <xsl:text>_</xsl:text>
  <xsl:call-template name="brace"><xsl:with-param name="VAL">
    <xsl:apply-templates select="*[2]"/>
  </xsl:with-param></xsl:call-template>
</xsl:template>

<!-- mi with function name -->
<xsl:template match="m:mi[string-length(.) > 1 and not(starts-with(., '\'))]">
  <xsl:variable name="FN">
    <xsl:choose>
      <xsl:when test="contains(string(.), '\thinspace ') and substring-after(string(.), '\thinspace ')=''">
        <xsl:value-of select="substring-before(string(.), '\thinspace ')"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="string(.)"/>
      </xsl:otherwise> 
    </xsl:choose>
  </xsl:variable>
  <xsl:choose>
    <xsl:when test="$FN = 'arccos'">\arccos</xsl:when>
    <xsl:when test="$FN = 'arcsin'">\arcsin</xsl:when>
    <xsl:when test="$FN = 'arctan'">\arctan</xsl:when>
    <xsl:when test="$FN = 'arg'">\arg</xsl:when>
    <xsl:when test="$FN = 'cos'">\cos</xsl:when>
    <xsl:when test="$FN = 'cosh'">\cosh</xsl:when>
    <xsl:when test="$FN = 'cot'">\cot</xsl:when>
    <xsl:when test="$FN = 'coth'">\coth</xsl:when>
    <xsl:when test="$FN = 'csc'">\csc</xsl:when>
    <xsl:when test="$FN = 'deg'">\deg</xsl:when>
    <xsl:when test="$FN = 'det'">\det</xsl:when>
    <xsl:when test="$FN = 'dim'">\dim</xsl:when>
    <xsl:when test="$FN = 'exp'">\exp</xsl:when>
    <xsl:when test="$FN = 'gcd'">\gcd</xsl:when>
    <xsl:when test="$FN = 'hom'">\hom</xsl:when>
    <xsl:when test="$FN = 'ker'">\ker</xsl:when>
    <xsl:when test="$FN = 'lg'">\lg</xsl:when>
    <xsl:when test="$FN = 'ln'">\ln</xsl:when>
    <xsl:when test="$FN = 'log'">\log</xsl:when>
    <xsl:when test="$FN = 'Pr'">\Pr</xsl:when>
    <xsl:when test="$FN = 'sec'">\sec</xsl:when>
    <xsl:when test="$FN = 'sin'">\sin</xsl:when>
    <xsl:when test="$FN = 'sinh'">\sinh</xsl:when>
    <xsl:when test="$FN = 'tan'">\tan</xsl:when>
    <xsl:when test="$FN = 'tanh'">\tanh</xsl:when>
    <xsl:when test="$FN = 'inf'">\inf</xsl:when>
    <xsl:when test="$FN = 'inj lim'">\injlim</xsl:when>
    <xsl:when test="$FN = 'lim'">\lim</xsl:when>
    <xsl:when test="$FN = 'lim inf'">\liminf</xsl:when>
    <xsl:when test="$FN = 'lum sup'">\limsup</xsl:when>
    <xsl:when test="$FN = 'max'">\max</xsl:when>
    <xsl:when test="$FN = 'min'">\min</xsl:when>
    <xsl:when test="$FN = 'proj lim'">\projlim</xsl:when>
    <xsl:when test="$FN = 'sup'">\sup</xsl:when>

    <!-- 
      If it isn't a known function, just pass the text through (there might
      be other reasons for having <mi> with several characters) 
      -->
    <xsl:otherwise><xsl:value-of select="."/></xsl:otherwise>
  </xsl:choose>

</xsl:template>

<!--
  Put spaces around some weird operators just to make it look nicer and match
  some of the input tests. 
  -->
<xsl:template match="m:mtext[string(.) = '.']">
  <xsl:text> . </xsl:text>
</xsl:template>
<xsl:template match="m:mtext[string(.) = ';']">
  <xsl:text> ; </xsl:text>
</xsl:template>
<xsl:template match="m:mtext[string(.) = '?']">
  <xsl:text> ? </xsl:text>
</xsl:template>
<xsl:template match="m:mtext[string(.) = '~']">
  <xsl:text> ~ </xsl:text>
</xsl:template>


<!-- mstyle -->

<!-- Displaystyle true; exclude auto-added wrapper -->
<xsl:template match="m:mstyle">
  <xsl:variable name="SKIP"><xsl:call-template name="is-local-style"/></xsl:variable>
  <xsl:choose>
    <!-- Skip if using dfrac/tfrac for this -->
    <xsl:when test="$SKIP = 'y'"/>
    <!-- Skip if this is display style at top level (because that's default)  -->
    <xsl:when test="@displaystyle='true' and parent::m:semantics"/>
    <xsl:when test="@displaystyle='true'">
      <xsl:text>\displaystyle </xsl:text>
    </xsl:when>
    <xsl:when test="@displaystyle='false' and (not(@scriptlevel) or @scriptlevel='0' or @scriptlevel='+1')">
      <xsl:text>\textstyle </xsl:text>
    </xsl:when>
    <xsl:when test="@displaystyle='true' and @scriptlevel='1'">
      <xsl:text>\scriptstyle </xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>\scriptscriptstyle </xsl:text>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:apply-templates/>
</xsl:template>

<!--
  Must be called on mstyle node. Returns true if the mstyle will be ignored
  because we are going to insert a dfrac instead of frac. This is done if:
  - the mstyle only contains one element and it is mfrac
  - the mstyle is not the last element in the document
  - this is either 'd' or 't' (scriptlevel 0)
  Put in a template to avoid duplication (used in 2 places)
 -->
<xsl:template name="is-local-style">
  <xsl:if test="count(child::*)=1 and (child::mfrac) and (following::*[1])
    and (not(@scriptlevel) or @scriptlevel='0')">
    <xsl:text>y</xsl:text>
  </xsl:if>
</xsl:template>

</xsl:stylesheet>
