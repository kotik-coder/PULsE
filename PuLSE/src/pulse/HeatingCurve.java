package pulse;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import pulse.input.ExperimentalData;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.tasks.TaskManager;
import pulse.ui.Messages;
import pulse.util.PropertyHolder;
import pulse.util.Saveable;

/**
 * The {@code HeatingCurve} represents a time-temperature profile either resulting
 * from a finite-difference calculation or measured directly in the experiment 
 * (and then it is called {@code ExperimentalData}). <p>
 * The notion of temperature is loosely used here, and this can represent just 
 * the pyrometer signal in mV. Unless a nonlinear problem statement is used,
 * the unit of the temperature can be arbitrary, and only the shape of the heating curve
 * matters when calculating the reverse solution of the heat problem.</p>
 *
 */

public class HeatingCurve extends PropertyHolder implements Saveable {
	
	protected int count;
	protected List<Double> temperature, baselineAdjustedTemperature;
	protected List<Double> time;
	protected Baseline baseline;	
	private String name;
	
	private final static int DEFAULT_CLASSIC_PRECISION = 200;
	
	/**
	 * Checks if the {@code temperature} list is empty. 	
	 * @return true if empty, false otherwise
	 */
	
	public boolean isEmpty() {
		return temperature.isEmpty();
	}

	@Override
	public boolean equals(Object o) {
		if(! (o instanceof HeatingCurve))
			return false;
	
		HeatingCurve other = (HeatingCurve)o;
		
		final double EPS = 1e-8;
		
		if( Math.abs(count - (Integer)other.getNumPoints().getValue()) > EPS )
			return false;
		
		if(temperature.hashCode() != other.temperature.hashCode())
			return false;
		
		if(time.hashCode() != other.time.hashCode())
			return false;
		
		return true;
		
	}
	
	/**
	 * Creates a {@code HeatingCurve} with the default number of points (set in the corresponding XML file).
	 */
	
	public HeatingCurve() {
		this(NumericProperty.def(NumericPropertyKeyword.NUMPOINTS));
	}
	
	/**
	 * Creates a {@code HeatingCurve}, where the number of elements in the 
	 * {@code time} and {@code temperature} collections are set to {@code count.getValue()}, and then calls {@code reinit()}.
	 * <p> Creates a new default {@code Baseline} and sets its parent to {@code this}.   
	 * @param count The {@code NumericProperty} that is derived from the {@code NumericPropertyKeyword.NUMPOINTS}.
	 * @see reinit
	 */
	
	public HeatingCurve(NumericProperty count) {
		this.count 	   = (int)count.getValue();
		temperature    = new ArrayList<Double>(this.count);
		baselineAdjustedTemperature = new ArrayList<Double>(this.count);
		time		   = new ArrayList<Double>(this.count);
		baseline	   = new Baseline();
		baseline.setParent(this);
		reinit();
	}
	
	/**
	 * Clears all elements from the three {@code List} objects, thus releasing memory.
	 */
	
	public void clear() {
		this.time.clear();
		this.temperature.clear();
		this.baselineAdjustedTemperature.clear();
	}
	
	/**
	 * Calls {@code clear()}, and then iterates over the three {@code List} objects
	 * containing data, only to fill them with zeroes, stopping at {@code getCount() - 1}
	 * @see getNumPoints() 
	 * @see clear()
	 */
	
	public void reinit() {
		clear();											
		
		for(int i = 0; i < count; i++) {
			this.time.add(0.0);
			this.temperature.add(0.0);			
			this.baselineAdjustedTemperature.add(0.0);
		}
		
	}
	
	/**
	 * Returns the size of the {@code List} object containing baseline-adjusted temperature values,
	 * used later in calculations and optimisation procedures. 
	 * @return the size of the {@code baselineAdjustTemperature}
	 */
	
	public int arraySize() {
		return baselineAdjustedTemperature.size();
	}
	
	/**
	 * Getter method providing accessibility to the {@code count NumericProperty}.
	 * @return a {@code NumericProperty} derived from {@code NumericPropertyKeyword.NUMPOINTS} with the value of {@code count}
	 */
	
	public NumericProperty getNumPoints() {
		return NumericProperty.derive(NumericPropertyKeyword.NUMPOINTS, count);
	}
	
	/**
	 * Return the {@code Baseline} of this {@code HeatingCurve}.
	 * @return the baseline
	 */
	
	public Baseline getBaseline() {
		return baseline;
	}
	
