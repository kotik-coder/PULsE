package pulse.properties;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class NumericProperty implements Property {

	private Number value;
	private Number minimum, maximum;
	private String descriptor;
	private String abbreviation;
	private Number dimensionFactor;
	private Number error;
	private NumericPropertyKeyword type;
	private boolean autoAdjustable = true;
	
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
			Number maximum, Number defaultValue, Number dimensionFactor, boolean autoAdjustable) {		
		this.type = type;
		this.descriptor = descriptor;
		this.abbreviation = abbreviation;
		this.value = value;
		this.dimensionFactor = dimensionFactor; 
		this.autoAdjustable = autoAdjustable;
		setBounds(minimum, maximum);
	}
	
	public NumericProperty(NumericPropertyKeyword type, String descriptor, String abbreviation, Number value, Number minimum, Number maximum, boolean autoAdjustable) {
		this(type, descriptor, abbreviation, value, minimum, maximum, 0.0, 1.0, autoAdjustable);
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
	
	public final static NumericProperty DIFFUSIVITY 		= new NumericProperty(NumericPropertyKeyword.DIFFUSIVITY, Messages.getString("Diffusivity.Descriptor"), Messages.getString("Diffusivity.Abbreviation"), 1E-6, 1E-10, 1E-3, 1E-6, 1E6, true); //$NON-NLS-1$
	public final static NumericProperty THICKNESS 			= new NumericProperty(NumericPropertyKeyword.THICKNESS, Messages.getString("Thickness.Descriptor"), Messages.getString("Thickness.Abbreviation"), 1E-3, 1E-6, 1E-1, 1E-3, 1E3, false); //$NON-NLS-1$
	public final static NumericProperty DIAMETER 			= new NumericProperty(NumericPropertyKeyword.DIAMETER, Messages.getString("Diameter.Descriptor"), Messages.getString("Diameter.Abbreviation"), 10E-3, 1E-6, 1E-1, 10E-3, 1E3, false); //$NON-NLS-1$
	public final static NumericProperty MAXTEMP				= new NumericProperty(NumericPropertyKeyword.MAXTEMP, Messages.getString("MaxTemp.Descriptor"), Messages.getString("MaxTemp.Abbreviation"), 1.0, 0.01, 100.0, 1.0, 1.0, true); //$NON-NLS-1$
	public final static NumericProperty NONLINEAR_PRECISION	= new NumericProperty(NumericPropertyKeyword.NONLINEAR_PRECISION, Messages.getString("NonlinearPrecision.Descriptor"), Messages.getString("NonlinearPrecision.Descriptor"), 1E-3, 1E-6, 1E-2, 1E-3, 1.0, false); //$NON-NLS-1$
	public final static NumericProperty COUNT				= new NumericProperty(NumericPropertyKeyword.NUMPOINTS, Messages.getString("NumPoints.Descriptor"), Messages.getString("NumPoints.Abbreviation"), 100, 10, 1000, 100, 1, false); //$NON-NLS-1$
	public final static NumericProperty ITERATION			= new NumericProperty(NumericPropertyKeyword.ITERATION, Messages.getString("Iteration.Descriptor"), Messages.getString("Iteration.Abbreviation"), 0, 0, 1000000, 0, 1, false); //$NON-NLS-1$
	public final static NumericProperty PULSE_WIDTH			= new NumericProperty(NumericPropertyKeyword.PULSE_WIDTH, Messages.getString("PulseWidth.Descriptor"), Messages.getString("PulseWidth.Abbreviation"), 1.5E-3, 1E-4, 1.0, 1.5E-3, 1E3, false); //$NON-NLS-1$
	public final static NumericProperty SPOT_DIAMETER		= new NumericProperty(NumericPropertyKeyword.SPOT_DIAMETER, Messages.getString("SpotDiameter.Descriptor"), Messages.getString("SpotDiameter.Abbreviation"), 10.0E-3, 0.1E-3, 50E-3, 10.0E-3, 1E3, false); //$NON-NLS-1$
	public final static NumericProperty TIME_LIMIT			= new NumericProperty(NumericPropertyKeyword.TIME_LIMIT, Messages.getString("TimeLimit.Descriptor"), Messages.getString("TimeLimit.Abbreviation"), 1.0, 1E-6, 20.0, 1.0, 1.0, true); //$NON-NLS-1$
	public final static NumericProperty GRID_DENSITY		= new NumericProperty(NumericPropertyKeyword.GRID_DENSITY, Messages.getString("GridDensity.Descriptor"), Messages.getString("GridDensity.Abbreviation"), 30, 10, 200, 30, 1.0, true); //$NON-NLS-1$
	public final static NumericProperty SPECIFIC_HEAT		= new NumericProperty(NumericPropertyKeyword.SPECIFIC_HEAT, Messages.getString("SpecificHeat.Descriptor"), Messages.getString("SpecificHeat.Abbreviation"), 0.0, 1.0, 10000.0, true); //$NON-NLS-1$
	public final static NumericProperty CONDUCTIVITY		= new NumericProperty(NumericPropertyKeyword.CONDUCTIVITY, Messages.getString("ThermalConductivity.Descriptor"), Messages.getString("ThermalConductivity.Abbreviation"), 0.0, 1.0, 10000.0, true); //$NON-NLS-1$
	public final static NumericProperty EMISSIVITY			= new NumericProperty(NumericPropertyKeyword.EMISSIVITY, Messages.getString("Emissivity.Descriptor"), Messages.getString("Emissivity.Abbreviation"), 0.0, 1.0, 2.0, true); //$NON-NLS-1$
	public final static NumericProperty DENSITY				= new NumericProperty(NumericPropertyKeyword.DENSITY, Messages.getString("Density.Descriptor"), Messages.getString("Density.Abbreviation"), 0.0, 10.0, 30000.0, true); //$NON-NLS-1$
	public final static NumericProperty ABSORBED_ENERGY		= new NumericProperty(NumericPropertyKeyword.ABSORBED_ENERGY, Messages.getString("AbsorbedEnergy.Descriptor"), Messages.getString("AbsorbedEnergy.Abbreviation"), 7.0, 0.1, 200.0, 7.0, 1.0, true); //$NON-NLS-1$
	public final static NumericProperty TEST_TEMPERATURE	= new NumericProperty(NumericPropertyKeyword.TEST_TEMPERATURE, Messages.getString("TestTemperature.Descriptor"), Messages.getString("TestTemperature.Abbreviation"), 298.0, 0.0, 10000.0, 298.0, 1.0, true); //$NON-NLS-1$
	public final static NumericProperty LINEAR_RESOLUTION 	= new NumericProperty(NumericPropertyKeyword.LINEAR_RESOLUTION, Messages.getString("LinearResolution.Descriptor"), Messages.getString("LinearResolution.Abbreviation"), 1E-7, 1E-8, 1E-1, 1E-7, 1.0, false); //$NON-NLS-1$
	public final static NumericProperty GRADIENT_RESOLUTION = new NumericProperty(NumericPropertyKeyword.GRADIENT_RESOLUTION, Messages.getString("GradientResolution.Descriptor"),Messages.getString("GradientResolution.Abbreviation"), 1E-4, 1E-7, 1E-1, 1E-4, 1.0, false ); //$NON-NLS-1$
	public final static NumericProperty BUFFER_SIZE			= new NumericProperty(NumericPropertyKeyword.BUFFER_SIZE, Messages.getString("BufferSize.Descriptor"), Messages.getString("BufferSize.Abbreviation"), 8, 1, 20, 4, 1, false); //$NON-NLS-1$
	public final static NumericProperty ERROR_TOLERANCE     = new NumericProperty(NumericPropertyKeyword.ERROR_TOLERANCE, Messages.getString("ErrorTolerance.Descriptor"), Messages.getString("ErrorTolerance.Abbreviation"), 1E-3, 1E-5, 1E-1, 1E-3, 1.0, false); //$NON-NLS-1$
	public final static NumericProperty PYROMETER_SPOT		= new NumericProperty(NumericPropertyKeyword.PYROMETER_SPOT, Messages.getString("PyrometerSpot.Descriptor"), Messages.getString("PyrometerSpot.Abbreviation"), 10E-3, 1E-6, 1E-1, 10E-3, 1E3, false); //$NON-NLS-1$
	public final static NumericProperty BASELINE_SLOPE 		= new NumericProperty(NumericPropertyKeyword.BASELINE_SLOPE, Messages.getString("BaselineSlope.Descriptor"), Messages.getString("BaselineSlope.Abbreviation"), 0.0, -10.0, 10.0, 0.0, 1.0, true);
	public final static NumericProperty BASELINE_INTERCEPT	= new NumericProperty(NumericPropertyKeyword.BASELINE_INTERCEPT, Messages.getString("BaselineIntercept.Descriptor"), Messages.getString("BaselineIntercept.Abbreviation"), 0.0, -10.0, 10.0, 0.0, 1.0, true); //$NON-NLS-1$
	
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
	public String getDescriptor() {
		return descriptor;
	}

	public void setDescriptor(String descriptor) {
		this.descriptor = descriptor;
	}

	public String getAbbreviation() {
		return abbreviation;
	}

	public void setAbbreviation(String abbreviation) {
		this.abbreviation = abbreviation;
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
	
}