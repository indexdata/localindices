<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:marc="http://www.loc.gov/MARC21/slim"
  >

  <xsl:strip-space elements="*"/>
  <xsl:output indent="yes" method="xml" version="1.0" encoding="UTF-8"/>

  <xsl:template match="collection">
    <collection>
        <xsl:apply-templates/>
    </collection>
  </xsl:template>

  <xsl:template match="record">
    <record>
        <xsl:for-each select="@* | node()">
            <xsl:copy-of select="."/>
        </xsl:for-each>
        <xsl:apply-templates/>
    </record>
  </xsl:template>

  <xsl:template match="//marc:record">
    <xsl:choose>
      <xsl:when test="marc:datafield[@tag='852']">
        <holdingsRecords>
           <arr>
             <xsl:for-each select="marc:datafield[@tag='852']">
               <i>
                 <permanentLocationIdHere><xsl:value-of select="marc:subfield[@code='b']"/></permanentLocationIdHere>
                 <callNumber>
                   <xsl:for-each select="marc:subfield[@code='h']">
                     <xsl:if test="position() > 1">
                       <xsl:text> </xsl:text>
                     </xsl:if>
                     <xsl:value-of select="."/>
                   </xsl:for-each>
                 </callNumber>
               </i>
             </xsl:for-each>
           </arr>
        </holdingsRecords>
      </xsl:when>
      <xsl:otherwise>
        <holdingsRecords>
          <arr>
            <i>
              <permanentLocationIdHere/>
            </i>
          </arr>
        </holdingsRecords>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template match="text()"/>
</xsl:stylesheet>