	/**
	 * Sets a new baseline. Calls {@code apply(baseline)} when done and sets the {@code parent} of the baseline to this object.
	 * @param baseline the new baseline.
	 */
	
	public void setBaseline(Baseline baseline) {
		this.baseline = baseline;
		apply(baseline);
		baseline.setParent(this);
	}
	
	/**
	 * Sets the number of points for this baseline. <p>The {@code List} data objects, containing time, temperature, and baseline-subtracted
	 * temperature are filled with zeroes.   
	 * @param c
	 */
	
	public void setNumPoints(NumericProperty c) {
		this.count 	   = (int)c.getValue();
		temperature    = new ArrayList<Double>(Collections.nCopies(this.count, 0.0));
		baselineAdjustedTemperature    = new ArrayList<Double>(Collections.nCopies(this.count, 0.0));
		time		   = new ArrayList<Double>(Collections.nCopies(this.count, 0.0));
	}
	
	/**
	 * Retrieves an element from the {@code time List} specified by {@code index}
	 * @param index the index of the element to be returned
	 * @return a time value corresponding to {@code index}
	 */
	
	public double timeAt(int index) {		
		return time.get(index);
	}
	
	/**
	 * Retrieves the <b>baseline-subtracted</b> temperature corresponding to {@code index} in the respective {@code List}.
	 * @param index the index of the element
	 * @return a double, respresenting the baseline-subtracted temperature at {@code index}
	 */
	
	public double temperatureAt(int index) {
		return baselineAdjustedTemperature.get(index);
	}
	
	/**
	 * Attempts to set the time {@code t} and temperature {@code T} values corresponding to index {@code i}. <p>
	 * A baseline-subtracted version of the temperature at the same index will be calculated using the current baseline. 
	 * @param i the index to be used for setting both {@code t} and {@code T}
	 * @param t the time double value
	 * @param T the temperature double value
	 */
	
	public void set(int i, double t, double T) {
		time.set(i, t);
		temperature.set(i, T);		
		baselineAdjustedTemperature.set(i, T + baseline.valueAt(i));
	}
	
	/**
	 * Sets the time {@code t} at the position {@code index} of the {@code time List}.
	 * @param index the index
	 * @param t the new time value at this index
	 */
	
	public void setTimeAt(int index, double t) {
		time.set(index, t);
	}
	
	/**
	 * Sets the temperature {@code t} at the position {@code index} of the {@code temperature List}.
	 * @param index the index
	 * @param t the new temperature value at this index
	 */
	
	public void setTemperatureAt(int index, double t) {
		temperature.set(index, t);
	}
	
	/**
	 * Scales the temperature values by a factor of {@code scale}. <p> 
	 * This is done by manually setting each temperature value to {@code T*scale}, where T is the current temperature value at this index. 
	 * Finally. applies the baseline to the scaled temperature values.</p>
	 * This method is used in the DifferenceScheme classes when a dimensionless solution needs to be re-scaled to the given maximum temperature
	 * (usually matching the {@code ExperimentalData}, but also used as a search variable by the {@code SearchTask}.
	 * @param scale the scale
	 * @see pulse.problem.schemes.DifferenceScheme 
	 * @see pulse.problem.statements.Problem 
	 * @see pulse.tasks.SearchTask 
	 */
	
	public void scale(double scale) {
		double tmp = 0;
		for(int i = 0; i < count; i++) {
			tmp = temperature.get(i);
			temperature.set(i,  tmp*scale);
		}
		apply(baseline);
	}
	
	/**
	 * Retrieves the absolute maximum (in arbitrary untis) of the <b>baseline-subtracted</b> temperature list.
	 * @return the absolute maximum of the baseline-adjusted temperature.
	 */
	
	public double maxTemperature() {
		return Collections.max(baselineAdjustedTemperature);
	}
	
	/**
	 * Retrieves the last element of the {@code time List}. This is used e.g. by the {@code DifferenceScheme} to set 
	 * the calculation limit for the finite-difference scheme.
	 * @see pulse.problem.schemes.DifferenceScheme 
	 * @return a double, equal to the last element of the {@code time List}.
	 */
	
	public double timeLimit() {
		return time.get(time.size() - 1);
	}
	
