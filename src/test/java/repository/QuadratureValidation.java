package repository;

import static java.lang.Math.pow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.INTEGRATION_SEGMENTS;
import static pulse.properties.NumericPropertyKeyword.QUADRATURE_POINTS;
import static repository.NonscatteringTestCase.approximatelyEquals;
import static repository.TestProfileLoader.loadTestProfileDense;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pulse.math.Segment;
import pulse.problem.schemes.rte.BlackbodySpectrum;
import pulse.problem.schemes.rte.exact.ChandrasekharsQuadrature;
import pulse.problem.schemes.rte.exact.CompositionProduct;
import pulse.problem.schemes.rte.exact.NewtonCotesQuadrature;
import pulse.problem.statements.ParticipatingMedium;

class QuadratureValidation {

	private static ParticipatingMedium problem;
	private static List<Double> testProfile;
	private static UnivariateFunction interpolation;
	
	private BlackbodySpectrum ef;
	
	private ChandrasekharsQuadrature quad1;
	private NewtonCotesQuadrature quad2;
	
	private final static double opticalThickness = 3.0;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		testProfile = loadTestProfileDense();
		problem = new NonscatteringTestCase(testProfile.size(), 0.1).getTestProblem();
	
		var tempArray = testProfile.stream().mapToDouble(d -> d).toArray();
		
		var xArray = new double[tempArray.length + 1];
		IntStream.range(0, xArray.length).forEach(i -> xArray[i] = (i-1)*opticalThickness/(tempArray.length - 1.0));
				
		var tarray = new double[tempArray.length + 1];
		System.arraycopy(tempArray, 0, tarray, 1, tempArray.length);
		final double alpha = (xArray[0] - xArray[1])/(xArray[2] - xArray[1]);
		tarray[0] = tarray[1] * alpha + (1.0 - alpha) * tarray[2];
		interpolation = new SplineInterpolator().interpolate(xArray, tarray);
	}
	
	private void prepareFirstOrder(CompositionProduct... quads) {
		for(CompositionProduct quad : quads) {
			quad.setOrder(1);
			quad.setBounds(new Segment(0.5, opticalThickness));
			quad.setCoefficients(-0.5,1.0);
			quad.setEmissionFunction(ef);
		}
	}
	
	private void prepareSecondOrder(CompositionProduct... quads) {
		for(CompositionProduct quad : quads) {
			quad.setOrder(2);
			quad.setBounds(new Segment(0.0, opticalThickness));
			quad.setCoefficients(opticalThickness,-1.0);
			quad.setEmissionFunction(ef);
		}
	}
	
	@BeforeEach
	void setUp() throws Exception {
		quad1 = new ChandrasekharsQuadrature();
		quad2 = new NewtonCotesQuadrature();
		ef = new BlackbodySpectrum(problem);
		ef.setInterpolation( interpolation );
	}

	private boolean test(final double margin) {
		var list = new ArrayList<Double>();
		var list2 = new ArrayList<Double>();
		
		double value;
		
		for(int i = 2; i < 9; i++) {
			quad1.setQuadraturePoints(derive(QUADRATURE_POINTS, i));
			list.add( value = quad1.integrate() );
			System.out.printf("%nPoints: %5d. Result = %3.4f", quad1.getQuadraturePoints().getValue(), value);
		}
		
		for(int i = 5; i < 20; i++) {
			quad2.getIntegrator().setIntegrationSegments(derive(INTEGRATION_SEGMENTS, (int) pow(2, i)));
			list2.add( value = quad2.integrate() );
			System.out.printf("%nSegments: %6d. Result = %3.4f", quad2.getIntegrator().getIntegrationSegments().getValue(), value);
		}
		
		return approximatelyEquals(list.get(list.size() - 1), list2.get(list2.size() - 1), margin);
	}
	
	@Test
	void testFirstOrderConvergence() {
		System.out.printf("%n%nFirst-order test");
		prepareFirstOrder(quad1, quad2);
		assertTrue( test(1E-3) );
	}
	
	@Test
	void testSecondOrderConvergence() {
		System.out.printf("%n%nSecond-order test");
		prepareSecondOrder(quad1, quad2);
		assertTrue( test(1E-3) );
	}
	
	@Test
	void testChandrasekhars() {
		prepareFirstOrder(quad1);
		quad1.setQuadraturePoints(derive(QUADRATURE_POINTS, 8));
		final double result = quad1.integrate();
		assertTrue(approximatelyEquals(result, 1961.617, 1E-6));
	}
	
	@Test
	void testNewtonCotes() {
		prepareFirstOrder(quad2);
		quad2.getIntegrator().setIntegrationSegments(derive(INTEGRATION_SEGMENTS, 4096));
		final double result = quad2.integrate();
		assertTrue(approximatelyEquals(result, 1962.45, 1E-6));
	}

}