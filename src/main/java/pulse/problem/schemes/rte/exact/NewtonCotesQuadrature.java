package pulse.problem.schemes.rte.exact;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericPropertyKeyword.INTEGRATION_CUTOFF;
import static pulse.properties.NumericPropertyKeyword.INTEGRATION_SEGMENTS;

import pulse.math.AbstractIntegrator;
import pulse.math.FixedIntervalIntegrator;
import pulse.math.Segment;
import pulse.math.SimpsonIntegrator;
import pulse.properties.NumericProperty;

public class NewtonCotesQuadrature extends Convolution {
	
	private final static int DEFAULT_SEGMENTS = 64;
	private final static double DEFAULT_CUTOFF = 16.0;
	private FixedIntervalIntegrator convolution;
	
	public NewtonCotesQuadrature() {
		this(new Segment(0, 1));
	}
	
	public NewtonCotesQuadrature(Segment bounds) {
		this(bounds, derive(INTEGRATION_SEGMENTS, DEFAULT_SEGMENTS));
	}

	public NewtonCotesQuadrature(Segment bounds, NumericProperty segments) {
		super(bounds);
		setCutoff(derive(INTEGRATION_CUTOFF, DEFAULT_CUTOFF));
		Convolution reference = this;
		convolution = new SimpsonIntegrator(new Segment(0.0, 1.0)) {

			@Override
			public double integrand(double... vars) {
				return reference.integrand(vars);
			}
			
			@Override
			public String toString() {
				return getDescriptor() + " ; " + getIntegrationSegments();
			}
			
			@Override
			public String getDescriptor() {
				return reference.getSimpleName();
			}

		};
	}
	
	@Override
	public double integrate() {
		convolution.setBounds(truncatedBounds());
		return convolution.integrate();
	}
	
	public AbstractIntegrator getIntegrator() {
		return convolution;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " : " + getCutoff() + " ; " + convolution.getIntegrationSegments();
	}
	
	private Segment truncatedBounds() {
		final double min = getBounds().getMinimum();
		final double max = getBounds().getMaximum();

		double bound = ((double)getCutoff().getValue() - getAlpha()) / getBeta();

		double a = 0.5 - getBeta() / 2;	//beta usually takes values of 1 or -1, so a is either 0 or 1
		double b = 1. - a;				//either 1 or 0
		
		return new Segment(max(bound, min) * a + min * b, max * a + min(bound, max) * b);
	}

}