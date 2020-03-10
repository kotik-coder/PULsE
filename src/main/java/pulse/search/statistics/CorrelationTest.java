package pulse.search.statistics;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

public abstract class CorrelationTest extends PropertyHolder implements Reflexive {
	
	protected static double threshold = (double) NumericProperty.theDefault(NumericPropertyKeyword.CORRELATION_THRESHOLD).getValue();
	private static String selectedTestDescriptor;

	public CorrelationTest() {
	}
	
	public abstract double evaluate(double[] x, double[] y);
	
	public boolean compareToThreshold(double value) {
		return Math.abs(value) > threshold;
	}
	
	public static NumericProperty getThreshold() {
		return NumericProperty.derive(NumericPropertyKeyword.CORRELATION_THRESHOLD, threshold);
	}
	
	public static void setThreshold(NumericProperty p) {
		if(p.getType() != NumericPropertyKeyword.CORRELATION_THRESHOLD)
			throw new IllegalArgumentException("Illegal type: " + p.getType());
		
		threshold = (double)p.getValue();
		
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		if(type == NumericPropertyKeyword.CORRELATION_THRESHOLD)
			threshold = (double)property.getValue();
	}
	
	public static String getSelectedTestDescriptor() {
		return selectedTestDescriptor;
	}

	public static void setSelectedTestDescriptor(String selectedTestDescriptor) {
		CorrelationTest.selectedTestDescriptor = selectedTestDescriptor;
	}
	
}