## Harvesting bibliographic records into FOLIO Inventory

### Transform to JSON and ingest

There are three phases to the ingestion of harvested bibliographic XML records into FOLIO Inventory

1.  XSLT transformation of bibliographic records with holdings and items to an XML format suitable for transformation to Inventory JSON records.
2.  Transform the XML records to JSON
3.  Push the JSON records to Inventory

### Writing the style sheets for phase 1

The input to phase 1 could be OAI-PMH MARC21 records for example.

The Harvester itself does not have any particular expectations with regards to which elements should be present and what the content should be, since ultimately it will be Inventory validating the contents of the instances, holdings and items records pushed to it. If using standard FOLIO Inventory modules, these FOLIO Inventory schemas define required/allowed data and structures:

*   [instance.json](https://github.com/folio-org/mod-inventory-storage/blob/master/ramls/instance.json)
*   [holdingsrecord.json](https://github.com/folio-org/mod-inventory-storage/blob/master/ramls/holdingsrecord.json)
*   [item.json](https://github.com/folio-org/mod-inventory-storage/blob/master/ramls/item.json)

Yet, in the Harvester there _are_ a few conventions, which are necessary to comply with in order to produce valid FOLIO JSON records, say, from incoming MARC XML records.

Firstly, due to the conceptual differences between XML and JSON in how each data framework expresses repeatable elements, the output of the XSLT transformation must specifically encode repeatable elements as arrays, in a way that will be understood be the Harvester in phase 2 (transform the XML to JSON).

The convention is to define arrays by the tag <arr> and within the array define each element by the tag <i>

Repeatable elements structured like that, for example

  ```
   <myelement>
    <arr>
       <i>value 1</i>
       <i>value 2</i>
     </arr>
   <myelement>
 ```
would subsequently be transformed to JSON arrays, like this
```
"myelement":  ["value1", "value2"]
```

Or for complex elements:
```
  <myelement>
    <arr>
       <i>
          <x>a</x>
          <y>b</y>
        </i>
        <i>
          <x>c</a>
          <y>d</y>
        </i>
     </arr>
  </myelement>
```

would end up as this before being pushed to Inventory:
```
 "myelement": [{ "x": "a",  "y": "b" }, { "x": "c",  "y": "d" }]</pre>
```

Secondly, any holdings records must be embedded as an array of holdings in the instance XML, and items must be embedded in the holdings elements. The above examples illustrate that too. The convention is:
```
<record>
  [instance properties]
  <holdingsRecords>
    <arr>
      <i>
        [holdings properties]
        <items>
          <arr>
            <i>
              [item properties]
            </i>
          </arr>
        </items>
      </i
    </arr>
  </holdingsRecords>
</record></pre>
```

Applying these conventions to an actual sample MARC record:
```
<record>
      <header>
        <identifier>oai:alma.01SSHELCOMILLRSVL:991256103569</identifier>
        <datestamp>2019-09-25T00:58:12Z</datestamp>
        <setSpec>IndexDataHoldItemPhysicalTitles</setSpec>
        <setSpec>EDS:Set01</setSpec>
        <setSpec>IndexDataHoldItem</setSpec>
      </header>
      <metadata>
        <record xmlns="http://www.loc.gov/MARC21/slim" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.loc.gov/MARC21/slim http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd" >
          <leader>00683cam a2200253I  4500</leader>
          <controlfield tag="005">20131024131349.0</controlfield>
          <controlfield tag="008">750603s1959    nyua   j      000 0beng  </controlfield>
          <controlfield tag="001">991256103569</controlfield>
          <datafield tag="010" ind1=" " ind2=" ">
            <subfield code="a">   59011414 </subfield>
          </datafield>
          <datafield tag="035" ind1=" " ind2=" ">
            <subfield code="9">186613</subfield>
          </datafield>
          <datafield tag="035" ind1=" " ind2=" ">
            <subfield code="a">ocm01369356</subfield>
          </datafield>
          <datafield tag="035" ind1=" " ind2=" ">
            <subfield code="a">125</subfield>
          </datafield>
          <datafield tag="035" ind1=" " ind2=" ">
            <subfield code="a">(PMilS)125-millerdb-Voyager</subfield>
          </datafield>
          <datafield tag="040" ind1=" " ind2=" ">
            <subfield code="a">DLC</subfield>
            <subfield code="c">ORL</subfield>
            <subfield code="d">OCL</subfield>
            <subfield code="d">m.c.</subfield>
            <subfield code="d">OCL</subfield>
            <subfield code="d">MVS</subfield>
          </datafield>
          <datafield tag="049" ind1=" " ind2=" ">
            <subfield code="a">MVSC</subfield>
          </datafield>
          <datafield tag="050" ind1="0" ind2=" ">
            <subfield code="a">QC16.E5</subfield>
            <subfield code="b">B4</subfield>
          </datafield>
          <datafield tag="082" ind1=" " ind2=" ">
            <subfield code="a">925.3</subfield>
          </datafield>
          <datafield tag="092" ind1=" " ind2=" ">
            <subfield code="a">921</subfield>
            <subfield code="b">E35b</subfield>
          </datafield>
          <datafield tag="100" ind1="1" ind2=" ">
            <subfield code="a">Beckhard, Arthur J.</subfield>
          </datafield>
          <datafield tag="245" ind1="1" ind2="0">
            <subfield code="a">Albert Einstein.</subfield>
            <subfield code="c">Illustrated by Charles Beck.</subfield>
          </datafield>
          <datafield tag="260" ind1=" " ind2=" ">
            <subfield code="a">New York,</subfield>
            <subfield code="b">Putnam</subfield>
            <subfield code="c">[1959]</subfield>
          </datafield>
          <datafield tag="300" ind1=" " ind2=" ">
            <subfield code="a">126 p.</subfield>
            <subfield code="b">illus.</subfield>
            <subfield code="c">21 cm.</subfield>
          </datafield>
          <datafield tag="490" ind1="1" ind2=" ">
            <subfield code="a">Lives to remember</subfield>
          </datafield>
          <datafield tag="600" ind1="1" ind2="0">
            <subfield code="a">Einstein, Albert,</subfield>
            <subfield code="d">1879-1955</subfield>
            <subfield code="v">Juvenile literature.</subfield>
          </datafield>
          <datafield tag="830" ind1=" " ind2="0">
            <subfield code="a">Lives to remember.</subfield>
          </datafield>
          <datafield tag="900" ind1="0" ind2=" ">
            <subfield code="b">MILL</subfield>
            <subfield code="d">J</subfield>
            <subfield code="f">QC16.E5</subfield>
            <subfield code="f">B4</subfield>
            <subfield code="8">2220300860003569</subfield>
          </datafield>
          <datafield tag="995" ind1=" " ind2=" ">
            <subfield code="ff">2220300860003569</subfield>
            <subfield code="u">1</subfield>
            <subfield code="j">0</subfield>
            <subfield code="aa">J</subfield>
            <subfield code="t">BOOK</subfield>
            <subfield code="s">33151001864364</subfield>
            <subfield code="z">J</subfield>
            <subfield code="q">ON_RESERVE: N | RESERVE_CHARGES: 0 | RECALLS_PLACED: 0 | HOLDS_PLACED: 0 | HISTORICAL_BOOKINGS: 0 | SHORT_LOAN_CHARGES: 0 | </subfield>
            <subfield code="a">2320300850003569</subfield>
            <subfield code="c">J</subfield>
            <subfield code="bb">QC16.E5 B4</subfield>
            <subfield code="v">false</subfield>
            <subfield code="b">0</subfield>
            <subfield code="r">Suppressed for Renovation</subfield>
            <subfield code="h">MILL</subfield>
            <subfield code="i">MILL</subfield>
          </datafield>
        </record>
      </metadata>
    </record>
```

the style sheets could transform that OAI-PMH record to this, a format the Harvester would know how to produce FOLIO JSON from:

```
<?xml version="1.0"?>
<collection xmlns:marc="http://www.loc.gov/MARC21/slim" xmlns:oai20="http://www.openarchives.org/OAI/2.0/">
  <record>
    <source>MARC</source>
    <instanceTypeId>6312d172-f0cf-40f6-b27d-9fa8feaf332f</instanceTypeId>
    <identifiers>
      <arr>
        <i>
          <value>991256103569</value>
          <identifierTypeId>04d081a1-5c52-4b84-8962-949fc5f6773c</identifierTypeId>
        </i>
        <i>
          <value>   59011414 </value>
          <identifierTypeId>c858e4f2-2b6b-4385-842b-60732ee14abb</identifierTypeId>
        </i>
        <i/>
        <i>
          <value>ocm01369356</value>
          <identifierTypeId>7e591197-f335-4afb-bc6d-a6d76ca3bace</identifierTypeId>
        </i>
        <i>
          <value>125</value>
          <identifierTypeId>7e591197-f335-4afb-bc6d-a6d76ca3bace</identifierTypeId>
        </i>
        <i>
          <value>(PMilS)125-millerdb-Voyager</value>
          <identifierTypeId>7e591197-f335-4afb-bc6d-a6d76ca3bace</identifierTypeId>
        </i>
      </arr>
    </identifiers>
    <classifications>
      <arr>
        <i>
          <classificationNumber>QC16.E5; B4</classificationNumber>
          <classificationTypeId>ce176ace-a53e-4b4d-aa89-725ed7b2edac</classificationTypeId>
        </i>
        <i>
          <classificationNumber>925.3</classificationNumber>
          <classificationTypeId>42471af9-7d25-4f3a-bf78-60d29dcf463b</classificationTypeId>
        </i>
      </arr>
    </classifications>
    <title>Albert Einstein</title>
    <matchKey>
      <title>Albert Einstein</title>
      <remainder-of-title> : </remainder-of-title>
      <medium/>
    </matchKey>
    <contributors>
      <arr>
        <i>
          <name>Beckhard, Arthur J</name>
          <contributorNameTypeId>2b94c631-fca9-4892-a730-03ee529ffe2a</contributorNameTypeId>
          <primary>true</primary>
        </i>
      </arr>
    </contributors>
    <publication>
      <arr>
        <i>
          <publisher>Putnam</publisher>
          <place>New York,</place>
          <dateOfPublication>[1959]</dateOfPublication>
        </i>
      </arr>
    </publication>
    <physicalDescriptions>
      <arr>
        <i>126 p</i>
      </arr>
    </physicalDescriptions>
    <subjects>
      <arr>
        <i>Einstein, Albert--1879-1955--Juvenile literature</i>
      </arr>
    </subjects>
    <holdingsRecords>
      <arr>
        <i>
          <formerIds>
            <arr>
              <i>2220300860003569</i>
            </arr>
          </formerIds>
          <permanentLocationId>004c14d3-fb87-40fc-b4db-9e91738b4f1b</permanentLocationId>
          <callNumber/>
          <items>
            <arr>
              <i>
                <itemIdentifier>2320300850003569</itemIdentifier>
                <barcode>33151001864364</barcode>
                <permanentLoanTypeId>2b94c631-fca9-4892-a730-03ee529ffe27</permanentLoanTypeId>
                <materialTypeId>1a54b431-2e4f-452d-9cae-9cee66c9a892</materialTypeId>
                <status>
                  <name>Available</name>
                </status>
              </i>
            </arr>
          </items>
        </i>
      </arr>
    </holdingsRecords>
  </record>
</collection>
```

