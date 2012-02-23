<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:m="http://www.w3.org/1998/Math/MathML"
    xmlns:w="http://ns.open.ac.uk/lts/webmaths">

<!--
  List of characters with accent=true. Obtained from operator dictionary file 'dict'
  saved from http://www.w3.org/TR/MathML2/appendixf.html using following Unix
  command: 
  grep accent= dict | awk '{print $1}' | awk '{printf("%s",$0);}' | sed -e 's/"//g;'
  -->
<xsl:variable name="DICT_ACCENTS">
  &Breve;&Cedilla;&DiacriticalGrave;&DiacriticalDot;&DiacriticalDoubleAcute;&LeftArrow;&LeftRightArrow;&LeftRightVector;&LeftVector;&DiacriticalAcute;&RightArrow;&RightVector;&DiacriticalTilde;&DoubleDot;&DownBreve;&Hacek;&Hat;&OverBar;&OverBrace;&OverBracket;&OverParenthesis;&TripleDot;&UnderBar;&UnderBrace;&UnderBracket;&UnderParenthesis;
</xsl:variable>

<!--
  Root template 
  -->
<xsl:template match="/m:math">
  <result>
    <xsl:apply-templates/>
  </result>
</xsl:template>

<!--
  For escapes, output escaped content
  -->
<xsl:template match="w:esc">
  <xsl:value-of select="@tex"/>
</xsl:template>

<!--
  Basic elements passed through
  -->
<xsl:template match="m:semantics|m:mn|m:mi|m:mrow">
  <xsl:apply-templates select="@*|node()"/>
</xsl:template>

<!-- One-character mtext passed through -->
<xsl:template match="m:mtext[string-length(normalize-space(.)) = 1]">
  <xsl:apply-templates select="@*|node()"/>
</xsl:template>
<!-- And one-special-character mtext -->
<xsl:template match="m:mtext[count(node()) = 1 and w:esc]">
  <xsl:apply-templates select="@*|node()"/>
</xsl:template>

<!-- mo for \sum, \int might need to change into \tsum, \dint, etc. -->
<xsl:template match="m:mo[(string(.) = '&Sum;' or string(.)='&int;') and
    (parent::m:munder or parent::m:mover or parent::m:munderover or
    parent::m:msub or parent::m:msup or parent::m:msubsup) and
    not(preceding-sibling::*) and parent::*/parent::m:mstyle]" priority="+1">
  <xsl:apply-templates select="@*"/>
  <xsl:variable name="TDFRAC">
    <xsl:for-each select="parent::*/parent::m:mstyle">
      <xsl:call-template name="is-tdfrac"/>
    </xsl:for-each>
  </xsl:variable>
  <xsl:variable name="THING" select="substring-after(string(w:esc/@tex), '\')"/>
  <xsl:choose>
    <xsl:when test="../parent::m:mstyle[@displaystyle='true'] and $TDFRAC = 'y'">
      <xsl:text>\d</xsl:text><xsl:value-of select="$THING"/>
    </xsl:when>
    <xsl:when test="../parent::m:mstyle[@displaystyle='false'] and $TDFRAC = 'y'">
      <xsl:text>\t</xsl:text><xsl:value-of select="$THING"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates select="node()"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- Direct passthrough for mo that is a single letter non-alpha, or TeX escape -->
<xsl:template match="m:mo[w:esc and count(node()) = 1]">
  <xsl:apply-templates select="@*"/>
  <xsl:apply-templates/>
</xsl:template>
<xsl:template match="m:mo[string-length(normalize-space(.)) = 1]">
  <xsl:apply-templates select="@*"/>
  <xsl:apply-templates/>
</xsl:template>
<!-- Special-case for mod -->
<xsl:template match="m:mo[normalize-space(.) = 'mod']">
  <xsl:apply-templates select="@*"/>
  <xsl:text>\mod </xsl:text>
</xsl:template>

<!-- Other mo uses operatorname -->
<xsl:template match="m:mo">
  <xsl:apply-templates select="@*"/>

  <xsl:text>\operatorname{</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>}</xsl:text>
</xsl:template>


<!--
  fontstyle = normal on mi can be ignored (eh maybe) 
  -->
<xsl:template match="m:mi/@fontstyle[string(.)='normal']"/>

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

