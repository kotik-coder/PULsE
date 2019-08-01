package pulse;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pulse.input.ExperimentalData;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.Property;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.util.PropertyHolder;
import pulse.util.Saveable;

public class HeatingCurve extends PropertyHolder implements Saveable {
	
	/**
	 * 
	 */
	protected int count;
	protected List<Double> temperature, correctedTemperature;
	protected List<Double> time;
	protected double baseline;
	
	private final static String EXPORT_EXTENSION = ".html";
	
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
	
	public HeatingCurve() {
		this(NumericProperty.DEFAULT_COUNT);
	}
	
	public HeatingCurve(NumericProperty count) {
		this.count 	   = (int)count.getValue();
		temperature    = new ArrayList<Double>(this.count);
		correctedTemperature = new ArrayList<Double>(this.count);
		time		   = new ArrayList<Double>(this.count);
	}
	
	public void clear() {
		this.time.clear();
		this.temperature.clear();
	}

	public void flattenToBaseline() {
		
		for(int i = 0; i < getCount(); i++) {
			this.time.add(0.0);
			this.temperature.add(0.0);
			correctedTemperature.add(i, baseline);
		}
		
	}
	
	public int realCount() {
		return temperature.size();
	}
	
	public NumericProperty getNumPoints() {
		return new NumericProperty(count, NumericProperty.DEFAULT_COUNT);
	}
	
	public NumericProperty getBaseline() {
		return new NumericProperty(baseline, NumericProperty.DEFAULT_BASELINE);
	}
	
	public void setBaselineValue(double baseline) {
		this.baseline = baseline;
		baselineCorrection();
	}
	
	public void setBaseline(NumericProperty baseline) {
		this.baseline = (double) baseline.getValue();
		baselineCorrection();
	}
	
	public double getBaselineValue() {
		return baseline;
	}
	
	public void setNumPoints(NumericProperty c) {
		this.count 	   = (int)c.getValue();
		temperature    = new ArrayList<Double>(Collections.nCopies(this.count, 0.0));
		correctedTemperature    = new ArrayList<Double>(Collections.nCopies(this.count, 0.0));
		time		   = new ArrayList<Double>(Collections.nCopies(this.count, 0.0));
	}
	
	public double timeAt(int index) {		
		return time.get(index);
	}
	
	public double temperatureAt(int index) {
		return correctedTemperature.get(index);
	}
	
	public void setTimeAt(int index, double t) {
		time.set(index, t);
	}
	
	public void setTemperatureAt(int index, double t) {
		temperature.set(index, t);
		correctedTemperature.set(index, t + baseline);
	}
	
	public void scale(double scale) {
		double tmp = 0;
		for(int i = 0; i < count; i++) {
			tmp = temperature.get(i);
			temperature.set(i,  tmp*scale);
			correctedTemperature.set(i, tmp*scale + baseline);
		}
	}
	
	public double maxTemperature() {
		double max = -1;
		for(Double t : correctedTemperature) 
			if(t > max)
				max = t;
		return max;
	}
	
	public double timeLimit() {
		return time.get(time.size() - 1);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName() + " ("); //$NON-NLS-1$
		sb.append(getNumPoints());
		sb.append(" ; ");
		sb.append(getBaseline());
		sb.append(")");		 //$NON-NLS-1$
		return sb.toString();
	}			
	
	@Override
	public Map<String,String> propertyNames() {
		Map<String,String> map = new HashMap<String,String>(1);
		map.put("NumPoints", Messages.getString("HeatingCurve.3")); //$NON-NLS-1$ //$NON-NLS-2$
		map.put("Baseline", Messages.getString("HeatingCurve.4")); //$NON-NLS-1$ //$NON-NLS-2$
		return map;
	}	
	
	public double deviationSquares(ExperimentalData curve) {		
		double timeInterval_1 = this.timeAt(1) - this.timeAt(0); 
		
		int cur;
		double b, interpolated, diff;	
		
		double sum = 0;
		
		for (int i = curve.getFittingStartIndex(); i <= curve.getFittingEndIndex(); i++) {
			cur 		 = (int) (curve.timeAt(i)/timeInterval_1); //find index corresponding to this curve time interval
			b  			 = ( curve.timeAt(i) - this.timeAt(cur) ) / timeInterval_1;
			interpolated = (1 - b)*this.temperatureAt(cur) + b*this.temperatureAt(cur + 1);
			diff		 = curve.temperatureAt(i) - interpolated;
			sum			+= diff*diff; 
		}		
		
		return sum;
		
	}
	
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

	public int getCount() {
		return count;
	}
	
	public void baselineCorrection() {
		
		int size = time.size();
		
		for(int i = 0; i < size; i++) {
			if(time.get(i) < 0)
				continue;
			
			correctedTemperature.set(i, temperature.get(i) - baseline);
			
		}
		
	}
	
	@Override
	public void updateProperty(Property property) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {	
		super.updateProperty(property);
		if(!Problem.isSingleStatement())
			return;
		
		HeatingCurve hc;
		
		for(SearchTask task : TaskManager.getTaskList()) {
			hc = task.getProblem().getHeatingCurve();
			
			if( hc.equals( this ) )
				continue;

			hc.superUpdateProperty(property);
			
		}
		
	}
	
	private void superUpdateProperty(Property property) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		super.updateProperty(property);
	}

	@Override
	public void printData(FileOutputStream fos) {
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
                stream.printf("%.4f %n", t); //$NON-NLS-1$
                stream.print("</td><td>"); //$NON-NLS-1$
                T = temperature.get(i);
                stream.printf("%.4f %n", T); //$NON-NLS-1$
                stream.print("</td>"); //$NON-NLS-1$
            
            stream.println("</tr>"); //$NON-NLS-1$
        }
        
        stream.print("</table>"); //$NON-NLS-1$
        stream.close();
        
	}
	
}
