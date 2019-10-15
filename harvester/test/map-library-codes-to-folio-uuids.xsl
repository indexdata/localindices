<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
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
    <identifierTypeId>
      <xsl:choose>
        <xsl:when test=".='EAST'">47a65482-f104-45e8-aead-1f12d70dcf32</xsl:when>
        <xsl:when test=".='WEST'">9db07825-8035-4d9a-8a41-d59a5f1c337b</xsl:when>
        <xsl:otherwise>2117d011-f52b-4efe-ab97-f11d0a4b77e5</xsl:otherwise> <!-- Unspecified local identifier -->
      </xsl:choose>
    </identifierTypeId>
  </xsl:template>

  <!-- Map legacy location code to a FOLIO location UUID -->
  <xsl:template match="holdingsRecords/arr/i/permanentLocation">
    <permanentLocationId>
      <xsl:choose>
        <xsl:when test=".='EAST'">81582666-305d-4c8e-82cc-061fd00e9c42</xsl:when>
        <xsl:when test=".='WEST'">d05b8941-a7b3-4519-b450-06d72ca13a0c</xsl:when>
        <xsl:otherwise>c8f57ff4-366f-4c94-8186-d6439fae1d22</xsl:otherwise>  <!-- Unspecified location -->
      </xsl:choose>
    </permanentLocationId>
  </xsl:template>
</xsl:stylesheet>
