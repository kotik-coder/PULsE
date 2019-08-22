package pulse;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import pulse.input.ExperimentalData;
import pulse.Messages;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;
import pulse.util.Saveable;

public class HeatingCurve extends PropertyHolder implements Saveable {
	
	protected int count;
	protected List<Double> temperature, baselineAdjustedTemperature;
	protected List<Double> time;
	protected Baseline baseline;	
	private String name;
	
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
		this(NumericProperty.COUNT);
		reinit();
	}
	
	public HeatingCurve(NumericProperty count) {
		this.count 	   = (int)count.getValue();
		temperature    = new ArrayList<Double>(this.count);
		baselineAdjustedTemperature = new ArrayList<Double>(this.count);
		time		   = new ArrayList<Double>(this.count);
		baseline	   = new Baseline();
		baseline.setParent(this);
		reinit();
	}
	
	public void clear() {
		this.time.clear();
		this.temperature.clear();
		this.baselineAdjustedTemperature.clear();
	}
	
	public void reinit() {
		clear();											
		
		for(int i = 0; i < getCount(); i++) {
			this.time.add(0.0);
			this.temperature.add(0.0);			
			this.baselineAdjustedTemperature.add(0.0);
		}
		
	}
	
	public int arraySize() {
		return baselineAdjustedTemperature.size();
	}
	
	public NumericProperty getNumPoints() {
		return new NumericProperty(count, NumericProperty.COUNT);
	}
	
	public Baseline getBaseline() {
		return baseline;
	}
	
	public void setBaseline(Baseline baseline) {
		this.baseline = baseline;
		apply(baseline);
		baseline.setParent(this);
	}
	
	public void setNumPoints(NumericProperty c) {
		this.count 	   = (int)c.getValue();
		temperature    = new ArrayList<Double>(Collections.nCopies(this.count, 0.0));
		baselineAdjustedTemperature    = new ArrayList<Double>(Collections.nCopies(this.count, 0.0));
		time		   = new ArrayList<Double>(Collections.nCopies(this.count, 0.0));
	}
	
	public double timeAt(int index) {		
		return time.get(index);
	}
	
	public double temperatureAt(int index) {
		return baselineAdjustedTemperature.get(index);
	}
	
	public void set(int i, double t, double T) {
		time.set(i, t);
		temperature.set(i, T);		
		baselineAdjustedTemperature.set(i, T + baseline.valueAt(i));
	}
	
	public void setTimeAt(int index, double t) {
		time.set(index, t);
	}
	
	public void setTemperatureAt(int index, double t) {
		temperature.set(index, t);
	}
	
	public void scale(double scale) {
		double tmp = 0;
		for(int i = 0; i < count; i++) {
			tmp = temperature.get(i);
			temperature.set(i,  tmp*scale);
		}
		apply(baseline);
	}
	
	public double maxTemperature() {
		return Collections.max(baselineAdjustedTemperature);
	}
	
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
	
	@Override
	public List<Property> listedParameters() {
		List<Property> list = new ArrayList<Property>();
		list.add(getNumPoints());
		return list;
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
	
	public void apply(Baseline baseline) {
		
		int size = time.size();
		double t;
		
		for(int i = 0; i < size; i++) {
			t = time.get(i);			
			baselineAdjustedTemperature.set(i, temperature.get(i) + baseline.valueAt(t));			
		}
		
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
		
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
	
	public static HeatingCurve classicSolution(Problem p, double timeLimit) {
		 HeatingCurve curve = p.getHeatingCurve();
		
		 final int N		= 30;
		 HeatingCurve classicCurve = new HeatingCurve(new NumericProperty(curve.count, NumericProperty.COUNT));
		 classicCurve.setBaseline(curve.getBaseline());
		 
		 double time;
		 double step = timeLimit/(curve.count-1);
		 
	     for(int i = 0; i < curve.count; i++) {
	    	 	time = i*step;
	    	 	classicCurve.set(i, time, p.classicSolutionAt(time, N));
	     }
	     
	     classicCurve.setName("Classic solution");
	     return classicCurve;
	     
	}
	
	public static HeatingCurve classicSolution(Problem p) {
		 return classicSolution(p, p.getHeatingCurve().timeLimit());
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch(type) {
			case NUMPOINTS : setNumPoints(property); break;
		}
	}
	
}