	public String toString() {
		if(name != null)
			return name;
		
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName() + " (");
		sb.append(getNumPoints());
		sb.append(" ; ");
		sb.append(getBaseline());
		sb.append(")");
		return sb.toString();
	}			
	
	/**
	 * Lists the number of points for this heating curve. 
	 */
	
	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<Property>();
		list.add(getNumPoints());
		return list;
	}
	
	/**
	 * Calculates the sum of squared deviations using {@code curve} as reference.
	 * <p> This calculates <math><munderover><mo>&#x2211;</mo><mrow><mi>i</mi><mo>=</mo><msub><mi>i</mi><mn>1</mn></msub></mrow><msub><mi>i</mi><mn>2</mn></msub></munderover><mo>(</mo><mover><mi>T</mi><mo>&#x23DE;</mo></mover><mo>(</mo><msub><mi>t</mi><mi>i</mi></msub><mo>)</mo><mo>-</mo><mi>T</mi><mo>(</mo><msub><mi>t</mi><mi>i</mi></msub><msup><mo>)</mo><mrow><mi>r</mi><mi>e</mi><mi>f</mi></mrow></msup><msup><mo>)</mo><mn>2</mn></msup></math>,
	 * where <math><msubsup><mi>T</mi><mi>i</mi><mrow><mi>r</mi><mi>e</mi><mi>f</mi></mrow></msubsup></math> is the temperature value corresponding to the {@code time} at index {@code i} for the reference {@code curve}.
	 * Note that the time <math><msub><mi>t</mi><mi>i</mi></msub></math> corresponds to the <b>reference's</b> time list, which generally does not match to that of this heating curve.
	 * The <math><mover><mi>T</mi><mo>&#x23DE;</mo></mover><mo>(</mo><msub><mi>t</mi><mi>i</mi></msub><mo>)</mo></math> is the interpolated value for this heating curve at the reference time. 
     * The temperature value is interpolated using two nearest elements of the <b>baseline-subtracted</b> temperature list. The value is interpolated using the experimental time <math><i>t</i><sub>i</sub></math>
     * and the nearest solution points to that time. 
     * The accuracy of this interpolation depends on the number of points.  
	 * The boundaries of the summation are set by the {@code curve.getFittingStartIndex()} and {@code curve.getFittingEndIndex()} methods.        
	 * @param curve The reference heating curve
	 * @return a double, representing the result of the calculation.
	 */
	
	public double deviationSquares(ExperimentalData curve) {		
		double timeInterval_1 = this.timeAt(1) - this.timeAt(0); 
		
		int cur;
		double b1, b2, interpolated, diff;	
		
		double sum = 0;
		
		/*Linear interpolation for the **solution**: 
		 * y* = y1*(x2-x*)/dx + y2*(x*-x1)/dx, 
		 * where y1,x1,y2,x2 are the calculated heating curve points, and 
		 * y* is the interpolated value.  
		*/ 
		
		for (int i = curve.getFittingStartIndex(); i <= curve.getFittingEndIndex(); i++) {
			/*find the point on the calculated heating curve 
			which has the closest time value smaller than the experimental points' time value*/
			cur 		 = (int) (curve.timeAt(i)/timeInterval_1);  
			
			b1			 = ( this.timeAt(cur + 1) - curve.timeAt(i)  ) / timeInterval_1; //(x2 -x*)/dx
			b2  		 = ( curve.timeAt(i) 	  - this.timeAt(cur) ) / timeInterval_1; //(x* -x1)/dx
			interpolated = b1*this.temperatureAt(cur) + b2*this.temperatureAt(cur + 1);
			
			diff		 = curve.temperatureAt(i) - interpolated; //y_exp - y*
			sum			+= diff*diff; 
		}		
		
		return sum;
		
	}
	
	/**
	 * Calculates the coefficient of determination, or simply the <math><msup><mi>R</mi><mn>2</mn></msup></math>
	 * value. 
	 * <p> First, the mean temperature of the {@code data} is calculated. Then, the {@code TSS} (total sum of squares) is calculated
	 * as proportional to the variance of data. The residual sum of squares ({@code RSS}) is calculated by calling {@code this.deviationSquares(curve)}. 
	 * Finally, these values are combined together as: {@code 1 - RSS/TSS}.   
	 * </p>
	 * @param data the experimental data, acting as reference for this curve
	 * @return a double, representing the coefficient of determination, which characterises
	 * the goodness of fit that this {@code HeatingCurve} provides for the {@code data}
	 * @see <a href="https://en.wikipedia.org/wiki/Coefficient_of_determination">Wikipedia page</a>
	 */
	
	public double rSquared(ExperimentalData data) {
		
		double mean = 0;
		
		for(int i = 0; i < data.count; i++)
			mean += data.temperatureAt(i);
		
		mean /= data.count;
		
		double TSS = 0;
		
		for(int i = 0; i < data.count; i++) 
			TSS += Math.pow(data.temperatureAt(i) - mean, 2);
		
		return (1. - this.deviationSquares(data)/TSS);
		
	}
	
	/**
	 * Subtracts the baseline values from each element of the {@code temperature} list.
	 * <p> The baseline.valueAt(...) is explicitly invoked for all {@code time} values,
	 * and the result of subtracting the baseline value from the corresponding {@code temperature}
	 * is assigned to a position in the {@code baselineAdjustedTemperature} list.</p>   
	 * @param baseline the baseline. Note it may not specifically belong to this heating curve.
	 */

	public void apply(Baseline baseline) {
		
		int size = time.size();
		double t;
		
		for(int i = 0; i < size; i++) {
			t = time.get(i);			
			baselineAdjustedTemperature.set(i, temperature.get(i) + baseline.valueAt(t));			
		}
		
	}
	
	public void applyBaseline() {
		apply(baseline);
	}
	
	@Override
	public void printData(FileOutputStream fos, Extension extension) {
		switch(extension) {
			case HTML : printHTML(fos); break;
			case CSV : printCSV(fos); break;
		}		
	}
	
	private void printHTML(FileOutputStream fos) {
		PrintStream stream = new PrintStream(fos);
		
		stream.print("<table>"); //$NON-NLS-1$
		stream.print("<tr>"); //$NON-NLS-1$
	
		final String TIME_LABEL = Messages.getString("HeatingCurve.6"); //$NON-NLS-1$
		final String TEMPERATURE_LABEL = Messages.getString("HeatingCurve.7"); //$NON-NLS-1$
		
       	stream.print("<td>"); stream.print(TIME_LABEL + "\t"); stream.print("</td>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
       	stream.print("<td>");
       	stream.print(TEMPERATURE_LABEL + "\t"); 
       	stream.print("</td>"); 
       	
        stream.print("</tr>"); //$NON-NLS-1$

        stream.println(" "); //$NON-NLS-1$
        
        double t, T;

        int size = temperature.size();
        int finalSize = size < count ? size : count;
        
        for (int i = 0; i < finalSize; i++) {
        	stream.print("<tr>"); //$NON-NLS-1$
            
        		stream.print("<td>"); //$NON-NLS-1$
            	t = time.get(i);
                stream.printf("%.6f %n", t); //$NON-NLS-1$
                stream.print("</td><td>"); //$NON-NLS-1$
                T = temperature.get(i);
                stream.printf("%.6f %n", T); //$NON-NLS-1$
                stream.print("</td>"); //$NON-NLS-1$
            
            stream.println("</tr>"); //$NON-NLS-1$
        }
        
        stream.print("</table>"); //$NON-NLS-1$
        stream.close();
        
	}
	
	private void printCSV(FileOutputStream fos) {
		PrintStream stream = new PrintStream(fos);
		
		final String TIME_LABEL = Messages.getString("HeatingCurve.6"); //$NON-NLS-1$
		final String TEMPERATURE_LABEL = Messages.getString("HeatingCurve.7"); //$NON-NLS-1$
		
       	stream.print(TIME_LABEL + "\t");
       	stream.print(TEMPERATURE_LABEL + "\t"); 
       	stream.println();
       	
        double t, T;

        int size = temperature.size();
        int finalSize = size < count ? size : count;
        
        for (int i = 0; i < finalSize; i++) {
           	t = time.get(i);
            stream.printf("%.6f %n", t); //$NON-NLS-1$
            stream.print("</td><td>"); //$NON-NLS-1$
            T = temperature.get(i);
            stream.printf("%.6f %n", T); //$NON-NLS-1$
            stream.print("</td>"); //$NON-NLS-1$            
            stream.println();
        }
        
        stream.close();
        
	}	
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * This creates a new {@code HeatingCurve} to match the time boundaries of the {@code data}.
	 * <p>Curves derived in this way are called <i>extended</i> and are used primarily
	 * to visually inspect how the calculated baseline correlates with the {@code data}
	 * at times {@code t < 0}. This method is not used in any calculation and is introduced
	 * primarily because the search for the reverse solution of the heat problems only regards
	 * time value at <math><mi>t</mi><mo>&#x2265;</mo><mn>0</mn></math>, whereas in reality it may not be consistent with the experimental
	 * baseline value at {@code t < 0}.</p>
	 * @param data the experimental data, with a time range broader than the time range of this {@code HeatingCurve}.
	 * @return a new {@code HeatingCurve}, extended to match the time limits of {@code data}
	 */
		
	public final HeatingCurve extendedTo(ExperimentalData data) {
		
		int dataStartIndex = data.getFittingStartIndex();
		
		if(dataStartIndex < 1)
			return this;
		
		List<Double> extendedTime		 = data.time.stream().filter(t -> t < 0).collect(Collectors.toList());
		List<Double> extendedTemperature = new ArrayList<Double>(dataStartIndex + count);		
		
		for(double time : extendedTime) 
			extendedTemperature.add(baseline.valueAt(time));						
		
		extendedTime.addAll(time);
		extendedTemperature.addAll(baselineAdjustedTemperature);
		
		HeatingCurve newCurve = new HeatingCurve();
		
		newCurve.time		= extendedTime;
		newCurve.baselineAdjustedTemperature = extendedTemperature;						
		newCurve.count		= newCurve.baselineAdjustedTemperature.size();
		newCurve.baseline	= baseline;
		newCurve.name		= name;		
		
		return newCurve;
		
	}
	
	/**
	 * A static factory method for calculating a heating curve based on the analytical solution of Parker et al. 
	 * <p>The math itself is done separately in the {@code Problem} class. 
	 * This method creates a {@code HeatingCurve} with the number of points equal to that of the {@code p.getHeatingCurve()}, 
	 * and with the same baseline. The solution is calculated for the time range {@code 0 <= t <= timeLimit}.</p> 
	 * @param p The problem statement, providing access to the {@code classicSolutionAt} method and to the {@code HeatingCurve} object it owns.
	 * @param timeLimit The upper time limit (in seconds)
	 * @param precision The second argument passed to the {@code classicSolutionAt}
	 * @return a {@code HeatingCurve} representing the analytical solution.
	 * @see <a href="https://doi.org/10.1063/1.1728417">Parker <i>et al.</i> Journal of Applied Physics <b>32</b> (1961) 1679</a>
	 * @see Problem.classicSolutionAt(double,int) 
	 */
	
	public static HeatingCurve classicSolution(Problem p, double timeLimit, int precision) {
		 HeatingCurve curve = p.getHeatingCurve();
		
		 HeatingCurve classicCurve = new 
				 HeatingCurve(NumericProperty.derive
						 (NumericPropertyKeyword.NUMPOINTS, curve.count));
		 classicCurve.setBaseline(curve.getBaseline());
		 
		 double time;
		 double step = TaskManager.getSelectedTask().getProblem().getHeatingCurve().timeAt(1) - TaskManager.getSelectedTask().getProblem().getHeatingCurve().timeAt(0);
		 
	     for(int i = 0; i < curve.count; i++) {
	    	 	time = i*step;
	    	 	classicCurve.set(i, time, p.classicSolutionAt(time, precision));
	     }
	     
	     classicCurve.setName("Classic solution");
	     
	     return classicCurve;
	     
	}
	
	/**
	 * Calculates the classic solution, using the default value of the {@code precision} 
	 * and the time limit specified by the {@code HeatingCurve} of {@code p}.
	 * @param p the problem statement
	 * @return a {@code HeatinCurve}, representing the classic solution.
	 * @see classicSolution
	 */
	
	public static HeatingCurve classicSolution(Problem p) {
		 return classicSolution(p, p.getHeatingCurve().timeLimit(), DEFAULT_CLASSIC_PRECISION);
	}
	
	/**
	 * Provides general setter accessibility for the number of points of this {@code HeatingCurve}.
	 * @param type must be equal to {@code NumericPropertyKeyword.NUMPOINTS} 
	 * @param property the property of the type {@code NumericPropertyKeyword.NUMPOINTS}
	 */

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch(type) {
			case NUMPOINTS : setNumPoints(property); break;
		}
	}
	
	/**
	 * The supported extensions for exporting the data contained in this object. Currently include {@code .html} and {@code .csv}.
	 */
	
	@Override
	public Extension[] getSupportedExtensions() {
		return new Extension[] {Saveable.Extension.HTML, Saveable.Extension.CSV};
	}
	
	/**
	 * Removes an element with the index {@code i} from all three {@code List}s
	 * (time, temperature, and baseline-subtracted temperature).
	 * @param i the element to be removed
	 */
	
	public void remove(int i) {
		this.time.remove(i);
		this.temperature.remove(i);
		this.baselineAdjustedTemperature.remove(i);
	}
	
}
