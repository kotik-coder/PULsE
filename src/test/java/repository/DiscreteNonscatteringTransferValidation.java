package repository;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.INTEGRATION_SEGMENTS;
import static pulse.properties.NumericPropertyKeyword.OPTICAL_THICKNESS;
import static repository.TestProfileLoader.loadTestProfileDense;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pulse.problem.schemes.rte.RadiativeTransferSolver;
import pulse.problem.schemes.rte.dom.DiscreteOrdinatesMethod;
import pulse.problem.schemes.rte.exact.NewtonCotesQuadrature;
import pulse.problem.schemes.rte.exact.NonscatteringDiscreteDerivatives;
import pulse.problem.schemes.rte.exact.NonscatteringRadiativeTransfer;
import pulse.problem.statements.ThermoOpticalProperties;

class DiscreteNonscatteringTransferValidation {

	private static List<Double> testProfile;
	private static NonscatteringTestCase testCase;

	private NonscatteringRadiativeTransfer newton;
	private RadiativeTransferSolver dom;

	@BeforeAll
	private static void setUpBeforeClass() {
		testProfile = loadTestProfileDense();
		testCase = new NonscatteringTestCase(testProfile.size(), 10.0);
	}

	@BeforeEach
	void setUp() throws Exception {
		newton = new NonscatteringDiscreteDerivatives(testCase.getTestProblem(), testCase.getTestScheme().getGrid());
		
		var quad = new NewtonCotesQuadrature();
		quad.getIntegrator().setIntegrationSegments(derive(INTEGRATION_SEGMENTS, 1024));
		newton.setQuadrature(quad);
				
		dom = new DiscreteOrdinatesMethod(testCase.getTestProblem(), testCase.getTestScheme().getGrid());
	}

	@Test
	void testFluxesLowThickness() {
		var properties = (ThermoOpticalProperties)testCase.getTestProblem().getProperties();
		properties.setOpticalThickness(derive(OPTICAL_THICKNESS, 0.1));
		assertTrue(testCase.testFluxesAndDerivatives(testProfile, newton, dom, 1e-2, 3e-1));
	}

	@Test
	void testFluxesMediumThickness() {
		var properties = (ThermoOpticalProperties)testCase.getTestProblem().getProperties();
		properties.setOpticalThickness(derive(OPTICAL_THICKNESS, 1.5));
		assertTrue(testCase.testFluxesAndDerivatives(testProfile, newton, dom, 1e-2, 3e-1));
	}

}