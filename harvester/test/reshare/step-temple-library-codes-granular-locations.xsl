<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:strip-space elements="*"/>
  <xsl:output indent="yes" method="xml" version="1.0" encoding="UTF-8"/>

  <xsl:template match="@* | node()">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()"/>
    </xsl:copy>
  </xsl:template>

  <!-- Map legacy code for the library/institution to a FOLIO resource identifier
       type UUID. Used for qualifying a local record identifier with the library
       it originated from in context of a shared index setup where the Instance
       represents bib records from multiple libraries.
  -->
  <xsl:template match="//identifierTypeIdHere">
    <identifierTypeId>17bb9b44-0063-44cc-8f1a-ccbb6188060b</identifierTypeId>
  </xsl:template>

  <!-- Map legacy location code to a FOLIO location UUID -->
  <xsl:template match="//permanentLocationIdHere">
    <permanentLocationId>
      <xsl:choose>
      <xsl:when test=".='aarc'">3b023939-4927-4ad2-928d-8528918d473b</xsl:when>
      <xsl:when test=".='aleisure'">fcd5b7dc-5070-43d2-9b6d-50f9ff5c901c</xsl:when>
      <xsl:when test=".='imc'">150ff2d9-3fc5-4b8d-b0ff-462e0bc48afe</xsl:when>
      <xsl:when test=".='intref'">00d66d45-3aec-4943-8f3a-d8a58bab9928</xsl:when>
      <xsl:when test=".='media'">654dbc19-477e-4c78-8ebd-ad13d1352641</xsl:when>
      <xsl:when test=".='micro'">db49487b-145a-4fe2-9bc5-5fa136e2b951</xsl:when>
      <xsl:when test=".='newbooks'">d4b1903a-ff15-42ff-a402-b52d9c7ae0e2</xsl:when>
      <xsl:when test=".='oversize'">f72e2dc0-caef-469c-901f-fef9c75c1d49</xsl:when>
      <xsl:when test=".='reference'">2d34dbdf-6604-4c3a-8a5d-6d7a4dd0bdee</xsl:when>
      <xsl:when test=".='reserve'">dfbbbb68-1407-44f6-b3a4-450202d4af40</xsl:when>
      <xsl:when test=".='stacks'">20fcc83e-a359-4ef8-9cc9-d12db6ab3835</xsl:when>
      <xsl:when test=".='asrs'">c2d4105c-69d3-4af5-a020-2f3b8c842ebc</xsl:when>
      <xsl:when test=".='media'">98f3ed1f-198a-493b-a9c8-0867a012e7a8</xsl:when>
      <xsl:when test=".='reference'">d608d295-e8f9-4cc6-a4df-17d05a33f0d1</xsl:when>
      <xsl:when test=".='rarestacks'">91bb0a23-2132-4084-b69f-ca3190546d70</xsl:when>
      <xsl:when test=".='media'">48cd53a4-fe39-4b37-a647-773700040fab</xsl:when>
      <xsl:when test=".='reserve'">94384975-2aad-4ef0-a75c-0d48a049b2fc</xsl:when>
      <xsl:when test=".='DSC'">872a75b9-d0b9-4be2-ba15-f544e7b2d093</xsl:when>
      <xsl:when test=".='games'">384ac5ea-701b-496e-8dc4-36d9eec3062c</xsl:when>
      <xsl:when test=".='REF'">5101ffe1-612e-4fe8-88b3-49f859ae891b</xsl:when>
      <xsl:when test=".='dss_reserv'">263481d0-e1eb-44ca-b2dd-b14c034d8627</xsl:when>
      <xsl:when test=".='circ'">8020fda6-8c67-4ef4-a6ac-1935d91ef40f</xsl:when>
      <xsl:when test=".='intref'">3828c24d-e15e-4c21-99e7-0b7eda7ff16c</xsl:when>
      <xsl:when test=".='leisure'">0de293b2-8ec1-4042-be66-f0aa00dc473b</xsl:when>
      <xsl:when test=".='medhum'">003feae2-f24b-4283-ac23-d960a14fe069</xsl:when>
      <xsl:when test=".='oversize'">4c2e7896-e287-4c47-8b65-98a27eaa5ace</xsl:when>
      <xsl:when test=".='reference'">6963cf8d-07bb-4082-a7c0-460d5e93ab5f</xsl:when>
      <xsl:when test=".='reserve'">78433f4a-a95f-409a-a841-08e25e705c7c</xsl:when>
      <xsl:when test=".='serials'">df64aae2-644c-41a9-b7f4-b44516ca6b01</xsl:when>
      <xsl:when test=".='stacks'">e7c39542-883a-4eb7-86ea-b31dc4a8260c</xsl:when>
      <xsl:when test=".='techserv'">80cc1d99-1869-4468-b59e-597f4085e084</xsl:when>
      <xsl:when test=".='harrisburg'">11bc705c-7219-47fe-99c1-4eacd62c1c0e</xsl:when>
      <xsl:when test=".='intref'">56e7990c-ec27-455e-ba67-ab3b32230e4b</xsl:when>
      <xsl:when test=".='media'">7e7a1811-4683-4d4e-bcb0-db7af5c168fa</xsl:when>
      <xsl:when test=".='oversize'">38124c16-5520-4ebc-a7c6-cd0f6c676f33</xsl:when>
      <xsl:when test=".='reference'">65f88edb-c906-4fe5-9137-baad6479a260</xsl:when>
      <xsl:when test=".='reserve'">31e0b189-5915-4d15-85ed-d33d6cf1dfb1</xsl:when>
      <xsl:when test=".='stacks'">a35b0715-9c5a-4722-a4e1-adb0712fccbf</xsl:when>
      <xsl:when test=".='media'">518c242a-f505-4841-9319-8ffd5f369a63</xsl:when>
      <xsl:when test=".='osaka'">f100eb85-ace6-4a84-b366-ec35f4bc43da</xsl:when>
      <xsl:when test=".='reference'">16589198-5090-4098-b884-f761db1838c1</xsl:when>
      <xsl:when test=".='remote'">cc40f151-c471-4a36-8ae5-00609b139971</xsl:when>
      <xsl:when test=".='reserve'">e702e038-dfd7-4118-a132-51b289ecf233</xsl:when>
      <xsl:when test=".='serials'">6a13dbad-20b2-4512-8961-490574bcf34b</xsl:when>
      <xsl:when test=".='stacks'">53b30500-b865-41e1-9557-9ceeab796dce</xsl:when>
      <xsl:when test=".='a_rare'">8be54c3d-46cb-4801-800e-07a2c2b8c9ad</xsl:when>
      <xsl:when test=".='b_rare'">3463eede-8815-448f-8114-5c6a56e10490</xsl:when>
      <xsl:when test=".='g_remote'">fb2a8560-6ca8-455a-851e-d3997260c43e</xsl:when>
      <xsl:when test=".='l_remote'">186747f0-ef5c-4bb3-b70f-7d0daab284f4</xsl:when>
      <xsl:when test=".='p_GovDocs'">b168c032-a8be-41db-a7e1-2ed08fb7640c</xsl:when>
      <xsl:when test=".='p_govdocmf'">4a66d65b-7097-4dc1-b0a6-50fc6598c7fe</xsl:when>
      <xsl:when test=".='p_media'">e8ac82cc-3683-44a5-a26d-0fb03b1d6a65</xsl:when>
      <xsl:when test=".='p_micro'">3c506982-123b-461e-9442-6bf88cb60018</xsl:when>
      <xsl:when test=".='p_oversize'">23ea8820-1db8-4204-890f-0b479d9b42bd</xsl:when>
      <xsl:when test=".='p_remote'">86e74702-9a3a-481e-b327-9cf5dea368bc</xsl:when>
      <xsl:when test=".='kiosk'">f703a962-9730-46d3-aa3e-b3ff5ffeef29</xsl:when>
      <xsl:when test=".='archives'">7725cc3f-b0a0-4a81-be09-7adfe2d01543</xsl:when>
      <xsl:when test=".='closestack'">dbb9222c-d9ca-4728-9f27-44838f142214</xsl:when>
      <xsl:when test=".='govdoc'">3550fe4d-05ee-4279-9d03-fe9acf6a4503</xsl:when>
      <xsl:when test=".='hirst'">d0324d78-bec4-4e77-9b84-07a21e199f0e</xsl:when>
      <xsl:when test=".='media'">4e9866bf-a85c-40d8-a267-d91294cd4c56</xsl:when>
      <xsl:when test=".='micro'">b5f98269-f3ab-415b-a101-3259a0aea907</xsl:when>
      <xsl:when test=".='open3'">d5a49b35-d971-42e0-8fa0-9396dfc29f3a</xsl:when>
      <xsl:when test=".='open3a'">136fd3a6-2208-4cfa-8e29-5b25ba137aa8</xsl:when>
      <xsl:when test=".='open4'">a3ad5984-0ddd-49e4-87f2-bbea2fdf5bc1</xsl:when>
      <xsl:when test=".='open4a'">26fc9e1c-f532-4d11-8857-d928fc67fb6b</xsl:when>
      <xsl:when test=".='open5'">f8fc5206-d0c4-4274-8ce5-e3f809b3560f</xsl:when>
      <xsl:when test=".='open5a'">36d37b24-af57-4bd0-b073-6b8aabdba8ca</xsl:when>
      <xsl:when test=".='open6a'">d15a73e8-3c03-4731-9a9e-112e0dccc1b7</xsl:when>
      <xsl:when test=".='oversize2'">c59d77ca-3a7e-46ef-9cdf-ccdf242c1c67</xsl:when>
      <xsl:when test=".='oversize5a'">26d62136-76a2-4fca-b03c-c84ad674e529</xsl:when>
      <xsl:when test=".='pamphlets'">049e502c-8e87-49fb-b85b-ce88339e8c62</xsl:when>
      <xsl:when test=".='rarestacks'">c0334544-f513-4701-986c-63203ee15965</xsl:when>
      <xsl:when test=".='rawle'">eed4bf5a-d1f6-4674-83f7-7ffde7a9b6fb</xsl:when>
      <xsl:when test=".='rawle3'">987376e4-d326-47ca-af57-eb251d027a6d</xsl:when>
      <xsl:when test=".='reference'">10be5aff-ab25-4db1-8e96-a9d41e8f2f7d</xsl:when>
      <xsl:when test=".='reserve'">dacaff15-4f92-45e4-ac55-b125c10ba822</xsl:when>
      <xsl:when test=".='serials'">34b41444-af38-41d1-abc8-2ee21d60af26</xsl:when>
      <xsl:when test=".='specintl'">6ca139c5-8bde-4c26-99be-69be5f79f743</xsl:when>
      <xsl:when test=".='specpa'">e90f2a00-8b42-4340-b38e-187bcedc0621</xsl:when>
      <xsl:when test=".='stacks'">fc888dce-7974-4762-b373-f8ef35039712</xsl:when>
      <xsl:when test=".='trials'">3718680f-aa78-40d0-a267-8f637886bc00</xsl:when>
      <xsl:when test=".='hirsch'">ba26466e-95c5-4931-b055-98d0396ff10f</xsl:when>
      <xsl:when test=".='juvenile'">f66c87e4-b27e-4d1e-909e-2d4d486ecc2f</xsl:when>
      <xsl:when test=".='leisure'">a54bd0b4-38f3-4cbe-a53e-c5572b72ad22</xsl:when>
      <xsl:when test=".='m_reserve'">9592fc29-37fb-438e-9ca8-7d585e5e82e3</xsl:when>
      <xsl:when test=".='newbooks'">29f88690-7cb9-4894-98e1-5ababf3e2b8a</xsl:when>
      <xsl:when test=".='reference'">19081061-eb1f-4a4a-b570-e15d5c9179c8</xsl:when>
      <xsl:when test=".='reserve'">7242cbe8-51db-4960-b0cd-804856d273af</xsl:when>
      <xsl:when test=".='serials'">66afe9ad-5db4-4a72-bdbc-c1281b5e0f5a</xsl:when>
      <xsl:when test=".='servicedsk'">2b3a68d6-d8b8-4a91-a8df-3338bc029ab9</xsl:when>
      <xsl:when test=".='stacks'">26e0bca0-65c3-4164-95c8-3a34788dbbbd</xsl:when>
      <xsl:when test=".='storage'">8df0b46c-7233-49e9-9575-3dc8c8c8772d</xsl:when>
      <xsl:when test=".='techserv'">d449059a-b306-4205-9b00-e579b259570f</xsl:when>
      <xsl:when test=".='intref'">65b9a2ad-c69c-4c5b-9ee2-65b879e56a72</xsl:when>
      <xsl:when test=".='leisure'">42e04209-bb1c-4d6b-80cf-4b14536b0e65</xsl:when>
      <xsl:when test=".='media'">691b4339-80d4-427e-9320-3b23359a4b03</xsl:when>
      <xsl:when test=".='reference'">043641d9-dfd2-4efc-9801-fc74bbd28735</xsl:when>
      <xsl:when test=".='reserve'">c399f199-89ee-4ac4-b20f-2020d51e8f81</xsl:when>
      <xsl:when test=".='serials'">c3b423eb-5899-405d-a0cd-dfa327de0f75</xsl:when>
      <xsl:when test=".='stacks'">911b0f8f-0192-4772-b530-a63ee21848e0</xsl:when>
      <xsl:when test=".='techserv'">083f2a86-c7f3-414b-9a4a-8c91bb6abc73</xsl:when>
      <xsl:when test=".='plc'">6dd970eb-3aa4-48a0-9be2-95c76043cadd</xsl:when>
      <xsl:when test=".='presser'">56adba42-e5db-4a43-8199-918e8acaaf75</xsl:when>
      <xsl:when test=".='exhibit'">239d242f-6a50-4b03-9609-b5e59a9cad0b</xsl:when>
      <xsl:when test=".='fiction'">21b44873-879e-4f6f-9f10-b858a6a081de</xsl:when>
      <xsl:when test=".='media'">c7491056-7ad4-4ffb-824d-18c5279be8b6</xsl:when>
      <xsl:when test=".='oversize'">334c1006-1f62-407f-bdbc-1aeb81b43448</xsl:when>
      <xsl:when test=".='reference'">294325af-ade4-4696-8f46-0d5af99c3aa6</xsl:when>
      <xsl:when test=".='reserve'">48af1fc8-0fc2-4815-b45d-293c0d556516</xsl:when>
      <xsl:when test=".='serials'">d0ac810a-cf5d-4db9-b90c-0987bffb2456</xsl:when>
      <xsl:when test=".='stacks'">38d66a15-7b49-44aa-8b5b-c37827f881f0</xsl:when>
      <xsl:when test=".='rarestacks'">07a5c264-2c20-44f8-94dc-d665cae54f07</xsl:when>
      <xsl:otherwise>87038e41-0990-49ea-abd9-1ad00a786e45</xsl:otherwise>
      </xsl:choose>
    </permanentLocationId>
  </xsl:template>

  <!-- Set FOLIO Inventory ID for the Temple institution -->
  <xsl:template match="//institutionIdHere">
     <institutionId>05770b43-8f13-41e3-9ffd-8c13ae570d18</institutionId>
  </xsl:template>
</xsl:stylesheet>
