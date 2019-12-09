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
                    <xsl:when test="marc:subfield[@code='d']='AID'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='B'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='BD'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='BDVD'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='BIND'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='BO'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='CAT'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='CKT'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='CPOS'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='CT'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='CTO'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='EA_AUDIO'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='EB_ACLS'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='EB_CHO'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='EB_EAI'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='EB_EBRARY'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='EB_EBRARYK'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='EB_JSTOR'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='EB_JSTOR15'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='EB_LCL'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='EB_NETL'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='EB_NETLK'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='EB_OXFORD'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='EB_PLCI_03'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='EB_PLCI_13'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='EB_PLCI_14'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='EB_PLCI_15'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='EB_PLNT_03'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='EI'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='EN_ZZZ'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='EP'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='EP_JST'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='EP_PJM'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='EP_SAGE'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='EP_ZZZ'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='ER'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='EXHIBIT'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='EZB'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='EZB_LEND'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='EZBM'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='GD'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='GDER'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='GDMC'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='GDMF'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='GDML'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='GDR'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='I'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='ILLB'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='J'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='JEB'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='JO'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='MEND'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='MF'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='MFD'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='N'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='N_M'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='OBOC'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='ONORDER'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='P'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='P_C'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='P_M'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='P_P'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='PROBLEM'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='PROC'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='PROCCA'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='PROCLA'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='R'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='RE'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='RESE'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='RESM'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='RESP'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='RESS'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='RLAW'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='SC'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='SCAR'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='SCBN'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='SCCR'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='SCD'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='SCDH'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='SCERS'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='SCH'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='SCM'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='SCMAP'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='SCMAPD'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='SCME'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='SCMS'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='SCPR'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='SCRB'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='SCRBD'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='SCRR'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='SCSTORAGE'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='SCT'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='SCW'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='STORAGE_02'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:when test="marc:subfield[@code='d']='UNASSIGNED'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='UNUSED'">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:when> <!-- Will not lend -->
                    <xsl:when test="marc:subfield[@code='d']='VC'">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:when> <!-- will lend -->
                    <xsl:otherwise>b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:otherwise> <!-- Will not lend -->
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
