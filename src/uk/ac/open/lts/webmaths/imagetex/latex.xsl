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
  <xsl:text>\frac{</xsl:text>
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

<!-- mstyle -->

<!-- Displaystyle true; exclude auto-added wrapper -->
<xsl:template match="m:mstyle[@displaystyle='true' and not(parent::m:semantics)]">
  <xsl:text>\displaystyle </xsl:text>
  <xsl:apply-templates/>
</xsl:template>

</xsl:stylesheet>
