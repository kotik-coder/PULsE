package pulse;

import java.util.ArrayList;
import java.util.List;

import pulse.input.ExperimentalData;
import pulse.properties.EnumProperty;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.tasks.SearchTask;
import pulse.util.PropertyHolder;
import pulse.util.UpwardsNavigable;
/**
 * A {@code Baseline} is determined by its {@code baselineType} and two parameters: {@code intercept} and {@code slope}.
 * <p>The mathematical equivalent is the following expression: {@code g(x) = intercept + slope * x}.
 * {@code Baseline}s are always used in conjunction with respective {@code HeatingCurve} objects. The 
 * {@code NumericPropertyKeyword} associated with the {@code intercept} and {@code slope} parameters 
 * can be used as fitting variables for {@code SearchTask}, if included in the appropriate 
 * {@code IndexedVector}, the latter representing an objective function of the current search.</p>
 * @see HeatingCurve, SearchTask, IndexedVector
 */
public class Baseline extends PropertyHolder {

	private double slope, intercept;
	private BaselineType baselineType;
	
	private final static double ZERO_LEFT = -1E-5;
	
	/**
	 * A primitive constructor, which assigns the object's BaselineType to CONSTANT
	 */
	
	public Baseline() {
		this.baselineType = BaselineType.CONSTANT;
	}
	
	/**
	 * A constructor, which allows to specify all three parameters in one go.
	 * @param baselineType The functional type of the baseline.
	 * @param intercept the intercept is the value of the Baseline's linear function at {@code x = 0}
	 * @param slope the slope determines the inclination angle of the Baseline's graph. 
	 */
	
	public Baseline(BaselineType baselineType, double intercept, double slope) {
		this.intercept = intercept;
		this.slope = slope;
		this.baselineType = baselineType;
	}
	
	/**
	 * Calculates the linear function {@code g(x) = intercept + slope*time}
	 * @param time the argument of the linear function
	 * @return the result of this simple calculation
	 */

	public double valueAt(double time) {
		return intercept + time*slope;
	}
	
	/**
	 * Provides getter accessibility to the slope as a NumericProperty
	 * @return a NumericProperty derived from NumericPropertyKeyword.BASELINE_SLOPE with a value equal to slop
	 */

	public NumericProperty getSlope() {
		return NumericProperty.derive(NumericPropertyKeyword.BASELINE_SLOPE, slope);
	}
		
	/**
	 * Provides getter accessibility to the intercept as a NumericProperty
	 * @return a NumericProperty derived from NumericPropertyKeyword.BASELINE_INTERCEPT with a value equal to slop
	 */

	public NumericProperty getIntercept() {
		return NumericProperty.derive(NumericPropertyKeyword.BASELINE_INTERCEPT, intercept); 
	}
	
	/**
	 * Lists the intercept, slope, and baselineType as accessible properties for this PropertyHolder.
	 * @see PropertyHolder
	 */
	
	@Override
	public List<Property> listedParameters() {
		List<Property> list = new ArrayList<Property>();
		list.add(getSlope()); 
		list.add(getIntercept()); 
		list.add(BaselineType.CONSTANT);
		return list;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " = " + String.format("%3.2f", intercept) + " + t * " + String.format("%3.2f", slope);
	}
	
	/**
	 * Calculates the {@code intercept} and/or {@code slope} parameters based on {@code data}. 
	 * <p>This will run a simple least-squares estimation of the parameters of this baseline
	 * using the specified {@code data} object. The estimation will only use the {@code T(t)} function values 
	 * corresponding to {@code rangeMin < t < rangeMax} of the loaded {@code data}.
	 * If the {@code getFittingStartIndex()} method of the {@code data} object returns a zero value,
	 * this method will do nothing. If no data at {@code rangeMin < t < rangeMax} is available, the method
	 * will not change the {@code intercept} and {@code slope} values.
	 * If the {@code BaselineType} is {@code CONSTANT}, only the {@code intercept} value will be 
	 * used as a fitting variable.
	 * Upon completing, the method will use the respective {@code set} methods of this class
	 * to update the {@code intercept} and {@code slope} values, triggering whatever events are associated with them.</p> 
	 * @param data the experimental curve, containing sufficient information for the fitting procedure.
	 * @param rangeMin the lower bound for the fitting range 
	 * @param rangeMax the upper bound for the fitting range
	 */

