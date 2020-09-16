package pulse.problem.statements;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.LASER_ABSORPTIVITY;
import static pulse.properties.NumericPropertyKeyword.NUMPOINTS;
import static pulse.properties.NumericPropertyKeyword.THERMAL_ABSORPTIVITY;

import java.util.List;

import pulse.math.IndexedVector;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.solvers.ImplicitTranslucentSolver;
import pulse.problem.statements.penetration.AbsorptionModel;
import pulse.problem.statements.penetration.BeerLambertAbsorption;
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

	public PenetrationProblem(Problem sdd) {
		super(sdd);
		getHeatingCurve().setNumPoints(derive(NUMPOINTS, DEFAULT_CURVE_POINTS));
		if (sdd instanceof PenetrationProblem) {
			PenetrationProblem tp = (PenetrationProblem) sdd;
			setAbsorptionModel(tp.absorption);
		} else {
			initAbsorption();
			instanceDescriptor.addListener(() -> initAbsorption());
		}
		absorption.setParent(this);
	}

	public PenetrationProblem(PenetrationProblem tp) {
		super(tp);
		getHeatingCurve().setNumPoints(derive(NUMPOINTS, DEFAULT_CURVE_POINTS));
		setAbsorptionModel(tp.absorption);
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
	public void optimisationVector(IndexedVector[] output, List<Flag> flags) {
		super.optimisationVector(output, flags);

		for (int i = 0, size = output[0].dimension(); i < size; i++) {
			switch (output[0].getIndex(i)) {
			case LASER_ABSORPTIVITY:
				output[0].set(i, log((double) (absorption.getLaserAbsorptivity()).getValue()));
				output[1].set(i, 2.0);
				break;
			case THERMAL_ABSORPTIVITY:
				output[0].set(i, log((double) (absorption.getThermalAbsorptivity()).getValue()));
				output[0].set(i, 2.0);
				break;
			default:
				continue;
			}
		}

	}

	@Override
	public void assign(IndexedVector params) {
		super.assign(params);

		for (int i = 0, size = params.dimension(); i < size; i++) {
			switch (params.getIndex(i)) {
			case LASER_ABSORPTIVITY:
				absorption.setLaserAbsorptivity(derive(LASER_ABSORPTIVITY, exp(params.get(i))));
				break;
			case THERMAL_ABSORPTIVITY:
				absorption.setThermalAbsorptivity(
						derive(THERMAL_ABSORPTIVITY, exp(params.get(i))));
				break;
			default:
				continue;
			}
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

}