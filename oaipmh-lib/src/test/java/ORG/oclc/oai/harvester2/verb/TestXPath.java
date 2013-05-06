package ORG.oclc.oai.harvester2.verb;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class TestXPath extends TestCase {

  
  public TestXPath() {
    
  }
  
  private Document readXML(InputStream is ) throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true); // never forget this!
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(is);
    return doc;
  }
  public void testXPathNamespaceRoot() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
    XPathHelper<NodeList> helper = new XPathHelper<NodeList>(XPathConstants.NODESET, new OaiPmhNamespaceContext());
    ByteArrayInputStream buffer = new ByteArrayInputStream(xmlWithNamespace.getBytes("UTF-8"));
    Document doc = readXML(buffer);
    NodeList list = helper.evaluate(doc, "/");
    assertTrue("Wrong size " + list.getLength(), list.getLength() == 1); 

    list = helper.evaluate(doc, "/oai20:OAI-PMH");
    assertTrue("Wrong size " + list.getLength(), list.getLength() == 1); 
  
    list = helper.evaluate(doc, "/oai20:OAI-PMH/oai20:ListRecords");
    assertTrue("Wrong size " + list.getLength(), list.getLength() == 1); 

    list = helper.evaluate(doc, "/oai20:OAI-PMH/oai20:ListRecords/oai20:record");
    assertTrue("Wrong size " + list.getLength(), list.getLength() == 10); 

    list = helper.evaluate(doc, "/oai20:OAI-PMH/oai20:ListRecords/oai20:record");
    assertTrue("Wrong size " + list.getLength(), list.getLength() == 10); 

    XPathHelper<Node> nodeHelper = new XPathHelper<Node>(XPathConstants.NODE, new OaiPmhNamespaceContext());
    
    Node headerNode = nodeHelper.evaluate(list.item(0), "oai20:header");
    assertTrue("No header list found", headerNode != null);
    assertTrue("Not Header: " + headerNode.getNodeName(), headerNode.getLocalName() == "header"); 
    
    Node identifierNode = nodeHelper.evaluate(list.item(0), "oai20:header/oai20:identifier");
    assertTrue("Not Identifier" + identifierNode.getNodeName(), identifierNode.getLocalName() == "identifier");
    
    XPathHelper<String> stringHelper = new XPathHelper<String>(XPathConstants.STRING, new OaiPmhNamespaceContext());
    
    String identifier  = stringHelper.evaluate(list.item(0), "oai20:header/oai20:identifier/text()");
    assertTrue("Identifier: " + identifier, identifier.equals("oai:intechopen.com:1")); 

    

  }

  
  public void testXPathRoot() {
    
    
  }


  String xmlWithNamespace = 
      
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
      "<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\"\n" + 
      "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
      "         xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/\n" + 
      "         http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">\n" + 
      " <responseDate>2013-03-07T08:37:01Z</responseDate>\n" + 
      " <request verb=\"ListRecords\" metadataPrefix=\"oai_dc\">http://www.intechopen.com/oai/index.php</request>\n" + 
      " <ListRecords>\n" + 
      "  <record>\n" + 
      "   <header>\n" + 
      "    <identifier>oai:intechopen.com:1</identifier>\n" + 
      "    <datestamp>2005-07-01</datestamp>\n" + 
      "    <setSpec>3-86611-038-302005-07-01</setSpec>\n" + 
      "   </header>\n" + 
      "   <metadata>\n" + 
      "     <oai_dc:dc\n" + 
      "       xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n" + 
      "       xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" + 
      "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
      "       xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/\n" + 
      "       http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" + 
      "      <dc:title>Dynamic Modelling and Adaptive Traction Control for Mobile Robots</dc:title>\n" + 
      "      <dc:creator>Abdulgani Albagul</dc:creator>\n" + 
      "      <dc:creator>Wahyudi Martono</dc:creator>\n" + 
      "      <dc:creator>Riza Muhida</dc:creator>\n" + 
      "      <dc:subject>Cutting Edge Robotics</dc:subject>\n" + 
      "      <dc:description>In this paper two control strategies are developed, and tested on the robot. The `low&#039; level controller performance deteriorated with the changes in the surface condition such as the traction condition (friction coefficient). Meanwhile the combined controller detects the changes and copes with them in an adequate manner, maintaining a largely consistent performance. Some of the issues concerning the environmental structure and the high level control have been presented. Determining the location of the mobile robot plays a vital role in maintaining fast, smooth path-tracking. Measuring the position of the robot in the workspace gives the high level controller an indication of whether the robot is experiencing any slippage or not. All these issues are important for the motion control. The combined control system has been investigated and tested on the differential drive mobile robot. Simulation results show that the performance of the mobile robot under the combined system has improved and the accuracy of path-tracking also improved significantly as it can be seen from the figures.</dc:description>\n" + 
      "      <dc:publisher>INTECH Open Access Publisher</dc:publisher>\n" + 
      "      <dc:date>2005-07-01</dc:date>\n" + 
      "      <dc:type>1</dc:type>\n" + 
      "      <dc:identifier>http://www.intechopen.com/articles/show/title/dynamic_modelling_and_adaptive_traction_control_for_mobile_robots</dc:identifier>\n" + 
      "      <dc:language>eng</dc:language>\n" + 
      "      <dc:relation>ISBN:3-86611-038-3</dc:relation>\n" + 
      "      <dc:source>http://www.intechopen.com/download/pdf/pdfs_id/1</dc:source>\n" + 
      "     </oai_dc:dc>\n" + 
      "   </metadata>\n" + 
      "  </record>\n" + 
      "  <record>\n" + 
      "   <header>\n" + 
      "    <identifier>oai:intechopen.com:2</identifier>\n" + 
      "    <datestamp>2005-07-01</datestamp>\n" + 
      "    <setSpec>3-86611-038-302005-07-01</setSpec>\n" + 
      "   </header>\n" + 
      "   <metadata>\n" + 
      "     <oai_dc:dc\n" + 
      "       xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n" + 
      "       xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" + 
      "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
      "       xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/\n" + 
      "       http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" + 
      "      <dc:title>Rapid Prototyping for Robotics</dc:title>\n" + 
      "      <dc:creator>Imme Ebert-Uphoff</dc:creator>\n" + 
      "      <dc:creator>Clement M. Gosselin</dc:creator>\n" + 
      "      <dc:creator>David W. Rosen</dc:creator>\n" + 
      "      <dc:creator>Thierry Laliberte</dc:creator>\n" + 
      "      <dc:subject>Cutting Edge Robotics</dc:subject>\n" + 
      "      <dc:description>The rapid prototyping framework presented in this chapter provides fast, simple and inexpensivemethods for the design and fabrication of prototypes of robotic mechanisms.As evidenced by the examples presented above, the prototypes can be of great help togain more insight into the functionality of the mechanisms, as well as to convey theconcepts to others, especially to non-technical people. Furthermore, physical prototypescan be used to validate geometric and kinematic properties such as mechanicalinterferences, transmission characteristics, singularities and workspace. Actuated prototypes have also been successfully built and controlled. Actuated mechanisms can be used in lightweight applications or for demonstration purposes. The main limitation in such cases is the compliance and limited strength of the plasticparts, which limits the forces and torques that can be produced. Finally, several comprehensive examples have been given to illustrate how the rapidprototyping framework presented here can be used throughout the design process. Two robotic hands and a SLA machine model demonstrate a wide variety of link and jointfabrication methods, as well as the possibility of embedding sensors and actuators directlyinto mechanisms. In these examples, rapid prototyping has been used to demonstrate,validate, experimentally test (including destructive tests), modify, redesign and,in one case, support the machining of a metal prototype. 43</dc:description>\n" + 
      "      <dc:publisher>INTECH Open Access Publisher</dc:publisher>\n" + 
      "      <dc:date>2005-07-01</dc:date>\n" + 
      "      <dc:type>2</dc:type>\n" + 
      "      <dc:identifier>http://www.intechopen.com/articles/show/title/rapid_prototyping_for_robotics</dc:identifier>\n" + 
      "      <dc:language>eng</dc:language>\n" + 
      "      <dc:relation>ISBN:3-86611-038-3</dc:relation>\n" + 
      "      <dc:source>http://www.intechopen.com/download/pdf/pdfs_id/2</dc:source>\n" + 
      "     </oai_dc:dc>\n" + 
      "   </metadata>\n" + 
      "  </record>\n" + 
      "  <record>\n" + 
      "   <header>\n" + 
      "    <identifier>oai:intechopen.com:3</identifier>\n" + 
      "    <datestamp>2005-07-01</datestamp>\n" + 
      "    <setSpec>3-86611-038-302005-07-01</setSpec>\n" + 
      "   </header>\n" + 
      "   <metadata>\n" + 
      "     <oai_dc:dc\n" + 
      "       xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n" + 
      "       xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" + 
      "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
      "       xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/\n" + 
      "       http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" + 
      "      <dc:title>The Role of 3D Simulation in the Advanced Robotic Design, Test and Control</dc:title>\n" + 
      "      <dc:creator>Laszlo Vajta</dc:creator>\n" + 
      "      <dc:creator>Tamas Juhasz</dc:creator>\n" + 
      "      <dc:subject>Cutting Edge Robotics</dc:subject>\n" + 
      "      <dc:description>In this paper we presented an overview about modern visualization approaches in the field of telerobotics and interactive robotics simulation. We have made a comparison presenting the advantages and disadvantages between the (binocular) anaglyph and the (monocular) motion stereo methods in our robot simulator application. At this time, our team in the Mobile- and Microrobotics Laboratory is in the very center of experimenting with some of the great amount of available devices supporting advanced monocular and binocular 3D techniques to find one that is the nearest to our requirements. From this aspect we presented the RobotMAX simulator, which is currently under development at our laboratory: it will integrate robotics design (advanced visualization, driving mechanisms- and kinematical structure planning, sensor layout optimalization, etc.) and interactive control with the effective hardware-in-the-loop testing methodology into a modern simulation framework. Our mobile robot simulation project has another objective also: RobotMAX is aimed be a new research and education platform for the next generation of students in the field of mobile robotics and 3D imaging in our department at BUTE. 59</dc:description>\n" + 
      "      <dc:publisher>INTECH Open Access Publisher</dc:publisher>\n" + 
      "      <dc:date>2005-07-01</dc:date>\n" + 
      "      <dc:type>3</dc:type>\n" + 
      "      <dc:identifier>http://www.intechopen.com/articles/show/title/the_role_of_3d_simulation_in_the_advanced_robotic_design__test_and_control</dc:identifier>\n" + 
      "      <dc:language>eng</dc:language>\n" + 
      "      <dc:relation>ISBN:3-86611-038-3</dc:relation>\n" + 
      "      <dc:source>http://www.intechopen.com/download/pdf/pdfs_id/3</dc:source>\n" + 
      "     </oai_dc:dc>\n" + 
      "   </metadata>\n" + 
      "  </record>\n" + 
      "  <record>\n" + 
      "   <header>\n" + 
      "    <identifier>oai:intechopen.com:4</identifier>\n" + 
      "    <datestamp>2005-07-01</datestamp>\n" + 
      "    <setSpec>3-86611-038-302005-07-01</setSpec>\n" + 
      "   </header>\n" + 
      "   <metadata>\n" + 
      "     <oai_dc:dc\n" + 
      "       xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n" + 
      "       xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" + 
      "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
      "       xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/\n" + 
      "       http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" + 
      "      <dc:title>Mechatronics Design of a Mecanum Wheeled Mobile Robot</dc:title>\n" + 
      "      <dc:creator>Peter Xu</dc:creator>\n" + 
      "      <dc:subject>Cutting Edge Robotics</dc:subject>\n" + 
      "      <dc:description>The motor driver circuit board designed for the drive of the Mecanum wheels met all the specifications given in Section 3. The limitation to its current handling capacity was the relays which are rated at five amps. The board has provided a cheap and compact 72</dc:description>\n" + 
      "      <dc:publisher>INTECH Open Access Publisher</dc:publisher>\n" + 
      "      <dc:date>2005-07-01</dc:date>\n" + 
      "      <dc:type>4</dc:type>\n" + 
      "      <dc:identifier>http://www.intechopen.com/articles/show/title/mechatronics_design_of_a_mecanum_wheeled_mobile_robot</dc:identifier>\n" + 
      "      <dc:language>eng</dc:language>\n" + 
      "      <dc:relation>ISBN:3-86611-038-3</dc:relation>\n" + 
      "      <dc:source>http://www.intechopen.com/download/pdf/pdfs_id/4</dc:source>\n" + 
      "     </oai_dc:dc>\n" + 
      "   </metadata>\n" + 
      "  </record>\n" + 
      "  <record>\n" + 
      "   <header>\n" + 
      "    <identifier>oai:intechopen.com:5</identifier>\n" + 
      "    <datestamp>2005-07-01</datestamp>\n" + 
      "    <setSpec>3-86611-038-302005-07-01</setSpec>\n" + 
      "   </header>\n" + 
      "   <metadata>\n" + 
      "     <oai_dc:dc\n" + 
      "       xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n" + 
      "       xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" + 
      "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
      "       xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/\n" + 
      "       http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" + 
      "      <dc:title>Tracking Skin-Colored Objects in Real-Time</dc:title>\n" + 
      "      <dc:creator>Antonis A. Argyros</dc:creator>\n" + 
      "      <dc:creator>Manolis I.A. Lourakis</dc:creator>\n" + 
      "      <dc:subject>Cutting Edge Robotics</dc:subject>\n" + 
      "      <dc:description>In this paper, a method for tracking multiple skin-colored objects has been presented. The proposed method can cope successfully with multiple objects moving in complex patterns as they dynamically enter and exit the field of view of a camera. Since the tracker is not based on explicit background modeling and subtraction, it may operate even on image sequences acquired by a moving camera. Moreover, the color modeling and detection modules facilitate robust performance in the case of varying illumination conditions. Owing to the fact that the proposed approach treats the problem of tracking under very loose assumptions and in a computationally efficient manner, it can serve as a building block of larger vision systems employed in diverse application areas. Further research efforts have focused on (1) combining the proposed method with binocular stereo processing in order to derive 3D information regarding the tracked objects, (2) providing means for discriminating various types of skin-colored areas (e.g. hands, faces, etc), (3) developing methods that build upon the proposed tracker in order to be able to track interesting parts of skin-colored areas (e.g. eyes for faces, fingertips for hands, etc) and (4) employing the proposed tracker for supporting human gesture interpretation in the context of applications such as effective human computer interaction. Acknowledgements This work was partially supported by EU IST-2001-32184 project ActIPret.</dc:description>\n" + 
      "      <dc:publisher>INTECH Open Access Publisher</dc:publisher>\n" + 
      "      <dc:date>2005-07-01</dc:date>\n" + 
      "      <dc:type>5</dc:type>\n" + 
      "      <dc:identifier>http://www.intechopen.com/articles/show/title/tracking_skin-colored_objects_in_real-time</dc:identifier>\n" + 
      "      <dc:language>eng</dc:language>\n" + 
      "      <dc:relation>ISBN:3-86611-038-3</dc:relation>\n" + 
      "      <dc:source>http://www.intechopen.com/download/pdf/pdfs_id/5</dc:source>\n" + 
      "     </oai_dc:dc>\n" + 
      "   </metadata>\n" + 
      "  </record>\n" + 
      "  <record>\n" + 
      "   <header>\n" + 
      "    <identifier>oai:intechopen.com:6</identifier>\n" + 
      "    <datestamp>2005-07-01</datestamp>\n" + 
      "    <setSpec>3-86611-038-302005-07-01</setSpec>\n" + 
      "   </header>\n" + 
      "   <metadata>\n" + 
      "     <oai_dc:dc\n" + 
      "       xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n" + 
      "       xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" + 
      "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
      "       xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/\n" + 
      "       http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" + 
      "      <dc:title>Feature Extraction and Grouping for Robot Vision Tasks</dc:title>\n" + 
      "      <dc:creator>Miguel Cazorla</dc:creator>\n" + 
      "      <dc:creator>Francisco Escolano</dc:creator>\n" + 
      "      <dc:subject>Cutting Edge Robotics</dc:subject>\n" + 
      "      <dc:description>Exploiting computer vision for performing robotic tasks, like recognizing a given place in the environment or simply computing the relative orientation of the robot with respect to the environment, requires an in depth analysis of the vision modules involved in such computations. In this paper, we have considered three types of computations: local computations (for the estimation of junctions), local-to-global computations (for finding a geometric sketck) and voting-accumulation computations (for obtaining the relative orientation of the robot). We have addressed the analysis of the latter modules from the point of view of three practical requirements: reliability (robustness), efficiency and flexibility. These requirements are partially fullfiled by the methodology used: the three modules (junction detection, grouping, and orientation estimation) share elements of Bayesian inference. Sometimes, as in the case of junction detection, these elements yield statistical robustness. In other cases, as in the grouping module, the Bayesian formulation has a deep impact in the reduction of computational complexity. The Bayesian integration of feature extraction, grouping and orientation estimation is a good example of how to get flexibility by exploiting both the visual cues and the prior assumptions about the environment in order to solve a given task. On the other hand, as the basic visual cues are edges and there are fundamental limits regarding whether certain tasks relying on edge cues may be solved or not, independently of the algorithm, we also stress the convenience of having this bounds in mind in order to devise practical solutions for robotics tasks driven by computer vision. 103</dc:description>\n" + 
      "      <dc:publisher>INTECH Open Access Publisher</dc:publisher>\n" + 
      "      <dc:date>2005-07-01</dc:date>\n" + 
      "      <dc:type>6</dc:type>\n" + 
      "      <dc:identifier>http://www.intechopen.com/articles/show/title/feature_extraction_and_grouping_for_robot_vision_tasks</dc:identifier>\n" + 
      "      <dc:language>eng</dc:language>\n" + 
      "      <dc:relation>ISBN:3-86611-038-3</dc:relation>\n" + 
      "      <dc:source>http://www.intechopen.com/download/pdf/pdfs_id/6</dc:source>\n" + 
      "     </oai_dc:dc>\n" + 
      "   </metadata>\n" + 
      "  </record>\n" + 
      "  <record>\n" + 
      "   <header>\n" + 
      "    <identifier>oai:intechopen.com:7</identifier>\n" + 
      "    <datestamp>2005-07-01</datestamp>\n" + 
      "    <setSpec>3-86611-038-302005-07-01</setSpec>\n" + 
      "   </header>\n" + 
      "   <metadata>\n" + 
      "     <oai_dc:dc\n" + 
      "       xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n" + 
      "       xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" + 
      "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
      "       xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/\n" + 
      "       http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" + 
      "      <dc:title>Comparison of Demosaicking Methods for Color Information Extraction</dc:title>\n" + 
      "      <dc:creator>Flore Faille</dc:creator>\n" + 
      "      <dc:subject>Cutting Edge Robotics</dc:subject>\n" + 
      "      <dc:description>After an overview over state of the art and recent demosaicking methods, selected algorithms were compared using images with various content. To verify if demosaicked images are suitable for computer or robot vision tasks, the average MSE in typical color spaces (RGB, YUV, HSI and Irb) was measured. While the high inter-channel correlation model improves interpolation results significantly, it was also shown to be inaccurate in colored areas. WACPI and MBP (+WACPI) provide the best results. WACPI performs better in colored and in homogeneous areas. MBP better reconstructs texture and reduces wrong color artifacts. The choice between both algorithms should depend on the application, according to the most relevant information and to the image type (indoor/outdoor, natural/man-made objects). The MBP algorithm could be enhanced by processing only edge pixels like in EMBP: this would reduce the computation time and improve the performance in homogeneous areas. In addition, regions with saturated colors could be left unchanged. If emphasis lies on efficiency, ACPI and WACPI achieve the best compromise between speed and quality.</dc:description>\n" + 
      "      <dc:publisher>INTECH Open Access Publisher</dc:publisher>\n" + 
      "      <dc:date>2005-07-01</dc:date>\n" + 
      "      <dc:type>7</dc:type>\n" + 
      "      <dc:identifier>http://www.intechopen.com/articles/show/title/comparison_of_demosaicking_methods_for_color_information_extraction</dc:identifier>\n" + 
      "      <dc:language>eng</dc:language>\n" + 
      "      <dc:relation>ISBN:3-86611-038-3</dc:relation>\n" + 
      "      <dc:source>http://www.intechopen.com/download/pdf/pdfs_id/7</dc:source>\n" + 
      "     </oai_dc:dc>\n" + 
      "   </metadata>\n" + 
      "  </record>\n" + 
      "  <record>\n" + 
      "   <header>\n" + 
      "    <identifier>oai:intechopen.com:8</identifier>\n" + 
      "    <datestamp>2005-07-01</datestamp>\n" + 
      "    <setSpec>3-86611-038-302005-07-01</setSpec>\n" + 
      "   </header>\n" + 
      "   <metadata>\n" + 
      "     <oai_dc:dc\n" + 
      "       xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n" + 
      "       xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" + 
      "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
      "       xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/\n" + 
      "       http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" + 
      "      <dc:title>Robot Motion Trajectory-Measurement with Linear Inertial Sensors</dc:title>\n" + 
      "      <dc:creator>Bernard Favre Bulle</dc:creator>\n" + 
      "      <dc:subject>Cutting Edge Robotics</dc:subject>\n" + 
      "      <dc:publisher>INTECH Open Access Publisher</dc:publisher>\n" + 
      "      <dc:date>2005-07-01</dc:date>\n" + 
      "      <dc:type>8</dc:type>\n" + 
      "      <dc:identifier>http://www.intechopen.com/articles/show/title/robot_motion_trajectory-measurement_with_linear_inertial_sensors</dc:identifier>\n" + 
      "      <dc:language>eng</dc:language>\n" + 
      "      <dc:relation>ISBN:3-86611-038-3</dc:relation>\n" + 
      "      <dc:source>http://www.intechopen.com/download/pdf/pdfs_id/8</dc:source>\n" + 
      "     </oai_dc:dc>\n" + 
      "   </metadata>\n" + 
      "  </record>\n" + 
      "  <record>\n" + 
      "   <header>\n" + 
      "    <identifier>oai:intechopen.com:9</identifier>\n" + 
      "    <datestamp>2005-07-01</datestamp>\n" + 
      "    <setSpec>3-86611-038-302005-07-01</setSpec>\n" + 
      "   </header>\n" + 
      "   <metadata>\n" + 
      "     <oai_dc:dc\n" + 
      "       xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n" + 
      "       xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" + 
      "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
      "       xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/\n" + 
      "       http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" + 
      "      <dc:title>Supervisory Controller for Task Assignment and Resource Dispatching in Mobile Wireless Sensor Networks</dc:title>\n" + 
      "      <dc:creator>Vincenzo Giordano</dc:creator>\n" + 
      "      <dc:creator>Frank Lewis</dc:creator>\n" + 
      "      <dc:creator>Prasanna Ballal</dc:creator>\n" + 
      "      <dc:creator>Biagio Turchiano</dc:creator>\n" + 
      "      <dc:subject>Cutting Edge Robotics</dc:subject>\n" + 
      "      <dc:description>In this chapter we have presented a discrete-event coordination scheme for sensor networks composed of both mobile and stationary nodes. This architecture supports highlevel planning for multiple heterogeneous agents with multiple concurrent goals in dynamic environment. The proposed formulation of the DEC represents a complete dynamical description that allows efficient computer simulation of the WSN prior to implementing a given DE coordination scheme on the actual system. The similarity between simulation and experimental results shows the effectiveness of the DEC for simulation analysis. The obtained results also prove the striking potentialities of the matrix formulation of the DEC, namely: straightforward implementation of missions on the ground of intuitive linguistic descriptions; possibility to tackle adaptability and scalability issues at a centralized level using simple matrix operations; guaranteed performances, since the DEC is a mathematical framework which constraints the behaviour of the single agents in a predictable way. Future research will be devoted to the development of highlevel decision making algorithms for the dynamic updates of the matrices of the DEC, in order to automatically reformulate the missions on-line according to the current topology of the network and the current perception of the environment.</dc:description>\n" + 
      "      <dc:publisher>INTECH Open Access Publisher</dc:publisher>\n" + 
      "      <dc:date>2005-07-01</dc:date>\n" + 
      "      <dc:type>9</dc:type>\n" + 
      "      <dc:identifier>http://www.intechopen.com/articles/show/title/supervisory_controller_for_task_assignment_and_resource_dispatching_in_mobile_wireless_sensor_networ</dc:identifier>\n" + 
      "      <dc:language>eng</dc:language>\n" + 
      "      <dc:relation>ISBN:3-86611-038-3</dc:relation>\n" + 
      "      <dc:source>http://www.intechopen.com/download/pdf/pdfs_id/9</dc:source>\n" + 
      "     </oai_dc:dc>\n" + 
      "   </metadata>\n" + 
      "  </record>\n" + 
      "  <record>\n" + 
      "   <header>\n" + 
      "    <identifier>oai:intechopen.com:10</identifier>\n" + 
      "    <datestamp>2005-07-01</datestamp>\n" + 
      "    <setSpec>3-86611-038-302005-07-01</setSpec>\n" + 
      "   </header>\n" + 
      "   <metadata>\n" + 
      "     <oai_dc:dc\n" + 
      "       xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n" + 
      "       xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" + 
      "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
      "       xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/\n" + 
      "       http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" + 
      "      <dc:title>Design of a Generic, Vectorised, Machine-Vision library</dc:title>\n" + 
      "      <dc:creator>Bing-Chang Lai</dc:creator>\n" + 
      "      <dc:creator>Phillip John McKerrow</dc:creator>\n" + 
      "      <dc:subject>Cutting Edge Robotics</dc:subject>\n" + 
      "      <dc:description>Existing generic libraries, such as STL and VIGRA, are difficult to vectorise because iterators do not provide algorithms with information on how data are arranged in memory. Without this information, the algorithm cannot decide whether to use the scalar processor or the VPU to process the data. A generic, vectorised library needs to consider how functors invoke VPU instructions, how algorithms access vectors efficiently, and how edges, unaligned data, and prefetching are handled. The generic, vectorised, machinevision library design presented in this paper addresses these issues. The functors access the VPU through an abstract VPU. An abstract VPU is a virtual VPU that represents a set of real VPUs through an idealised instruction set and common constraints. The implementation used has no significant overheads in scalar mode, and for char types in AltiVec mode. Functors must also provide two implementations, one for the scalar processor and one for the VPU. This is necessary because the solution proposed uses both the scalar processor and the VPU to process data. Since VPU programs are difficult to implement efficiently, a categorisation scheme based on input-to-output correlation was used to reduce the number of algorithms required. Three categories were specified for VVIS: quantitative, transformative and convolutive. Quantitative operations require one input element per input set to produce zero or more output elements per output set. Transformative operations are a subset of quantitative and convolutive operations, requiring one input element per input set to produce one output element per output set. Convolutive operations accept a rectangle of input elements per input set to produce one output element per output set. Storages provide information on how data are arranged in memory to the algorithm, allowing the algorithm to automatically select appropriate implementations. Three main storage types were specified: contiguous, unknown or illife. Contiguous and unknown storages are one-dimensional while illife storages are n-dimensional storages. Only contiguous storages are expected to be processed using the VPU. Two types of contiguous storages were also specified: contiguous aligned storages, and contiguous unaligned storages. The iterator returned by begin() is always aligned for contiguous aligned storages, but may be unaligned for contiguous unaligned storages. Different algorithm implementations are required for different storage types. To support processing of different storage types simultaneously, storage types are designed to be subsets of one another. This allows an algorithm to gracefully degrade VPU usage and to provide efficient performance in the absence of VPUs. 172</dc:description>\n" + 
      "      <dc:publisher>INTECH Open Access Publisher</dc:publisher>\n" + 
      "      <dc:date>2005-07-01</dc:date>\n" + 
      "      <dc:type>10</dc:type>\n" + 
      "      <dc:identifier>http://www.intechopen.com/articles/show/title/design_of_a_generic__vectorised__machine-vision_library</dc:identifier>\n" + 
      "      <dc:language>eng</dc:language>\n" + 
      "      <dc:relation>ISBN:3-86611-038-3</dc:relation>\n" + 
      "      <dc:source>http://www.intechopen.com/download/pdf/pdfs_id/10</dc:source>\n" + 
      "     </oai_dc:dc>\n" + 
      "   </metadata>\n" + 
      "  </record>\n" + 
      "  <resumptionToken expirationDate=\"2013-03-08T08:37:01Z\"\n" + 
      "     cursor=\"0\">1362645447155</resumptionToken>\n" + 
      " </ListRecords>\n" + 
      "</OAI-PMH>";
}
