<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:marc="http://www.loc.gov/MARC21/slim"
  >

  <xsl:strip-space elements="*"/>
  <xsl:output indent="yes" method="xml" version="1.0" encoding="UTF-8"/>

  <xsl:template match="record">
    <record>
        <xsl:for-each select="@* | node()">
            <xsl:copy-of select="."/>
        </xsl:for-each>
        <xsl:apply-templates/>
    </record>
  </xsl:template>

  <xsl:variable name="WILL_LEND">46970b40-918e-47a4-a45d-b1677a2d3d46</xsl:variable> <!-- FOLIO Inventory ILL Policy value 'Will lend' -->
  <xsl:variable name="WILL_NOT_LEND">b0f97013-87f5-4bab-87f2-ac4a5191b489</xsl:variable> <!-- FOLIO Inventory ILL Policy value 'Will not lend' -->

  <xsl:template match="//marc:record">
    <xsl:choose>
      <xsl:when test="marc:datafield[@tag='HLD']">
        <holdingsRecords>
           <arr>
             <xsl:for-each select="marc:datafield[@tag='HLD']">
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
                    <xsl:when test="marc:subfield[@code='c']='aarc'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='aleisure'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='imc'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='intref'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='media'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='micro'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='newbooks'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='oversize'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='reference'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='reserve'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='stacks'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='asrs'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='media'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='reference'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='rarestacks'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='media'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='reserve'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='DSC'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='games'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='REF'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='dss_reserv'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='circ'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='intref'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='leisure'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='medhum'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='oversize'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='reference'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='reserve'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='serials'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='stacks'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='techserv'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='harrisburg'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='intref'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='media'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='oversize'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='reference'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='reserve'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='stacks'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='media'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='osaka'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='reference'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='remote'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='reserve'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='serials'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='stacks'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='a_rare'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='b_rare'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='g_remote'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='l_remote'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='p_GovDocs'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='p_govdocmf'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='p_media'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='p_micro'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='p_oversize'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='p_remote'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='kiosk'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='archives'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='closestack'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='govdoc'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='hirst'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='media'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='micro'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='open3'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='open3a'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='open4'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='open4a'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='open5'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='open5a'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='open6a'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='oversize2'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='oversize5a'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='pamphlets'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='rarestacks'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='rawle'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='rawle3'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='reference'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='reserve'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='serials'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='specintl'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='specpa'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='stacks'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='trials'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='hirsch'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='juvenile'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='leisure'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='m_reserve'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='newbooks'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='reference'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='reserve'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='serials'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='servicedsk'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='stacks'"><xsl:value-of select="$WILL_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='storage'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='techserv'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='intref'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='leisure'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='media'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='reference'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='reserve'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='serials'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='stacks'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='techserv'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='plc'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='presser'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='exhibit'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='fiction'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='media'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='oversize'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='reference'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='reserve'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='serials'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='stacks'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:when test="marc:subfield[@code='c']='rarestacks'"><xsl:value-of select="$WILL_NOT_LEND"/></xsl:when>
                    <xsl:otherwise><xsl:value-of select="$WILL_NOT_LEND"/></xsl:otherwise>
                    </xsl:choose>
                </illPolicyId>
                 <permanentLocationIdHere><xsl:value-of select="marc:subfield[@code='c']"/></permanentLocationIdHere>
                 <callNumber>
                   <xsl:for-each select="marc:subfield[@code='h' or @code='i']">
                     <xsl:if test="position() > 1">
                       <xsl:text> </xsl:text>
                     </xsl:if>
                     <xsl:value-of select="."/>
                   </xsl:for-each>
                 </callNumber>
                 <items>
                   <arr>
                     <xsl:for-each select="../marc:datafield[@tag='ITM']">
                        <xsl:if test="marc:subfield[@code='x']=$holdingsId">
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
  <xsl:template match="text()"/>
</xsl:stylesheet>