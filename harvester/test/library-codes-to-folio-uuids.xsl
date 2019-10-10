<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:pz="http://www.indexdata.com/pazpar2/1.0">
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
    <xsl:choose>
      <xsl:when test=".='EAST'">
        <identifierTypeId>47a65482-f104-45e8-aead-1f12d70dcf32</identifierTypeId>
      </xsl:when>
      <xsl:when test=".='WEST'">
        <identifierTypeId>9db07825-8035-4d9a-8a41-d59a5f1c337b</identifierTypeId>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <!-- Map legacy location code to a FOLIO location UUID -->
  <xsl:template match="holdingsRecords/arr/i/permanentLocation">
    <xsl:choose>
      <xsl:when test=".='EAST'">
        <permanentLocationId>81582666-305d-4c8e-82cc-061fd00e9c42</permanentLocationId>
      </xsl:when>
      <xsl:when test=".='WEST'">
        <permanentLocationId>d05b8941-a7b3-4519-b450-06d72ca13a0c</permanentLocationId>
      </xsl:when>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>
