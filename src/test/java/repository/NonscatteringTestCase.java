package repository;

import static java.lang.Math.abs;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.DENSITY;
import static pulse.properties.NumericPropertyKeyword.GRID_DENSITY;
import static pulse.properties.NumericPropertyKeyword.LASER_ENERGY;
import static pulse.properties.NumericPropertyKeyword.SCATTERING_ALBEDO;
import static pulse.properties.NumericPropertyKeyword.SPECIFIC_HEAT;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;

import java.util.List;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.rte.RadiativeTransferSolver;
import pulse.problem.schemes.solvers.ImplicitCoupledSolver;
import pulse.problem.statements.ParticipatingMedium;
import pulse.problem.statements.Pulse2D;
import pulse.problem.statements.model.ThermoOpticalProperties;

public class NonscatteringTestCase {

	private ParticipatingMedium testProblem;
	private DifferenceScheme testScheme;
	
	public NonscatteringTestCase(final int testProfileSize, final double maxHeating) {
		testProblem = new ParticipatingMedium();
		var properties = (ThermoOpticalProperties)testProblem.getProperties();
		properties.setSpecificHeat(derive(SPECIFIC_HEAT, 300.0));
		properties.setDensity(derive(DENSITY, 10000.0));
		testProblem.getPulse().setLaserEnergy(derive(LASER_ENERGY, 1.0));

		final double factor = maxHeating / properties.maximumHeating((Pulse2D)testProblem.getPulse());
		testProblem.getPulse().setLaserEnergy(derive(LASER_ENERGY, factor));

		properties.setTestTemperature(derive(TEST_TEMPERATURE, 800.0));
		properties.setScatteringAlbedo(derive(SCATTERING_ALBEDO, 0.0));

		testScheme = new ImplicitCoupledSolver();
		var grid = testScheme.getGrid();
		grid.setGridDensity(derive(GRID_DENSITY, testProfileSize - 1));

		System.out.printf("Testing at: maximum heating: %3.4f and a test temperature of %5.2f %n",
				properties.maximumHeating((Pulse2D)testProblem.getPulse()), properties.getTestTemperature().getValue());
	}

	public ParticipatingMedium getTestProblem() {
		return testProblem;
	}

	public DifferenceScheme getTestScheme() {
		return testScheme;
	}

	public static boolean approximatelyEquals(final double a, final double b, final double diff) {
		return 2.0 * abs(a - b) / (abs(a + b)) < diff;
	}

	private void compute(List<Double> testProfile, RadiativeTransferSolver... solvers) {
		for (RadiativeTransferSolver rte : solvers) {
			rte.init(testProblem, testScheme.getGrid());
			rte.compute(testProfile.stream().mapToDouble(d -> d).toArray());
		}

	}

	public boolean testFluxesAndDerivatives(List<Double> testProfile, RadiativeTransferSolver solver1, 
			RadiativeTransferSolver solver2, double margin1, double margin2) {
		boolean pass = true;

		compute(testProfile, solver1, solver2);

		System.out.printf("%nTest at tau0 = %4.2f with the following margins: %1.0e and %1.0e %n",
				( (ThermoOpticalProperties)testProblem.getProperties() ).getOpticalThickness().getValue(), margin1, margin2);

		var fluxes1 = solver1.getFluxes();
		var fluxes2 = solver2.getFluxes();

		for (int i = 1, size = testProfile.size(); i < size - 1 && pass; i++) {
			pass &= approximatelyEquals(fluxes1.getFlux(i), fluxes2.getFlux(i), margin1);
			pass &= approximatelyEquals(fluxes1.fluxDerivative(i), fluxes2.fluxDerivative(i), margin2);
			System.out.printf("%1.4e ; % 1.4e %1.4e ; % 1.4e %n", fluxes1.getFlux(i), fluxes2.getFlux(i),
					fluxes1.fluxDerivative(i), fluxes2.fluxDerivative(i));
		}
		System.out.println(pass ? "SUCCESS" : "FAILED");
		return pass;
	}

}