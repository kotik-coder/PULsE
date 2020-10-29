package pulse.properties;

import java.util.Arrays;
import java.util.Optional;

/**
 * Contains a list of NumericProperty types recognised by the constituent modules of PULsE.
 *
 */

public enum NumericPropertyKeyword {

	/**
	 * The thermal diffusivity of the sample.
	 */

	DIFFUSIVITY,
	
	/**
	 * Not implemented yet.
	 */

	COATING_DIFFUSIVITY,

	/**
	 * Sample thickness.
	 */

	THICKNESS,

	/**
	 * Sample diameter.
	 */

	DIAMETER,

	/**
	 * The maximum temperature, or the signal height -- if a relative temperature
	 * scale is used.
	 */

	MAXTEMP,

	/**
	 * The number of points in the {@code HeatingCurve}.
	 */

	NUMPOINTS,

	/**
	 * The precision parameter used to solve nonlinear problems.
	 */

	NONLINEAR_PRECISION,

	/**
	 * Pulse width (time).
	 */

	PULSE_WIDTH,

	/**
	 * Laser spot diameter.
	 */

	SPOT_DIAMETER,

	/**
	 * Calculation time limit.
	 */

	TIME_LIMIT,

	/**
	 * Grid (space partitioning) density.
	 */

	GRID_DENSITY, 
	
	/**
	 * Not implemented yet.
	 */

	
	SHELL_GRID_DENSITY,

	/**
	 * Not implemented yet.
	 */

	
	AXIAL_COATING_THICKNESS,

	/**
	 * Not implemented yet.
	 */
	
	RADIAL_COATING_THICKNESS,

	/**
	 * Specific heat (<math><i>C</i><sub>p</sub></math>).
	 */

	SPECIFIC_HEAT,

	/**
	 * Thermal conductivity.
	 */

	CONDUCTIVITY,

	/**
	 * Emissivity of the sample (nonlinear problems).
	 */

	EMISSIVITY,

	/**
	 * Density of the sample.
	 */

	DENSITY,

	/**
	 * Absorbed energy (nonlinear problems).
	 */

	LASER_ENERGY,

	/**
	 * Test temperature, at which the laser was fired at the sample.
	 */

	TEST_TEMPERATURE,

	/**
	 * The resolution of linear search.
	 */

	LINEAR_RESOLUTION,

	/**
	 * The accuracy of gradient calculation.
	 */

	GRADIENT_RESOLUTION,

	/**
	 * The buffer size that is used to establish whether results are converging.
	 */

	BUFFER_SIZE,

	/**
	 * The total error tolerance.
	 */

	ERROR_TOLERANCE,

	/**
	 * The outer field of view diameter for the detector sighting.
	 */

	FOV_OUTER,

	/**
	 * The inner field of view diameter for the detector sighting.
	 */

	FOV_INNER,

	/**
	 * The baseline slope (for linear baselines).
	 */

	BASELINE_SLOPE,

	/**
	 * Frequency of the sinusoidal baseline.
	 */

	BASELINE_FREQUENCY,

	/**
	 * Phase shift of the sinusoidal baseline.
	 */

	BASELINE_PHASE_SHIFT,

	/**
	 * Amplitude of the sinusoidal baseline.
	 */

	BASELINE_AMPLITUDE,

	/**
	 * The baseline intercept value.
	 */

	BASELINE_INTERCEPT,

	/**
	 * The factor used to convert squared grid spacing to time step.
	 */

	TAU_FACTOR,

	/**
	 * The detector gain (amplification).
	 */

	DETECTOR_GAIN,

	/**
	 * The detector iris (aperture).
	 */

	DETECTOR_IRIS,

	/**
	 * The coefficient of heat losses from the side surface of the sample
	 * (two-dimensional problems).
	 */

	HEAT_LOSS_SIDE,

	/**
	 * A general keyword for the coefficient of heat losses. Indicates primarily
	 * those on the front and rear faces.
	 */

	HEAT_LOSS,

	/**
	 * Search iteration.
	 */

	ITERATION,

	/**
	 * Task identifier.
	 */

	IDENTIFIER,

	/**
	 * Iteration limit for reverse problem solution.
	 */

	ITERATION_LIMIT,

	/**
	 * Dimensionless coefficient of laser energy absorption (&gamma;<sub>0</sub>).
	 */

	LASER_ABSORPTIVITY,

	/**
	 * Dimensionless coefficient of thermal radiation absorption.
	 */	
	
	THERMAL_ABSORPTIVITY,

	/**
	 * Reflectance of the sample (0 &lt; R &le; 1).
	 */

	REFLECTANCE,

