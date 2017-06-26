<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:gmd="http://www.isotc211.org/2005/gmd"
                xmlns:gco="http://www.isotc211.org/2005/gco"
                xmlns:che="http://www.geocat.ch/2008/che"
                xmlns:xslutil="java:org.fao.geonet.util.XslUtil"
                version="2.0">
  <xsl:include href="shared-object-util.xsl"/>
  <xsl:include href="che_ci_responsibleparty-functions.xsl"/>


  <xsl:template match="/">

    <Documents>
      <xsl:variable name="root" select="/"/>
      <xsl:variable name="title">
        <xsl:call-template name="title"/>
      </xsl:variable>

      <!--Get one index in eng if no multilanguage fields are detected-->
      <xsl:if test="count(distinct-values(//gmd:LocalisedCharacterString/@locale)) = 0">
        <xsl:variable name="locale" select="'eng'"/>
        <Document locale="{$locale}">
          <Field name="_locale" string="{$locale}" store="true" index="false"/>
          <Field name="_title" string="{$title}" store="true" index="false"/>
          <xsl:apply-templates mode="sharedobject" select="
                        $root//*[normalize-space(@locale) = normalize-space($locale) and not(ancestor::che:parentResponsibleParty)] |
                        $root//gco:CharacterString[not(ancestor::che:parentResponsibleParty)] |
                        $root//gmd:CI_RoleCode[not(ancestor::che:parentResponsibleParty)]
                    "/>
          <Field name="any" store="false" index="true">
            <xsl:attribute name="string">
              <xsl:value-of select="$root//(gco:CharacterString|gmd:LocalisedCharacterString)"/>
            </xsl:attribute>
          </Field>
        </Document>
      </xsl:if>

      <xsl:for-each select="distinct-values(//gmd:LocalisedCharacterString/@locale)">
        <xsl:variable name="locale" select="string(.)"/>
        <xsl:variable name="langId" select="xslutil:threeCharLangCode(substring($locale,2,2))"/>
        <Document locale="{$langId}">
          <Field name="_locale" string="{$langId}" store="true" index="false"/>
          <Field name="_title" string="{$title}" store="true" index="false"/>
          <xsl:apply-templates mode="sharedobject" select="
                        $root//*[normalize-space(@locale) = normalize-space($locale) and not(ancestor::che:parentResponsibleParty)] |
                        $root//gco:CharacterString[not(ancestor::che:parentResponsibleParty)] |
                        $root//gmd:CI_RoleCode[not(ancestor::che:parentResponsibleParty)]
                    "/>
          <Field name="any" store="false" index="true">
            <xsl:attribute name="string">
              <xsl:value-of select="$root//(gco:CharacterString|gmd:LocalisedCharacterString)"/>
            </xsl:attribute>
          </Field>
        </Document>
      </xsl:for-each>
    </Documents>
  </xsl:template>


</xsl:stylesheet>
