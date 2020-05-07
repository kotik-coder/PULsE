package pulse.problem.statements;

import static pulse.properties.NumericPropertyKeyword.OPTICAL_THICKNESS;
import static pulse.properties.NumericPropertyKeyword.PLANCK_NUMBER;

import java.util.List;

import pulse.input.ExperimentalData;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.search.math.IndexedVector;
import pulse.ui.Messages;

public class AbsorbingEmittingProblem extends NonlinearProblem {

	private final static boolean DEBUG = false;
	
	private double opticalThickness;
	private double planckNumber;
	private double emissivity;

	private final static double DEFAULT_BIOT = 0.1;
	private final static int DEFAULT_CURVE_POINTS = 300;
	
	public AbsorbingEmittingProblem() {
		super();
		curve.setNumPoints(NumericProperty.derive(NumericPropertyKeyword.NUMPOINTS, DEFAULT_CURVE_POINTS));
		this.opticalThickness = (double)NumericProperty.def(OPTICAL_THICKNESS).getValue();
		this.planckNumber = (double)NumericProperty.def(PLANCK_NUMBER).getValue();
		Bi1 = DEFAULT_BIOT;
		Bi2 = DEFAULT_BIOT;
		emissivity = 1.0;
	}
	
	public AbsorbingEmittingProblem(Problem p) {
		super(p);
		this.opticalThickness = (double)NumericProperty.theDefault(OPTICAL_THICKNESS).getValue();
		this.planckNumber = (double)NumericProperty.theDefault(PLANCK_NUMBER).getValue();
		emissivity = 1.0;
	}
	
	public AbsorbingEmittingProblem(AbsorbingEmittingProblem p) {
		super(p);
		this.opticalThickness = p.opticalThickness;
		this.planckNumber = p.planckNumber;
		this.emissivity = p.emissivity;
	}
	
	@Override
	public String toString() {
		return Messages.getString("DistributedEmissionAbsorptionProblem.Descriptor"); 
	}
	
	@Override
	public boolean isEnabled() {
		return !DEBUG;
	}

	public NumericProperty getOpticalThickness() {
		return NumericProperty.derive(OPTICAL_THICKNESS, opticalThickness);
	}

	public void setOpticalThickness(NumericProperty tau0) {
		if(tau0.getType() != OPTICAL_THICKNESS)
			throw new IllegalArgumentException("Illegal type: " + tau0.getType());
		this.opticalThickness = (double)tau0.getValue();
	}

	public NumericProperty getPlanckNumber() {
		return NumericProperty.derive(PLANCK_NUMBER, planckNumber);
	}

	public void setPlanckNumber(NumericProperty planckNumber) {
		if(planckNumber.getType() != PLANCK_NUMBER)
			throw new IllegalArgumentException("Illegal type: " + planckNumber.getType());
		this.planckNumber = (double)planckNumber.getValue();
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty value) {
		super.set(type, value);
		
		switch(type) {
			case PLANCK_NUMBER		 :  setPlanckNumber(value); break;
			case OPTICAL_THICKNESS	 :  setOpticalThickness(value); break;
			default: break;
		}				
		
	}
	
	public double getEmissivity() {
		return emissivity;
	}
	
	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(NumericProperty.def(PLANCK_NUMBER));
		list.add(NumericProperty.def(OPTICAL_THICKNESS));
		return list;
	}
	
	@Override
	public void optimisationVector(IndexedVector[] output, List<Flag> flags) {
		super.optimisationVector(output, flags);
		
		for(int i = 0, size = output[0].dimension(); i < size; i++) {
			switch( output[0].getIndex(i) ) {
				case PLANCK_NUMBER		:	
					output[0].set(i, planckNumber);
					output[1].set(i, 2.0);
					break;									
				case OPTICAL_THICKNESS	:	
					output[0].set(i, Math.log(opticalThickness));
					output[1].set(i, 1.0);
					break;
				default 				: 	continue;
			}
		}
		
	}
		
	@Override
	public void assign(IndexedVector params) {
		super.assign(params);
		
		for(int i = 0, size = params.dimension(); i < size; i++) {
			switch( params.getIndex(i) ) {
				case PLANCK_NUMBER		:	
					planckNumber = params.get(i);
					break;				
				case OPTICAL_THICKNESS		:	
					opticalThickness = Math.exp(params.get(i));
					break;				
				case HEAT_LOSS :
				case DIFFUSIVITY :
					evaluateDependentParameters();
					break;
				default 				: 	continue;
			}
		}
		
	}
	
	@Override
	public void useTheoreticalEstimates(ExperimentalData c) {		
		super.useTheoreticalEstimates(c);
		if(this.allDetailsPresent()) {
			final double nSq = 4;
			final double lambda = thermalConductivity();
			planckNumber = lambda/(4*nSq*STEFAN_BOTLZMAN*Math.pow(T, 3)*l);
			evaluateDependentParameters();
		}
	}
	
}