package pulse.problem.statements;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.tanh;
import static pulse.math.MathUtils.atanh;
import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS_SIDE;
import static pulse.properties.NumericPropertyKeyword.SPOT_DIAMETER;

import java.util.List;

import pulse.math.IndexedVector;
import pulse.problem.laser.DiscretePulse;
import pulse.problem.laser.DiscretePulse2D;
import pulse.problem.schemes.ADIScheme;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.Grid;
import pulse.problem.schemes.Grid2D;
import pulse.properties.Flag;
import pulse.properties.NumericPropertyKeyword;
import pulse.ui.Messages;

/**
 * The complete problem statement for a fully two-dimensional problem, which
 * includes side heat losses, a variable field of view and variable
 * pulse-to-diameter ratio.
 *
 */

public class ClassicalProblem2D extends Problem {

	public ClassicalProblem2D() {
		super();
		setPulse( new Pulse2D() );
		setComplexity(ProblemComplexity.MODERATE);
	}

	public ClassicalProblem2D(Problem lp2) {
		super(lp2);
		setPulse( new Pulse2D(lp2.getPulse()) );
		setComplexity(ProblemComplexity.MODERATE);
	}
	
	@Override
	public void initProperties() {
		setProperties( new ExtendedThermalProperties() );
	}

	@Override
	public void initProperties(ThermalProperties properties) {
		setProperties(new ExtendedThermalProperties(properties));
	}

	@Override
	public Class<? extends DifferenceScheme> defaultScheme() {
		return ADIScheme.class;
	}

	@Override
	public String toString() {
		return Messages.getString("LinearizedProblem2D.Descriptor"); //$NON-NLS-1$
	}

	@Override
	public DiscretePulse discretePulseOn(Grid grid) {
		return grid instanceof Grid2D ? new DiscretePulse2D(this, (Grid2D) grid) : super.discretePulseOn(grid);
	}

	@Override
	public void optimisationVector(IndexedVector[] output, List<Flag> flags) {
		super.optimisationVector(output, flags);
		var properties = (ExtendedThermalProperties) getProperties();
		
		double value;
		final double d = (double)properties.getSampleDiameter().getValue();
		
		for (int i = 0, size = output[0].dimension(); i < size; i++) {
			switch (output[0].getIndex(i)) {
			case FOV_OUTER:
				value = (double)properties.getFOVOuter().getValue();
			case FOV_INNER:
				value = (double)properties.getFOVInner().getValue();
			case SPOT_DIAMETER:
				value = (double) ((Pulse2D) getPulse()).getSpotDiameter().getValue();
				output[0].set(i, value / d);
				output[1].set(i, 0.25);
				break;
			case HEAT_LOSS_SIDE:
				final double Bi3 = (double)properties.getSideLosses().getValue();
				output[0].set(i,
						properties.areThermalPropertiesLoaded() ? atanh(2.0 * Bi3 / properties.maxBiot() - 1.0) : log(Bi3));
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
		var properties = (ExtendedThermalProperties) getProperties();
		NumericPropertyKeyword type;
		
		final double d = (double)properties.getSampleDiameter().getValue();

		// TODO one-to-one mapping for FOV and SPOT_DIAMETER
		for (int i = 0, size = params.dimension(); i < size; i++) {
			type = params.getIndex(i);
			switch (type) {
			case FOV_OUTER:
			case FOV_INNER:
				properties.set(type, derive(type, params.get(i) * d));
				break;
			case SPOT_DIAMETER:
				var spotDiameter = derive(SPOT_DIAMETER, params.get(i) * d);
				((Pulse2D) getPulse()).setSpotDiameter(spotDiameter);
				break;
			case HEAT_LOSS_SIDE:
				final double bi = properties.areThermalPropertiesLoaded() ? 0.5 * properties.maxBiot() * (tanh(params.get(i)) + 1.0) : exp(params.get(i));
				properties.setSideLosses( derive(HEAT_LOSS_SIDE, bi) );
				break;
			default:
				continue;
			}
		}
	}

	@Override
	public boolean isReady() {
		return true;
	}

}