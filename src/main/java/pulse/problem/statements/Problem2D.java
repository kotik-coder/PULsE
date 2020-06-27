package pulse.problem.statements;

import static pulse.properties.NumericPropertyKeyword.DIAMETER;
import static pulse.properties.NumericPropertyKeyword.FOV_INNER;
import static pulse.properties.NumericPropertyKeyword.FOV_OUTER;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS_SIDE;
import static pulse.properties.NumericPropertyKeyword.SPOT_DIAMETER;

import java.util.ArrayList;
import java.util.List;

import pulse.input.ExperimentalData;
import pulse.math.IndexedVector;
import pulse.problem.schemes.ADIScheme;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.DiscretePulse;
import pulse.problem.schemes.DiscretePulse2D;
import pulse.problem.schemes.Grid;
import pulse.problem.schemes.Grid2D;
import pulse.problem.schemes.rte.MathUtils;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

public abstract class Problem2D extends Problem implements TwoDimensional {

	protected double d, Bi3, fovOuter, fovInner;
	private final static boolean DEBUG = false;

	protected Problem2D() {
		super();
		this.pulse = new Pulse2D();
		Bi3 = (double) NumericProperty.def(HEAT_LOSS_SIDE).getValue();
		d = (double) NumericProperty.def(DIAMETER).getValue();
		fovOuter = (double) NumericProperty.def(FOV_OUTER).getValue();
		fovInner = (double) NumericProperty.def(FOV_INNER).getValue();
		setComplexity(ProblemComplexity.MODERATE);
	}

	protected Problem2D(Problem sdd) {
		super(sdd);
		this.pulse = new Pulse2D(sdd.getPulse());
		Bi3 = (double) NumericProperty.def(HEAT_LOSS_SIDE).getValue();
		d = (double) NumericProperty.def(DIAMETER).getValue();
		fovOuter = (double) NumericProperty.def(FOV_OUTER).getValue();
		fovInner = (double) NumericProperty.def(FOV_INNER).getValue();
		setComplexity(ProblemComplexity.MODERATE);
	}

	protected Problem2D(Problem2D sdd) {
		super(sdd);
		this.pulse = new Pulse2D(sdd.getPulse());
		this.d = sdd.d;
		this.Bi3 = sdd.Bi3;
		this.fovOuter = sdd.fovOuter;
		this.fovInner = sdd.fovInner;
		setComplexity(ProblemComplexity.MODERATE);
	}
	
	public void useTheoreticalEstimates(ExperimentalData c) {
		super.useTheoreticalEstimates(c);
		if (areThermalPropertiesLoaded()) 
			Bi3 = biot();	
	}

	@Override
	public NumericProperty getSampleDiameter() {
		return NumericProperty.derive(DIAMETER, d);
	}

	public void setSampleDiameter(NumericProperty d) {
		this.d = (double) d.getValue();
	}

	public NumericProperty getSideLosses() {
		return NumericProperty.derive(HEAT_LOSS_SIDE, Bi3);
	}

	public void setSideLosses(NumericProperty bi3) {
		this.Bi3 = (double) bi3.getValue();
	}

	@Override
	public NumericProperty getFOVOuter() {
		return NumericProperty.derive(FOV_OUTER, fovOuter);
	}

	public void setFOVOuter(NumericProperty fovOuter) {
		this.fovOuter = (double) fovOuter.getValue();
	}

	@Override
	public NumericProperty getFOVInner() {
		return NumericProperty.derive(FOV_INNER, fovInner);
	}

	public void setFOVInner(NumericProperty fovInner) {
		this.fovInner = (double) fovInner.getValue();
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<Property>();
		list.addAll(super.listedTypes());
		list.add(NumericProperty.def(HEAT_LOSS_SIDE));
		list.add(NumericProperty.def(DIAMETER));
		list.add(NumericProperty.def(FOV_OUTER));
		list.add(NumericProperty.def(FOV_INNER));
		return list;
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		super.set(type, property);
		switch (type) {
		case FOV_OUTER:
			setFOVOuter(property);
			break;
		case FOV_INNER:
			setFOVInner(property);
			break;
		case DIAMETER:
			setSampleDiameter(property);
			break;
		case HEAT_LOSS:
			setSideLosses(property);
			break;
		default:
			break;
		}
	}

	@Override
	public boolean isEnabled() {
		return !DEBUG;
	}

	@Override
	public DiscretePulse discretePulseOn(Grid grid) {
		return grid instanceof Grid2D ? new DiscretePulse2D(this, (Pulse2D) pulse, (Grid2D) grid)
				: super.discretePulseOn(grid);
	}

	@Override
	public void optimisationVector(IndexedVector[] output, List<Flag> flags) {
		super.optimisationVector(output, flags);

		for (int i = 0, size = output[0].dimension(); i < size; i++) {
			switch (output[0].getIndex(i)) {
			case FOV_OUTER:
				output[0].set(i, fovOuter / d);
				output[1].set(i, 0.25);
				break;
			case FOV_INNER:
				output[0].set(i, fovInner / d);
				output[1].set(i, 0.25);
				break;
			case SPOT_DIAMETER:
				double fov = (double) ((Pulse2D) pulse).getSpotDiameter().getValue();
				output[0].set(i, fov / d);
				output[1].set(i, 0.25);
				break;
			case HEAT_LOSS_SIDE:
				output[0].set(i,
						areThermalPropertiesLoaded() ? MathUtils.atanh(2.0 * Bi3 / maxBiot() - 1.0) : Math.log(Bi3));
				output[1].set(i, 2.0);
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
			case FOV_OUTER:
				fovOuter = params.get(i) * d;
				break;
			case FOV_INNER:
				fovInner = params.get(i) * d;
				break;
			case SPOT_DIAMETER:
				NumericProperty spotDiameter = NumericProperty.derive(SPOT_DIAMETER, params.get(i) * d);
				((Pulse2D) pulse).setSpotDiameter(spotDiameter);
				break;
			case HEAT_LOSS_SIDE:
				Bi3 = areThermalPropertiesLoaded() ? 0.5 * maxBiot() * (Math.tanh(params.get(i)) + 1.0)
						: Math.exp(params.get(i));
				break;
			default:
				continue;
			}
		}
	}

	@Override
	public Class<? extends DifferenceScheme> defaultScheme() {
		return ADIScheme.class;
	}

}