package pulse.problem.statements;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.tanh;
import static pulse.math.MathUtils.atanh;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.NUMPOINTS;

import java.util.List;

import pulse.math.IndexedVector;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.solvers.MixedCoupledSolver;
import pulse.problem.statements.model.ThermalProperties;
import pulse.problem.statements.model.ThermoOpticalProperties;
import pulse.properties.Flag;
import pulse.properties.NumericPropertyKeyword;
import pulse.ui.Messages;

public class ParticipatingMedium extends NonlinearProblem {

	private final static int DEFAULT_CURVE_POINTS = 300;

	public ParticipatingMedium() {
		super();
		getHeatingCurve().setNumPoints(derive(NUMPOINTS, DEFAULT_CURVE_POINTS));
		setComplexity(ProblemComplexity.HIGH);
	}
	
	public ParticipatingMedium(ParticipatingMedium p) {
		super(p);
		setComplexity(ProblemComplexity.HIGH);
	}
	
	@Override
	public String toString() {
		return Messages.getString("ParticipatingMedium.Descriptor");
	}

	@Override
	public void optimisationVector(IndexedVector[] output, List<Flag> flags) {
		super.optimisationVector(output, flags);
		var properties = (ThermoOpticalProperties)getProperties();

		for (int i = 0, size = output[0].dimension(); i < size; i++) {
			switch (output[0].getIndex(i)) {
			case PLANCK_NUMBER:
				final double planckNumber = (double)properties.getPlanckNumber().getValue();
				output[0].set(i, atanh(2.0 * planckNumber / properties.maxNp() - 1.0));
				output[1].set(i, 1.0);
				break;
			case OPTICAL_THICKNESS:
				final double opticalThickness = (double)properties.getOpticalThickness().getValue();
				output[0].set(i, log(opticalThickness));
				output[1].set(i, 1.0);
				break;
			case SCATTERING_ALBEDO:
				final double scatteringAlbedo = (double)properties.getScatteringAlbedo().getValue();
				output[0].set(i, atanh(2.0 * scatteringAlbedo - 1.0));
				output[1].set(i, 1.0);
				break;
			case SCATTERING_ANISOTROPY:
				final double scatteringAnisotropy = (double)properties.getScatteringAnisostropy().getValue();
				output[0].set(i, atanh(scatteringAnisotropy));
				output[1].set(i, 1.0);
				break;
			default:
				continue;
			}
		}

	}

	@Override
	public void assign(IndexedVector params) {
		super.assign(params);
		var properties = (ThermoOpticalProperties)getProperties();
		
		NumericPropertyKeyword type;
		
		for (int i = 0, size = params.dimension(); i < size; i++) {
			type = params.getIndex(i);
			switch (type) {

			case PLANCK_NUMBER:
				var nP = derive(type, 0.5 * properties.maxNp() * (tanh(params.get(i)) + 1.0));
				properties.setPlanckNumber(nP);
				break;
			case OPTICAL_THICKNESS:
				var tau0 = derive(type, exp(params.get(i)));
				properties.setOpticalThickness(tau0);
				break;
			case SCATTERING_ALBEDO:
				var omega0 = derive(type, 0.5 * (tanh(params.get(i)) + 1.0));
				properties.setScatteringAlbedo(omega0);
				break;
			case SCATTERING_ANISOTROPY:
				var anisotropy = derive(type, tanh(params.get(i)));
				properties.setScatteringAnisotropy(anisotropy);
				break;
			case HEAT_LOSS:
			case DIFFUSIVITY:
				getProperties().emissivity();
				break;
			default:
				break;
			}

		}

	}

	@Override
	public Class<? extends DifferenceScheme> defaultScheme() {
		return MixedCoupledSolver.class;
	}
	
	@Override
	public void initProperties(ThermalProperties properties) {
		setProperties(new ThermoOpticalProperties(properties));
	}
	
	@Override
	public void initProperties() {
		setProperties( new ThermoOpticalProperties() );
	}
	
	@Override
	public Problem copy() {
		return new ParticipatingMedium(this);
	}

}