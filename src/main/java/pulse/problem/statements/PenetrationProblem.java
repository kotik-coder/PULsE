package pulse.problem.statements;

import static pulse.math.transforms.StandardTransformations.LOG;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.NUMPOINTS;

import java.util.List;

import pulse.math.ParameterVector;
import pulse.math.Segment;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.solvers.ImplicitTranslucentSolver;
import pulse.problem.statements.model.AbsorptionModel;
import pulse.problem.statements.model.BeerLambertAbsorption;
import pulse.properties.Flag;
import pulse.properties.Property;
import pulse.ui.Messages;
import pulse.util.InstanceDescriptor;

public class PenetrationProblem extends ClassicalProblem {
	private final static int DEFAULT_CURVE_POINTS = 300;

	private static InstanceDescriptor<? extends AbsorptionModel> instanceDescriptor = new InstanceDescriptor<AbsorptionModel>(
			"Absorption model selector", AbsorptionModel.class);

	static {
		instanceDescriptor.setSelectedDescriptor(BeerLambertAbsorption.class.getSimpleName());
	}

	private AbsorptionModel absorption = instanceDescriptor.newInstance(AbsorptionModel.class);

	public PenetrationProblem() {
		super();
		getHeatingCurve().setNumPoints(derive(NUMPOINTS, DEFAULT_CURVE_POINTS));
		instanceDescriptor.addListener(() -> initAbsorption());
		absorption.setParent(this);
	}

	public PenetrationProblem(PenetrationProblem p) {
		super(p);
		initAbsorption();
	}

	private void initAbsorption() {
		setAbsorptionModel(instanceDescriptor.newInstance(AbsorptionModel.class));
	}

	public AbsorptionModel getAbsorptionModel() {
		return absorption;
	}

	public void setAbsorptionModel(AbsorptionModel model) {
		this.absorption = model;
		this.absorption.setParent(this);
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(instanceDescriptor);
		return list;
	}

	public static InstanceDescriptor<? extends AbsorptionModel> getAbsorptionSelector() {
		return instanceDescriptor;
	}

	@Override
	public void optimisationVector(ParameterVector output, List<Flag> flags) {
		super.optimisationVector(output, flags);

		for (int i = 0, size = output.dimension(); i < size; i++) {
			var key = output.getIndex(i);
			double value = 0;

			switch(key) {
			case LASER_ABSORPTIVITY:
				value = (double) (absorption.getLaserAbsorptivity()).getValue();
				break;
			case THERMAL_ABSORPTIVITY : 
				value = (double) (absorption.getThermalAbsorptivity()).getValue();
				break;
			default :
				continue;
			}
				
			//do this for the listed key values 
			output.setTransform(i, LOG);
			output.set(i, value);
			output.setParameterBounds(i, new Segment(1E-2, 1000.0));

		}

	}

	@Override
	public void assign(ParameterVector params) {
		super.assign(params);

		double value;
		
		for (int i = 0, size = params.dimension(); i < size; i++) {
			var key = params.getIndex(i);
			
			switch(key) {
			case LASER_ABSORPTIVITY :
			case THERMAL_ABSORPTIVITY :
				value = params.inverseTransform(i);
				break;
			default : 
				continue;
			}
			
			absorption.set(key, derive(key, value) );
			
		}
	}

	@Override
	public Class<? extends DifferenceScheme> defaultScheme() {
		return ImplicitTranslucentSolver.class;
	}

	@Override
	public String toString() {
		return Messages.getString("DistributedProblem.Descriptor");
	}

	@Override
	public Problem copy() {
		return new PenetrationProblem(this);
	}

}