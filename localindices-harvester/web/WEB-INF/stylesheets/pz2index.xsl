<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:pz="http://www.indexdata.com/pazpar2/1.0"
    xmlns:z="http://indexdata.com/zebra-2.0" 
    version="1.0">

  <xsl:output indent="yes" method="xml" version="1.0" encoding="UTF-8"/>

  <!-- disable all default text node output -->
  <xsl:template match="text()"/>
  
  <xsl:template match="pz:record">
   <z:record z:id="{pz:metadata[@type='id']}">
     <z:index name="any:w">
         
      <xsl:for-each select="pz:metadata">
       <z:index name="{@type}:w {@type}:p">
        <xsl:value-of select="."/>
       </z:index>
      </xsl:for-each>
      
    </z:index>
   </z:record>
  </xsl:template>
  
  </xsl:stylesheet>