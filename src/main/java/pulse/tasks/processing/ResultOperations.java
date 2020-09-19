package pulse.tasks.processing;

import java.util.List;

class ResultOperations {
	
	private static double[] av;
	private static double[] dev;
	
	public static ResultProcessing average = ( (p, i) -> ((Number) p.getValue()).doubleValue());
	
	public static ResultProcessing stdev = (p, i) -> { 
		double x = ( (Number) p.getValue() ).doubleValue() - av[i];
		return x*x;	
	};
	
	private ResultOperations() {
		//intentionaly blank
	}
	
	public static void process(List<AbstractResult> results, final int properties) {
		av = average.process(results, properties);
		dev = stdev.process(results, properties);
	}

	public static double[] getAverages() {
		return av;
	}

	public static double[] getDeviations() {
		return dev;
	}
	
}