package pulse.math;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

public class FFTTransformer {

    private double[] amplitudeSpec;
    private double[] phaseSpec;
    
    private int n;  //number of input points
    private Complex[] buffer;

    private Window window;

    public FFTTransformer(double[] realInput) {
        this(Window.HANN, realInput, new double[realInput.length]);
    }
    
    public FFTTransformer(Window window, double[] realInput) {
        this(window, realInput, new double[realInput.length]);
    }

    public FFTTransformer(Window window, double[] realInput, double[] imagInput) {
        this.window = window;
        n = realInput.length;

        if (realInput.length != imagInput.length) {
            throw new IllegalArgumentException(
                    String.format("Invalid data array lengths: %5d and %5d",
                            realInput.length, imagInput.length));
        }

        //if the input array is a power of two, simply make a shallow copy of the input array
        if (IsPowerOfTwo(realInput.length)) {
            buffer = new Complex[realInput.length];
            fill(realInput, imagInput, realInput.length);
        } else {
            int pow2 = numBits(realInput.length);
            int nextPowerOfTwo = (int) Math.pow(2, pow2 + 1);
            int previousPowerOfTwo = (int) Math.pow(2, pow2);

            final double TOLERANCE_FACTOR = 0.25;

            /*
             * if we cut the tails, do we end up removing less elements than the number 
             * of zeros we had to add to reach next power of two?
             */
            if ((nextPowerOfTwo - realInput.length
                    > realInput.length - previousPowerOfTwo)
                    && //in this case, do we have to add too many zeros?
                    (nextPowerOfTwo - realInput.length
                    > TOLERANCE_FACTOR * realInput.length)) {
                cutTails(realInput, imagInput, previousPowerOfTwo);
            } else {
                zeroPad(realInput, imagInput, nextPowerOfTwo);
            }

        }
        //create power and phase arrays
        amplitudeSpec = new double[buffer.length / 2];
        phaseSpec = new double[buffer.length / 2];

    }

    public double[] sampling(double[] x) {
        final double totalTime = x[n - 2] - x[0];
        double[] sample = new double[buffer.length / 2];
        double fs = n/totalTime; //sampling rate 
        for (int i = 0; i < sample.length; i++) {
            sample[i] = i * fs / buffer.length;
        }
        return sample;
    }

    public void transform() {
        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);

        Complex[] result = fft.transform(buffer, TransformType.FORWARD);

        final double _2_N = 2.0 / amplitudeSpec.length;

        amplitudeSpec[0] = result[0].abs() / amplitudeSpec.length;
        phaseSpec[0] = result[0].getArgument();

        for (int i = 1; i < amplitudeSpec.length; i++) {
            amplitudeSpec[i] = _2_N * result[i].abs();
            phaseSpec[i] = result[i].getArgument();
        }

    }

    private void fill(double[] realInput, double[] imagInput, int size) {
        for (int i = 0; i < size; i++) {
            buffer[i] = new Complex(
                    window.evaluate(i, realInput.length) * realInput[i],
                    imagInput[i]);
        }
    }

    private void cutTails(double[] realInput, double[] imagInput, int previousPowerOfTwo) {
        buffer = new Complex[previousPowerOfTwo];
        fill(realInput, imagInput, previousPowerOfTwo);
    }

    private void zeroPad(double[] realInput, double[] imagInput, int nextPowerOfTwo) {
        buffer = new Complex[nextPowerOfTwo];
        fill(realInput, imagInput, realInput.length);
        for (int i = realInput.length; i < nextPowerOfTwo; i++) {
            buffer[i] = new Complex(0.0, 0.0);
        }
    }

    /**
     * Checks if the argument (positive integer) is a power of 2. Returns trues
     * if the argument is zero.
     */
    private static boolean IsPowerOfTwo(int x) {
        return x > 0 && ((x & (x - 1)) == 0);
    }

    private int numBits(int value) {
        return (int) (Math.log(value) / Math.log(2));
    }

    public double[] getAmpltiudeSpectrum() {
        return amplitudeSpec;
    }

    public double[] getPhaseSpectrum() {
        return phaseSpec;
    }
    
}