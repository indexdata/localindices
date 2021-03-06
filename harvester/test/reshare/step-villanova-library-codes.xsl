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
  <xsl:template match="//identifierTypeIdHere">
    <identifierTypeId>170f6942-fec5-42af-9b5d-6bbba3e0a44a</identifierTypeId>
  </xsl:template>

  <!-- Map legacy location code to a FOLIO location UUID -->
  <xsl:template match="//permanentLocationIdHere">
    <permanentLocationId>42e5ba2f-d935-44f9-87e5-d6e9f01d2fb1</permanentLocationId> <!-- Villanova -->
  </xsl:template>

  <!-- Set FOLIO Inventory ID for the Villanova institution -->
  <xsl:template match="//institutionIdHere">
     <institutionId>943aa176-7612-4e34-a1b9-ea318f92facd</institutionId>
  </xsl:template>

</xsl:stylesheet>
