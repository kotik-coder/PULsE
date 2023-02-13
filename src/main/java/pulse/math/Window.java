package pulse.math;

import java.io.Serializable;

public interface Window extends Serializable {

    public final static Window NONE = (n, N) -> 1.0;
    public final static Window HANN = (n, N) -> Math.pow(Math.sin(Math.PI * n / ((double) N)), 2);
    public final static Window HAMMING = (n, N) -> 0.54 + 0.46 * Math.cos(2.0 * Math.PI * n / ((double) N));
    public final static Window BLACKMANN_HARRIS = (n, N) -> {
        final double x = 2.0 * Math.PI * n / ((double) N);
        return 0.35875 - 0.48829 * Math.cos(x) + 0.14128 * Math.cos(2.0 * x) - 0.01168 * Math.cos(3.0 * x);
    };
    public final static Window FLAT_TOP = (n, N) -> {
        final double x = 2.0 * Math.PI * n / ((double) N);
        return 0.21557895 - 0.41663158 * Math.cos(x) + 0.277263158 * Math.cos(2.0 * x)
                - 0.083578947 * Math.cos(3.0 * x) + 0.006947368 * Math.cos(4.0 * x);
    };
    public final static Window TUKEY = new Window() {

        private final static double alpha = 0.6;

        @Override
        public double evaluate(int n, int N) {

            double result = 0;

            if (n < 0.5 * alpha * N) {
                result = 0.5 * (1 - Math.cos(2.0 * Math.PI * n / (alpha * N)));
            } else if (n <= N / 2) {
                result = 1.0;
            } else {
                result = TUKEY.evaluate(N - n, N);
            }

            return result;

        }
    };

    public final static Window HANN_POISSON = (n, N) -> {

        final double alpha = 2.0;
        return HANN.evaluate(n, N) * Math.exp(-alpha * (N - 2 * n) / N);

    };

    public default double[] apply(double[] input) {
        double[] output = new double[input.length];
        for (int i = 0; i < output.length; i++) {
            output[i] = input[i] * evaluate(i, input.length);
        }
        return output;
    }

    public abstract double evaluate(int n, int N);

}
