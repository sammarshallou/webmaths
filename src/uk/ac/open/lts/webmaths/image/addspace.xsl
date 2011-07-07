<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:m="http://www.w3.org/1998/Math/MathML">

<!-- Copy everything -->
<xsl:template match="node()|@*">
  <xsl:copy>
    <xsl:apply-templates select="@*"/>
    <xsl:apply-templates/>
  </xsl:copy>
</xsl:template>

<!-- Add space around mi tag -->
<xsl:template match="mi[count(child::text())=1 and count(child::node()) = 1]">
  <xsl:variable name="TEXT" select="string(.)"/>
  <mi>
    <xsl:apply-templates select="@*"/>
    <!-- Add space before if there is no existing space before and there is some previous thing -->
    <xsl:if test="preceding::* and string-length(
      normalize-space(substring($TEXT, 1, 1))) = 1">
      <xsl:text>&#x2006;</xsl:text>
    </xsl:if>
    <xsl:value-of select="$TEXT"/>
    <!-- Add space after if there is no existing space after and there is some following thing -->
    <xsl:if test="following::* and string-length(
      normalize-space(substring($TEXT, string-length($TEXT), 1))) = 1">
      <xsl:text>&#x2006;</xsl:text>
    </xsl:if>
  </mi>
</xsl:template>

</xsl:stylesheet>
