<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

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
  <xsl:template match="identifiers/arr/i/identifierType">
    <identifierTypeId>170f6942-fec5-42af-9b5d-6bbba3e0a44a</identifierTypeId>
  </xsl:template>

  <!-- Map legacy location code to a FOLIO location UUID -->
  <xsl:template match="holdingsRecords/arr/i/permanentLocation">
    <permanentLocationId>42e5ba2f-d935-44f9-87e5-d6e9f01d2fb1</permanentLocationId> <!-- Villanova -->
  </xsl:template>

</xsl:stylesheet>
