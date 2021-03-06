<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output indent="yes" method="xml" version="1.0" encoding="UTF-8"/>
  <xsl:template match="@* | node()">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()"/>
    </xsl:copy>
  </xsl:template>

  <!-- Map legacy code for the library/institution to a FOLIO resource identifier
       type UUID. Used for qualifying a local record identifier with the library
       it originated from in context of a shared index setup where the Instance
       represents bib records from multiple libraries.
  -->
  <xsl:template match="//identifierTypeIdHere">
    <identifierTypeId>04d081a1-5c52-4b84-8962-949fc5f6773c</identifierTypeId>
  </xsl:template>

  <!-- Map legacy location code to a FOLIO location UUID -->
  <xsl:template match="//permanentLocationIdHere">
    <permanentLocationId>004c14d3-fb87-40fc-b4db-9e91738b4f1b</permanentLocationId>  <!-- Millersville -->
  </xsl:template>

  <!-- Set institutionId for Millersville -->
  <xsl:template match="//institutionIdHere">
    <institutionId>b4578dbc-4dd9-4ac1-9c01-8a13f65aa95e</institutionId>> <!-- Millersville -->
  </xsl:template>

</xsl:stylesheet>
