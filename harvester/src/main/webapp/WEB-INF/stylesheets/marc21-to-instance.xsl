<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:marc="http://www.loc.gov/MARC21/slim">


  <xsl:output indent="yes" method="xml" version="1.0" encoding="UTF-8"/>

<!-- Extract metadata from MARC21/USMARC
      http://www.loc.gov/marc/bibliographic/ecbdhome.html
-->
  <xsl:include href="pz2-ourl-marc21.xsl" />

  <xsl:template match="/">
    <collection>
      <xsl:apply-templates />
    </collection>
  </xsl:template>

  <xsl:template match="//delete">
     <xsl:copy-of select="."/>
  </xsl:template>

  <xsl:template match="//marc:record">

    <record>
      <instanceTypeId>
        <!-- UUIDs for resource types -->
        <xsl:choose>
          <xsl:when test="substring(marc:leader,7,1)='a'">6312d172-f0cf-40f6-b27d-9fa8feaf332f</xsl:when>
          <xsl:when test="substring(marc:leader,7,1)='c'">497b5090-3da2-486c-b57f-de5bb3c2e26d</xsl:when>
          <xsl:when test="substring(marc:leader,7,1)='d'">497b5090-3da2-486c-b57f-de5bb3c2e26d</xsl:when>
          <xsl:when test="substring(marc:leader,7,1)='e'">79a09d3a-bae8-433f-af70-9db9e134c662</xsl:when>
          <xsl:when test="substring(marc:leader,7,1)='f'">79a09d3a-bae8-433f-af70-9db9e134c662</xsl:when>
          <xsl:when test="substring(marc:leader,7,1)='g'">77469f80-b517-457e-b6e9-306237340287</xsl:when>
          <xsl:when test="substring(marc:leader,7,1)='i'">f7f4d525-5de9-4ba0-8625-ae2f96dbfc1c</xsl:when>
          <xsl:when test="substring(marc:leader,7,1)='j'">3be24c14-3551-4180-9292-26a786649c8b</xsl:when>
          <xsl:when test="substring(marc:leader,7,1)='k'">9eeda240-e773-4643-b037-06db69553b74</xsl:when>
          <xsl:when test="substring(marc:leader,7,1)='m'">db4e4829-351b-4825-94a9-72d1e442c49b</xsl:when>
          <xsl:when test="substring(marc:leader,7,1)='o'">a2c91e87-6bab-44d6-8adb-1fd02481fc4f</xsl:when>
          <xsl:when test="substring(marc:leader,7,1)='p'">a2c91e87-6bab-44d6-8adb-1fd02481fc4f</xsl:when>
          <xsl:when test="substring(marc:leader,7,1)='r'">c1e95c2b-4efc-48cf-9e71-edb622cf0c22</xsl:when>
          <xsl:when test="substring(marc:leader,7,1)='t'">6312d172-f0cf-40f6-b27d-9fa8feaf332f</xsl:when>
          <xsl:otherwise>a2c91e87-6bab-44d6-8adb-1fd02481fc4f</xsl:otherwise>
        </xsl:choose>
      </instanceTypeId>

      <source>HARVEST</source>

      <xsl:for-each select="marc:datafield[@tag='245']">
        <title>
          <xsl:value-of select="marc:subfield[@code='a']"/>
        </title>
      </xsl:for-each>
      <xsl:if test="marc:datafield[@tag='100']">
        <contributors>
          <arr>
          <xsl:for-each select="marc:datafield[@tag='100']">
            <i>
              <name>
                <xsl:value-of select="marc:subfield[@code='a']"/>
              </name>
              <contributorNameTypeId>2b94c631-fca9-4892-a730-03ee529ffe2a</contributorNameTypeId> <!-- personal name -->
              <contributorTypeId>6e09d47d-95e2-4d8a-831b-f777b8ef6d81</contributorTypeId> <!-- Author -->
            </i>
          </xsl:for-each>
          </arr>
        </contributors>
      </xsl:if>
      <xsl:if test="marc:datafield[@tag='250']">
        <editions>
          <arr>
          <xsl:for-each select="marc:datafield[@tag='250']">
            <i>
              <xsl:value-of select="marc:subfield[@code='a']"/>
              <xsl:if test="marc:subfield[@code='b']">; <xsl:value-of select="marc:subfield[@code='b']"/></xsl:if>
            </i>
          </xsl:for-each>
          </arr>
        </editions>
      </xsl:if>
      <xsl:if test="marc:datafield[@tag='260']">
        <publication>
          <arr>
            <xsl:for-each select="marc:datafield[@tag='260']">
              <i>
                <publisher>
                  <xsl:value-of select="marc:subfield[@code='b']"/>
                </publisher>
                <place>
                  <xsl:value-of select="marc:subfield[@code='a']"/>
                </place>
                <dateOfPublication>
                  <xsl:value-of select="marc:subfield[@code='c']"/>
                </dateOfPublication>
              </i>
            </xsl:for-each>
          </arr>
        </publication>
      </xsl:if>
      <xsl:if test="marc:datafield[@tag='264']">
        <publication>
          <arr>
            <xsl:for-each select="marc:datafield[@tag='264']">
              <i>
                <publisher>
                  <xsl:value-of select="marc:subfield[@code='b']"/>
                </publisher>
                <place>
                  <xsl:value-of select="marc:subfield[@code='a']"/>
                </place>
                <dateOfPublication>
                  <xsl:value-of select="marc:subfield[@code='c']"/>
                </dateOfPublication>
              </i>
            </xsl:for-each>
          </arr>
        </publication>
      </xsl:if>

      <xsl:if test="marc:datafield[@tag='600' or @tag='610' or @tag='611' or @tag='630' or @tag='648' or @tag='650' or @tag='651' or @tag='653' or @tag='654' or @tag='655' or @tag='656' or @tag='657' or @tag='658' or @tag='662' or @tag='69X']">
        <subjects>
          <arr>
          <xsl:for-each select="marc:datafield[@tag='600' or @tag='610' or @tag='611' or @tag='630' or @tag='648' or @tag='650' or @tag='651' or @tag='653' or @tag='654' or @tag='655' or @tag='656' or @tag='657' or @tag='658' or @tag='662' or @tag='69X']">
            <i>
              <xsl:value-of select="marc:subfield[@code='a']"/>
            </i>
          </xsl:for-each>
          </arr>
        </subjects>
      </xsl:if>
    </record>
  </xsl:template>

  <xsl:template match="text()"/>
</xsl:stylesheet>