	public void fitTo(ExperimentalData data, double rangeMin, double rangeMax) {
		if(data.getFittingStartIndex() < 1)
			return;
		
		List<Double> x = new ArrayList<Double>();
		List<Double> y = new ArrayList<Double>();
		
		double t;
		
		int size = 0;
		
		for(int i = 0; i < data.getFittingStartIndex(); i++) {
			t = data.time.get(i);
			
			if(t < rangeMin)
				continue;
			
			if(t > rangeMax)
				break;	
			
			x.add(data.time.get(i));
			y.add(data.temperature.get(i));
			size++;
		}		
		
        // first pass: compute xbar and ybar
        double meanx = 0.0, meany = 0.0;
        double x1, y1;
        for(int i = 0; i < size; i++) {
        	x1 = x.get(i); y1 = y.get(i);
            meanx += x1; meany += y1;
        }
        meanx /= size; meany /= size;

        // second pass: compute summary statistics
        double xxbar = 0.0, xybar = 0.0;
        for (int i = 0; i < size; i++) {
        	x1 = x.get(i); y1 = y.get(i);
            xxbar += (x1 - meanx) * (x1 - meanx);
            xybar += (x1 - meanx) * (y1 - meany);
        }
        
        if(baselineType == BaselineType.LINEAR) {
        	slope		= xybar / xxbar;
        	intercept	= meany - slope * meanx;
		} else {
			slope		= 0;
        	intercept	= meany;
		}	
        
        x.clear(); x = null;
        y.clear(); y = null;
        
        set(NumericPropertyKeyword.BASELINE_INTERCEPT, 
        		NumericProperty.derive(NumericPropertyKeyword.BASELINE_INTERCEPT, intercept));
        set(NumericPropertyKeyword.BASELINE_SLOPE, 
        		NumericProperty.derive(NumericPropertyKeyword.BASELINE_SLOPE, slope));
        
	}
	
	/** Creates and returns an array representing the parameters of this {@code Baseline}. 
	 * <p>Creates a double array with a size 2 with the following elements and in the following order: {@code intercept} and {@code slope}.
	 * This method is used when the baseline is flagged as belonging to the objective function of the corresponding {@code Problem}.</p>
	 * @return an array of size two with the numeric parameters of the baseline
	 * @see Problem
	 */
	
	public double[] parameters() {
		return new double[] {intercept, slope};
	}
	
	/**
	 * <p>A method called by the {@code Problem} to assign {@code intercept} and/or {@code slope} parameters, 
	 * which have been calculated when running the {@code SearchTask}.</p>
	 * @param parameters an array of size 2, the elements of which are the intercept and the slope.
	 * @see Problem, SearchTask
	 */
	
	public void setParameters(double[] parameters) {
		intercept = parameters[0];
		slope = parameters[1];
	}
	
	/**
	 * A method called by the {@code Problem} to assign the {@code intercept} and/or {@code slope} parameters, 
	 * which have been calculated when running the associated {@code SearchTask}.
	 * @param index 0 for the {@code intercept} and 1 for the {@code slope}. 
	 * @param parameter the value of either intercept or slope to be assigned for this baseline
	 * @see Problem, SearchTask
	 */

	public void setParameter(int index, double parameter) {
		if(index == 0)
			intercept = parameter;
		else if(index == 1)
			slope = parameter;
		
	}
	
	/**
	 * Calls {@code fitTo} using the default time range for the data: {@code -Infinity < t < 0}
	 * @param data the experimental data stretching to negative time values
	 * @see fitTo
	 */
	
	public void fitTo(ExperimentalData data) {
		fitTo(data, Double.NEGATIVE_INFINITY, ZERO_LEFT);
	}
	
	/**
	 * Represents the current baseline type.
	 * @return the baseline type
	 */

	public BaselineType getBaselineType() {
		return baselineType;
	}
	
	/**
	 * Sets the baseline type. <p>
	 * Searches the {@code UpwardsNavigable} queue for an ancestor assignable from the {@code SearchTask} class
	 * (or simply: searches for the instance of {@code SearchTask} owning this baseline).  
	 * If the latter is found, it will call the {@code fitTo(...)} method, the argument of which is the
	 * {@code ExperimentalData} accessed by a getter method from the {@code SearchTask}.</p>
	 * @param baselineType the new type of the baseline.
	 */

	public void setBaselineType(BaselineType baselineType) {
		this.baselineType = baselineType;
		
		UpwardsNavigable ancestorTask = super.specificAncestor(SearchTask.class);
		if(ancestorTask == null)
			return;
		
		this.fitTo( ((SearchTask)ancestorTask).getExperimentalCurve() );		
	} 
	
	/**
	 * An {@code enum} (and an {@code EnumProperty}) representing the type of this linear baseline
	 *
	 */

	public enum BaselineType implements EnumProperty {
		LINEAR, CONSTANT;
		
		@Override
		public Object getValue() {
			return this;
		}

		@Override
		public String getDescriptor(boolean addHtmlTags) {
			return "Baseline type";
		}

		@Override
		public EnumProperty evaluate(String string) {
			return valueOf(string);
		}

	}
	
	/**
	 * Provides setter accessibility to the {@code slope} and {@code intercept}, treating them as instances of {@code NumericProperty}. <p>
	 * Note this method does not have a built-in mechanism for checking whether {@code NumericPropertyKeyword} corresponds to 
	 * a suitable value. However, the method only works
	 * if the {@code type} is set to either either {@code BASELINE_INTERCEPT} or {@code BASELINE_SLOPE}.</p>
	 * @param type the keyword associated with the property
	 * @param property the NumericProperty object with the NumericPropertyKeyword set to either BASELINE_INTERCEPT or BASELINE_SLOPE
	 */

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch(type) {
		case BASELINE_INTERCEPT : this.intercept = ((Number)property.getValue()).doubleValue(); break;
		case BASELINE_SLOPE : this.slope = ((Number)property.getValue()).doubleValue(); break;
		default:
			throw new IllegalArgumentException("Illegal type passed to method: " + property);
		}
		
	}
		
}