package pulse.search.statistics;

public abstract class OptimiserStatistic extends ResidualStatistic {

	private static String selectedOptimiserDescriptor;
	
	public OptimiserStatistic(OptimiserStatistic stat) {
		super(stat);
	}
	
	protected OptimiserStatistic() {
		super();
	}
	
	public static String getSelectedOptimiserDescriptor() {
		return selectedOptimiserDescriptor;
	}
	
	public static void setSelectedOptimiserDescriptor(String selectedTestDescriptor) {
		OptimiserStatistic.selectedOptimiserDescriptor = selectedTestDescriptor;
	}
	
	public abstract OptimiserStatistic copy();
	public abstract double variance();

}