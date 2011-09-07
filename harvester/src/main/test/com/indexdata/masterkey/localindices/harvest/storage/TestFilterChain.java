package com.indexdata.masterkey.localindices.harvest.storage;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import junit.framework.TestCase;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

import com.indexdata.xml.factory.XmlFactory;

public class TestFilterChain extends TestCase {

	String xml 
	= "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
	"<pz:collection xmlns:pz=\"http://www.indexdata.com/pazpar2/1.0\" xmlns:marc=\"http://www.loc.gov/MARC21/slim\">\n" + 
	"  <pz:record mergekey=\"title The not-just-anybody family. author Byars, Betsy Cromer. medium book\">\n" + 
	"    <pz:metadata type=\"id\">70307</pz:metadata>\n" + 
	"    <pz:metadata type=\"isbn\">0440459516</pz:metadata>\n" + 
	"    <pz:metadata type=\"author\">Byars, Betsy Cromer.</pz:metadata>\n" + 
	"    <pz:metadata type=\"author-title\"/>\n" + 
	"    <pz:metadata type=\"author-date\"/>\n" + 
	"    <pz:metadata type=\"title\">The not-just-anybody family.</pz:metadata>\n" + 
	"    <pz:metadata type=\"title-remainder\"/>\n" + 
	"    <pz:metadata type=\"title-responsibility\"/>\n" + 
	"    <pz:metadata type=\"title-dates\"/>\n" + 
	"    <pz:metadata type=\"title-medium\"/>\n" + 
	"    <pz:metadata type=\"title-number-section\"/>\n" + 
	"    <pz:metadata type=\"medium\">book</pz:metadata>\n" + 
	"    <pz:metadata tag=\"tag100\"/>\n" + 
	"    <pz:metadata type=\"meta-marc-tags\">020 090 100 245 942 952 </pz:metadata>\n" + 
	"    <pz:metadata type=\"meta-marc-cf008\">950323s19uu           c      000 1 eng d</pz:metadata>\n" + 
	"    <pz:metadata type=\"meta-frbr-short-title\">The not-just-anybody family.</pz:metadata>\n" + 
	"    <pz:metadata type=\"meta-frbr-full-title\">The not-just-anybody family.</pz:metadata>\n" + 
	"    <pz:metadata type=\"meta-frbr-lang\">eng</pz:metadata>\n" + 
	"  </pz:record>" +
	"</pz:collection>";

	String pz2solr =  
	"<?xml version=\"1.0\"?>\n" + 
	"<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" \n" + 
	"                xmlns:pz=\"http://www.indexdata.com/pazpar2/1.0\" >\n" + 
	"  <xsl:template  match=\"/\">\n" + 
	"    <add>\n" + 
	"      <xsl:apply-templates></xsl:apply-templates>\n" + 
	"    </add>\n" + 
	"  </xsl:template>\n" + 
	"  <xsl:template match=\"pz:record\">\n" + 
	"    <doc>\n" + 
	"      <xsl:apply-templates></xsl:apply-templates>\n" + 
	"    </doc>\n" + 
	"  </xsl:template>\n" + 
	"  <xsl:template match=\"pz:metadata\">\n" + 
	"    <field>\n" + 
	"      <xsl:attribute  name=\"name\">\n" + 
	"        <xsl:value-of select=\"@type\"/>\n" + 
	"      </xsl:attribute>\n" + 
	"        <xsl:value-of select=\".\"/>\n" + 
	"    </field>\n" + 
	"  </xsl:template>\n" + 
	"</xsl:stylesheet>";

