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

  <xsl:variable name="WILL_LEND">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:variable> <!-- FOLIO Inventory ILL Policy value 'Will lend' -->
  <xsl:variable name="WILL_NOT_LEND">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:variable> <!-- FOLIO Inventory ILL Policy value 'Will not lend' -->

  <xsl:template match="passthrough">
    <xsl:choose>
      <xsl:when test="marc:datafield[@tag='900' and @ind1!='4' and @ind1!=' ']">
        <holdingsRecords>
           <arr>
             <xsl:for-each select="marc:datafield[@tag='900' and @ind1!='4' and @ind1!=' ']">
               <xsl:variable name="holdingsId" select="marc:subfield[@code='8']"/>
               <i>
                 <formerIds>
                   <arr>
                     <i>
                       <xsl:value-of select="marc:subfield[@code='8']"/>
                     </i>
                   </arr>
                 </formerIds>
                 <illPolicyId>
                  <xsl:choose>
                    <xsl:when test="marc:subfield[@code='d']='AID'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='B'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='BD'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='BDVD'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='BIND'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='BO'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='CAT'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='CKT'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='CPOS'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='CT'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='CTO'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EA_AUDIO'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EB_ACLS'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EB_CHO'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EB_EAI'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EB_EBRARY'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EB_EBRARYK'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EB_JSTOR'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EB_JSTOR15'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EB_LCL'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EB_NETL'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EB_NETLK'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EB_OXFORD'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EB_PLCI_03'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EB_PLCI_13'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EB_PLCI_14'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EB_PLCI_15'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EB_PLNT_03'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EI'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EN_ZZZ'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EP'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EP_JST'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EP_PJM'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EP_SAGE'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EP_ZZZ'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='ER'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EXHIBIT'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EZB'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EZB_LEND'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='EZBM'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='GD'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='GDER'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='GDMC'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='GDMF'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='GDML'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='GDR'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='I'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='ILLB'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='J'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='JEB'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='JO'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='MEND'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='MF'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='MFD'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='N'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='N_M'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='OBOC'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='ONORDER'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='P'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='P_C'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='P_M'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='P_P'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='PROBLEM'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='PROC'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='PROCCA'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='PROCLA'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='R'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='RE'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='RESE'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='RESM'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='RESP'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='RESS'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='RLAW'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='SC'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='SCAR'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='SCBN'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='SCCR'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='SCD'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='SCDH'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='SCERS'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='SCH'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='SCM'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='SCMAP'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='SCMAPD'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='SCME'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='SCMS'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='SCPR'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='SCRB'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='SCRBD'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='SCRR'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='SCSTORAGE'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='SCT'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='SCW'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='STORAGE_02'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='UNASSIGNED'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='UNUSED'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='d']='VC'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:otherwise><xsl:value-of select="$WILL_NOT_LEND"/></xsl:otherwise>
                  </xsl:choose>
                 </illPolicyId>
                 <permanentLocationIdHere><xsl:value-of select="marc:subfield[@code='d']"/></permanentLocationIdHere>
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
                          <xsl:if test="marc:subfield[@code='g']">
                            <volume>
                              <xsl:value-of select="marc:subfield[@code='g']"/>
                            </volume>
                          </xsl:if>
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
      <xsl:when test="marc:datafield[@tag='995']">
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
                <permanentLocationIdHere/>
                <items>
                  <arr>
                    <i>
                      <itemIdentifier>
                        <xsl:value-of select="marc:subfield[@code='a']"/>
                      </itemIdentifier>
                      <barcode>
                        <xsl:value-of select="marc:subfield[@code='s']"/>
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
                      <xsl:if test="marc:subfield[@code='g']">
                        <volume>
                          <xsl:value-of select="marc:subfield[@code='g']"/>
                        </volume>
                      </xsl:if>
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
              <permanentLocationIdHere/>
            </i>
          </arr>
        </holdingsRecords>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>
