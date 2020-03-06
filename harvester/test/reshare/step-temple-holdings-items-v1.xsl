<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:marc="http://www.loc.gov/MARC21/slim"
  >

  <xsl:strip-space elements="*"/>
  <xsl:output indent="yes" method="xml" version="1.0" encoding="UTF-8"/>

  <xsl:template match="@* | node()">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="passthrough">
    <xsl:choose>
      <xsl:when test="marc:datafield[@tag='852']">
        <holdingsRecords>
           <arr>
             <xsl:for-each select="marc:datafield[@tag='852']">
               <xsl:variable name="holdingsId" select="marc:subfield[@code='8']"/>
               <xsl:variable name="holdingPos" select="position()"/>
               <i>
                 <formerIds>
                   <arr>
                     <i>
                       <xsl:value-of select="marc:subfield[@code='8']"/>
                     </i>
                   </arr>
                 </formerIds>
                 <permanentLocationIdHere><xsl:value-of select="marc:subfield[@code='b']"/></permanentLocationIdHere>
                 <callNumber>
                   <xsl:for-each select="marc:subfield[@code='h']">
                     <xsl:if test="position() > 1">
                       <xsl:text> </xsl:text>
                     </xsl:if>
                     <xsl:value-of select="."/>
                   </xsl:for-each>
                 </callNumber>
                 <items>
                   <arr>
                     <xsl:for-each select="../marc:datafield[@tag='954']">
                        <xsl:if test="position() = $holdingPos">
                        <i>
                          <itemIdentifier>
                            <xsl:value-of select="marc:subfield[@code='a']"/>
                          </itemIdentifier>
                          <barcode>
                            <xsl:value-of select="marc:subfield[@code='b']"/>
                          </barcode>
                          <permanentLoanTypeId>2b94c631-fca9-4892-a730-03ee529ffe27</permanentLoanTypeId>                    <!-- Can circulate -->
                          <materialTypeId>
                            <xsl:choose>
                              <xsl:when test="marc:subfield[@code='d']='BOOK'">1a54b431-2e4f-452d-9cae-9cee66c9a892</xsl:when> <!-- Book -->
                              <xsl:otherwise>71fbd940-1027-40a6-8a48-49b44d795e46</xsl:otherwise>                              <!-- Unspecified -->
                            </xsl:choose>
                          </materialTypeId>
                          <status>
                            <name>Unknown</name>
                          </status>
                        </i>
                        </xsl:if>
                     </xsl:for-each>
                   </arr>
                 </items>
               </i>
             </xsl:for-each>
           </arr>
        </holdingsRecords>
      </xsl:when>
      <xsl:when test="marc:datafield[@tag='954']">
        <holdingsRecords>
          <arr>
            <xsl:for-each select="marc:datafield[@tag='954']">
              <i>
                <!-- No "852" tag (no holdings record), use ID of item as holdingsRecord ID as well -->
                <formerIds>
                  <arr>
                    <i>
                      <xsl:value-of select="marc:subfield[@code='a']"/>
                    </i>
                  </arr>
                </formerIds>
                <permanentLocationIdHere />
                <items>
                  <arr>
                    <i>
                      <itemIdentifier>
                        <xsl:value-of select="marc:subfield[@code='a']"/>
                      </itemIdentifier>
                      <barcode>
                        <xsl:value-of select="marc:subfield[@code='b']"/>
                      </barcode>
                      <permanentLoanTypeId>2b94c631-fca9-4892-a730-03ee529ffe27</permanentLoanTypeId>                      <!-- Can circulate -->
                      <materialTypeId>
                        <xsl:choose>
                          <xsl:when test="marc:subfield[@code='d']='BOOK'">1a54b431-2e4f-452d-9cae-9cee66c9a892</xsl:when> <!-- Book -->
                          <xsl:otherwise>71fbd940-1027-40a6-8a48-49b44d795e46</xsl:otherwise>                              <!-- Unspecified -->
                        </xsl:choose>
                      </materialTypeId>
                      <status>
                        <name>Unknown</name>
                      </status>
                    </i>
                  </arr>
                </items>
              </i>
            </xsl:for-each>
          </arr>
        </holdingsRecords>
      </xsl:when>
      <xsl:otherwise>
        <holdingsRecords>
          <arr>
            <i>
              <permanentLocationIdHere />
            </i>
          </arr>
        </holdingsRecords>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>