	String solrXml = 
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
		"<response>\n" + 
		"<result name=\"response\" numFound=\"3\" start=\"0\">\n" + 
		"  <doc>\n" + 
		"    <arr name=\"ACNo\"><str>arcUP1497</str></arr>\n" + 
		"    <arr name=\"AlGNo\"><str>arcUP1463</str></arr>\n" + 
		"    <arr name=\"CoGNo\"><str>arcUP1463</str></arr>\n" + 
		"    <arr name=\"DateJP\"><str>明治年間</str></arr>\n" + 
		"    <arr name=\"DateJP1\"><str>ヵ</str></arr>\n" + 
		"    <arr name=\"DateYear\"><str>1911</str></arr>\n" + 
		"    <arr name=\"DescriptionColNo\"><str>1</str></arr>\n" + 
		"    <arr name=\"DescriptionPosition\"><str>01;01/01:01</str></arr>\n" + 
		"    <arr name=\"GroupExplanation\"><str>二帖一組の肉筆画帖。</str></arr>\n" + 
		"    <arr name=\"SystemClassification\"><str>能狂言画 肉筆</str></arr>\n" + 
		"    <arr name=\"creator\"><str>不明</str></arr>\n" + 
		"    <arr name=\"date\"><str>1911</str></arr>\n" + 
		"    <arr name=\"identifier\"><str>arcUP1497.jpg</str></arr>\n" + 
		"    <arr name=\"language\"><str>Japanese</str></arr>\n" + 
		"    <arr name=\"rights\"><str>立命館ARC</str></arr>\n" + 
		"    <arr name=\"subject\"><str>邯鄲男</str><str>黒髭</str><str>大飛手</str>\n" + 
		"      <str>大天神</str></arr>\n" + 
		"    <arr name=\"subjectAlternative\"><str>大☆見</str><str>小☆見</str><str>悪尉</str>\n" + 
		"      <str>平太</str></arr>\n" + 
		"    <arr name=\"thumburl\"><str>arcUP1497.jpg</str></arr>\n" + 
		"    <arr name=\"type\"><str>肉筆</str></arr>\n" + 
		"    <arr name=\"typeOrientation\"><str>横</str></arr>\n" + 
		"  </doc>\n" + 
		"  <doc>\n" + 
		"    <arr name=\"ACNo\"><str>arcUP0001</str></arr>\n" + 
		"    <arr name=\"CoGNo\"><str>arcUP0001</str></arr>\n" + 
		"    <arr name=\"ContributorGenerationRealname\"><str>〈5〉市川　団十郎</str></arr>\n" + 
		"    <arr name=\"CoverageSpatial\"><str>江戸</str></arr>\n" + 
		"    <arr name=\"CreatorSignature\"><str>春章画</str></arr>\n" + 
		"    <arr name=\"DateJP\"><str>安永０９</str></arr>\n" + 
		"    <arr name=\"DateModifiedDay\"><str>1</str></arr>\n" + 
		"    <arr name=\"DateModifiedMonth\"><str>2</str></arr>\n" + 
		"    <arr name=\"DateMonthDay\"><str>02・01</str></arr>\n" + 
		"    <arr name=\"DateYear\"><str>1780</str></arr>\n" + 
		"    <arr name=\"DescriptionPosition\"><str>01;03/01:01</str></arr>\n" + 
		"    <arr name=\"ReadingTitleMainPerformed\"><str>はつもんびくるわそが</str></arr>\n" + 
		"    <arr name=\"ReadingTitlePerformance\"><str>はつもんびくるわそが   はつもんびくるわそが</str></arr>\n" + 
		"    <arr name=\"Readingtitle\"><str>はつもんびくるわそが</str></arr>\n" + 
		"    <arr name=\"SpecialNote\"><str>暖簾に「てうしや」の文字あり。</str></arr>\n" + 
		"    <arr name=\"Theater\"><str>中村</str></arr>\n" + 
		"    <arr name=\"TitleMainPerformed\"><str>初紋日艶郷曽我</str></arr>\n" + 
		"    <arr name=\"TitlePerformance\"><str>初紋日艶郷曽我   初紋日艶郷曽我</str></arr>\n" + 
		"    <arr name=\"contributor\"><str>極印千右衛門</str></arr>\n" + 
		"    <arr name=\"coverage\"><str>江戸</str></arr>\n" + 
		"    <arr name=\"creator\"><str>春章</str></arr>\n" + 
		"    <arr name=\"date\"><str>1780-02-01</str></arr>\n" + 
		"    <arr name=\"format\"><str>細判</str></arr>\n" + 
		"    <arr name=\"identifier\"><str>arcUP0001.jpg</str></arr>\n" + 
		"    <arr name=\"language\"><str>Japanese</str></arr>\n" + 
		"    <arr name=\"playcycle\"><str>二番目</str></arr>\n" + 
		"    <arr name=\"rights\"><str>立命館ARC</str></arr>\n" + 
		"    <arr name=\"subject\"><str>画題等</str></arr>\n" + 
		"    <arr name=\"thumburl\"><str>arcUP0001.jpg</str></arr>\n" + 
		"    <arr name=\"title\"><str>初紋日艶郷曽我</str></arr>\n" + 
		"    <arr name=\"type\"><str>錦絵</str></arr>\n" + 
		"    <arr name=\"typeOrientation\"><str>横</str></arr>\n" + 
		"  </doc>\n" + 
		"  <doc>\n" + 
		"    <arr name=\"ACNo\"><str>arcUP0283</str></arr>\n" + 
		"    <arr name=\"CoGNo\"><str>arcUP0283</str></arr>\n" + 
		"    <arr name=\"ContributorGenerationRealname\"><str>市川　筆之助</str></arr>\n" + 
		"    <arr name=\"CoverageSpatial\"><str>大坂</str></arr>\n" + 
		"    <arr name=\"CreatorSignature\"><str>小信画</str></arr>\n" + 
		"    <arr name=\"DateJP\"><str>慶応０２</str></arr>\n" + 
		"    <arr name=\"DateModifiedDay\"><str>吉</str></arr>\n" + 
		"    <arr name=\"DateModifiedMonth\"><str>9</str></arr>\n" + 
		"    <arr name=\"DateMonthDay\"><str>09・吉</str></arr>\n" + 
		"    <arr name=\"DateYear\"><str>1866</str></arr>\n" + 
		"    <arr name=\"DescriptionColNo\"><str>1</str></arr>\n" + 
		"    <arr name=\"DescriptionPosition\"><str>01;02/01:01</str></arr>\n" + 
		"    <arr name=\"ReadingTitleMainPerformed\"><str>あねいもうとだてのおおきど</str></arr>\n" + 
		"    <arr name=\"ReadingTitlePerformance\"><str>そでがうらこきょうのにしき   あねいもうとだてのおおきど</str></arr>\n" + 
		"    <arr name=\"Readingtitle\"><str>そでがうらこきょうのにしき</str></arr>\n" + 
		"    <arr name=\"SystemClassification\"><str>上方絵</str></arr>\n" + 
		"    <arr name=\"Theater\"><str>堀江</str></arr>\n" + 
		"    <arr name=\"TitleMainPerformed\"><str>姉妹達大礎</str></arr>\n" + 
		"    <arr name=\"TitlePerformance\"><str>袖浦故郷錦   姉妹達大礎</str></arr>\n" + 
		"    <arr name=\"contributor\"><str>房の</str></arr>\n" + 
		"    <arr name=\"coverage\"><str>大坂</str></arr>\n" + 
		"    <arr name=\"creator\"><str>小信〈1〉</str></arr>\n" + 
		"    <arr name=\"date\"><str>1866-09</str></arr>\n" + 
		"    <arr name=\"format\"><str>中判</str></arr>\n" + 
		"    <arr name=\"identifier\"><str>arcUP0283.jpg</str></arr>\n" + 
		"    <arr name=\"language\"><str>Japanese</str></arr>\n" + 
		"    <arr name=\"rights\"><str>立命館ARC</str></arr>\n" + 
		"    <arr name=\"subject\"><str>一寸徳兵衛</str><str>夏祭浪華鑑切</str></arr>\n" + 
		"    <arr name=\"subjectAlternative\"><str>袖浦故郷錦</str><str>☆房の　市川筆之助</str></arr>\n" + 
		"    <arr name=\"thumburl\"><str>arcUP0283.jpg</str></arr>\n" + 
		"    <arr name=\"title\"><str>袖浦故郷錦</str></arr>\n" + 
		"    <arr name=\"type\"><str>錦絵</str></arr>\n" + 
		"    <arr name=\"typeOrientation\"><str>横</str></arr>\n" + 
		"  </doc>\n" + 
		"</result>\n" + 
		"</response>\n" + 
		"";
	String stylesheet2 =  
		"<?xml version=\"1.0\"?>\n" + 
		"<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" \n" + 
		"                xmlns:pz=\"http://www.indexdata.com/pazpar2/1.0\" >\n" + 		"\n" + 
		"  <xsl:param name=\"medium\" />\n" + 
		"  <xsl:template  match=\"/\">\n" + 
		"      <xsl:apply-templates></xsl:apply-templates>\n" + 
		"  </xsl:template>\n" + 
		"  <xsl:template match=\"doc\">\n" + 
		"    <pz:record>\n" + 
		"      <xsl:apply-templates></xsl:apply-templates>\n" + 
		"    </pz:record>\n" + 
		"  </xsl:template>\n" + 
		"  <xsl:template match=\"str[@name]\">\n" + 
		"    <pz:metadata>\n" + 
		"        <xsl:attribute  name=\"type\">\n" + 
		"          <xsl:value-of select=\"@name\"/>\n" + 
		"        </xsl:attribute>\n" + 
		"        <xsl:value-of select=\".\"/>\n" + 
		"    </pz:metadata>\n" + 
		"  </xsl:template>\n" + 
		"  <xsl:template match=\"arr\">\n" + 
		"    <xsl:for-each select=\"str\">\n" + 
		"      <xsl:call-template name=\"string\"/>\n" + 
		"    </xsl:for-each>\n" + 
		"  </xsl:template>\n" + 
		"  <xsl:template name=\"string\">\n" + 
		"      <pz:metadata>\n" + 
		"        <xsl:attribute  name=\"type\">\n" + 
		"          <xsl:value-of select=\"../@name\"/>\n" + 
		"        </xsl:attribute>\n" + 
		"        <xsl:choose>\n" + 
		"          <xsl:when test=\"../@name = 'medium' and string-length($medium) > 0\">\n" + 
		"            <xsl:value-of select=\"$medium\"/>\n" + 
		"          </xsl:when>\n" + 
		"          <xsl:otherwise>\n" + 
		"            <xsl:value-of select=\".\"/>\n" + 
		"          </xsl:otherwise>\n" + 
		"        </xsl:choose>\n" + 
		"      </pz:metadata>\n" + 
		"  </xsl:template>\n" + 
		"</xsl:stylesheet>";

