package repository;

import static java.lang.Math.abs;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericPropertyKeyword.DENSITY;
import static pulse.properties.NumericPropertyKeyword.GRID_DENSITY;
import static pulse.properties.NumericPropertyKeyword.OPTICAL_THICKNESS;
import static pulse.properties.NumericPropertyKeyword.QUADRATURE_POINTS;
import static pulse.properties.NumericPropertyKeyword.SCATTERING_ALBEDO;
import static pulse.properties.NumericPropertyKeyword.SPECIFIC_HEAT;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pulse.problem.schemes.rte.FluxesAndExplicitDerivatives;
import pulse.problem.schemes.rte.dom.DiscreteOrdinatesMethod;
import pulse.problem.schemes.rte.exact.ChandrasekharsQuadrature;
import pulse.problem.schemes.rte.exact.NonscatteringAnalyticalDerivatives;
import pulse.problem.schemes.rte.exact.NonscatteringRadiativeTransfer;
import pulse.problem.schemes.solvers.ImplicitCoupledSolver;
import pulse.problem.statements.ParticipatingMedium;

class RadiativeTransferCrossVerification {

	private static List<Double> testProfile;

	private FluxesAndExplicitDerivatives analyticalFluxes;
	private FluxesAndExplicitDerivatives domFluxes;

	@BeforeAll
	private static void setUpBeforeClass() {
		File test = null;
		try {
			test = new File(NonscatteringRadiativeTransfer.class.getResource("/test/TestSolution.dat").toURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		Scanner scanner = null;
		try {
			scanner = new Scanner(test);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		testProfile = new ArrayList<Double>();

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			var numbersStrings = line.split(" ");
			testProfile.add(Double.valueOf(numbersStrings[0]));
		}

		scanner.close();
	}
	
	@BeforeEach
	void setUp() throws Exception {
		var testProblem = new ParticipatingMedium();
		testProblem.setSpecificHeat(derive(SPECIFIC_HEAT, 540.0));
		testProblem.setDensity(derive(DENSITY, 10000.0));
		testProblem.setTestTemperature(derive(TEST_TEMPERATURE, 800.0));
		testProblem.setOpticalThickness(derive(OPTICAL_THICKNESS, 0.1));
		testProblem.setScatteringAlbedo(derive(SCATTERING_ALBEDO, 0.0));

		var testScheme = new ImplicitCoupledSolver();
		int size = testProfile.size();
		testScheme.getGrid().setGridDensity(derive(GRID_DENSITY, size - 1));
		
		System.out.printf("Testing at: maximum heating: %3.4f and a test temperature of %5.2f %n",
				testProblem.maximumHeating(), testProblem.getTestTemperature().getValue());

		var analyticalDerivatives = new NonscatteringAnalyticalDerivatives(testProblem, testScheme.getGrid());
		// rad.setQuadrature(new NewtonCotesQuadrature());
		var quad = new ChandrasekharsQuadrature();
		quad.setQuadraturePoints(derive(QUADRATURE_POINTS, 2));
		analyticalDerivatives.setQuadrature(quad);
		var dom = new DiscreteOrdinatesMethod(testProblem, testScheme.getGrid());

		/*
		 * Compute all
		 */
		
		analyticalDerivatives.compute(testProfile.stream().mapToDouble(d -> d).toArray());
		dom.compute(testProfile.stream().mapToDouble(d -> d).toArray());

		analyticalFluxes = (FluxesAndExplicitDerivatives)analyticalDerivatives.getFluxes();
		domFluxes = (FluxesAndExplicitDerivatives)dom.getFluxes();
	}

	@Test
	void testFluxes() {
		boolean pass = true;

		final double margin = 1E-3;
		System.out.printf("%nTesting fluxes with a margin of %1.0e %n", margin);
		
		for (int i = 1, size = testProfile.size(); i < size - 1 && pass; i++) {
			System.out.printf("%1.4e ; % 1.4e %n",analyticalFluxes.getFlux(i), domFluxes.getFlux(i));
			pass &= approximatelyEquals(analyticalFluxes.getFlux(i), domFluxes.getFlux(i), margin);
		}

		assertTrue(pass);
	}

	@Test
	void testDerivatives() {
		boolean pass = true;

		final double margin = 1E-1;
		System.out.printf("%nTesting flux derivatives with a margin of %1.0e %n", margin);
		
		for (int i = 1, size = testProfile.size(); i < size - 1 && pass; i++) {
			pass &= approximatelyEquals(analyticalFluxes.fluxDerivative(i), domFluxes.fluxDerivative(i), margin);
			System.out.printf("%1.4e ; % 1.4e %n",analyticalFluxes.getFluxDerivative(i), domFluxes.getFluxDerivative(i));
		}

		assertTrue(pass);
	}

	public static boolean approximatelyEquals(final double a, final double b, final double diff) {
		return abs(a) - abs(b) <= diff;
	}

}