/**
 * 
 */
package pulse.problem.statements;

import java.util.List;
import pulse.input.ExperimentalData;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ExplicitScheme;
import pulse.problem.schemes.ImplicitScheme;
import pulse.problem.schemes.MixedScheme;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.search.math.IndexedVector;
import pulse.tasks.TaskManager;
import pulse.ui.Messages;

import static pulse.properties.NumericPropertyKeyword.*;

public class NonlinearProblem extends Problem {
	
	protected double qAbs, T;
	protected double nonlinearPrecision;	
	
	private final static boolean DEBUG = true;	
	
	public NonlinearProblem(NumericProperty a, NumericProperty cV, NumericProperty rho, NumericProperty qAbs, NumericProperty T) {
		this();
		this.a		= (double)a.getValue();
		this.cV		= (double)cV.getValue();
		this.rho	= (double)rho.getValue();
		this.qAbs	= (double)qAbs.getValue();
		this.T		= (double)T.getValue();
	}
	
	public NonlinearProblem() {
		super();
		nonlinearPrecision	= (double)NumericProperty.def(NONLINEAR_PRECISION).getValue();
		this.a 				= (double)NumericProperty.def(DIFFUSIVITY).getValue();		
		this.qAbs			= (double)NumericProperty.def(ABSORBED_ENERGY).getValue();
		this.T				= (double)NumericProperty.def(TEST_TEMPERATURE).getValue();
	}
	
	public NonlinearProblem(Problem p) {
		super(p);		
		this.qAbs 			= (double)NumericProperty.def(ABSORBED_ENERGY).getValue();
		nonlinearPrecision	= (double)NumericProperty.def(NONLINEAR_PRECISION).getValue();			
	}
	
	public NonlinearProblem(NonlinearProblem p) {
		super(p);
		this.qAbs 				= p.qAbs;
		this.T 					= p.T;
		this.nonlinearPrecision = p.nonlinearPrecision;
	}
	
	@Override
	public void retrieveData(ExperimentalData c) {
		super.retrieveData(c);
		this.setTestTemperature(c.getMetadata().getTestTemperature());		
		makeAdjustments();			
	}
	
	@Override
	public void setSpecificHeat(NumericProperty cV) {
		super.setSpecificHeat(cV);
		makeAdjustments();
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty rho) {
		super.set(type, rho);
		makeAdjustments();
	}
	
	private void makeAdjustments() {
		
		final double tempError	= 5e-2*this.signalHeight;
		final double error1 = 1e-5;

		if(cV < error1 || rho < error1) 
			return;
			
		if( Math.abs( estimateHeating() - this.signalHeight ) > tempError) 
			adjustAbsorbedEnergy();
		
	}

	public NumericProperty getAbsorbedEnergy() {
		return NumericProperty.derive(ABSORBED_ENERGY, qAbs);
	}

	public void setAbsorbedEnergy(NumericProperty qAbs) {
		this.qAbs = (double)qAbs.getValue(); 
	}

	public NumericProperty getNonlinearPrecision() {
		return NumericProperty.derive(NONLINEAR_PRECISION, nonlinearPrecision);
	}

	public void setNonlinearPrecision(NumericProperty nonlinearPrecision) {
		this.nonlinearPrecision = (double)nonlinearPrecision.getValue(); 
	}

	public NumericProperty getTestTemperature() {
		return NumericProperty.derive(TEST_TEMPERATURE, T);
	}

	public void setTestTemperature(NumericProperty T) {
		this.T  = (double)T.getValue();
		
		if(TaskManager.getSpecificHeatCurve() != null)
			cV = TaskManager.getSpecificHeatCurve().interpolateAt(this.T);
		
		if(TaskManager.getDensityCurve() != null) 
			rho = TaskManager.getDensityCurve().interpolateAt(this.T);
		
		makeAdjustments();
		
	}		
	
	@Override
	public List<Property> listedParameters() {
		List<Property> list = super.listedParameters();
		list.add(NumericProperty.def(ABSORBED_ENERGY));
		list.add(NumericProperty.def(DENSITY));
		list.add(NumericProperty.def(NONLINEAR_PRECISION));
		list.add(NumericProperty.def(TEST_TEMPERATURE));
		list.add(NumericProperty.def(SPECIFIC_HEAT));
		return list;
	}

	@Override
	public DifferenceScheme[] availableSolutions() {
		return new DifferenceScheme[]{new ExplicitScheme(), new ImplicitScheme(), new MixedScheme()};
	}
	
	@Override
	public IndexedVector objectiveFunction(List<Flag> flags) {	
		IndexedVector objectiveFunction = super.objectiveFunction(flags);
		int size = objectiveFunction.dimension(); 		
		
		for(int i = 0; i < size; i++) {

			if( objectiveFunction.getIndex(i) == NumericPropertyKeyword.MAXTEMP ) {
				objectiveFunction.set(i, qAbs);	
				break;		
			}
		}
		
		return objectiveFunction;
		
	}
	
	@Override
	public void assign(IndexedVector params) {
		super.assign(params);		
		int size = params.dimension(); 		
		
		for(int i = 0; i < size; i++) {

			if( params.getIndex(i) == NumericPropertyKeyword.MAXTEMP ) {
				qAbs = params.get(i);	
				break;		
			}
		}
		
	}
	
	public double estimateHeating() {
		return qAbs/(cV*rho*Math.PI*Math.pow((double)pulse.getSpotDiameter().getValue(), 2)*l);
	}
	
	public void adjustAbsorbedEnergy() {
		qAbs = signalHeight*cV*rho*Math.PI*Math.pow((double)pulse.getSpotDiameter().getValue(), 2)*l;
	}
	
	@Override
	public String toString() {
		return Messages.getString("NonlinearProblem.Descriptor"); //$NON-NLS-1$
	}

	@Override
	public boolean isReady() {
		if(TaskManager.getDensityCurve() == null || TaskManager.getSpecificHeatCurve() == null)
			return false;
		return super.isReady();
	}
	
	@Override
	public boolean isEnabled() {
		return !DEBUG;
	}

}