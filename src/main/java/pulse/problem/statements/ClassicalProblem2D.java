package pulse.problem.statements;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.SPOT_DIAMETER;

import java.util.List;

import pulse.math.ParameterVector;
import pulse.math.Segment;
import pulse.math.transforms.InvDiamTransform;
import pulse.problem.laser.DiscretePulse;
import pulse.problem.laser.DiscretePulse2D;
import pulse.problem.schemes.ADIScheme;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.Grid;
import pulse.problem.schemes.Grid2D;
import pulse.problem.schemes.solvers.SolverException;
import pulse.problem.statements.model.ExtendedThermalProperties;
import pulse.problem.statements.model.ThermalProperties;
import pulse.properties.Flag;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS_SIDE;
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
		setPulse(new Pulse2D());
		setComplexity(ProblemComplexity.MODERATE);
	}

	public ClassicalProblem2D(Problem p) {
		super(p);
		setPulse(new Pulse2D(p.getPulse()));
		setComplexity(ProblemComplexity.MODERATE);
	}

	@Override
	public void initProperties() {
		setProperties(new ExtendedThermalProperties());
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
	public void optimisationVector(ParameterVector output, List<Flag> flags) {
		super.optimisationVector(output, flags);
		var properties = (ExtendedThermalProperties) getProperties();
		double value;
		
		for (int i = 0, size = output.dimension(); i < size; i++) {

			var key = output.getIndex(i);

			switch (key) {
			case FOV_OUTER:
				value = (double) properties.getFOVOuter().getValue();
				break;
			case FOV_INNER:
				value = (double) properties.getFOVInner().getValue();
				break;
			case SPOT_DIAMETER:
				value = (double) ((Pulse2D) getPulse()).getSpotDiameter().getValue();
				break;
			case HEAT_LOSS_SIDE:
				final double Bi = (double) properties.getSideLosses().getValue();
				setHeatLossParameter(output, i, Bi);	
				continue;
                        case HEAT_LOSS_COMBINED:
				final double combined = (double) properties.getHeatLoss().getValue();
				setHeatLossParameter(output, i, combined);	
				continue;
			default:
				continue;
			}
	
			output.setTransform(i, new InvDiamTransform(properties));
			output.set(i, value);
			output.setParameterBounds(i, new Segment(0.5 * value, 1.5 * value));

		}

	}

	@Override
	public void assign(ParameterVector params) throws SolverException {
		super.assign(params);
		var properties = (ExtendedThermalProperties) getProperties();

		// TODO one-to-one mapping for FOV and SPOT_DIAMETER
		for (int i = 0, size = params.dimension(); i < size; i++) {
			var type = params.getIndex(i);
			switch (type) {
			case FOV_OUTER:
			case FOV_INNER:
			case HEAT_LOSS_SIDE: 
                        case HEAT_LOSS_COMBINED:
				properties.set(type, derive(type, params.inverseTransform(i) ));
				break;
			case SPOT_DIAMETER:
				((Pulse2D) getPulse()).setSpotDiameter( derive(SPOT_DIAMETER, params.inverseTransform(i) ));
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

	@Override
	public Problem copy() {
		return new ClassicalProblem2D(this);
	}

}