	public void testFilterChainPz2Solr() {
		String[] test = { xml, pz2solr};
		doTest(test);
	}

	public void testFilterChainSolr2Pz2() {
		String[] test = { solrXml, stylesheet2 };
		doTest(test);
	}

	private void doTest(String[] argv) {
		if (argv.length < 2) {
			System.err.println(
					"Usage: java FilterChain xmlfile stylesheet1 [stylesheet2] ... ");
			throw new RuntimeException("Too few parameters");
		}

		try {
			// Set up the input stream
			InputSource input = new InputSource(new ByteArrayInputStream( argv[0].getBytes()));

			// Set up to read the input file
			SAXParserFactory spf = XmlFactory.newSAXParserFactoryInstance();
			SAXParser parser = spf.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			SAXTransformerFactory stf = (SAXTransformerFactory) XmlFactory.newTransformerInstance();
			XMLFilter filter = null;
			XMLReader parent = reader; 
			int index = 1;
			while (index < argv.length ) {
				filter = stf.newXMLFilter(new StreamSource(new ByteArrayInputStream(argv[index].getBytes())));
				filter.setParent(parent);
				parent = filter;
				index++;
			}
			assert(filter != null);
			// Set up the output stream
			StreamResult result = new StreamResult(System.out);
			// Set up the transformer to process the SAX events generated
			// by the last filter in the chain
			Transformer transformer = stf.newTransformer();
			SAXSource transformSource = new SAXSource(filter, input);
			transformer.transform(transformSource, result);
		} catch (TransformerConfigurationException tce) {
			// Error generated by the parser
			System.out.println("\n** Transformer Factory error");
			System.out.println("   " + tce.getMessage());

			// Use the contained exception, if any
			Throwable x = tce;

			if (tce.getException() != null) {
				x = tce.getException();
			}

			x.printStackTrace();
		} catch (TransformerException te) {
			// Error generated by the parser
			System.out.println("\n** Transformation error");
			System.out.println("   " + te.getMessage());

			// Use the contained exception, if any
			Throwable x = te;

			if (te.getException() != null) {
				x = te.getException();
			}

			x.printStackTrace();
		} catch (SAXException sxe) {
			// Error generated by this application
			// (or a parser-initialization error)
			Exception x = sxe;

			if (sxe.getException() != null) {
				x = sxe.getException();
			}

			x.printStackTrace();
		} catch (ParserConfigurationException pce) {
			// Parser with specified options can't be built
			pce.printStackTrace();
		} // doTest
	}	
	
	static void main(String argv[]) {
		new TestFilterChain().doTest(argv);
	}
}

