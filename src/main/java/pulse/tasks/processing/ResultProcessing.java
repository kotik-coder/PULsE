package pulse.tasks.processing;

import java.util.List;

import pulse.properties.NumericProperty;

interface ResultProcessing {
	
	public default double[] process(List<AbstractResult> results, final int properties) {		
		final int size = results.size();
		double[] av = new double[properties];

		for (int i = 0; i < av.length; i++) {
			for (AbstractResult r : results) {
				av[i] += value(r.getProperty(i), i);
			}
			av[i] /= size;
		}
		return av;
	}
	
	public abstract double value(NumericProperty p, int i);
	
}