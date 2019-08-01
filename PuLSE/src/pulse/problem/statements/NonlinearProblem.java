/**
 * 
 */
package pulse.problem.statements;

import java.util.Map;

import pulse.input.ExperimentalData;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ExplicitScheme;
import pulse.problem.schemes.ImplicitScheme;
import pulse.problem.schemes.MixedScheme;
import pulse.properties.BooleanProperty;
import pulse.properties.NumericProperty;
import pulse.search.math.ObjectiveFunctionIndex;
import pulse.search.math.Vector;
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
		nonlinearPrecision = (double)NumericProperty.DEFAULT_NONLINEAR_PRECISION.getValue();
		this.a = (double)NumericProperty.DEFAULT_DIFFUSIVITY.getValue();		
		this.qAbs = (double)NumericProperty.DEFAULT_QABS.getValue();
		setTestTemperature(NumericProperty.DEFAULT_T);
	}
	
	public NonlinearProblem(Problem p) {
		super(p);
		
		if(! (p instanceof NonlinearProblem) ) {
			this.qAbs = (double)NumericProperty.DEFAULT_QABS.getValue();
			nonlinearPrecision = (double)NumericProperty.DEFAULT_NONLINEAR_PRECISION.getValue();	
			return;
		}
		
		NonlinearProblem np = (NonlinearProblem) p;
		
		this.qAbs 				= np.qAbs;
		this.T 					= np.T;
		this.nonlinearPrecision = np.nonlinearPrecision;
	}
	
	@Override
	public void couple(ExperimentalData c) {
		super.couple(c);
		this.setTestTemperature(c.getMetadata().getTestTemperature());
		
		makeAdjustments();	
		
	}
	
	@Override
	public void setSpecificHeat(NumericProperty cV) {
		super.setSpecificHeat(cV);
		makeAdjustments();
	}

	@Override
	public void setDensity(NumericProperty rho) {
		super.setDensity(rho);
		makeAdjustments();
	}
	
	private void makeAdjustments() {
		
		final double tempError	= 5e-2*this.maximumTemperature;
		final double error1 = 1e-5;

		if(cV < error1 || rho < error1) 
			return;
			
		if( Math.abs( estimateHeating() - this.maximumTemperature ) > tempError) 
			adjustAbsorbedEnergy();
		
	}

	public NumericProperty getAbsorbedEnergy() {
		return new NumericProperty(qAbs, NumericProperty.DEFAULT_QABS);
	}

	public void setAbsorbedEnergy(NumericProperty qAbs) {
		this.qAbs = (double)qAbs.getValue(); 
	}

	public NumericProperty getNonlinearPrecision() {
		return new NumericProperty(nonlinearPrecision, NumericProperty.DEFAULT_NONLINEAR_PRECISION);
	}

	public void setNonlinearPrecision(NumericProperty nonlinearPrecision) {
		this.nonlinearPrecision = (double)nonlinearPrecision.getValue(); 
	}

	public NumericProperty getTestTemperature() {
		return new NumericProperty(T, NumericProperty.DEFAULT_T);
	}

	public void setTestTemperature(NumericProperty T) {
		this.T  = (double)T.getValue();
		
		if(TaskManager.getSpecificHeatCurve() != null)
			setSpecificHeat( 
					new NumericProperty(TaskManager.getSpecificHeatCurve().valueAt(this.T), 
							NumericProperty.DEFAULT_CV));
		
		if(TaskManager.getDensityCurve() != null)
			setDensity( 
					new NumericProperty(TaskManager.getDensityCurve().valueAt(this.T), 
							NumericProperty.DEFAULT_RHO));
	}		
	
	@Override
	public Map<String,String> propertyNames() {
		Map<String,String> map = super.propertyNames();
		map.put(getAbsorbedEnergy().getSimpleName(), Messages.getString("NonlinearProblem.0")); //$NON-NLS-1$
		map.put(getDensity().getSimpleName(), Messages.getString("NonlinearProblem.1")); //$NON-NLS-1$
		map.put(getNonlinearPrecision().getSimpleName(), Messages.getString("NonlinearProblem.2")); //$NON-NLS-1$
		map.put(getTestTemperature().getSimpleName(), Messages.getString("NonlinearProblem.3")); //$NON-NLS-1$
		map.put(getSpecificHeat().getSimpleName(), Messages.getString("NonlinearProblem.4")); //$NON-NLS-1$
		return map;
	}

	@Override
	public DifferenceScheme[] availableSolutions() {
		return new DifferenceScheme[]{new ExplicitScheme(), new ImplicitScheme(), new MixedScheme()};
	}

	@Override
	public Vector objectiveFunction(BooleanProperty[] flags) {
		Vector v = super.objectiveFunction(flags);

		for(int i = 0; i < flags.length; i++) {
			if(! (boolean) flags[i].getValue() )
				continue; 
			
			if( ObjectiveFunctionIndex.valueOf(flags[i].getSimpleName()) .equals(ObjectiveFunctionIndex.MAX_TEMP)) {
				v.set(i, qAbs);	
				break;		
			}
			
		}
		
		return v;
	}
	
	@Override 
	public void assign(Vector params, BooleanProperty[] flags) {
		super.assign(params, flags);
		
		for(int i = 0, realIndex = 0; i < flags.length; i++) {
			if(! (boolean) flags[i].getValue() )
				continue;
			
			realIndex = convert(i, flags);
			
			if( ObjectiveFunctionIndex.valueOf(flags[i].getSimpleName()) .equals(ObjectiveFunctionIndex.MAX_TEMP)) {
				qAbs = params.get(realIndex); 
				break;
			}

		}
	}
	
	public double estimateHeating() {
		return qAbs/(cV*rho*Math.PI*Math.pow((double)pulse.getSpotDiameter().getValue(), 2)*l);
	}
	
	public void adjustAbsorbedEnergy() {
		qAbs = maximumTemperature*cV*rho*Math.PI*Math.pow((double)pulse.getSpotDiameter().getValue(), 2)*l;
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

}
