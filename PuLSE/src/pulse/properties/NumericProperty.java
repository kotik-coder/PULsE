package pulse.properties;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class NumericProperty implements Property {

	private Number value;
	private Number minimum, maximum;
	private String simpleName;
	private Number dimensionFactor;
	private Number error;
	private boolean autoAdjustable = true;
	
	public NumericProperty(Number value, NumericProperty pattern) {
		this(pattern);
		this.value = value;
	}
	
	public NumericProperty(String simpleName, Number value, NumericProperty pattern) {
		this(pattern);
		this.simpleName = simpleName;
		this.value = value;
	}
	
	public NumericProperty(String shortName, Number value, Number minimum, Number maximum, Number defaultValue, Number dimensionFactor, boolean autoAdjustable) {
		this.simpleName = shortName;
		this.value = value;
		this.dimensionFactor = dimensionFactor; 
		this.autoAdjustable = autoAdjustable;
		setBounds(minimum, maximum);
	}
	
	public NumericProperty(String shortName, Number value, Number minimum, Number maximum, boolean autoAdjustable) {
		this(shortName, value, minimum, maximum, 0.0, 1.0, autoAdjustable);
	}
	
	public NumericProperty(String shortName, Number value) {
		this.simpleName = shortName; 
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
		this.minimum = num.minimum;
		this.maximum = num.maximum;
		this.simpleName = num.simpleName;
		this.dimensionFactor = num.dimensionFactor;
		this.autoAdjustable = num.autoAdjustable;
	}
	
	public String getSimpleName() {
		return simpleName;
	}

	public Object getValue() {
		return value;
	}
	
	public boolean isValueSensible(Number value) {
		if( value.doubleValue() > maximum.doubleValue() ) 
			return false;
		
		if( value.doubleValue() < minimum.doubleValue() )
			return false;

		return true;
		
	}

	public void setValue(Number value) {
		if( ! isValueSensible(value) ) {
			String msg = "Allowed range for " + simpleName + " : " + this.value.getClass().getSimpleName() 
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
		if(simpleName != null) {
			sb.append(simpleName);
			sb.append(" = ");
		}
		sb.append(formattedValue(false));
		return sb.toString();
	}
	
	@Override
	public String formattedValue() {
		return formattedValue(true);
	}
	
	public String formattedValue(boolean convertDimension) {
		
		if(value instanceof Integer) { 
			Number val = (int)value * (int)dimensionFactor;
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
	
	public final static NumericProperty DEFAULT_DIFFUSIVITY 		= new NumericProperty(Messages.getString("NumericProperty.15"), 1E-6, 1E-10, 1E-3, 1E-6, 1E6, true); //$NON-NLS-1$
	public final static NumericProperty DEFAULT_BASELINE	 		= new NumericProperty(Messages.getString("NumericProperty.16"), 0.0, -10.0, 10.0, 0.0, 1.0, true); //$NON-NLS-1$
	public final static NumericProperty DEFAULT_THICKNESS 			= new NumericProperty(Messages.getString("NumericProperty.17"), 1E-3, 1E-6, 1E-1, 1E-3, 1E3, false); //$NON-NLS-1$
	public final static NumericProperty DEFAULT_DIAMETER 			= new NumericProperty(Messages.getString("NumericProperty.18"), 10E-3, 1E-6, 1E-1, 10E-3, 1E3, false); //$NON-NLS-1$
	public final static NumericProperty DEFAULT_BIOT				= new NumericProperty(Messages.getString("NumericProperty.19"), 0.0, 0.0, 10.0, 0.0, 1.0, true); //$NON-NLS-1$
	public final static NumericProperty DEFAULT_MAXTEMP				= new NumericProperty(Messages.getString("NumericProperty.20"), 1.0, 0.01, 100.0, 1.0, 1.0, true); //$NON-NLS-1$
	public final static NumericProperty DEFAULT_NONLINEAR_PRECISION	= new NumericProperty(Messages.getString("NumericProperty.21"), 1E-3, 1E-6, 1E-2, 1E-3, 1.0, false); //$NON-NLS-1$
	public final static NumericProperty DEFAULT_COUNT				= new NumericProperty(Messages.getString("NumericProperty.22"), 100, 10, 1000, 100, 1, false); //$NON-NLS-1$
	public final static NumericProperty DEFAULT_PULSE_WIDTH			= new NumericProperty(Messages.getString("NumericProperty.23"), 1.5E-3, 1E-4, 1.0, 1.5E-3, 1E3, false); //$NON-NLS-1$
	public final static NumericProperty DEFAULT_SPOT_DIAMETER		= new NumericProperty(Messages.getString("NumericProperty.24"), 10.0E-3, 0.1E-3, 50E-3, 10.0E-3, 1E3, false); //$NON-NLS-1$
	public final static NumericProperty DEFAULT_TIME_LIMIT			= new NumericProperty(Messages.getString("NumericProperty.25"), 1.0, 1E-6, 20.0, 1.0, 1.0, true); //$NON-NLS-1$
	public final static NumericProperty DEFAULT_GRID_DENSITY		= new NumericProperty(Messages.getString("NumericProperty.26"), 30, 10, 200, 30, 1.0, true); //$NON-NLS-1$
	public final static NumericProperty DEFAULT_CV					= new NumericProperty(Messages.getString("NumericProperty.27"), 0.0, 1.0, 10000.0, true); //$NON-NLS-1$
	public final static NumericProperty DEFAULT_LAMBDA				= new NumericProperty(Messages.getString("NumericProperty.35"), 0.0, 1.0, 10000.0, true); //$NON-NLS-1$
	public final static NumericProperty DEFAULT_EMISSIVITY			= new NumericProperty(Messages.getString("NumericProperty.36"), 0.0, 1.0, 2.0, true); //$NON-NLS-1$
	public final static NumericProperty DEFAULT_RHO					= new NumericProperty(Messages.getString("NumericProperty.28"), 0.0, 10.0, 30000.0, true); //$NON-NLS-1$
	public final static NumericProperty DEFAULT_QABS				= new NumericProperty(Messages.getString("NumericProperty.29"), 7.0, 0.1, 200.0, 7.0, 1.0, true); //$NON-NLS-1$
	public final static NumericProperty DEFAULT_T					= new NumericProperty(Messages.getString("NumericProperty.30"), 298.0, 0.0, 10000.0, 298.0, 1.0, true); //$NON-NLS-1$
	public final static NumericProperty DEFAULT_LINEAR_RESOLUTION 	= new NumericProperty(Messages.getString("NumericProperty.31"), 1E-7, 1E-8, 1E-1, 1E-7, 1.0, false); //$NON-NLS-1$
	public final static NumericProperty DEFAULT_GRADIENT_RESOLUTION = new NumericProperty(Messages.getString("NumericProperty.32"), 1E-4, 1E-7, 1E-1, 1E-4, 1.0, false ); //$NON-NLS-1$
	public final static NumericProperty DEFAULT_BUFFER_SIZE			= new NumericProperty(Messages.getString("NumericProperty.33"), 8, 1, 20, 4, 1, false); //$NON-NLS-1$
	public final static NumericProperty DEFAULT_ERROR_TOLERANCE     = new NumericProperty(Messages.getString("NumericProperty.34"), 1E-3, 1E-5, 1E-1, 1E-3, 1.0, false); //$NON-NLS-1$
	public final static NumericProperty DEFAULT_PYROMETER_SPOT		= new NumericProperty(Messages.getString("NumericProperty.37"), 10E-3, 1E-6, 1E-1, 10E-3, 1E3, false); //$NON-NLS-1$
	
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
	
}