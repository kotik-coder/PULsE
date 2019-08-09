package pulse.input;

import pulse.HeatingCurve;
import pulse.util.SaveableDirectory;

public class ExperimentalData extends HeatingCurve implements SaveableDirectory {
	
	private Metadata metadata;
	
	private	int fittingStartIndex = -1;
	private int fittingEndIndex = -1;
	
	private double BASELINE_TIME_THRESHOLD = 1E-5; //in seconds
	private final static double CUTOFF_FACTOR = 6;
	
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
		this.correctedTemperature.add(temperature);
	}
	
	public void remove(int i) {
		this.time.remove(i);
		this.temperature.remove(i);
		this.correctedTemperature.remove(i);
	}
	
	public void setFittingRange(double a, double b) {
		System.out.println(a + " ; " + b);
		
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
	
	public int findIndexOfMaximumBetween(int start, int end) {
		double max = Double.NEGATIVE_INFINITY;
		int iStart = start;
		
		for(int i = start; i < end; i++) {
			if(max < correctedTemperature.get(i)) {
				max = correctedTemperature.get(i);
				iStart = i;
			}
		}
		
		return iStart;
	}
	
	public double findAveragedMaximum() {		
		int maxIndex1;
		int i = 0;
		
		int startIndex = getFittingStartIndex(); 
		int endIndex = getFittingEndIndex();

		do {
			maxIndex1 = this.findIndexOfMaximumBetween(startIndex + i, endIndex);
			i += (endIndex - startIndex)/20;
		}
		while(maxIndex1 < 0.4*endIndex); 	
		
		//try to find second maximum
		
		int n = 0;
		double avMax = 0;
		final int pointsToAverage = count / 20;
			
		for(int j = maxIndex1 - pointsToAverage; j < maxIndex1 + pointsToAverage; j++) {
			if(j > endIndex)
				break;
			avMax += correctedTemperature.get(j);
			n++;
		}
	
		return avMax/n;
		
	}
	
	public double findHalfMaximumTime() {
		
		double halfMax = this.findAveragedMaximum()/2.0;
		
		int i = 0;
		
		final int acceptableStart = (int) (0.1*getFittingEndIndex());
		
		for(int start = getFittingStartIndex(); i < acceptableStart; start = i + 1) {
		
			for(i = start; i <= getFittingEndIndex(); i++) 
				if(correctedTemperature.get(i) >= halfMax)
					break;
	
		}
		
		return this.timeAt(i);
	}
	
	public Metadata getMetadata() {
		return metadata;
	}
	
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
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
	
	public void estimateBaseline() {
		
		baseline = 0;
		
		int i = 0;
		int n = 0;
		
		for(i = count-1; i >= 0; i--) {
			if(time.get(i) < BASELINE_TIME_THRESHOLD) {
				baseline += temperature.get(i);
				n++;
				if(time.get(i) < 0) {
					remove(i);
					i++;
				}
			}
		}
		
		count = this.time.size();
		
		baseline /= n;
		
	}

	public int getFittingStartIndex() {
		return fittingStartIndex > 0 ? fittingStartIndex : 0;
	}

	public int getFittingEndIndex() {
		return fittingEndIndex > 0 ? fittingEndIndex : (count - 1);
	}
	
	public void updateCount() {
		this.count = this.time.size();
	}
	
	public void truncate() {			
		double halfMaximum = findHalfMaximumTime();
		double cutoff = CUTOFF_FACTOR*halfMaximum;
		
		for(int i = count-1; time.get(i) > cutoff ; i--) 
			remove(i);		
		
		updateCount();		
	}
	
	public boolean isAcquisitionTimeSensible() {
		double halfMaximum = findHalfMaximumTime();
		double cutoff = CUTOFF_FACTOR*halfMaximum;
		return time.get(count-1) < cutoff; 
	}
	
}