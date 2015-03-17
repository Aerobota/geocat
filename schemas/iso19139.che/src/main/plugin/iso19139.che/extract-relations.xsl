<?xml version="1.0" encoding="UTF-8"?>
<!-- 
  Create a simple XML tree for relation description.
  <relations>
    <relation type="related|services|children">
      + super-brief representation.
-->
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:gmd="http://www.isotc211.org/2005/gmd" xmlns:gco="http://www.isotc211.org/2005/gco"
  xmlns:gmx="http://www.isotc211.org/2005/gmx"
  xmlns:geonet="http://www.fao.org/geonetwork"
  xmlns:util="java:org.fao.geonet.util.XslUtil"
  xmlns:che="http://www.geocat.ch/2008/che"
  xmlns:exslt="http://exslt.org/common"
  xmlns:gn-fn-rel="http://geonetwork-opensource.org/xsl/functions/relations"
  exclude-result-prefixes="gn-fn-rel geonet exslt che gmd gco">

  <xsl:include href="../iso19139/convert/functions.xsl" />

  <xsl:function name="gn-fn-rel:translate">
    <xsl:param name="el"/>
    <xsl:param name="lang"/>
    <xsl:choose>
      <xsl:when test="$el/gco:CharacterString!=''"><xsl:value-of select="$el/gco:CharacterString"/></xsl:when>
      <xsl:when test="($el/gmd:PT_FreeText//gmd:LocalisedCharacterString[@locale = $lang][text() != ''])[1]">
        <xsl:value-of select="($el/gmd:PT_FreeText//gmd:LocalisedCharacterString[@locale = $lang][text() != ''])[1]"/>
      </xsl:when>
      <xsl:otherwise><xsl:value-of select="($el/gmd:PT_FreeText//gmd:LocalisedCharacterString[text() != ''])[1]"/></xsl:otherwise>
    </xsl:choose>
  </xsl:function>

  <!-- Relation contained in the metadata record has to be returned
  It could be document or thumbnails
  -->
  <xsl:template mode="relation" match="metadata[gmd:MD_Metadata or *[contains(@gco:isoType, 'MD_Metadata')]]" priority="99">

    <xsl:for-each select="*/descendant::*[name(.) = 'gmd:graphicOverview']/*">
      <relation type="thumbnail">
        <id><xsl:value-of select="gmd:fileName/gco:CharacterString"/></id>
        <title><xsl:value-of select="gmd:fileDescription/gco:CharacterString"/></title>
      </relation>
    </xsl:for-each>

    <xsl:for-each select="*/descendant::*[name(.) = 'gmd:onLine']/*[gmd:linkage/gmd:URL!='' or
                                                                    gmd:linkage/che:PT_FreeURL//che:LocalisedURL[text() != ''] or
                                                                    gmd:linkage/che:LocalisedURL!='']">
      <relation type="onlinesrc">
        <xsl:variable name="langCode">
          <xsl:value-of select="concat('#', upper-case(util:twoCharLangCode($lang, 'EN')))"/>
        </xsl:variable>
        <xsl:variable name="url">
          <xsl:choose>
            <xsl:when test="gmd:linkage/gmd:URL!=''"><xsl:value-of select="gmd:linkage/gmd:URL"/></xsl:when>
            <xsl:when test="gmd:linkage/che:LocalisedURL!=''"><xsl:value-of select="gmd:linkage/che:LocalisedURL"/></xsl:when>
            <xsl:when test="(gmd:linkage/che:PT_FreeURL//che:LocalisedURL[@locale = $langCode][text() != ''])[1]">
              <xsl:value-of select="(gmd:linkage/che:PT_FreeURL//che:LocalisedURL[@locale = $langCode][text() != ''])[1]"/>
            </xsl:when>
            <xsl:otherwise><xsl:value-of select="(gmd:linkage/che:PT_FreeURL//che:LocalisedURL[text() != ''])[1]"/></xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
        <!-- Compute title based on online source info-->
        <xsl:variable name="title">
          <xsl:variable name="title" select="''"/>
          <xsl:value-of select="if ($title = '' and ../@uuidref) then ../@uuidref else $title"/><xsl:text> </xsl:text>
          <xsl:value-of select="if (gn-fn-rel:translate(gmd:name, $langCode) != '')
            then gn-fn-rel:translate(gmd:name, $langCode)
            else if (gmd:name/gmx:MimeFileType != '')
            then gmd:name/gmx:MimeFileType
            else if (gn-fn-rel:translate(gmd:description, $langCode) != '')
            then gn-fn-rel:translate(gmd:description, $langCode)
            else $url"/>
        </xsl:variable>
        <id><xsl:value-of select="$url"/></id>
        <title>
          <xsl:value-of select="if ($title != '') then $title else $url"/>
        </title>
        <url>
          <xsl:value-of select="$url"/>
        </url>
        <name>
          <xsl:value-of select="gn-fn-rel:translate(gmd:name, $langCode)"/>
        </name>
        <abstract><xsl:value-of select="gn-fn-rel:translate(gmd:description, $langCode)"/></abstract>
        <description><xsl:value-of select="gn-fn-rel:translate(gmd:description, $langCode)"/></description>
        <protocol><xsl:value-of select="gn-fn-rel:translate(gmd:protocol, $langCode)"/></protocol>
      </relation>
    </xsl:for-each>
  </xsl:template>
</xsl:stylesheet>
