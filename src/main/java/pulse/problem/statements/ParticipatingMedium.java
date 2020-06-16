package pulse.problem.statements;

import static pulse.properties.NumericPropertyKeyword.OPTICAL_THICKNESS;
import static pulse.properties.NumericPropertyKeyword.PLANCK_NUMBER;
import static pulse.properties.NumericPropertyKeyword.SCATTERING_ALBEDO;
import static pulse.properties.NumericPropertyKeyword.SCATTERING_ANISOTROPY;

import java.util.List;

import pulse.input.ExperimentalData;
import pulse.math.IndexedVector;
import pulse.problem.schemes.rte.MathUtils;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.ui.Messages;

public class ParticipatingMedium extends NonlinearProblem {

	private final static boolean DEBUG = false;

	private double opticalThickness;
	private double planckNumber;
	private double scatteringAlbedo;
	private double scatteringAnisotropy;

	private final static double DEFAULT_BIOT = 0.1;
	private final static int DEFAULT_CURVE_POINTS = 300;

	public ParticipatingMedium() {
		super();
		curve.setNumPoints(NumericProperty.derive(NumericPropertyKeyword.NUMPOINTS, DEFAULT_CURVE_POINTS));
		this.opticalThickness = (double) NumericProperty.def(OPTICAL_THICKNESS).getValue();
		this.planckNumber = (double) NumericProperty.def(PLANCK_NUMBER).getValue();
		scatteringAnisotropy = (double) NumericProperty.theDefault(NumericPropertyKeyword.SCATTERING_ANISOTROPY)
				.getValue();
		scatteringAlbedo = (double) NumericProperty.theDefault(NumericPropertyKeyword.SCATTERING_ALBEDO).getValue();
		Bi1 = DEFAULT_BIOT;
		Bi2 = DEFAULT_BIOT;
		emissivity = 1.0;
	}

	public ParticipatingMedium(Problem p) {
		super(p);
		this.opticalThickness = (double) NumericProperty.theDefault(OPTICAL_THICKNESS).getValue();
		this.planckNumber = (double) NumericProperty.theDefault(PLANCK_NUMBER).getValue();
		scatteringAlbedo = (double) NumericProperty.theDefault(NumericPropertyKeyword.SCATTERING_ALBEDO).getValue();
		scatteringAnisotropy = (double) NumericProperty.theDefault(NumericPropertyKeyword.SCATTERING_ANISOTROPY)
				.getValue();
		emissivity = 1.0;
	}

	public ParticipatingMedium(ParticipatingMedium p) {
		super(p);
		this.opticalThickness = p.opticalThickness;
		this.planckNumber = p.planckNumber;
		this.emissivity = p.emissivity;
		this.scatteringAlbedo = p.scatteringAlbedo;
		this.scatteringAnisotropy = p.scatteringAnisotropy;
	}

	@Override
	public String toString() {
		return Messages.getString("ParticipatingMedium.Descriptor");
	}

	@Override
	public boolean isEnabled() {
		return !DEBUG;
	}

	public NumericProperty getOpticalThickness() {
		return NumericProperty.derive(OPTICAL_THICKNESS, opticalThickness);
	}

	public void setOpticalThickness(NumericProperty tau0) {
		if (tau0.getType() != OPTICAL_THICKNESS)
			throw new IllegalArgumentException("Illegal type: " + tau0.getType());
		this.opticalThickness = (double) tau0.getValue();
	}

	public NumericProperty getPlanckNumber() {
		return NumericProperty.derive(PLANCK_NUMBER, planckNumber);
	}

	public void setPlanckNumber(NumericProperty planckNumber) {
		if (planckNumber.getType() != PLANCK_NUMBER)
			throw new IllegalArgumentException("Illegal type: " + planckNumber.getType());
		this.planckNumber = (double) planckNumber.getValue();
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty value) {
		super.set(type, value);

		switch (type) {
		case PLANCK_NUMBER:
			setPlanckNumber(value);
			break;
		case OPTICAL_THICKNESS:
			setOpticalThickness(value);
			break;
		case SCATTERING_ALBEDO:
			setScatteringAlbedo(value);
			break;
		case SCATTERING_ANISOTROPY:
			setScatteringAnisotropy(value);
			break;
		default:
			break;
		}

	}

	public double getEmissivity() {
		return emissivity;
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(NumericProperty.def(PLANCK_NUMBER));
		list.add(NumericProperty.def(OPTICAL_THICKNESS));
		list.add(NumericProperty.def(SCATTERING_ALBEDO));
		list.add(NumericProperty.def(SCATTERING_ANISOTROPY));
		return list;
	}

	@Override
	public void optimisationVector(IndexedVector[] output, List<Flag> flags) {
		super.optimisationVector(output, flags);

		for (int i = 0, size = output[0].dimension(); i < size; i++) {
			switch (output[0].getIndex(i)) {
			case PLANCK_NUMBER:
				output[0].set(i, planckNumber);
				output[1].set(i, 2.0);
				break;
			case OPTICAL_THICKNESS:
				output[0].set(i, Math.log(opticalThickness));
				output[1].set(i, Double.POSITIVE_INFINITY);
				break;
			case SCATTERING_ALBEDO:
				output[0].set(i, MathUtils.atanh(2.0*scatteringAlbedo - 1) );
				output[1].set(i, Double.POSITIVE_INFINITY);
				break;
			case SCATTERING_ANISOTROPY:
				output[0].set(i, MathUtils.atanh(scatteringAnisotropy) );
				output[1].set(i, Double.POSITIVE_INFINITY);
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
			case PLANCK_NUMBER:
				planckNumber = params.get(i);
				break;
			case OPTICAL_THICKNESS:
				opticalThickness = Math.exp(params.get(i));
				break;
			case SCATTERING_ALBEDO:
				scatteringAlbedo = 0.5*(Math.tanh( params.get(i) ) + 1.0 );
				break;
			case SCATTERING_ANISOTROPY:
				scatteringAnisotropy = Math.tanh( params.get(i) );
				break;
			case HEAT_LOSS:
			case DIFFUSIVITY:
				evaluateDependentParameters();
				break;
			default:
				continue;
			}
		}

	}

	public NumericProperty getScatteringAnisostropy() {
		return NumericProperty.derive(NumericPropertyKeyword.SCATTERING_ANISOTROPY, scatteringAnisotropy);
	}

	public void setScatteringAnisotropy(NumericProperty A1) {
		if (A1.getType() != NumericPropertyKeyword.SCATTERING_ANISOTROPY)
			throw new IllegalArgumentException("Illegal type: " + A1.getType());
		this.scatteringAnisotropy = (double) A1.getValue();
	}

	public NumericProperty getScatteringAlbedo() {
		return NumericProperty.derive(NumericPropertyKeyword.SCATTERING_ALBEDO, scatteringAlbedo);
	}

	public void setScatteringAlbedo(NumericProperty omega0) {
		if (omega0.getType() != NumericPropertyKeyword.SCATTERING_ALBEDO)
			throw new IllegalArgumentException("Illegal type: " + omega0.getType());
		this.scatteringAlbedo = (double) omega0.getValue();
	}

	@Override
	public void useTheoreticalEstimates(ExperimentalData c) {
		super.useTheoreticalEstimates(c);
		if (this.allDetailsPresent()) {
			final double nSq = 4;
			final double lambda = thermalConductivity();
			planckNumber = lambda / (4 * nSq * STEFAN_BOTLZMAN * Math.pow(T, 3) * l);
			evaluateDependentParameters();
		}
	}

}