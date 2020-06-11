package pulse.problem.schemes.rte.dom;

import java.util.ArrayList;
import java.util.List;

import pulse.problem.schemes.rte.EmissionFunction;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.search.math.Vector;

public abstract class AdaptiveIntegrator extends NumericIntegrator {
	
	private double atol;
	private double rtol;
	private double scalingFactor;

	protected double[][] f;
	protected double[][] Ik;
	protected double[][] fk;
	protected double[] qLast;

	protected boolean firstRun;
	private boolean rescaled;
	
	public AdaptiveIntegrator(DiscreteIntensities intensities, EmissionFunction ef, PhaseFunction ipf) {
		super(intensities, ef, ipf);
		atol = (double) NumericProperty.theDefault(NumericPropertyKeyword.ATOL).getValue();
		rtol = (double) NumericProperty.theDefault(NumericPropertyKeyword.RTOL).getValue();
		scalingFactor = (double) NumericProperty.theDefault(NumericPropertyKeyword.GRID_SCALING_FACTOR).getValue();
		f = new double[intensities.grid.getDensity() + 1][intensities.n]; // first index - spatial steps, second index - quadrature points
	}
	
	public boolean isFirstRun() {
		return firstRun;
	}
	
	protected void init() {
		qLast	= new double[intensities.n];
		rescaled = false;
	}
	
	public void storeIteration() {
		Ik = new double[intensities.grid.getDensity() + 1][intensities.mu.length];
		fk = new double[intensities.grid.getDensity() + 1][intensities.mu.length];
		
		//store k-th components
		for(int j = 0; j < Ik.length; j++) {
			System.arraycopy(intensities.I[j], 0, Ik[j], 0, Ik[0].length);
			System.arraycopy(f[j], 0, fk[j], 0, fk[0].length);
		}
		
	}
	
	public void successiveOverrelaxation(double W) {
		
		double ONE_MINUS_W = 1.0 - W;
		
		for(int i = 0, N = intensities.grid.getDensity() + 1; i < N; i++) 
			for(int j = 0; j < intensities.n; j++) {
				intensities.I[i][j] = ONE_MINUS_W*Ik[i][j] + W*intensities.I[i][j];
				f[i][j]				= ONE_MINUS_W*fk[i][j] + W*f[i][j];
			}
	
	}

	@Override
	public void integrate() {
		Vector[] v;
		int N = intensities.grid.getDensity();
		init();
		
		for (double error = 1.0, relFactor = 0.0, i0Max = 0, i1Max = 0; error > atol + relFactor * rtol; N = intensities.grid.getDensity()) {

			error = 0;
			f = new double[N + 1][intensities.n]; // first index - spatial steps, second index - quadrature points

			treatZeroIndex();

			/*
			 * First set of ODE's. Initial condition corresponds to I(0) /t ----> tau0 The
			 * streams propagate in the positive hemisphere
			 */

			intensities.left(emissionFunction); // initial value for tau = 0
			i0Max = (new Vector(intensities.I[0])).maxAbsComponent();
			
			firstRun = true;

			for (int j = 0; j < N && error < atol + relFactor * rtol; j++) {
				
				v = step(j, 1.0);
				System.arraycopy(v[0].getData(), 0, intensities.I[j + 1], nPositiveStart, nH);
				
				i1Max		= (new Vector(intensities.I[j + 1])).maxAbsComponent();
				relFactor	= Math.max( i0Max, i1Max );
				i0Max		= i1Max;
				
				error		= v[1].maxAbsComponent();
			
			}
			
			/*
			 * Second set of ODE. Initial condition corresponds to I(tau0) /0 <---- t The
			 * streams propagate in the negative hemisphere
			 */

			intensities.right(emissionFunction); // initial value for tau = tau_0
			i0Max = (new Vector(intensities.I[N])).maxAbsComponent();
			
			firstRun = true;
			
			for (int j = N; j > 0 && error < atol + relFactor * rtol; j--) {
				
				v = step(j, -1.0);
				System.arraycopy(v[0].getData(), 0, intensities.I[j - 1], nNegativeStart, nH);
				
				i1Max		= (new Vector(intensities.I[j - 1])).maxAbsComponent();
				relFactor	= Math.max( i0Max, i1Max );
				i0Max		= i1Max;
				
				error		= v[1].maxAbsComponent();
			}
			
			// store derivatives for Hermite interpolation
			for (int i = 0; i < intensities.n; i++) {
				f[N][i] = f[N - 1][i]; 
				f[0][i] = f[1][i]; 
			}

//			System.out.printf("%n Steps: %5d Error: %1.5e, h_min = %3.6f, h_max = %3.6f", N, error, intensities.grid.stepRight(0), intensities.grid.stepLeft(N/2));

			if (error > atol + relFactor * rtol) {
				reduceStepSize();
				f = new double[0][0]; //clear derivatives
				HermiteInterpolator.clear();
			}

		}
		
	}

	public abstract Vector[] step(final int j, final double sign);

	public void reduceStepSize() {
		int nNew = (roundEven(scalingFactor * intensities.grid.getDensity()));
		generateGrid(nNew);
		this.intensities.reinitInternalArrays();
		intensities.clearBoundaryFluxes();
		rescaled = true;
	}
	
	public boolean wasRescaled() {
		return rescaled;
	}
	
	public void generateGrid(int nNew) {
		intensities.grid.generateUniform(nNew, true);
	}

	private int roundEven(double a) {
		return (int) (a / 2 * 2);
	}
	
	public NumericProperty getRelativeTolerance() {
		return NumericProperty.derive(NumericPropertyKeyword.RTOL, rtol);
	}
	
	public NumericProperty getAbsoluteTolerance() {
		return NumericProperty.derive(NumericPropertyKeyword.ATOL, atol);
	}
	
	public NumericProperty getGridScalingFactor() {
		return NumericProperty.derive(NumericPropertyKeyword.GRID_SCALING_FACTOR, scalingFactor);
	}
	
	public void setRelativeTolerance(NumericProperty p) {
		if(p.getType() != NumericPropertyKeyword.RTOL)
			throw new IllegalArgumentException("Illegal type: " + p.getType());
		this.rtol = (double)p.getValue();
	}
	
	public void setAbsoluteTolerance(NumericProperty p) {
		if(p.getType() != NumericPropertyKeyword.ATOL)
			throw new IllegalArgumentException("Illegal type: " + p.getType());
		this.atol = (double)p.getValue();
	}
	
	public void setGridScalingFactor(NumericProperty p) {
		if(p.getType() != NumericPropertyKeyword.GRID_SCALING_FACTOR)
			throw new IllegalArgumentException("Illegal type: " + p.getType());
		this.scalingFactor = (double)p.getValue();
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch (type) {
		case RTOL:
			setRelativeTolerance(property);
			break;
		case ATOL:
			setAbsoluteTolerance(property);
			break;
		case GRID_SCALING_FACTOR:
			setGridScalingFactor(property);
			break;
		default:
			return;
		}

		notifyListeners(this, property);

	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<Property>();
		list.add(NumericProperty.def(NumericPropertyKeyword.RTOL));
		list.add(NumericProperty.def(NumericPropertyKeyword.ATOL));
		list.add(NumericProperty.def(NumericPropertyKeyword.GRID_SCALING_FACTOR));
		return list;
	}

}