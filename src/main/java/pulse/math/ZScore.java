package pulse.math;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/**
 * This class finds peaks in data using the Z-score algorithm:
 * https://en.wikipedia.org/wiki/Standard_score This splits the data into a
 * number of population defined by the 'lag' number. A standard score is
 * calculated as the difference of the current value and population mean divided
 * by the population standard deviation.
 */

public class ZScore {
    
    private double[] avgFilter;
    private double[] stdFilter;
    private int[] signals;
    
    private int lag;
    private double threshold;
    private double influence;
    
    public ZScore(int lag, double threshold, double influence) {
        this.lag = lag;
        this.threshold = threshold;
        this.influence = influence;
    }
    
    public ZScore() {
        this(40, 3.5, 0.3);
    }

    public void process(double[] input) {

        signals = new int[input.length];
        List<Double> filteredY = DoubleStream.of(input).boxed().collect(Collectors.toList());
       
        var initialWindow = filteredY.subList(input.length - lag, input.length - 1);

        avgFilter = new double[input.length];
        stdFilter = new double[input.length];
        
        avgFilter[input.length - lag + 1] = mean(initialWindow);
        stdFilter[input.length - lag + 1] = stdev(initialWindow);

        for (int i = input.length - lag; i > 0; i--) {
            
            if (Math.abs(input[i] - avgFilter[i + 1]) > threshold * stdFilter[i + 1]) {
                
                signals[i] = (input[i] > avgFilter[i + 1]) ? 1 : -1;
                filteredY.set(i, influence * input[i] 
                                 + (1 - influence) * filteredY.get(i + 1));
            
            } else {
            
                signals[i] = 0;
                filteredY.set(i, input[i]);
            
            }

            // Update rolling average and deviation
            var slidingWindow = filteredY.subList(i, i + lag - 1);

            avgFilter[i] = mean(slidingWindow);
            stdFilter[i] = stdev(slidingWindow);
        }

    }

    private static double mean(List<Double> list) {
        return list.stream().mapToDouble(d -> d).average().getAsDouble();
    }

    private static double stdev(List<Double> values) {
        double ret = 0;
        int size = values.size();
        if (size > 0) {
            double avg = mean(values);
            double sum = values.stream().mapToDouble(d -> Math.pow(d - avg, 2)).sum();
            ret = Math.sqrt(sum / (size - 1));
        }
        return ret;
    }
    
    public int[] getSignals() {
        return signals;
    }
    
    public double[] getFilteredAverage() {
        return avgFilter;
    }
    
    public double[] getFilteredStdev() {
        return stdFilter;
    }
    
    /*
    public static void main(String[] args) {
        Scanner sc = null;
        try {
            sc = new Scanner(new File("fft.txt"));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ZScore.class.getName()).log(Level.SEVERE, null, ex);
        }
 
        // we just need to use \\Z as delimiter
        sc.useDelimiter("\\n");
        
        var list = new ArrayList<Double>();
        
        while(sc.hasNext()) {
            list.add(sc.nextDouble());
        }
        
        var zscore = new ZScore();
        zscore.process(list.stream().mapToDouble(d -> d).toArray());
        var signals = zscore.getSignals();
        
        for(int i = 0; i < signals.length; i++) {
            System.out.println(list.get(i) + " " + signals[i]);
        }
        
    }
    */
      
}