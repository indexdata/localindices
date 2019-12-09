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
  <xsl:template match="identifiers/arr/i/identifierTypeIdHere">
    <identifierTypeId>17bb9b44-0063-44cc-8f1a-ccbb6188060b</identifierTypeId>
  </xsl:template>

  <!-- Map legacy location code to a FOLIO location UUID -->
  <xsl:template match="holdingsRecords/arr/i/permanentLocationIdHere">
    <permanentLocationId>87038e41-0990-49ea-abd9-1ad00a786e45</permanentLocationId> <!-- Temple -->
  </xsl:template>

</xsl:stylesheet>