	/**
	 * A dimensionless coefficient in the radiation flux expression for the
	 * radiative heat transfer between the front and the rear (coated) surfaces.
	 * Used by the <code>DiathermicMaterialProblem</code>.
	 */

	DIATHERMIC_COEFFICIENT,
	
	/**
	 * The Planck number.
	 */

	PLANCK_NUMBER,
	
	/**
	 * The optical thickness of a material, equal to a product of its geometric thickness and the absorptivity.
	 */

	OPTICAL_THICKNESS,
	
	/**
	 * Time shift (pulse sync)
	 */

	TIME_SHIFT,
	
	/**
	 * Statistical significance.
	 */

	SIGNIFICANCE,
	
	/**
	 * Statistical probability.
	 */

	PROBABILITY,
	
	/**
	 * Optimiser statistic (usually, RSS).
	 */

	OPTIMISER_STATISTIC,
	
	/**
	 * Model selection criterion (AIC, BIC, etc.)
	 */
	
	MODEL_CRITERION,
	
	/**
	 * Test statistic (e.g. normality test criterion).
	 */

	TEST_STATISTIC,
	
	/**
	 * Lower calculation bound for optimiser.
	 */

	LOWER_BOUND,
	
	/**
	 * Upper calculation bound for optimiser.
	 */

	UPPER_BOUND,

	/**
	 * Averaging window.
	 */
	
	WINDOW,
	
	/**
	 * Intensity of incident radiation.
	 */

	INCIDENT_INTENSITY,
	
	/**
	 * Threshold above which properties are thought to be strongly correlated.
	 */

	CORRELATION_THRESHOLD,
	
	/**
	 * Number of subdivisions for numeric integration.
	 */

	INTEGRATION_SEGMENTS,
	
	/**
	 * Cutoff for numeric integration.
	 */

	INTEGRATION_CUTOFF,
	
	/**
	 * Weight of the semi-implicit finite-difference scheme.
	 */

	SCHEME_WEIGHT,
	
	/**
	 * Number of quadrature points (RTE).
	 */

	QUADRATURE_POINTS,
	
	/**
	 * Albedo of single scattering.
	 */

	SCATTERING_ALBEDO,
	
	/**
	 * Anisotropy coefficient for the phase function of scattering.
	 */

	SCATTERING_ANISOTROPY,
	
	/**
	 * Iteration error tolerance in DOM calculations.
	 */

	DOM_ITERATION_ERROR,
	
	/**
	 * Number of independent directions (DOM).
	 */

	DOM_DIRECTIONS,
	
	/**
	 * Error tolerance for the Laguerre solver (RTE).
	 */

	LAGUERRE_SOLVER_ERROR,
	
	/**
	 * Absolute tolerance (atol) for RK calculations (RTE).
	 */

	ATOL,

	/**
	 * Relative tolerance (atol) for RK calculations (RTE).
	 */
	
	RTOL,
	
	/**
	 * Grid scaling factor.
	 */

	GRID_SCALING_FACTOR,
	
	/**
	 * Internal DOM grid density (RTE).
	 */

	DOM_GRID_DENSITY,

	/**
	 * Grid stretching factor (RTE).
	 */
	
	GRID_STRETCHING_FACTOR,
	
	/**
	 * Relaxation parameter of iterative solver (RTE).
	 */

	RELAXATION_PARAMETER,
	
	/**
	 * Iteration threshold for RTE calculations.
	 */

	RTE_MAX_ITERATIONS,
	
	/**
	 * Maximum allowed time spent on integration (RTE).
	 */

	RTE_INTEGRATION_TIMEOUT,

	/**
	 * Percentage of initial (rise) segment of the pulse trapezoid.
	 */
	
	TRAPEZOIDAL_RISE_PERCENTAGE,
	
	/**
	 * Percentage of final (fall) segment of the pulse trapezoid.
	 */

	TRAPEZOIDAL_FALL_PERCENTAGE,

	/**
	 * &mu; parameter for skewed normal distribution.
	 */
	
	SKEW_MU,

	/**
	 * &sigma; parameter for skewed normal distribution.
	 */
	
	SKEW_SIGMA,

	/**
	 * &lambda; parameter for skewed normal distribution.
	 */
	
	SKEW_LAMBDA,
	
	/**
	 * A weight indicating how good a calculation model is.
	 */
	
	MODEL_WEIGHT,
	
	/**
	 * Levenberg-Marquardt damping ratio. A zero value presents pure Levenberg damping. A value of 1 gives pure Marquardt damping.
	 */
	
	DAMPING_RATIO;

	public static Optional<NumericPropertyKeyword> findAny(String key) {
		return Arrays.asList(values()).stream().filter(keys -> keys.toString().equalsIgnoreCase(key)).findAny();
	}

}