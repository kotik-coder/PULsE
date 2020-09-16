package repository;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.OPTICAL_THICKNESS;
import static pulse.properties.NumericPropertyKeyword.QUADRATURE_POINTS;
import static repository.TestProfileLoader.loadTestProfileDense;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pulse.problem.schemes.rte.RadiativeTransferSolver;
import pulse.problem.schemes.rte.dom.DiscreteOrdinatesMethod;
import pulse.problem.schemes.rte.exact.ChandrasekharsQuadrature;
import pulse.problem.schemes.rte.exact.NonscatteringAnalyticalDerivatives;
import pulse.problem.schemes.rte.exact.NonscatteringRadiativeTransfer;
import pulse.problem.statements.ThermoOpticalProperties;

class AnalyticalNonscatteringTransferValidation {

	private static List<Double> testProfile;
	private static NonscatteringTestCase testCase;

	private NonscatteringRadiativeTransfer chand;
	private RadiativeTransferSolver dom;
	private ChandrasekharsQuadrature quadrature;
	
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		testProfile = loadTestProfileDense();
		testCase = new NonscatteringTestCase(testProfile.size(), 10.0);
	}

	@BeforeEach
	void setUp() throws Exception {
		chand = new NonscatteringAnalyticalDerivatives(testCase.getTestProblem(), testCase.getTestScheme().getGrid());
		quadrature = new ChandrasekharsQuadrature();
		chand.setQuadrature(quadrature);
		dom = new DiscreteOrdinatesMethod(testCase.getTestProblem(), testCase.getTestScheme().getGrid());
	}
	
	@Test
	void testFluxesLowThickness() {
		quadrature.setQuadraturePoints(derive(QUADRATURE_POINTS, 2));
		var properties = (ThermoOpticalProperties)testCase.getTestProblem().getProperties();
		properties.setOpticalThickness(derive(OPTICAL_THICKNESS, 0.1));
		assertTrue(testCase.testFluxesAndDerivatives(testProfile, chand, dom, 1e-2, 3e-1));
	}

	@Test
	void testFluxesMediumThickness() {
		quadrature.setQuadraturePoints(derive(QUADRATURE_POINTS, 3));
		var properties = (ThermoOpticalProperties)testCase.getTestProblem().getProperties();
		properties.setOpticalThickness(derive(OPTICAL_THICKNESS, 1.5));
		assertTrue(testCase.testFluxesAndDerivatives(testProfile, chand, dom, 1e-2, 1));
	}
	
	@Test
	void testFluxesHighThickness() {
		quadrature.setQuadraturePoints(derive(QUADRATURE_POINTS, 6));
		var properties = (ThermoOpticalProperties)testCase.getTestProblem().getProperties();
		properties.setOpticalThickness(derive(OPTICAL_THICKNESS, 100.0));
		assertTrue(testCase.testFluxesAndDerivatives(testProfile, chand, dom, 1e-1, Double.POSITIVE_INFINITY));
	}
	
}