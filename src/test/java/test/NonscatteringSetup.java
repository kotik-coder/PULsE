package test;

import static java.lang.Math.abs;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.DENSITY;
import static pulse.properties.NumericPropertyKeyword.GRID_DENSITY;
import static pulse.properties.NumericPropertyKeyword.LASER_ENERGY;
import static pulse.properties.NumericPropertyKeyword.SCATTERING_ALBEDO;
import static pulse.properties.NumericPropertyKeyword.SPECIFIC_HEAT;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;

import java.util.List;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.rte.RadiativeTransferSolver;
import pulse.problem.schemes.solvers.ImplicitCoupledSolver;
import pulse.problem.statements.ParticipatingMedium;
import pulse.problem.statements.Pulse2D;
import pulse.problem.statements.model.ThermoOpticalProperties;

public class NonscatteringSetup {

	private ParticipatingMedium testProblem;
	private DifferenceScheme testScheme;
	
	public NonscatteringSetup(final int testProfileSize, final double maxHeating) {
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

		final int n = testProfile.size();
		double[][] test1 = new double[n][2];
		double[][] test2 = new double[n][2];
		
		System.out.printf("%12s ; %12s ; %12s ; %12s %n", "Flux (Solver1)", "Flux (Solver2)", "Derivative (Solver1)", "Derivative (Solver2)");
		for (int i = 1, size = testProfile.size(); i < size - 1 && pass; i++) {
			test1[i][0] = fluxes1.getFlux(i);
			test1[i][1] = fluxes2.getFlux(i);
			
			test2[i][0] = fluxes1.fluxDerivative(i);
			test2[i][1] = fluxes2.fluxDerivative(i);
			
			System.out.printf("%1.4e ; %1.4e %1.4e ; %1.4e %n", test1[i][0], test1[i][1], test2[i][0], test2[i][1]);
		}
		
		
		var regression1 = new SimpleRegression();
		regression1.addData(test1);
		double rsq1 = regression1.getRSquare();
		System.out.println("R-Squared of linear regression for fluxes: " + rsq1);
		
		var regression2 = new SimpleRegression();
		regression2.addData(test2);
		double rsq2 = regression2.getRSquare();
		System.out.println("R-Squared of linear regression for derivatives: " + rsq2);
		
		pass &= approximatelyEquals(rsq1, 1.0, margin1);
		pass &= approximatelyEquals(rsq2, 1.0, margin2);
		
		System.out.println(pass ? "SUCCESS" : "FAILED");
		return pass;
	}

}