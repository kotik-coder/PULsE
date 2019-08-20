package pulse.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import pulse.HeatingCurve;
import pulse.util.SaveableDirectory;
import pulse.util.geom.Point2D;

public class ExperimentalData extends HeatingCurve implements SaveableDirectory {
	
	private Metadata metadata;
	
	private	int fittingStartIndex = -1;
	private int fittingEndIndex = -1;
	
	private final static double CUTOFF_FACTOR = 6;
	private final static int REDUCTION_FACTOR = 16;
	private final static double POSITIVE_ZERO = 1E-7;
	
	private Comparator<Point2D> pointComparator = 
			(p1, p2) -> Double.valueOf(p1.getY()).compareTo(Double.valueOf(p2.getY()));
	
	public ExperimentalData() {
		super();
		this.clear();
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Experimental data "); //$NON-NLS-1$
		if(metadata.getSampleName() != null)
			sb.append("for " + metadata.getSampleName() + " "); //$NON-NLS-1$
		sb.append("(" + metadata.getTestTemperature().formattedValue(false) + ")");
		return sb.toString();
	}
	
	public void add(double time, double temperature) {
		this.time.add(time);
		this.temperature.add(temperature);
		this.baselineAdjustedTemperature.add(temperature);
	}
	
	public void remove(int i) {
		this.time.remove(i);
		this.temperature.remove(i);
		this.baselineAdjustedTemperature.remove(i);
	}
	
	public void setFittingRange(double a, double b) {
		
		if(a > b) 
			return;
		
		for(fittingStartIndex = 0; fittingStartIndex < count - 1; fittingStartIndex++) {
			if(timeAt(fittingStartIndex) >= a) 
				break;
		}
		
		for(fittingEndIndex = count-2; fittingEndIndex > fittingStartIndex; fittingEndIndex--) {
			if(timeAt(fittingEndIndex) < b) 
				break;
		}
		
		fittingEndIndex++;
		
	}
	
	public double startTime() {
		return time.get(fittingStartIndex);
	}
	
	public double endTime() {
		return time.get(fittingEndIndex);
	}	
	
	/*
	 * Crude heating curve
	 */
	
	public List<Point2D> crudeAverage(int reductionFactor) {
		
		List<Point2D> crudeAverage = new ArrayList<Point2D>(count/reductionFactor);	
		
		int start = getFittingStartIndex();
		int end = getFittingEndIndex();
		
		int step = (end - start)/(count/reductionFactor);
		double tmp = 0;
		
		int i1, i2;
				
		for(int i = 0; i < (count/reductionFactor)-1; i++) {										
			i1 = start+step*i;
			i2 = i1 + step;			
			tmp = 0;		
			
			for(int j = i1; j < i2; j++) 
				tmp += temperature.get(j);			
						
			tmp *= 1.0/step;
			
			crudeAverage.add(new Point2D( 
					time.get((i1+i2)/2), 
					tmp));	
						
		}
								
		return crudeAverage;
		
	}
	
	@Override
	public double maxTemperature() {
		return crudeMaximum();
	}

	
	public double crudeMaximum() {				
		List<Point2D> degraded = crudeAverage(REDUCTION_FACTOR);
		return (Collections.max(degraded, pointComparator)).getY();
	}
	
	/*
	 * returns the most probable time corresponding to the temperature half-maximum
	 */
	
	public double halfRiseTime() {
		List<Point2D> degraded = crudeAverage(REDUCTION_FACTOR);		
		double max = (Collections.max(degraded, pointComparator)).getY();				
		baseline.fitTo(this);
		
		double halfMax = (max + baseline.valueAt(0))/2.0;
		
		int size = degraded.size();
		int index = -1;
		
		for(int i = 0; i < size-1; i++) 
			if(halfMax > degraded.get(i).getY())
				if(halfMax <= degraded.get(i+1).getY())
					index = i;
		
		return degraded.get(index).getX();				
		
	}
	
	public Metadata getMetadata() {
		return metadata;
	}
	
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;					
	}
	
	public void updateFittingRange() {
		double pulseWidth = (double) metadata.getPulseWidth().getValue();
		int t = closestIndexOf(pulseWidth, time);		
		fittingStartIndex = t > fittingStartIndex ? t : fittingStartIndex;
	}
	
	@Override
	public boolean equals(Object o) {
		if(! (o instanceof ExperimentalData))
			return false;
		
		ExperimentalData other = (ExperimentalData)o;

		if(! this.metadata.equals( other.getMetadata() ))
			return false;
		
		return super.equals(o);
		
	}
	
	/*
	 * Returns the fitting start index, which can only point to a positive moment in time
	 */

	public int getFittingStartIndex() {
		return fittingStartIndex > 0 ? fittingStartIndex : closestIndexOf(POSITIVE_ZERO, time);
	}

	public int getFittingEndIndex() {
		return fittingEndIndex > 0 ? fittingEndIndex : (count - 1);
	}		
	
	public void updateCount() {
		this.count = this.time.size();
	}
	
	public void truncate() {			
		double halfMaximum = halfRiseTime();
		double cutoff = CUTOFF_FACTOR*halfMaximum;
		
		for(int i = count-1; time.get(i) > cutoff ; i--) 
			remove(i);		
		
		updateCount();		
	}
	
	/*
	 * return the index of the list element which is closest ot t
	 */
	
	private static int closestIndexOf(double t, List<Double> list) {
		int size = list.size();
		for(int i = 0; i < size-1; i++) 
			if(t > list.get(i))
				if(t <= list.get(i+1))
					return i;
		return -1;
	}
	
	public boolean isAcquisitionTimeSensible() {
		double halfMaximum = halfRiseTime();
		double cutoff = CUTOFF_FACTOR*halfMaximum;
		return time.get(count-1) < cutoff; 
	}
	
}