package pulse.problem.schemes.radiation;

import java.util.ArrayList;
import java.util.List;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;

public abstract class Integrator extends PropertyHolder {

	protected double cutoff;
	private double cutoffMinusDx;
	protected int lookupTableSize;
	protected int integrationSegments;
	
	private double lookupTable[][];
	private double dx;
	
	public Integrator(double cutoff, int numPartitions, int expIntPrecision) {
		this.cutoff = cutoff;
		this.lookupTableSize = numPartitions;
		this.integrationSegments = expIntPrecision;
		dx = cutoff/lookupTableSize;
		cutoffMinusDx = cutoff - dx;
	}
	
	/*
	 * Initialises integration table.
	 */
	
	public void init() {
		lookupTable = new double[lookupTableSize + 1][4];
		dx = cutoff/lookupTableSize;
		for(int i = 0; i < lookupTableSize; i++) { 
			for(int j = 1; j < 4; j++) 
				lookupTable[i][j] = integrate(j, i*dx);
		}
	}
	
	/**
	 * Uses linear interpolation to retrieve integral value from pre-calculated table.
	 */
	
	public double integralAt(double t, int n) {
		double _tH = Math.min(t, cutoffMinusDx)/dx;
		int i = (int) _tH;
		double delta = _tH - i;
		
		return lookupTable[i+1][n]*delta + lookupTable[i][n]*(1.0 - delta);
	}
	
	/**
	 * t = params[0]
	 * @param params
	 * @return
	 */
	
	public abstract double integrate(int order, double...params);
	
	/**
	 * mu = params[0]
	 * t = params[1]
	 * @param params
	 * @return
	 */
	
	public abstract double integrand(int order, double... params);

	public int getNumPartitions() {
		return lookupTableSize;
	}

	public void setNumPartitions(int numPartitions) {
		this.lookupTableSize = numPartitions;
	}

	public NumericProperty getCutoff() {
		return NumericProperty.derive(NumericPropertyKeyword.INTEGRATION_CUTOFF, cutoff);
	}

	public void setCutoff(NumericProperty cutoff) {
		if(cutoff.getType() != NumericPropertyKeyword.INTEGRATION_CUTOFF)
			throw new IllegalArgumentException("Keyword not recognised: " + cutoff.getType());
		this.cutoff = (double)cutoff.getValue();
		dx = this.cutoff/lookupTableSize;
		cutoffMinusDx = this.cutoff - dx;
	}
	
	
	public NumericProperty getIntegrationSegments() {
		return NumericProperty.derive(NumericPropertyKeyword.INTEGRATION_SEGMENTS, integrationSegments);
	}
	
	public void setIntegrationSegments(NumericProperty integrationSegments) {
		if(integrationSegments.getType() != NumericPropertyKeyword.INTEGRATION_SEGMENTS)
			throw new IllegalArgumentException("Illegal type: " + integrationSegments.getType());
		this.integrationSegments = (int)integrationSegments.getValue();
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch(type) {
			case INTEGRATION_CUTOFF : setCutoff(property); break;
			case INTEGRATION_SEGMENTS : setIntegrationSegments(property); break;
			default: return;
		}
		
		notifyListeners(this, property);
		 
	}
	
	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<Property>();
		list.add(NumericProperty.def(NumericPropertyKeyword.INTEGRATION_CUTOFF));
		list.add(NumericProperty.def(NumericPropertyKeyword.INTEGRATION_SEGMENTS));
		return list;				
	}
	
	@Override
	public String toString() {
		return "[" +getClass().getSimpleName() + " ; " + getIntegrationSegments() + " ; " + getNumPartitions() + "]";
	}
		
}