package pulse.problem.statements;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.tanh;
import static pulse.math.MathUtils.atanh;
import static pulse.math.MathUtils.fastPowLoop;
import static pulse.properties.NumericProperty.*;
import static pulse.properties.NumericPropertyKeyword.NUMPOINTS;
import static pulse.properties.NumericPropertyKeyword.OPTICAL_THICKNESS;
import static pulse.properties.NumericPropertyKeyword.PLANCK_NUMBER;
import static pulse.properties.NumericPropertyKeyword.SCATTERING_ALBEDO;
import static pulse.properties.NumericPropertyKeyword.SCATTERING_ANISOTROPY;

import java.util.List;

import pulse.input.ExperimentalData;
import pulse.math.IndexedVector;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.solvers.MixedCoupledSolver;
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
		getHeatingCurve().setNumPoints(derive(NUMPOINTS, DEFAULT_CURVE_POINTS));
		this.opticalThickness = (double) def(OPTICAL_THICKNESS).getValue();
		this.planckNumber = (double) def(PLANCK_NUMBER).getValue();
		scatteringAnisotropy = (double) theDefault(SCATTERING_ANISOTROPY).getValue();
		scatteringAlbedo = (double) theDefault(SCATTERING_ALBEDO).getValue();
		setComplexity(ProblemComplexity.HIGH);
	}

	public ParticipatingMedium(Problem p) {
		super(p);
		this.opticalThickness = (double) theDefault(OPTICAL_THICKNESS).getValue();
		this.planckNumber = (double) theDefault(PLANCK_NUMBER).getValue();
		scatteringAlbedo = (double) theDefault(SCATTERING_ALBEDO).getValue();
		scatteringAnisotropy = (double) theDefault(SCATTERING_ANISOTROPY).getValue();
		setComplexity(ProblemComplexity.HIGH);
	}

	public ParticipatingMedium(ParticipatingMedium p) {
		super(p);
		this.opticalThickness = p.opticalThickness;
		this.planckNumber = p.planckNumber;
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
		return derive(OPTICAL_THICKNESS, opticalThickness);
	}

	public void setOpticalThickness(NumericProperty tau0) {
		requireType(tau0, OPTICAL_THICKNESS);
		this.opticalThickness = (double) tau0.getValue();
	}

	public NumericProperty getPlanckNumber() {
		return derive(PLANCK_NUMBER, planckNumber);
	}

	public void setPlanckNumber(NumericProperty planckNumber) {
		requireType(planckNumber, PLANCK_NUMBER);
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

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(def(PLANCK_NUMBER));
		list.add(def(OPTICAL_THICKNESS));
		list.add(def(SCATTERING_ALBEDO));
		list.add(def(SCATTERING_ANISOTROPY));
		return list;
	}

	private double maxNp() {
		final double l = (double) getSampleThickness().getValue();
		final double T = (double) getTestTemperature().getValue();
		return thermalConductivity() / (4.0 * NonlinearProblem.STEFAN_BOTLZMAN * fastPowLoop(T, 3) * l);
	}

	@Override
	public void optimisationVector(IndexedVector[] output, List<Flag> flags) {
		super.optimisationVector(output, flags);

		for (int i = 0, size = output[0].dimension(); i < size; i++) {
			switch (output[0].getIndex(i)) {
			case PLANCK_NUMBER:
				output[0].set(i, atanh(2.0 * planckNumber / maxNp() - 1.0));
				output[1].set(i, 1.0);
				break;
			case OPTICAL_THICKNESS:
				output[0].set(i, log(opticalThickness));
				output[1].set(i, 1.0);
				break;
			case SCATTERING_ALBEDO:
				output[0].set(i, atanh(2.0 * scatteringAlbedo - 1.0));
				output[1].set(i, 1.0);
				break;
			case SCATTERING_ANISOTROPY:
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

		NumericProperty changed = null;

		for (int i = 0, size = params.dimension(); i < size; i++) {
			switch (params.getIndex(i)) {

			case PLANCK_NUMBER:
				planckNumber = 0.5 * maxNp() * (tanh(params.get(i)) + 1.0);
				changed = getPlanckNumber();
				break;
			case OPTICAL_THICKNESS:
				opticalThickness = exp(params.get(i));
				changed = getOpticalThickness();
				break;
			case SCATTERING_ALBEDO:
				scatteringAlbedo = 0.5 * (tanh(params.get(i)) + 1.0);
				changed = getScatteringAlbedo();
				break;
			case SCATTERING_ANISOTROPY:
				scatteringAnisotropy = tanh(params.get(i));
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
		return derive(SCATTERING_ANISOTROPY, scatteringAnisotropy);
	}

	public void setScatteringAnisotropy(NumericProperty A1) {
		if (A1.getType() != SCATTERING_ANISOTROPY)
			throw new IllegalArgumentException("Illegal type: " + A1.getType());
		this.scatteringAnisotropy = (double) A1.getValue();
	}

	public NumericProperty getScatteringAlbedo() {
		return derive(SCATTERING_ALBEDO, scatteringAlbedo);
	}

	public void setScatteringAlbedo(NumericProperty omega0) {
		if (omega0.getType() != SCATTERING_ALBEDO)
			throw new IllegalArgumentException("Illegal type: " + omega0.getType());
		this.scatteringAlbedo = (double) omega0.getValue();
	}

	@Override
	public void useTheoreticalEstimates(ExperimentalData c) {
		super.useTheoreticalEstimates(c);
		if (this.allDetailsPresent()) {
			final double nSq = 4;
			final double lambda = thermalConductivity();
			final double l = (double) getSampleThickness().getValue();
			final double T = (double) getTestTemperature().getValue();
			planckNumber = lambda / (4.0 * nSq * STEFAN_BOTLZMAN * fastPowLoop(T, 3) * l);
		}
	}

	@Override
	public Class<? extends DifferenceScheme> defaultScheme() {
		return MixedCoupledSolver.class;
	}

}