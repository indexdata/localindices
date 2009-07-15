<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:pz="http://www.indexdata.com/pazpar2/1.0"
    xmlns:z="http://indexdata.com/zebra-2.0" 
    version="1.0">

  <xsl:output indent="yes" method="xml" version="1.0" encoding="UTF-8"/>

  <!-- disable all default text node output -->
  <xsl:template match="text()"/>
  
  <xsl:template match="pz:record">
   <xsl:variable name="id">
       <xsl:choose>
           <xsl:when test="pz:metadata[@type='zebra-id']">
               <xsl:value-of select="pz:metadata[@type='zebra-id']"/>
           </xsl:when>
           <xsl:otherwise>
               <xsl:value-of select="pz:metadata[@type='id']"/>
           </xsl:otherwise>
       </xsl:choose>
   </xsl:variable>
   <z:record z:id="{$id}">
    <xsl:for-each select="pz:metadata">
     <z:index name="any:w {@type}:w {@type}:p">
      <xsl:value-of select="."/>
     </z:index>
    </xsl:for-each>
   </z:record>
  </xsl:template>
  
  </xsl:stylesheet>