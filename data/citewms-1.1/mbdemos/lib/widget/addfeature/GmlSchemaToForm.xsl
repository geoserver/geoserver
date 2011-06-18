<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:gml="http://www.opengis.net/gml" version="1.0">

  <xsl:output method="html" encoding="utf-8"/>

  <!--============================================================================-->
  <!-- Parameters passed into this xsl                                            -->
  <!--============================================================================-->
  <!-- Location of XSD, ideally these are local to reduce bandwidth -->
  <xsl:param name="feature_xsd" select='"feature.xsd"'/>
  <xsl:param name="geometry_xsd" select='"geometry.xsd"'/>

  <!-- Set TRUE to produce extra debugging html -->
  <xsl:param name="debug" select='"TRUE"'/>


  <!--============================================================================-->
  <!-- Todo:
       * Move XSD type matches and processing from element template into its own
         template.
  -->
  <!--============================================================================-->


  <!--============================================================================-->
  <!-- Main html                                                                  -->
  <!--============================================================================-->
  <xsl:template match="/">
    <html>
      <head>
        <title>Feature Entry - Community Map Builder</title>
      </head>
      <body>
        <h1>Feature Entry - Community Map Builder</h1>
        <form>
          <ul>
            <xsl:apply-templates select="/xsd:schema/xsd:element"/>
          </ul>
        </form>
      </body>
    </html>
  </xsl:template>

  <!--============================================================================-->
  <!-- element                                                                    -->
  <!--============================================================================-->
  <xsl:template match="xsd:element | element">
      <!-- ref elements rename other elements.  We don't display the name of the
      element being renamed.  Eg, display the name "gml:description" instead of
      "xsd:string".  -->
      <xsl:param name="displayName"/>

      <xsl:variable name="typeName" select="substring-after(@type,':')"/>
      <xsl:choose>
        <!-- match schema primative and derived data types. -->
        <xsl:when test="@type">
          <xsl:if test="$debug">
            <b>Element: </b>
            <xsl:for-each select="ancestor::*">
              ancestor=<xsl:value-of select="name(.)"/><br/>
            </xsl:for-each>
            <br/>
          </xsl:if>
          <xsl:value-of select="@name"/>
          <xsl:call-template name="processType">
            <xsl:with-param name="type"><xsl:value-of select="@type"/></xsl:with-param>
            <xsl:with-param name="calledby">element type <xsl:value-of select="@name"/></xsl:with-param>
          </xsl:call-template>
        </xsl:when>

        <xsl:when test="@ref">
          <xsl:if test="$debug">
            <b>Element Ref: </b>
            <xsl:value-of select="@ref"/><br/>
          </xsl:if>
          <!--
          <xsl:call-template name="processType">
            <xsl:with-param name="type"><xsl:value-of select="@ref"/></xsl:with-param>
            <xsl:with-param name="calledby">element ref <xsl:value-of select="@name"/></xsl:with-param>
          </xsl:call-template>
          -->
          <xsl:variable name="ref" select="@ref"/>
          <xsl:apply-templates select="//xsd:element[@name=substring-after($ref,':')]"/>
        </xsl:when>

        <!-- @ref means this element renames another.  Display this element's name
        but use displayName="false" to ensure the renamed element is not printed. -->
        <!--
        <xsl:when test="@ref">
          <xsl:if test="$debug">
            <xsl:if test="not(contains($displayName,'false'))">
              <li>
                <b>Ref Element: </b>
                <xsl:value-of select="@ref"/>
                <xsl:if test="@type">
                  - <xsl:value-of select="@type"/>
                </xsl:if>
              </li>
            </xsl:if>
          </xsl:if>
          <xsl:variable name="ref" select="substring-after(@ref,':')"/>
          <xsl:apply-templates select="//xsd:element[@name=$ref]">
            <xsl:with-param name="displayName">false</xsl:with-param>
          </xsl:apply-templates>
        </xsl:when>
        -->

        <!-- Elements that can be expanded -->
        <xsl:otherwise>
          <xsl:if test="not(contains($displayName,'false'))">
            <xsl:if test="$debug"><b>Element to expand: </b></xsl:if>
            <xsl:value-of select="@name"/>
            <xsl:apply-templates/>
            <xsl:if test="$debug">...end Element to expand.<br/></xsl:if>
          </xsl:if>
        </xsl:otherwise>
      </xsl:choose>
  </xsl:template>

  <!--============================================================================-->
  <!-- complexType                                                                -->
  <!--============================================================================-->
  <xsl:template match="xsd:complexType">
    <xsl:if test="$debug">
      <b>complexType:</b> <xsl:value-of select="@name"/><br/>
    </xsl:if>
    <xsl:apply-templates/>
  </xsl:template>

  <!--============================================================================-->
  <!-- simpleType                                                                 -->
  <!--============================================================================-->
  <xsl:template match="xsd:simpleType">
    <xsl:if test="$debug">
      <b>simpleType:</b> <xsl:value-of select="@name"/><br/>
    </xsl:if>
    <xsl:apply-templates/>
  </xsl:template>

  <!--============================================================================-->
  <!-- extension                                                                  -->
  <!--============================================================================-->
  <xsl:template match="xsd:extension">
    <xsl:if test="$debug">
    <b>Extension:</b>
    </xsl:if>
    <xsl:call-template name="processType">
      <xsl:with-param name="type">
        <xsl:value-of select="@base"/>
        <xsl:with-param name="calledby">extension <xsl:value-of select="@name"/></xsl:with-param>
      </xsl:with-param>
    </xsl:call-template>
    <xsl:apply-templates/>
  </xsl:template>

  <!--============================================================================-->
  <!-- restriction (of types)                                                     -->
  <!-- Provide simple re-casting to the parent type                               -->
  <!--============================================================================-->
  <xsl:template match="xsd:restriction">
    <xsl:if test="$debug">
      <b>Restriction of: </b>
      <xsl:value-of select="@base"/>
    </xsl:if>
    <xsl:call-template name="processType">
      <xsl:with-param name="type"><xsl:value-of select="@base"/></xsl:with-param>
      <xsl:with-param name="calledby">restriction <xsl:value-of select="@base"/></xsl:with-param>
    </xsl:call-template>
    <xsl:apply-templates/>
  </xsl:template>

  <!--============================================================================-->
  <!-- processType                                                                -->
  <!-- Strip namespace off type and call processType2, xs:int becomes int         -->
  <!--============================================================================-->
  <xsl:template name="processType">
    <xsl:param name="type"/>
    <xsl:param name="calledby"/>

    <xsl:message>
      <xsl:value-of select="$type"/>           (called by)            <xsl:value-of select="$calledby"/>
      <xsl:value-of select="$type"/>           (called by)            <xsl:value-of select="$calledby"/>
    </xsl:message>

    <xsl:choose>
      <xsl:when test="contains($type,':')">
        <xsl:call-template name="processType2">
          <xsl:with-param name="type">
            <xsl:value-of select="substring-after($type,':')"/>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="processType2">
          <xsl:with-param name="type">
            <xsl:value-of select="$type"/>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!--============================================================================-->
  <!-- processType2                                                               -->
  <!--============================================================================-->
  <xsl:template name="processType2">
    <xsl:param name="type"/>

    - <i><xsl:value-of select="$type"/></i>
    <xsl:choose>
      <xsl:when test="
          $type='string' or
          $type='boolean' or
          $type='float' or
          $type='double' or
          $type='decimal' or
          $type='duration' or
          $type='dateTime' or
          $type='time' or
          $type='date' or
          $type='gYearMonth' or
          $type='gYear' or
          $type='gMonthDay' or
          $type='gDay' or
          $type='gMonth' or
          $type='hexBinary' or
          $type='base64Binary' or
          $type='anyURI' or
          $type='QName' or
          $type='NOTATION' or
          $type='normalizedString' or
          $type='token' or
          $type='language' or
          $type='IDREFS' or
          $type='ENTITIES' or
          $type='NMTOKEN' or
          $type='NMTOKENS' or
          $type='Name' or
          $type='NCName' or
          $type='ID' or
          $type='IDREF' or
          $type='ENTITY' or
          $type='integer' or
          $type='nonPositiveInteger' or
          $type='negativeInteger' or
          $type='long' or
          $type='int' or
          $type='short' or
          $type='byte' or
          $type='nonNegativeInteger' or
          $type='unsignedLong' or
          $type='unsignedInt' or
          $type='unsignedShort' or
          $type='unsignedByte' or
          $type='positiveInteger' or
          $type='derivationControl' or
          $type='simpleDerivationSet'">
        <input type="text" maxlength="20" name="text"/>
        <br/>
      </xsl:when>
      <xsl:otherwise>
        <ul>
          <xsl:apply-templates
            select="document($geometry_xsd)/xsd:schema/*[@name=$type]"/>
          <xsl:apply-templates
            select="document($feature_xsd)/xsd:schema/*[@name=$type]"/>
          <xsl:apply-templates
            select="/xsd:schema/*[@name=$type]"/>
        </ul>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!--============================================================================-->
  <!-- documentation                                                              -->
  <!-- Don't print the embedded docs                                              -->
  <!--============================================================================-->
  <xsl:template match="xsd:documentation"/>

  <!--============================================================================-->
  <!-- Apply templates which match "match" from main schema and imported schemas  -->
  <!--============================================================================-->
  <!--
  <xsl:template name="importedcomplextype">
    <xsl:param name="type"/>

    <xsl:if test="//xsd:import | xsd:include">
      <xsl:for-each select="//xsd:import | //xsd:include">
        <xsl:if test="document(@schemaLocation)/xsd:schema/xsd:complexType[@name=$type]">
          <b>Imported Schema:</b> <xsl:value-of select="@schemaLocation"/>
          from namespace <xsl:value-of select="@namespace"/><br/>
          <xsl:apply-templates select="document(@schemaLocation)/xsd:schema/xsd:complexType[@name=$type]"/>
          end Imported schemata.
        </xsl:if>
      </xsl:for-each>
    </xsl:if>
  </xsl:template>
  -->

</xsl:stylesheet>

