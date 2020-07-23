package pulse.problem.schemes.rte;

import static pulse.properties.NumericProperty.def;
import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.INTEGRATION_CUTOFF;
import static pulse.properties.NumericPropertyKeyword.INTEGRATION_SEGMENTS;

import java.util.ArrayList;
import java.util.List;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

public abstract class Integrator extends PropertyHolder implements Reflexive {

	private double dx;
	private double cutoff;
	private double cutoffMinusDx;
	
	private double lookupTable[][];
	private int lookupTableSize;
	
	private int integrationSegments;
	
	public Integrator(double cutoff, int numPartitions, int expIntPrecision) {
		this.cutoff = cutoff;
		this.lookupTableSize = numPartitions;
		this.integrationSegments = expIntPrecision;
		dx = cutoff / lookupTableSize;
		cutoffMinusDx = cutoff - dx;
	}

	/*
	 * Initialises integration table.
	 */

	public void init() {
		lookupTable = new double[lookupTableSize + 1][4];
		dx = cutoff / lookupTableSize;
		for (int i = 0; i < lookupTableSize; i++) {
			for (int j = 1; j < 4; j++) {
                            lookupTable[i][j] = integrate(j, i * dx);
                        }
		}
	}

	/**
	 * Uses linear interpolation to retrieve integral value from pre-calculated
	 * table.
	 */

	public double integralAt(double t, int n) {
		double _tH = Math.min(t, cutoffMinusDx) / dx;
		int i = (int) _tH;
		double delta = _tH - i;

		return lookupTable[i + 1][n] * delta + lookupTable[i][n] * (1.0 - delta);
	}

	/**
	 * t = params[0]
	 * 
	 * @param params
	 * @return
	 */

	public abstract double integrate(int order, double... params);

	/**
	 * mu = params[0] t = params[1]
	 * 
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
		return derive(INTEGRATION_CUTOFF, cutoff);
	}

	public void setCutoff(NumericProperty cutoff) {
		requireType(cutoff, INTEGRATION_CUTOFF);
		this.cutoff = (double) cutoff.getValue();
		dx = this.cutoff / lookupTableSize;
		cutoffMinusDx = this.cutoff - dx;
	}

	public NumericProperty getIntegrationSegments() {
		return derive(INTEGRATION_SEGMENTS, integrationSegments);
	}

	public void setIntegrationSegments(NumericProperty integrationSegments) {
		requireType(integrationSegments, INTEGRATION_SEGMENTS);
		this.integrationSegments = (int) integrationSegments.getValue();
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch (type) {
		case INTEGRATION_CUTOFF:
			setCutoff(property);
			break;
		case INTEGRATION_SEGMENTS:
			setIntegrationSegments(property);
			break;
		default:
			return;
		}

		firePropertyChanged(this, property);

	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<>();
		list.add(def(INTEGRATION_CUTOFF));
		list.add(def(INTEGRATION_SEGMENTS));
		return list;
	}

	@Override
	public String toString() {
		return "[" + getClass().getSimpleName() + " ; " + getIntegrationSegments() + " ; " + getNumPartitions() + "]";
	}

}