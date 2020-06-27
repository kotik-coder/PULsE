package pulse.problem.statements;

import java.util.List;

import pulse.math.IndexedVector;
import pulse.problem.statements.penetration.AbsorptionModel;
import pulse.problem.statements.penetration.BeerLambertAbsorption;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.ui.Messages;
import pulse.util.InstanceDescriptor;

public class PenetrationProblem extends LinearisedProblem {
	private final static int DEFAULT_CURVE_POINTS = 300;

	private static InstanceDescriptor<? extends AbsorptionModel> instanceDescriptor = new InstanceDescriptor<AbsorptionModel>(
			"Absorption model selector", AbsorptionModel.class);

	static {
		instanceDescriptor.setSelectedDescriptor(BeerLambertAbsorption.class.getSimpleName());
	}

	private AbsorptionModel absorption = instanceDescriptor.newInstance(AbsorptionModel.class);

	private final static double SENSITIVITY = 100;

	public PenetrationProblem() {
		super();
		curve.setNumPoints(NumericProperty.derive(NumericPropertyKeyword.NUMPOINTS, DEFAULT_CURVE_POINTS));
		absorption.setParent(this);
		this.parameterListChanged();
		instanceDescriptor.addListener(() -> {
			absorption = instanceDescriptor.newInstance(AbsorptionModel.class);
			absorption.setParent(this);
		});
	}

	public PenetrationProblem(Problem sdd) {
		super(sdd);
		curve.setNumPoints(NumericProperty.derive(NumericPropertyKeyword.NUMPOINTS, DEFAULT_CURVE_POINTS));
		if (sdd instanceof PenetrationProblem) {
			PenetrationProblem tp = (PenetrationProblem) sdd;
			this.absorption = tp.absorption;
		}
	}

	public PenetrationProblem(PenetrationProblem tp) {
		super(tp);
		curve.setNumPoints(NumericProperty.derive(NumericPropertyKeyword.NUMPOINTS, DEFAULT_CURVE_POINTS));
		this.absorption = tp.absorption;
	}

	public AbsorptionModel getAbsorptionModel() {
		return absorption;
	}

	public void setAbsorptionModel(AbsorptionModel model) {
		this.absorption = model;
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		super.set(type, property);
		absorption.set(type, property);
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(instanceDescriptor);
		return list;
	}

	public InstanceDescriptor<? extends AbsorptionModel> getAbsorptionSelector() {
		return instanceDescriptor;
	}

	@Override
	public void optimisationVector(IndexedVector[] output, List<Flag> flags) {
		super.optimisationVector(output, flags);

		for (int i = 0, size = output[0].dimension(); i < size; i++) {
			switch (output[0].getIndex(i)) {
			case LASER_ABSORPTIVITY:
				output[0].set(i, (double) (absorption.getLaserAbsorptivity()).getValue() / SENSITIVITY);
				output[1].set(i, 0.1);
				break;
			case THERMAL_ABSORPTIVITY:
				output[0].set(i, (double) (absorption.getThermalAbsorptivity()).getValue() / SENSITIVITY);
				output[0].set(i, 0.1);
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
				absorption.setLaserAbsorptivity(
						NumericProperty.derive(NumericPropertyKeyword.LASER_ABSORPTIVITY, params.get(i) * SENSITIVITY));
				break;
			case THERMAL_ABSORPTIVITY:
				absorption.setThermalAbsorptivity(NumericProperty.derive(NumericPropertyKeyword.THERMAL_ABSORPTIVITY,
						params.get(i) * SENSITIVITY));
				break;
			default:
				continue;
			}
		}
	}

	@Override
	public String toString() {
		return Messages.getString("DistributedProblem.Descriptor");
	}

}