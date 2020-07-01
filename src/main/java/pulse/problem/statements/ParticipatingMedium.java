package pulse.problem.statements;

import static pulse.properties.NumericPropertyKeyword.OPTICAL_THICKNESS;
import static pulse.properties.NumericPropertyKeyword.PLANCK_NUMBER;
import static pulse.properties.NumericPropertyKeyword.SCATTERING_ALBEDO;
import static pulse.properties.NumericPropertyKeyword.SCATTERING_ANISOTROPY;

import java.util.List;

import pulse.input.ExperimentalData;
import pulse.math.IndexedVector;
import pulse.math.MathUtils;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.MixedScheme;
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

	private final static int DEFAULT_CURVE_POINTS = 300;

	public ParticipatingMedium() {
		super();
		curve.setNumPoints(NumericProperty.derive(NumericPropertyKeyword.NUMPOINTS, DEFAULT_CURVE_POINTS));
		this.opticalThickness = (double) NumericProperty.def(OPTICAL_THICKNESS).getValue();
		this.planckNumber = (double) NumericProperty.def(PLANCK_NUMBER).getValue();
		scatteringAnisotropy = (double) NumericProperty.theDefault(NumericPropertyKeyword.SCATTERING_ANISOTROPY)
				.getValue();
		scatteringAlbedo = (double) NumericProperty.theDefault(NumericPropertyKeyword.SCATTERING_ALBEDO).getValue();
		setComplexity(ProblemComplexity.HIGH);
	}

	public ParticipatingMedium(Problem p) {
		super(p);
		this.opticalThickness = (double) NumericProperty.theDefault(OPTICAL_THICKNESS).getValue();
		this.planckNumber = (double) NumericProperty.theDefault(PLANCK_NUMBER).getValue();
		scatteringAlbedo = (double) NumericProperty.theDefault(NumericPropertyKeyword.SCATTERING_ALBEDO).getValue();
		scatteringAnisotropy = (double) NumericProperty.theDefault(NumericPropertyKeyword.SCATTERING_ANISOTROPY)
				.getValue();
		setComplexity(ProblemComplexity.HIGH);
	}

	public ParticipatingMedium(ParticipatingMedium p) {
		super(p);
		this.opticalThickness = p.opticalThickness;
		this.planckNumber = p.planckNumber;
		this.emissivity = p.emissivity;
		this.scatteringAlbedo = p.scatteringAlbedo;
		this.scatteringAnisotropy = p.scatteringAnisotropy;
		setComplexity(ProblemComplexity.HIGH);
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
			return;
		}

		firePropertyChanged(this, value);

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

	private double maxNp() {
		return thermalConductivity() / (4.0 * NonlinearProblem.STEFAN_BOTLZMAN * MathUtils.fastPowLoop(T, 3) * l);
	}

	@Override
	public void optimisationVector(IndexedVector[] output, List<Flag> flags) {
		super.optimisationVector(output, flags);

		for (int i = 0, size = output[0].dimension(); i < size; i++) {
			switch (output[0].getIndex(i)) {
			case PLANCK_NUMBER:
				output[0].set(i, MathUtils.atanh(2.0 * planckNumber / maxNp() - 1.0));
				output[1].set(i, 1.0);
				break;
			case OPTICAL_THICKNESS:
				output[0].set(i, Math.log(opticalThickness));
				output[1].set(i, 10.0);
				break;
			case SCATTERING_ALBEDO:
				output[0].set(i, MathUtils.atanh(2.0 * scatteringAlbedo - 1.0));
				output[1].set(i, 10.0);
				break;
			case SCATTERING_ANISOTROPY:
				output[0].set(i, MathUtils.atanh(scatteringAnisotropy));
				output[1].set(i, 10.0);
				break;
			default:
				continue;
			}
		}

	}

	@Override
	public void assign(IndexedVector params) {
		super.assign(params);

		NumericProperty changed = null;

		for (int i = 0, size = params.dimension(); i < size; i++) {
			switch (params.getIndex(i)) {

			case PLANCK_NUMBER:
				planckNumber = 0.5 * maxNp() * (Math.tanh(params.get(i)) + 1.0);
				changed = getPlanckNumber();
				break;
			case OPTICAL_THICKNESS:
				opticalThickness = Math.exp(params.get(i));
				changed = getOpticalThickness();
				break;
			case SCATTERING_ALBEDO:
				scatteringAlbedo = 0.5 * (Math.tanh(params.get(i)) + 1.0);
				changed = getScatteringAlbedo();
				break;
			case SCATTERING_ANISOTROPY:
				scatteringAnisotropy = Math.tanh(params.get(i));
				changed = getScatteringAnisostropy();
				break;
			case HEAT_LOSS:
				changed = getHeatLoss();
			case DIFFUSIVITY:
				super.evaluateDependentParameters();
				changed = getDiffusivity();
				break;
			default:
				continue;
			}

			this.firePropertyChanged(this, changed);

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
			planckNumber = lambda / (4.0 * nSq * STEFAN_BOTLZMAN * Math.pow(T, 3) * l);
		}
	}

	@Override
	public Class<? extends DifferenceScheme> defaultScheme() {
		return MixedScheme.class;
	}

        @Override
	public boolean isBatchProcessingEnabled() {
		return false;
	}

}