package pulse.properties;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import pulse.ui.Messages;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class NumericProperty implements Property, Comparable<NumericProperty> {

	private Number value;
	private Number minimum, maximum;
	private String descriptor;
	private String abbreviation;
	private Number dimensionFactor;
	private Number error;
	private NumericPropertyKeyword type;
	private boolean autoAdjustable = true;
	
	private final static List<NumericProperty> DEFAULT = readDefaultXML();
	
	public NumericProperty(Number value, NumericProperty pattern) {
		this(pattern);
		this.value = value;
	}
	
	public NumericProperty(NumericPropertyKeyword type, String descriptor, String abbreviation, Number value, NumericProperty pattern) {
		this(pattern);
		this.type = type;
		this.descriptor = descriptor;
		this.abbreviation = abbreviation;
		this.value = value;
	}
	
	public NumericProperty(NumericPropertyKeyword type, String descriptor, String abbreviation, Number value, Number minimum, 
			Number maximum, Number dimensionFactor, boolean autoAdjustable) {		
		this.type = type;
		this.descriptor = descriptor;
		this.abbreviation = abbreviation;
		this.value = value;
		this.dimensionFactor = dimensionFactor; 
		this.autoAdjustable = autoAdjustable;
		setBounds(minimum, maximum);
	}
	
	public NumericProperty(NumericPropertyKeyword type, String descriptor, String abbreviation, Number value, Number minimum, Number maximum, boolean autoAdjustable) {
		this(type, descriptor, abbreviation, value, minimum, maximum, 1.0, autoAdjustable);
	}
	
	public NumericProperty(NumericPropertyKeyword type, String descriptor, String abbreviation, Number value) {
		this.type = type;
		this.descriptor = descriptor;
		this.abbreviation = abbreviation;
		this.value = value;
		if(value instanceof Integer) {
			setBounds(Integer.MIN_VALUE, Integer.MAX_VALUE);
			this.dimensionFactor = 1;
		}
		else {
			setBounds(Double.MIN_VALUE, Double.MAX_VALUE);
			this.dimensionFactor = 1.0;
		}
	}
	
	public NumericProperty(NumericProperty num) {
		this.value 	 = num.value;
		this.descriptor = num.descriptor;
		this.abbreviation = num.abbreviation;
		this.minimum = num.minimum;
		this.maximum = num.maximum;
		this.type = num.type;
		this.dimensionFactor = num.dimensionFactor;
		this.autoAdjustable = num.autoAdjustable;
	}
	
	public NumericPropertyKeyword getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}
	
	public static boolean isValueSensible(NumericProperty property, Number val) {
		double value = val.doubleValue();
		double max = property.getMaximum().doubleValue();
		
		if( value > max ) 
			return false;
		
		double min = property.getMinimum().doubleValue();
		
		if( value < min )
			return false;

		return true;
		
	}

	public void setValue(Number value) {
		if( ! NumericProperty.isValueSensible(this, value) ) {
			String msg = "Allowed range for " + type + " : " + this.value.getClass().getSimpleName() 
					+ " from " + minimum + " to " + maximum + ". Received value: " + value; 
			throw new IllegalArgumentException(msg);
		}

		if(this.value instanceof Integer)
			this.value = value.intValue();
		else
			this.value = value.doubleValue();
		
	}
	
	public void setBounds(Number minimum, Number maximum) {
		Class<? extends Number> minClass = minimum.getClass();
		Class<? extends Number> maxClass = maximum.getClass();
		if(! minClass.equals(maxClass))
				throw new IllegalArgumentException("Types of minimum and maximum do not match: " + minClass + " and " + maxClass); 
		if(! minClass.equals(value.getClass()))
				throw new IllegalArgumentException("Interrupted attempt of setting " + minClass.getSimpleName()  
												   + " boundaries to a " + value.getClass().getSimpleName() + " property"); //$NON-NLS-1$ //$NON-NLS-2$
		this.minimum = minimum;
		this.maximum = maximum;
	}

	public Number getMinimum() {
		return minimum;
	}

	public void setMinimum(Number minimum) {
		this.minimum = minimum;
	}

	public Number getMaximum() {
		return maximum;
	}

	public void setMaximum(Number maximum) {
		this.maximum = maximum;
	}
	
	public boolean containsDouble() {
		return value instanceof Double;
	}
	
	public boolean containsInteger() {
		return value instanceof Integer;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(type != null) {
			sb.append(type);
			sb.append(" = ");
		}
		sb.append(formattedValue(false));
		return sb.toString();
	}
	
	@Override
	public String formattedValue() {
		return this.formattedValue(true);
	}
	
	public String formattedValue(boolean convertDimension) {
		
		if(value instanceof Integer) { 
			Number val = ((Number)value).intValue() * ((Number)dimensionFactor).intValue();
			return (NumberFormat.getIntegerInstance()).format(val);
		}
		
		final String PLUS_MINUS = Messages.getString("NumericProperty.PlusMinus"); //$NON-NLS-1$
		
		final double UPPER_LIMIT = 1e4;
		final double LOWER_LIMIT = 1e-2;
		final double ZERO		 = 1e-30;
		
		double adjustedValue = convertDimension ? (double) value * this.getDimensionFactor().doubleValue() : 
			(double) value;
		double absAdjustedValue = Math.abs(adjustedValue);
		
		DecimalFormat selectedFormat = null;
		
		if( (absAdjustedValue > UPPER_LIMIT) || (absAdjustedValue < LOWER_LIMIT && absAdjustedValue > ZERO) )
			selectedFormat = new DecimalFormat(Messages.getString("NumericProperty.BigNumberFormat")); //$NON-NLS-1$
		else
			selectedFormat = new DecimalFormat(Messages.getString("NumericProperty.NumberFormat")); //$NON-NLS-1$
		
		if(error != null)
			return selectedFormat.format(adjustedValue) 
				+ PLUS_MINUS 
				+ selectedFormat.format( convertDimension ? (double) error*getDimensionFactor().doubleValue() :
															(double) error );
		else
			return selectedFormat.format(adjustedValue);
			
	}
	
	public Number getDimensionFactor() {
		return dimensionFactor;
	}

	public void setDimensionFactor(Number dimensionFactor) {
		this.dimensionFactor = dimensionFactor;
	}
	
	public void setAutoAdjustable(boolean autoAdjustable) {
		this.autoAdjustable = autoAdjustable;
	}
	
	public boolean isAutoAdjustable() {
		return autoAdjustable;
	}

	public Number getError() {
		return error;
	}

	public void setError(Number error) {
		this.error = error;
	}

	@Override
	public String getDescriptor(boolean addHtmlTag) {
		return addHtmlTag ? "<html>" + descriptor + "</html>" : descriptor;
	}

	public void setDescriptor(String descriptor) {
		this.descriptor = descriptor;
	}

	public String getAbbreviation(boolean addHtmlTags) {
		return addHtmlTags ? "<html>" + abbreviation + "</html>" : abbreviation;
	}

	public void setAbbreviation(String abbreviation) {
		this.abbreviation = abbreviation;
	}
	
	public static NumericProperty derive(NumericPropertyKeyword keyword, Number value) {
		return new NumericProperty(
				value, DEFAULT.stream().filter(p -> p.getType() == keyword).findFirst().get());
	}
	
	public static NumericProperty def(NumericPropertyKeyword keyword) {
		return new NumericProperty(
				DEFAULT.stream().filter(p -> p.getType() == keyword).findFirst().get());
	}
	
	@Override
	public boolean equals(Object o) {
		if(! (o instanceof NumericProperty) )
			return false;

		NumericProperty onp = (NumericProperty) o;
		
		if(onp.getType() != this.getType())
			return false;
		
		if(!onp.getValue().equals(this.getValue()))
			return false;
		
		return true;
		
	}

	@Override
	public int compareTo(NumericProperty arg0) {
		int result = this.getType().compareTo(arg0.getType());
		
		if(result != 0) 
			return result;
		
		return compareValues(arg0);					
		
	}
	
	public int compareValues(NumericProperty arg0) {
		Double d1 = ((Number)value).doubleValue();
		Double d2 = ((Number)arg0.getValue()).doubleValue(); 
		
		return d1.compareTo(d2);
	}
	
	public void toXML(Document doc, Element rootElement) {
        Element property = doc.createElement(getClass().getSimpleName());
        rootElement.appendChild(property);

        Attr keyword = doc.createAttribute("keyword");
        keyword.setValue(type.toString());
        property.setAttributeNode(keyword);
        
        Attr descriptor = doc.createAttribute("descriptor");
        descriptor.setValue(this.descriptor);
        property.setAttributeNode(descriptor);

        Attr abbreviation = doc.createAttribute("abbreviation");
        abbreviation.setValue(this.abbreviation);
        property.setAttributeNode(abbreviation);
        
        Attr value = doc.createAttribute("value");
        value.setValue(this.value+"");
        property.setAttributeNode(value);
        
        Attr minimum = doc.createAttribute("minimum");
        minimum.setValue(this.minimum+"");
        property.setAttributeNode(minimum);
        
        Attr maximum = doc.createAttribute("maximum");
        maximum.setValue(this.maximum+"");
        property.setAttributeNode(maximum);
        
        Attr dim = doc.createAttribute("dimensionfactor");
        dim.setValue(this.dimensionFactor+"");
        property.setAttributeNode(dim);
        
        Attr autoAdj = doc.createAttribute("auto-adjustable");
        autoAdj.setValue(this.autoAdjustable+"");
        property.setAttributeNode(autoAdj);
        
        Attr primitiveType = doc.createAttribute("primitive-type");
        primitiveType.setValue(this.value instanceof Double ? "double" : "int");
        property.setAttributeNode(primitiveType);
        
        
	}
	
	/*
	 * Utility method that creates an .xml file listing all public final static numeric properties
	 * found in this class
	 */
	
	public static void saveXML() throws ParserConfigurationException, TransformerException {
        DocumentBuilderFactory dbFactory =
        DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.newDocument();

        Element rootElement = doc.createElement("NumericProperties");
        doc.appendChild(rootElement);
        
        List<NumericProperty> properties = new ArrayList<NumericProperty>();
        
        int modifiers; 
        
        for (Field field : NumericProperty.class.getDeclaredFields()) {
        		
        	 modifiers = field.getModifiers();

        	 if(!(Modifier.isPublic(modifiers) 
        			 && Modifier.isStatic(modifiers) 
        			 && Modifier
        	        .isFinal(modifiers)))
        		 continue;
        	
            if(!field.getType().equals(NumericProperty.class))
            	continue;
            NumericProperty value = null;
			try {
				value = (NumericProperty)field.get(null);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(value != null)
				properties.add(value);
        }
        
        properties.stream().forEach(p -> p.toXML(doc, rootElement));
        
        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(
        		new File(NumericProperty.class.getSimpleName()+".xml"));
        transformer.transform(source, result);
        
        // Output to console for testing
        StreamResult consoleResult = new StreamResult(System.out);
        transformer.transform(source, consoleResult);
        
	}
	
	/*
	 * Utility method used to read constants from XML file
	 */
	
	public static List<NumericProperty> readXML(InputStream inputStream) throws ParserConfigurationException, SAXException, IOException {
		
		List<NumericProperty> properties = new ArrayList<NumericProperty>();
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputStream);
        
        doc.getDocumentElement().normalize();
        NodeList nList = doc.getElementsByTagName(NumericProperty.class.getSimpleName());

        for (int temp = 0; temp < nList.getLength(); temp++) {
           Node nNode = nList.item(temp);
   
           if (nNode.getNodeType() == Node.ELEMENT_NODE) {
              Element eElement = (Element) nNode;
              NumericPropertyKeyword keyword = NumericPropertyKeyword.valueOf(
            		  eElement.getAttribute("keyword"));
              boolean autoAdjustable = Boolean.valueOf(
            		  eElement.getAttribute("auto-adjustable"));
              String descriptor = eElement.getAttribute("descriptor");
              String abbreviation = eElement.getAttribute("abbreviation");
              
              Number value, minimum, maximum, dimensionFactor;
              
              if(eElement.getAttribute("primitive-type").equalsIgnoreCase("double")) {
	              value = Double.valueOf(eElement.getAttribute("value"));
	              minimum = Double.valueOf(eElement.getAttribute("minimum"));
	              maximum = Double.valueOf(eElement.getAttribute("maximum"));
	              dimensionFactor = Double.valueOf(eElement.getAttribute("dimensionfactor"));
              } else {
	              value   = Integer.valueOf(eElement.getAttribute("value"));
	              minimum = Integer.valueOf(eElement.getAttribute("minimum"));
	              maximum = Integer.valueOf(eElement.getAttribute("maximum"));	  
	              dimensionFactor = Integer.valueOf(eElement.getAttribute("dimensionfactor"));
              }
              
              properties.add(new NumericProperty(keyword, descriptor, abbreviation, 
            		  value, minimum, maximum, dimensionFactor, autoAdjustable));
           }
        }
 
        return properties;
        
	}
	
	public static List<NumericProperty> readDefaultXML() {
		try {
			return readXML(NumericProperty.class.getResourceAsStream
					(Messages.getString("NumericProperty.XMLFile")));
		} catch (ParserConfigurationException | SAXException | IOException e) {
			System.err.println("Unable to read list of default numeric properties");
			e.printStackTrace();
		}
		return null;
	}
	
	public static NumericProperty theDefault(NumericPropertyKeyword keyword) {
		return DEFAULT.stream().filter(p -> p.getType() == keyword).findFirst().get();
	}
	
}