package pulse.problem.laser;

import static org.apache.commons.math3.special.Erf.*;
import static java.lang.Math.*;
import static pulse.properties.NumericProperty.def;
import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.*;

import java.util.List;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

public class SkewNormalPulse extends PulseTemporalShape {

	private double mu;
	private double sigma;
	private double lambda;
	private double norm;

	private int DEFAULT_POINTS = 100;

	public SkewNormalPulse() {
		mu = (double) def(SKEW_MU).getValue();
		lambda = (double) def(SKEW_LAMBDA).getValue();
		sigma = (double) def(SKEW_SIGMA).getValue();
		norm = 1.0;
	}

	@Override
	public void init(DiscretePulse pulse) {
		super.init(pulse);
		var width = pulse.getDiscretePulseWidth();
		norm = 1.0 / area(width);
	}

	private double area(double width) {

		final double dt = width / (DEFAULT_POINTS - 1);

		double sum = 0;
		norm = 1.0;

		for (int i = 0; i < DEFAULT_POINTS; i++)
			sum += evaluateAt( (i + 0.5) * dt) * dt;

		return sum;

	}

	@Override
	public double evaluateAt(double time) {
		final var reducedTime = time / getPulseWidth();

		final double lambdaHalf = 0.5 * lambda;
		final double sigmaSq = sigma * sigma;
		
		return norm * lambdaHalf * exp(lambdaHalf * (2.0 * mu + lambda * sigmaSq - 2.0 * reducedTime))
				* erfc( (mu + lambda * sigmaSq - reducedTime) / (sqrt(2) * sigma));

	}

	@Override
	public List<Property> listedTypes() {
		var list = super.listedTypes();
		list.add(def(SKEW_MU));
		list.add(def(SKEW_LAMBDA));
		list.add(def(SKEW_SIGMA));
		return list;
	}

	public NumericProperty getMu() {
		return derive(SKEW_MU, mu);
	}
	
	public NumericProperty getSigma() {
		return derive(SKEW_SIGMA, sigma);
	}

	public NumericProperty getLambda() {
		return derive(SKEW_LAMBDA, lambda);
	}

	public void setLambda(NumericProperty p) {
		requireType(p, SKEW_LAMBDA);
		this.lambda = (double) p.getValue();
	}

	public void setMu(NumericProperty p) {
		requireType(p, SKEW_MU);
		this.mu = (double) p.getValue();
	}

	public void setSigma(NumericProperty p) {
		requireType(p, SKEW_SIGMA);
		this.sigma = (double) p.getValue();
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch (type) {
		case SKEW_MU:
			setMu(property);
			break;
		case SKEW_LAMBDA:
			setLambda(property);
			break;
		case SKEW_SIGMA:
			setSigma(property);
			break;
		default:
			break;
		}
		firePropertyChanged(this, property);
	}

}
