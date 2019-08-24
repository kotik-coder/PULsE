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

/**
 * @author Artem V. Lunev
 *
 */
public class NonlinearProblem extends Problem {
	
	protected double qAbs, T;
	protected double nonlinearPrecision;	

	
	/**
	 * @param cV
	 * @param rho
	 * @param qAbs
	 */
	
	public NonlinearProblem(NumericProperty a, NumericProperty cV, NumericProperty rho, NumericProperty qAbs, NumericProperty T) {
		this();
		this.a = (double)a.getValue();
		this.cV = (double)cV.getValue();
		this.rho = (double)rho.getValue();
		this.qAbs = (double)qAbs.getValue();
		this.T = (double)T.getValue();
	}
	
	public NonlinearProblem() {
		super();
		nonlinearPrecision = (double)NumericProperty.NONLINEAR_PRECISION.getValue();
		this.a = (double)NumericProperty.DIFFUSIVITY.getValue();		
		this.qAbs = (double)NumericProperty.ABSORBED_ENERGY.getValue();
		setTestTemperature(NumericProperty.TEST_TEMPERATURE);
	}
	
	public NonlinearProblem(Problem p) {
		super(p);
		
		if(! (p instanceof NonlinearProblem) ) {
			this.qAbs = (double)NumericProperty.ABSORBED_ENERGY.getValue();
			nonlinearPrecision = (double)NumericProperty.NONLINEAR_PRECISION.getValue();	
			return;
		}
		
		NonlinearProblem np = (NonlinearProblem) p;
		
		this.qAbs 				= np.qAbs;
		this.T 					= np.T;
		this.nonlinearPrecision = np.nonlinearPrecision;
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
		return new NumericProperty(qAbs, NumericProperty.ABSORBED_ENERGY);
	}

	public void setAbsorbedEnergy(NumericProperty qAbs) {
		this.qAbs = (double)qAbs.getValue(); 
	}

	public NumericProperty getNonlinearPrecision() {
		return new NumericProperty(nonlinearPrecision, NumericProperty.NONLINEAR_PRECISION);
	}

	public void setNonlinearPrecision(NumericProperty nonlinearPrecision) {
		this.nonlinearPrecision = (double)nonlinearPrecision.getValue(); 
	}

	public NumericProperty getTestTemperature() {
		return new NumericProperty(T, NumericProperty.TEST_TEMPERATURE);
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
		list.add(NumericProperty.ABSORBED_ENERGY);
		list.add(NumericProperty.DENSITY);
		list.add(NumericProperty.NONLINEAR_PRECISION);
		list.add(NumericProperty.TEST_TEMPERATURE);
		list.add(NumericProperty.SPECIFIC_HEAT);
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
		return false;
	}

}
