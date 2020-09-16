package pulse.io.export;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.ui.Messages;

/**
 * Used to read and write XML files containing information about the default
 * {@code NumericPropert}ies. Is invoked at program start to retrieve the
 * information XML file in the resource folder.
 *
 */

public class XMLConverter {

	private XMLConverter() {
	}

	private static void toXML(NumericProperty np, Document doc, Element rootElement) {
		Element property = doc.createElement(np.getClass().getSimpleName());
		rootElement.appendChild(property);

		Attr keyword = doc.createAttribute("keyword");
		keyword.setValue(np.getType().toString());
		property.setAttributeNode(keyword);

		Attr descriptor = doc.createAttribute("descriptor");
		descriptor.setValue(np.getDescriptor(false));
		property.setAttributeNode(descriptor);

		Attr abbreviation = doc.createAttribute("abbreviation");
		abbreviation.setValue(np.getAbbreviation(false));
		property.setAttributeNode(abbreviation);

		Attr value = doc.createAttribute("value");
		value.setValue(np.getValue().toString());
		property.setAttributeNode(value);

		Attr minimum = doc.createAttribute("minimum");
		minimum.setValue(np.getMinimum().toString());
		property.setAttributeNode(minimum);

		Attr maximum = doc.createAttribute("maximum");
		maximum.setValue(np.getMaximum().toString());
		property.setAttributeNode(maximum);

		Attr dim = doc.createAttribute("dimensionfactor");
		dim.setValue(np.getDimensionFactor().toString());
		property.setAttributeNode(dim);

		Attr autoAdj = doc.createAttribute("auto-adjustable");
		autoAdj.setValue(np.isAutoAdjustable() + "");
		property.setAttributeNode(autoAdj);

		Attr primitiveType = doc.createAttribute("primitive-type");
		primitiveType.setValue(np.getValue() instanceof Double ? "double" : "int");
		property.setAttributeNode(primitiveType);

		Attr defSearch = doc.createAttribute("default-search-variable");
		primitiveType.setValue(np.isDefaultSearchVariable() + "");
		property.setAttributeNode(defSearch);

	}

	/**
	 * Utility method that creates an {@code .xml} file listing all public final
	 * static instances of {@code NumericProperty} found in the
	 * {@code NumericProperty} class.
	 */

	public static void writeXML() throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.newDocument();

		Element rootElement = doc.createElement("NumericProperties");
		doc.appendChild(rootElement);

		List<NumericProperty> properties = new ArrayList<>();

		int modifiers;

		/**
		 * Reads all final static {@code NumericProperty} constants in the
		 * {@code NumericProperty} class
		 */

		for (Field field : NumericProperty.class.getDeclaredFields()) {

			modifiers = field.getModifiers();

			// filter only public final static NumericProperties
			if ((Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers))
					&& field.getType().equals(NumericProperty.class)) {

				NumericProperty value = null;
				try {
					value = (NumericProperty) field.get(null);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					System.out.println("Unable to access field: " + field);
					e.printStackTrace();
				}
				if (value != null)
					properties.add(value);

			}

		}

		properties.stream().forEach(p -> XMLConverter.toXML(p, doc, rootElement));

		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(NumericProperty.class.getSimpleName() + ".xml"));
		transformer.transform(source, result);

		// Output to console for testing
		StreamResult consoleResult = new StreamResult(System.out);
		transformer.transform(source, consoleResult);

	}

	/**
	 * Utility method used to read {@code NumericProperty} constants from
	 * {@code xml} files.
	 * 
	 * @param inputStream the input stream used to read data from.
	 * @return a list of {@code NumericProperty} objects with their attributes
	 *         specified in the {@code xml} file.
	 */

	public static List<NumericProperty> readXML(InputStream inputStream)
			throws ParserConfigurationException, SAXException, IOException {

		List<NumericProperty> properties = new ArrayList<>();

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(inputStream);

		doc.getDocumentElement().normalize();
		NodeList nList = doc.getElementsByTagName(NumericProperty.class.getSimpleName());

		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);

			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				NumericPropertyKeyword keyword = NumericPropertyKeyword.valueOf(eElement.getAttribute("keyword"));
				boolean autoAdjustable = Boolean.valueOf(eElement.getAttribute("auto-adjustable"));
				boolean discreet = Boolean.valueOf(eElement.getAttribute("discreet"));
				String descriptor = eElement.getAttribute("descriptor");
				String abbreviation = eElement.getAttribute("abbreviation");
				boolean defSearch = Boolean.valueOf(eElement.getAttribute("default-search-variable"));

				Number value, minimum, maximum, dimensionFactor;

				if (eElement.getAttribute("primitive-type").equalsIgnoreCase("double")) {
					value = Double.valueOf(eElement.getAttribute("value"));
					minimum = Double.valueOf(eElement.getAttribute("minimum"));
					maximum = Double.valueOf(eElement.getAttribute("maximum"));
					dimensionFactor = Double.valueOf(eElement.getAttribute("dimensionfactor"));
				} else {
					value = Integer.valueOf(eElement.getAttribute("value"));
					minimum = Integer.valueOf(eElement.getAttribute("minimum"));
					maximum = Integer.valueOf(eElement.getAttribute("maximum"));
					dimensionFactor = Integer.valueOf(eElement.getAttribute("dimensionfactor"));
				}

				var np = new NumericProperty(keyword, value, minimum, maximum, dimensionFactor);
				np.setDescriptor(descriptor);
				np.setAbbreviation(abbreviation);
				np.setAutoAdjustable(autoAdjustable);
				np.setDiscreet(discreet);
				np.setDefaultSearchVariable(defSearch);
				properties.add(np);
			}
		}

		return properties;

	}

	/**
	 * The default XML file is specific in the 'messages.properties' text file in
	 * the {@code pulse.ui} package
	 * 
	 * @return a list of default instances of {@code NumericProperty}.
	 */

	public static List<NumericProperty> readDefaultXML() {
		try {
			return readXML(NumericProperty.class.getResourceAsStream(Messages.getString("NumericProperty.XMLFile")));
		} catch (ParserConfigurationException | SAXException | IOException e) {
			System.err.println("Unable to read list of default numeric properties");
			e.printStackTrace();
		}
		return null;
	}

}