package pulse.problem.schemes;

import java.util.stream.IntStream;

import pulse.problem.statements.model.AbsorptionModel;
import pulse.problem.statements.model.SpectralRange;

/**
 * An interface providing the ability to calculate the integral signal out from
 * a finite-depth material layer. The depth is governed by the current
 * {@code AbsorptionModel}.
 *
 */
public class DistributedDetection {

    /**
     * Calculates the effective signal registered by the detector, which takes
     * into account a distributed emission pattern. The emissivity is assumed
     * equal to the average absorptivity in the thermal region of the spectrum,
     * as per the Kirchhoff's law.
     *
     * @param absorption the absorption model
     * @param V the current time-temperature profile
     * @return the effective detector signal (arbitrary units)
     */
    public static double evaluateSignal(final AbsorptionModel absorption, final Grid grid, final double[] V) {
        final double hx = grid.getXStep();
        final int N = grid.getGridDensityValue();

        double signal = IntStream.range(0, N)
                .mapToDouble(i -> V[N - i] * absorption.absorption(SpectralRange.THERMAL, i * hx)
                + V[N - 1 - i] * absorption.absorption(SpectralRange.THERMAL, (i + 1) * hx))
                .reduce((a, b) -> a + b).getAsDouble();

        return signal * 0.5 * hx;
    }

}