<!-- Skip elements -->
<xsl:template match="m:annotation"/>
<xsl:template match="m:annotation-xml"/>

<!-- mrow as \pmod -->
<xsl:template match="m:mrow[count(*) = 4 and *[1][self::m:mo and string(.) = '('] and
    *[4][self::m:mo and string(.) = ')'] and *[2][self::m:mo[string(.) = 'mod']] and
    *[3][self::m:mn]]">
  <xsl:apply-templates select="@*"/>
  <xsl:text>\pmod{</xsl:text>
  <xsl:apply-templates select="m:mn"/>
  <xsl:text>}</xsl:text>
</xsl:template>

<!-- mrow as \binom -->
<xsl:template match="m:mrow[count(*) = 3 and *[1][self::m:mo and string(.) = '('] and
    *[3][self::m:mo and string(.) = ')'] and *[2][self::m:mfrac[@linethickness='0' and
    count(@*)=1]]]">
  <xsl:apply-templates select="@*"/>
  <xsl:for-each select="m:mo">
      <xsl:apply-templates select="@*"/>
  </xsl:for-each>

  <xsl:variable name="TDFRAC">
    <xsl:for-each select="parent::m:mstyle">
      <xsl:call-template name="is-tdfrac"/>
    </xsl:for-each>
  </xsl:variable>

  <xsl:for-each select="m:mfrac">
    <xsl:choose>
      <xsl:when test="../parent::m:mstyle[@displaystyle='true'] and $TDFRAC = 'y'">
        <xsl:text>\dbinom{</xsl:text>
      </xsl:when>
      <xsl:when test="../parent::m:mstyle[@displaystyle='false'] and $TDFRAC = 'y'">
        <xsl:text>\tbinom{</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>\binom{</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates select="*[1]"/>
    <xsl:text>}{</xsl:text>
    <xsl:apply-templates select="*[2]"/>
    <xsl:text>} </xsl:text>
  </xsl:for-each>
</xsl:template>

<!-- mfrac -->
<xsl:template match="m:mfrac">
  <xsl:apply-templates select="@*"/>
  <xsl:variable name="TDFRAC">
    <xsl:for-each select="parent::m:mstyle">
      <xsl:call-template name="is-tdfrac"/>
    </xsl:for-each>
  </xsl:variable>
  <xsl:choose>
    <xsl:when test="parent::m:mstyle[@displaystyle='true'] and $TDFRAC = 'y'">
      <xsl:text>\dfrac{</xsl:text>
    </xsl:when>
    <xsl:when test="parent::m:mstyle[@displaystyle='false'] and $TDFRAC = 'y'">
      <xsl:text>\tfrac{</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>\frac{</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:apply-templates select="*[1]"/>
  <xsl:text>}{</xsl:text>
  <xsl:apply-templates select="*[2]"/>
  <xsl:text>} </xsl:text>
</xsl:template>

<!-- msub, under -->
<xsl:template match="m:msub|m:munder">
  <xsl:apply-templates select="@*"/>
  <xsl:call-template name="brace"><xsl:with-param name="VAL">
    <xsl:apply-templates select="*[1]"/>
  </xsl:with-param></xsl:call-template>
  <xsl:text>_</xsl:text>
  <xsl:call-template name="brace"><xsl:with-param name="VAL">
    <xsl:apply-templates select="*[2]"/>
  </xsl:with-param></xsl:call-template>
  <xsl:text> </xsl:text>
</xsl:template>

<!-- msup, mover -->
<xsl:template match="m:msup|m:mover">
  <xsl:apply-templates select="@*"/>
  <xsl:call-template name="brace"><xsl:with-param name="VAL">
    <xsl:apply-templates select="*[1]"/>
  </xsl:with-param></xsl:call-template>
  <xsl:text>^</xsl:text>
  <xsl:call-template name="brace"><xsl:with-param name="VAL">
    <xsl:apply-templates select="*[2]"/>
  </xsl:with-param></xsl:call-template>
  <xsl:text> </xsl:text>
</xsl:template>

<!-- msubsup, munderover -->
<xsl:template match="m:msubsup|m:munderover">
  <xsl:apply-templates select="@*"/>
  <xsl:call-template name="brace"><xsl:with-param name="VAL">
    <xsl:apply-templates select="*[1]"/>
  </xsl:with-param></xsl:call-template>
  <xsl:text>_</xsl:text>
  <xsl:call-template name="brace"><xsl:with-param name="VAL">
    <xsl:apply-templates select="*[2]"/>
  </xsl:with-param></xsl:call-template>
  <xsl:text>^</xsl:text>
  <xsl:call-template name="brace"><xsl:with-param name="VAL">
    <xsl:apply-templates select="*[3]"/>
  </xsl:with-param></xsl:call-template>
  <xsl:text> </xsl:text>
</xsl:template>

<!-- msqrt -->
<xsl:template match="m:msqrt">
  <xsl:apply-templates select="@*"/>
  <xsl:text>\sqrt{</xsl:text>
  <xsl:apply-templates select="*"/>
  <xsl:text>}</xsl:text>
</xsl:template>

<!-- mi with function name -->
<xsl:template match="m:mi[string-length(.) > 1 and not(count(node()) = 1 and w:esc)]">
  <xsl:apply-templates select="@*"/>
  <xsl:variable name="FN">
    <xsl:choose>
      <xsl:when test="contains(string(.), '&ThinSpace;') and substring-after(string(.), '&ThinSpace;')=''">
        <xsl:value-of select="substring-before(string(.), '&ThinSpace;')"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="string(.)"/>
      </xsl:otherwise> 
    </xsl:choose>
  </xsl:variable>
  <xsl:if test="@mathvariant != 'normal'">
    <xsl:text>\UNSUPPORTED{mi function missing @mathvariant=normal}</xsl:text>
  </xsl:if>
  <xsl:choose>
    <xsl:when test="$FN = 'arccos'"><xsl:text>\arccos </xsl:text></xsl:when>
    <xsl:when test="$FN = 'arcsin'"><xsl:text>\arcsin </xsl:text></xsl:when>
    <xsl:when test="$FN = 'arctan'"><xsl:text>\arctan </xsl:text></xsl:when>
    <xsl:when test="$FN = 'arg'"><xsl:text>\arg </xsl:text></xsl:when>
    <xsl:when test="$FN = 'cos'"><xsl:text>\cos </xsl:text></xsl:when>
    <xsl:when test="$FN = 'cosh'"><xsl:text>\cosh </xsl:text></xsl:when>
    <xsl:when test="$FN = 'cot'"><xsl:text>\cot </xsl:text></xsl:when>
    <xsl:when test="$FN = 'coth'"><xsl:text>\coth </xsl:text></xsl:when>
    <xsl:when test="$FN = 'csc'"><xsl:text>\csc </xsl:text></xsl:when>
    <xsl:when test="$FN = 'deg'"><xsl:text>\deg </xsl:text></xsl:when>
    <xsl:when test="$FN = 'det'"><xsl:text>\det </xsl:text></xsl:when>
    <xsl:when test="$FN = 'dim'"><xsl:text>\dim </xsl:text></xsl:when>
    <xsl:when test="$FN = 'exp'"><xsl:text>\exp </xsl:text></xsl:when>
    <xsl:when test="$FN = 'gcd'"><xsl:text>\gcd </xsl:text></xsl:when>
    <xsl:when test="$FN = 'hom'"><xsl:text>\hom </xsl:text></xsl:when>
    <xsl:when test="$FN = 'ker'"><xsl:text>\ker </xsl:text></xsl:when>
    <xsl:when test="$FN = 'lg'"><xsl:text>\lg </xsl:text></xsl:when>
    <xsl:when test="$FN = 'ln'"><xsl:text>\ln </xsl:text></xsl:when>
    <xsl:when test="$FN = 'log'"><xsl:text>\log </xsl:text></xsl:when>
    <xsl:when test="$FN = 'Pr'"><xsl:text>\Pr </xsl:text></xsl:when>
    <xsl:when test="$FN = 'sec'"><xsl:text>\sec </xsl:text></xsl:when>
    <xsl:when test="$FN = 'sin'"><xsl:text>\sin </xsl:text></xsl:when>
    <xsl:when test="$FN = 'sinh'"><xsl:text>\sinh </xsl:text></xsl:when>
    <xsl:when test="$FN = 'tan'"><xsl:text>\tan </xsl:text></xsl:when>
    <xsl:when test="$FN = 'tanh'"><xsl:text>\tanh </xsl:text></xsl:when>
    <xsl:when test="$FN = 'inf'"><xsl:text>\inf </xsl:text></xsl:when>
    <xsl:when test="$FN = 'inj lim'"><xsl:text>\injlim </xsl:text></xsl:when>
    <xsl:when test="$FN = 'lim'"><xsl:text>\lim </xsl:text></xsl:when>
    <xsl:when test="$FN = 'lim inf'"><xsl:text>\liminf </xsl:text></xsl:when>
    <xsl:when test="$FN = 'lum sup'"><xsl:text>\limsup </xsl:text></xsl:when>
    <xsl:when test="$FN = 'max'"><xsl:text>\max </xsl:text></xsl:when>
    <xsl:when test="$FN = 'min'"><xsl:text>\min </xsl:text></xsl:when>
    <xsl:when test="$FN = 'proj lim'"><xsl:text>\projlim </xsl:text></xsl:when>
    <xsl:when test="$FN = 'sup'"><xsl:text>\sup </xsl:text></xsl:when>

    <!-- 
      If it isn't a known function, just pass the text through (there might
      be other reasons for having <mi> with several characters) 
      -->
    <xsl:otherwise>
      <xsl:text>\UNSUPPORTED{mi function unknown}</xsl:text>
      <xsl:value-of select="."/>
    </xsl:otherwise>
  </xsl:choose>

</xsl:template>
<xsl:template match="m:mi[string-length(.) > 1 and not(count(node()) = 1 and w:esc)]/@mathvariant"/>

<!--
  Put spaces around some weird operators just to make it look nicer and match
  some of the input tests. 
  -->
<xsl:template match="m:mtext[string(.) = '.' or string(.) = ';' or
    string(.) = '?' or string(.) = '&nbsp;']">
  <xsl:apply-templates select="@*"/>
  <xsl:text> </xsl:text>
  <xsl:apply-templates/>
  <xsl:text> </xsl:text>
</xsl:template>


<!-- mstyle -->

<!-- Supported attributes -->
<xsl:template match="m:mstyle/@displaystyle"/>
<xsl:template match="m:mstyle/@scriptlevel"/>

<!-- Displaystyle true; exclude auto-added wrapper -->
<xsl:template match="m:mstyle">
  <xsl:apply-templates select="@*"/>
  
  <xsl:variable name="DISPLAYSTYLE">
    <xsl:call-template name="get-displaystyle"/>
  </xsl:variable>
  <xsl:variable name="PARENTDISPLAYSTYLE">
    <xsl:for-each select="parent::*">
      <xsl:call-template name="get-displaystyle"/>
    </xsl:for-each>
  </xsl:variable>
  
  <xsl:variable name="SCRIPTLEVEL">
    <xsl:call-template name="get-scriptlevel"/>
  </xsl:variable>
  <xsl:variable name="PARENTSCRIPTLEVEL">
    <xsl:for-each select="parent::*">
      <xsl:call-template name="get-scriptlevel"/>
    </xsl:for-each>
  </xsl:variable>

  <xsl:variable name="NOCHANGE">
    <xsl:if test="$SCRIPTLEVEL = $PARENTSCRIPTLEVEL and
        $DISPLAYSTYLE = $PARENTDISPLAYSTYLE">y</xsl:if> 
  </xsl:variable>

  <xsl:variable name="SKIP">
    <xsl:call-template name="is-tdfrac"/>
  </xsl:variable>

  <!-- Change display style -->
  <xsl:choose>
    <!-- Skip if using dfrac/tfrac for this, or same as parent -->
    <xsl:when test="$SKIP = 'y' or $NOCHANGE = 'y'"/>
    <xsl:when test="$DISPLAYSTYLE = 'true'">
      <xsl:text>{ \displaystyle </xsl:text>
    </xsl:when>
    <xsl:when test="$DISPLAYSTYLE = 'false' and $SCRIPTLEVEL = '0'">
      <xsl:text>{ \textstyle </xsl:text>
    </xsl:when>
    <xsl:when test="$DISPLAYSTYLE = 'false' and $SCRIPTLEVEL = '1'">
      <xsl:text>{ \scriptstyle </xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>{ \scriptscriptstyle </xsl:text>
    </xsl:otherwise>
  </xsl:choose>

  <xsl:apply-templates/>

  <!-- Put it back to parent style -->
  <xsl:choose>
    <xsl:when test="$SKIP = 'y' or $NOCHANGE = 'y'"/>
    <xsl:otherwise>
      <xsl:text>} </xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!--
  Must be called on mstyle node. Returns true if we are going to use tfrac/dfrac
  to replace a parent mstyle.
 -->
<xsl:template name="is-tdfrac">
  <xsl:if test="count(child::*)=1 and (m:mfrac or
      *[self::m:munderover or self::m:munder or self::m:mover or self::m:msub or
        self::m:msup or self::m:msubsup]/*[1][self::m:mo and
        (string(.) = '&Sum;' or string(.) = '&int;')] or m:mrow[
          count(*) = 3 and *[1][self::m:mo and string(.) = '('] and
          *[3][self::m:mo and string(.) = ')'] and *[2][self::m:mfrac[@linethickness='0' and
          count(@*)=1]]
        ]) and
      @displaystyle and not(@scriptlevel)">
    <!-- Need to check if the displaystyle actually did anything! -->
    <xsl:variable name="PREVIOUSDISPLAYSTYLE">
      <xsl:for-each select="parent::*">
        <xsl:call-template name="get-displaystyle"/>
      </xsl:for-each>
    </xsl:variable>
    <xsl:if test="$PREVIOUSDISPLAYSTYLE != @displaystyle">
      <xsl:text>y</xsl:text>
    </xsl:if>
  </xsl:if>
</xsl:template>



<!-- MathML utilities -->

<!--
 Gets details for an embellished operator. Embellished operator logic is
 defined in http://www.w3.org/TR/MathML2/chapter3.html#id.3.2.5.7
 If context node is not an embellished operator, returns empty string.
 TYPE - type value 

 Type values:
 'accent': get value of accent setting or from dictionary (true/false)
 -->
<xsl:template name="get-embellished-operator-info">
  <xsl:param name="TYPE"/>
  <xsl:choose>

    <xsl:when test="self::m:mo">
      <xsl:call-template name="get-embellished-operator-info-inner">
        <xsl:with-param name="TYPE" select="$TYPE"/>
      </xsl:call-template>
    </xsl:when>

    <xsl:when test="self::m:msub or self::m:msup or self::m:msubsup or
        self::m:munder or self::m:mover or self::m:munderover or
        self::m:mmultiscripts or self::m:mfrac or self::m:semantics">
      <xsl:for-each select="child::*">
        <xsl:call-template name="get-embellished-operator-info">
          <xsl:with-param name="TYPE" select="$TYPE"/>
        </xsl:call-template>
      </xsl:for-each>
    </xsl:when>

    <xsl:when test="self::m:mrow or self::m:mstyle or self::m:mphantom or
        self::m:mpadded">
      <!-- Get a count of non-spacelike things -->
      <xsl:variable name="NOTSPACELIKELIST">
        <xsl:for-each select="*">
          <xsl:variable name="SPACELIKE">
            <xsl:call-template name="is-space-like"/>
          </xsl:variable>
          <xsl:if test="$SPACELIKE = 'n'">
            <xsl:text>x</xsl:text>
          </xsl:if>
        </xsl:for-each>
      </xsl:variable>
      <!-- It must be 1 -->
      <xsl:if test="string-length($NOTSPACELIKELIST) = 1">
        <xsl:for-each select="*">
          <xsl:variable name="SPACELIKE">
            <xsl:call-template name="is-space-like"/>
          </xsl:variable>
          <xsl:if test="$SPACELIKE = 'n'">
            <xsl:call-template name="get-embellished-operator-info">
              <xsl:with-param name="TYPE" select="$TYPE"/>
            </xsl:call-template>
          </xsl:if>
        </xsl:for-each>
      </xsl:if>
    </xsl:when>

    <xsl:when test="self::m:maction">
      <xsl:variable name="SELECTION">
        <xsl:choose>
          <xsl:when test="@selection">
            <xsl:value-of select="@selection"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>1</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:for-each select="child::*[$SELECTION]">
        <xsl:call-template name="get-embellished-operator-info">
          <xsl:with-param name="TYPE" select="$TYPE"/>
        </xsl:call-template>
      </xsl:for-each>
    </xsl:when>

  </xsl:choose>
</xsl:template>

<!--
  Inner template used by get-embellished-operator-info. 
  -->
<xsl:template name="get-embellished-operator-info-inner">
  <xsl:param name="TYPE"/>

  <xsl:choose>
    <xsl:when test="type='accent'">
      <xsl:choose>
        <xsl:when test="@accent">
          <xsl:value-of select="@accent"/>
        </xsl:when>
        <xsl:when test="contains($DICT_ACCENTS, normalize-space(.))">
          <xsl:text>true</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>false</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
  </xsl:choose>
  
</xsl:template>

<!--
  Returns 'y' if the current node is a space-like element or 'n' if it is not.
  From http://www.w3.org/TR/MathML2/chapter3.html#id.3.2.7.3
  -->
<xsl:template name="is-space-like">
  <xsl:choose>
    <xsl:when test="self::m:mtext or self::m:mspace or self::m:maligngroup or
        self::m:malignmark">
      <xsl:text>y</xsl:text>
    </xsl:when>
    <xsl:when test="self::m:mstyle or self::m:mphanton or self::m:mpadded or
        self::m:mrow">
      <xsl:variable name="CHILDREN">
        <xsl:for-each select="*">
          <xsl:call-template name="is-space-like"/>
        </xsl:for-each>
      </xsl:variable>
      <xsl:choose>
        <xsl:when test="contains($CHILDREN, 'n')">
          <xsl:text>n</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>y</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:when test="self::m:maction">
      <xsl:variable name="SELECTION">
        <xsl:choose>
          <xsl:when test="@selection">
            <xsl:value-of select="@selection"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>1</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:for-each select="child::*[$SELECTION]">
        <xsl:call-template name="is-space-like"/>
      </xsl:for-each>
      <!-- 
        I think usually there should be a selected element, but just in
        case, if there is none, let's call it space-like? 
        -->
      <xsl:if test="not(child::*[$SELECTION])">
        <xsl:text>y</xsl:text>
      </xsl:if>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>n</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!--
  Gets the current in-effect value of displaystyle attribute.
  -->
<xsl:template name="get-displaystyle">
  <xsl:choose>
    <!-- Explicitly specified -->
    <xsl:when test="self::m:mstyle/@displaystyle or self::m:mtable/@displaystyle">
      <xsl:value-of select="@displaystyle"/>
    </xsl:when>
    <!-- Tags defined to set value to false within second+ child -->
    <xsl:when test="(parent::m:msub or parent::m:msup or parent::m:subsup or
        parent::m:munder or parent::m:mover or parent::m:munderover or
        parent::m:mmultiscripts or parent::m:mroot) and preceding-sibling::*">
      <xsl:text>false</xsl:text>
    </xsl:when>
    <!-- Tags defined to set value to false within all children -->
    <xsl:when test="parent::m:mfrac">
      <xsl:text>false</xsl:text>
    </xsl:when>
    <!-- Root element (we default to true for this rendering) -->
    <xsl:when test="not(parent::*)">
      <xsl:text>true</xsl:text>
    </xsl:when>
    <!-- Default: as parent -->
    <xsl:otherwise>
      <xsl:for-each select="parent::*">
        <xsl:call-template name="get-displaystyle"></xsl:call-template>
      </xsl:for-each>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!--
  Gets current in-effect value of scriptlevel attribute.
  -->
<xsl:template name="get-scriptlevel">
  <!-- Get parent value -->
  <xsl:variable name="PARENTVAL">
    <xsl:for-each select="parent::*">
      <xsl:call-template name="get-scriptlevel"></xsl:call-template>
    </xsl:for-each>
    <xsl:if test="not(parent::*)">
      <xsl:text>0</xsl:text>
    </xsl:if>
  </xsl:variable>

  <!-- Check specified option -->
  <xsl:choose>
    <!-- Increment -->
    <xsl:when test="self::m:mstyle and starts-with(string(@scriptlevel), '+')">
      <xsl:variable name="SHIFT" select="substring-after(@scriptlevel, '+')"/>
      <xsl:value-of select="number($PARENTVAL) + number($SHIFT)"/>
    </xsl:when>

    <!-- Decrement -->
    <xsl:when test="self::m:mstyle and starts-with(@scriptlevel, '-')">
      <xsl:variable name="SHIFT" select="substring-after(@scriptlevel, '-')"/>
      <xsl:value-of select="number($PARENTVAL) - number($SHIFT)"/>
    </xsl:when>

    <!-- Fixed value -->
    <xsl:when test="self::m:mstyle and @scriptlevel">
      <xsl:value-of select="@scriptlevel"/>
    </xsl:when>

    <!-- Tags defined to increment within second+ child -->
    <xsl:when test="(parent::m:msub or parent::m:msup or parent::m:subsup or
        parent::m:mmultiscripts) and preceding-sibling::*">
      <xsl:value-of select="number($PARENTVAL) + 1"/>
    </xsl:when>

    <!-- underscript on munder or munderover -->
    <xsl:when test="(parent::m:munder and preceding-sibling::*) or
        (parent::m:munderover and preceding-sibling::* and following-sibling::*)">
      <xsl:variable name="ACCENTUNDER">
        <xsl:choose>
          <xsl:when test="parent::*[@accentunder]">
            <xsl:value-of select="parent::*/@accentunder"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:variable name="ACCENT">
              <xsl:call-template name="get-embellished-operator-info">#
                <xsl:with-param name="TYPE">accent</xsl:with-param>
              </xsl:call-template>
            </xsl:variable>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:choose>
        <xsl:when test="$ACCENTUNDER='true'">
          <xsl:value-of select="$PARENTVAL"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="number($PARENTVAL) + 1"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>

    <!-- overscript on mover or munderover -->
    <xsl:when test="(parent::m:mover and preceding-sibling::*) or
        (parent::m:munderover and preceding-sibling::* and not(following-sibling::*))">
      <xsl:variable name="ACCENTOVER">
        <xsl:choose>
          <xsl:when test="parent::*[@accent]">
            <xsl:value-of select="parent::*/@accent"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:variable name="ACCENT">
              <xsl:call-template name="get-embellished-operator-info">
                <xsl:with-param name="TYPE">accent</xsl:with-param>
              </xsl:call-template>
            </xsl:variable>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:choose>
        <xsl:when test="$ACCENTOVER='true'">
          <xsl:value-of select="$PARENTVAL"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="number($PARENTVAL) + 1"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>

    <!--
      Fractions are complicated; increment scriptlevel if displaystyle was
      already false
      -->
    <xsl:when test="parent::m:mfrac">
      <xsl:variable name="DISPLAYSTYLE">
        <xsl:for-each select="parent::*">
          <xsl:call-template name="get-displaystyle"/>
        </xsl:for-each>
      </xsl:variable>
      <xsl:choose>
        <xsl:when test="$DISPLAYSTYLE='true'">
          <xsl:value-of select="$PARENTVAL"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="number($PARENTVAL) + 1"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>

    <!-- mroot index -->
    <xsl:when test="parent::m:mroot and preceding-sibling::*">
      <xsl:value-of select="number($PARENTVAL) + 2"/>
    </xsl:when>

    <!-- Anything else, inherit -->
    <xsl:otherwise>
      <xsl:value-of select="$PARENTVAL"/>
    </xsl:otherwise>

  </xsl:choose>
</xsl:template>

<!--
  Characters which are permitted after a backslash \
  -->
<xsl:variable name="SLASHCHARS">ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz</xsl:variable>
<!--
  Include something without braces if it's a single character, otherwise
  use braces.
  -->
<xsl:template name="brace">
  <xsl:param name="VAL"/>
  <xsl:choose>
    <!-- Single characters don't need braces -->
    <xsl:when test="string-length(string($VAL)) = 1">
      <xsl:value-of select="$VAL"/>
    </xsl:when>
    <!-- Backslash followed by any single char -->
    <xsl:when test="starts-with($VAL, '\') and string-length(normalize-space($VAL)=2)">
      <xsl:value-of select="$VAL"/>
    </xsl:when>
    <!-- Backslash followed by letters -->
    <xsl:when test="starts-with($VAL, '\') and
      string-length(translate(substring($VAL, 2), $SLASHCHARS, '')) = 0">
      <xsl:value-of select="$VAL"/>
    </xsl:when>
    <!-- Any other string - use braces -->
    <xsl:otherwise>
      <xsl:text>{</xsl:text>
      <xsl:value-of select="$VAL"/>
      <xsl:text>}</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>


</xsl:stylesheet>
