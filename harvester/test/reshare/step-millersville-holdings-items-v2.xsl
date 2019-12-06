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
      <xsl:when test="marc:datafield[@tag='900' and @ind1!='4']">
        <holdingsRecords>
           <arr>
             <xsl:for-each select="marc:datafield[@tag='900' and @ind1!='4']">
               <xsl:variable name="holdingsId" select="marc:subfield[@code='8']"/>
               <i>
                 <formerIds>
                   <arr>
                     <i>
                       <xsl:value-of select="marc:subfield[@code='8']"/>
                     </i>
                   </arr>
                 </formerIds>
                 <permanentLocation><xsl:value-of select="marc:subfield[@code='d']"/></permanentLocation>
                 <callNumber>
                   <xsl:for-each select="marc:subfield[@code='f']">
                     <xsl:if test="position() > 1">
                       <xsl:text> </xsl:text>
                     </xsl:if>
                     <xsl:value-of select="."/>
                   </xsl:for-each>
                 </callNumber>
                 <items>
                   <arr>
                     <xsl:for-each select="../marc:datafield[@tag='995']">
                      <xsl:if test="marc:subfield[@code='ff']=$holdingsId">
                        <i>
                          <itemIdentifier>
                            <xsl:value-of select="marc:subfield[@code='a']"/>
                          </itemIdentifier>
                          <barcode>
                            <xsl:value-of select="marc:subfield[@code='s']"/>
                          </barcode>
                          <permanentLoanTypeId>2b94c631-fca9-4892-a730-03ee529ffe27</permanentLoanTypeId>                    <!-- Can circulate -->
                          <materialTypeId>
                            <xsl:choose>
                              <xsl:when test="marc:subfield[@code='t']='BOOK'"      >1a54b431-2e4f-452d-9cae-9cee66c9a892</xsl:when> <!-- book -->
                              <xsl:when test="marc:subfield[@code='t']='ELEC'"      >615b8413-82d5-4203-aa6e-e37984cb5ac3</xsl:when> <!-- electronic resourse -->
                              <xsl:when test="marc:subfield[@code='t']='ISSUE'"     >d9acad2f-2aac-4b48-9097-e6ab85906b25</xsl:when> <!-- text -->
                              <xsl:when test="marc:subfield[@code='t']='MANUSCRIPT'">d9acad2f-2aac-4b48-9097-e6ab85906b25</xsl:when> <!-- text -->
                              <xsl:when test="marc:subfield[@code='t']='MAP'"       >71fbd940-1027-40a6-8a48-49b44d795e46</xsl:when> <!-- unspecified -->
                              <xsl:when test="marc:subfield[@code='t']='OTHER'"     >71fbd940-1027-40a6-8a48-49b44d795e46</xsl:when> <!-- unspecified -->
                              <xsl:when test="marc:subfield[@code='t']='SCORE'"     >71fbd940-1027-40a6-8a48-49b44d795e46</xsl:when> <!-- unspecified -->
                              <xsl:when test="marc:subfield[@code='t']='RECORD'"    >dd0bf600-dbd9-44ab-9ff2-e2a61a6539f1</xsl:when> <!-- sound recording -->
                              <xsl:otherwise>71fbd940-1027-40a6-8a48-49b44d795e46</xsl:otherwise>                                    <!-- unspecified -->
                            </xsl:choose>
                          </materialTypeId>
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
      <xsl:otherwise>
        <holdingsRecords>
          <arr>
            <xsl:for-each select="marc:datafield[@tag='995']">
              <i>
                <!-- No "900" tag (no holdings record), use ID of item as holdingsRecord ID as well -->
                <formerIds>
                  <arr>
                    <i>
                      <xsl:value-of select="marc:subfield[@code='a']"/>
                    </i>
                  </arr>
                </formerIds>
                <items>
                  <arr>
                    <i>
                      <itemIdentifier>
                        <xsl:value-of select="marc:subfield[@code='a']"/>
                      </itemIdentifier>
                      <barcode>
                        <xsl:value-of select="../marc:subfield[@code='s']"/>
                      </barcode>
                      <permanentLoanTypeId>2b94c631-fca9-4892-a730-03ee529ffe27</permanentLoanTypeId>                      <!-- Can circulate -->
                      <materialTypeId>
                            <xsl:choose>
                              <xsl:when test="marc:subfield[@code='t']='BOOK'"      >1a54b431-2e4f-452d-9cae-9cee66c9a892</xsl:when> <!-- book -->
                              <xsl:when test="marc:subfield[@code='t']='ELEC'"      >615b8413-82d5-4203-aa6e-e37984cb5ac3</xsl:when> <!-- electronic resourse -->
                              <xsl:when test="marc:subfield[@code='t']='ISSUE'"     >d9acad2f-2aac-4b48-9097-e6ab85906b25</xsl:when> <!-- text -->
                              <xsl:when test="marc:subfield[@code='t']='MANUSCRIPT'">d9acad2f-2aac-4b48-9097-e6ab85906b25</xsl:when> <!-- text -->
                              <xsl:when test="marc:subfield[@code='t']='MAP'"       >71fbd940-1027-40a6-8a48-49b44d795e46</xsl:when> <!-- unspecified -->
                              <xsl:when test="marc:subfield[@code='t']='OTHER'"     >71fbd940-1027-40a6-8a48-49b44d795e46</xsl:when> <!-- unspecified -->
                              <xsl:when test="marc:subfield[@code='t']='SCORE'"     >71fbd940-1027-40a6-8a48-49b44d795e46</xsl:when> <!-- unspecified -->
                              <xsl:when test="marc:subfield[@code='t']='RECORD'"    >dd0bf600-dbd9-44ab-9ff2-e2a61a6539f1</xsl:when> <!-- sound recording -->
                              <xsl:otherwise>71fbd940-1027-40a6-8a48-49b44d795e46</xsl:otherwise>                                    <!-- unspecified -->
                            </xsl:choose>
                          </materialTypeId>
                    </i>
                  </arr>
                </items>
              </i>
            </xsl:for-each>
          </arr>
        </holdingsRecords>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>
