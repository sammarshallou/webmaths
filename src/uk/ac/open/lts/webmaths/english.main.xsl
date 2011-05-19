<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:m="http://www.w3.org/1998/Math/MathML">

<xsl:template match="/m:math">
<result>
<xsl:apply-templates/>
</result>
</xsl:template>

<xsl:template match="m:mo[normalize-space() = '&InvisibleTimes;']">
<xsl:text> times </xsl:text>
</xsl:template>

<xsl:template match="m:msup">
    <xsl:apply-templates select="*[1]"/>
    to the power
    <xsl:apply-templates select="*[2]"/>
</xsl:template>

<xsl:template match="m:msub">
    <xsl:apply-templates select="*[1]"/>
    subscript
    <xsl:apply-templates select="*[2]"/>
</xsl:template>

<xsl:template match="m:msup[child::*[position()=2 and self::m:mn and normalize-space(.) = '2']]">
    <xsl:apply-templates select="*[1]"/>
    squared
</xsl:template> 

<xsl:template match="m:msup[child::*[position()=2 and self::m:mn and normalize-space(.) = '3']]">
    <xsl:apply-templates select="*[1]"/>
    cubed
</xsl:template> 

<xsl:template match="m:mrow[count(*) > 1 and not(parent::m:math)]">
    (
    <xsl:apply-templates/>
    )
</xsl:template>

<xsl:template match="m:mfrac">
    (
    <xsl:apply-templates select="*[1]"/>
    over
    <xsl:apply-templates select="*[2]"/>
    )
</xsl:template>

<xsl:template match="m:msqrt">
    square root of
    <xsl:apply-templates/>
</xsl:template>

<xsl:template match="m:mroot[child::*[position()=2 and self::m:mn and normalize-space(.) = '3']]">
    cube root of
    <xsl:apply-templates select="*[1]"/>
</xsl:template>

<xsl:template match="m:mroot[child::*[position()=2 and self::m:mn]]">
    <xsl:value-of select="child::*[position()=2 and self::m:mn]"/>th root of
    <xsl:apply-templates select="*[1]"/>
</xsl:template>

<xsl:template match="m:mroot">
    <xsl:apply-templates select="*[2]"/>
    -th root of
    <xsl:apply-templates select="*[1]"/>
</xsl:template>

<!-- Skipped mstyle: style only -->

<xsl:template match="m:merror">
    ERROR [
    <xsl:apply-templates/>
    ]
</xsl:template>

<!-- Skipped mpadded: style only -->

<xsl:template match="m:mphantom">
    <!-- mphantom is not rendered -->
</xsl:template>

<!-- Skipped mfenced: removed by normalise script -->

<!-- TODO Got to here! -->



</xsl:stylesheet>
