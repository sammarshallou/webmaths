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

<!-- 
  Insert mrow elements where they are optional. For lots of cases you do not
  have to use mrow if there is only a single child element. In order to
  normalise the input, I am making it insert mrow for these cases. The list
  comes from:
  http://www.w3.org/TR/MathML2/chapter3.html#id.3.1.3.2 
  -->
<xsl:template match="*[(self::m:msqrt or self::m:mstyle or self::m:merror
    or self::m:mpadded or self::m:mphantom or self::m:menclose or self::m:mtd
    or self::m:math) and count(*)=1 and not(child::m:mrow)]">
    <xsl:copy>
        <xsl:apply-templates select="@*"/>
        <mrow>
            <xsl:apply-templates/>
        </mrow>
    </xsl:copy>
</xsl:template>

<!--
  Normalise 'fenced' expressions using the algorithm given in the specification. 
  http://www.w3.org/TR/MathML2/chapter3.html#id.3.3.8 
 -->
<xsl:template match="m:mfenced">
    <xsl:variable name="OPEN">
        <xsl:choose>
            <xsl:when test="@open"><xsl:value-of select="@open"/></xsl:when>
            <xsl:otherwise>(</xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="CLOSE">
        <xsl:choose>
            <xsl:when test="@close"><xsl:value-of select="@close"/></xsl:when>
            <xsl:otherwise>)</xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="SEPARATORS">
        <xsl:choose>
            <xsl:when test="@separators"><xsl:value-of select="@separators"/></xsl:when>
            <xsl:otherwise>,</xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <!-- Get list of separators with no whitespace -->
    <xsl:variable name="SEPS"><xsl:call-template name="remove-whitespace">
        <xsl:with-param name="IN" select="$SEPARATORS"/> 
    </xsl:call-template></xsl:variable>
    <mrow>
        <mo fence="true"><xsl:value-of select="$OPEN"/></mo>
        <xsl:if test="count(*) &gt; 1">
            <mrow>
                <xsl:for-each select="*">
                    <xsl:if test="position() != 1 and $SEPS != ''">
                        <mo separator="true"><xsl:call-template name="mfenced-separator">
                            <xsl:with-param name="SEPS" select="$SEPS"/>
                            <xsl:with-param name="POS" select="position()-1"/>
                        </xsl:call-template></mo>
                    </xsl:if>
                    <xsl:apply-templates select="."/>
                </xsl:for-each>
            </mrow>
        </xsl:if>
        <xsl:if test="count(*) = 1">
            <xsl:for-each select="*">
                <xsl:apply-templates select="."/>
            </xsl:for-each>
        </xsl:if>
        <mo fence="true"><xsl:value-of select="$CLOSE"/></mo>
    </mrow>
    
</xsl:template>

<!-- 
    Removes all whitespace characters from the input string.
    $IN - Input string
 -->
<xsl:template name="remove-whitespace">
    <xsl:param name="IN"/>
    <xsl:choose>
        <xsl:when test="normalize-space($IN) = ''"></xsl:when>
        <xsl:otherwise>
            <xsl:if test="normalize-space(substring($IN, 1, 1)) != ''">
                <xsl:value-of select="substring($IN, 1, 1)"/>
            </xsl:if>
            <xsl:call-template name="remove-whitespace">
                <xsl:with-param name="IN" select="substring($IN, 2)"/>
            </xsl:call-template>
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<!--
    Displays the appropriate separator within an mfenced construct.
    $SEPS - List of separators (must have at least one), with no whitespace
    $POS - Position (starting at 1) 
 -->
<xsl:template name="mfenced-separator">
    <xsl:param name="SEPS"/>
    <xsl:param name="POS"/>
    <xsl:choose>
        <xsl:when test="number($POS) &lt;= string-length($SEPS)">
            <xsl:value-of select="substring($SEPS, number($POS), 1)"/>
        </xsl:when>
        <xsl:otherwise>
            <xsl:value-of select="substring($SEPS, string-length($SEPS), 1)"/>
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

</xsl:stylesheet>
