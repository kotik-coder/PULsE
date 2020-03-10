package pulse.search.statistics;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import pulse.HeatingCurve;
import pulse.input.ExperimentalData;
import pulse.tasks.SearchTask;
import pulse.util.Reflexive;

public abstract class ResidualStatistic extends Statistic {

	private List<double[]> residuals;
	private static String selectedOptimiserDescriptor;

	public ResidualStatistic() {
		super();
		residuals = new LinkedList<double[]>();
		setPrefix("Residuals");
	}
	
	public double[] transformResiduals(SearchTask task) {
		var residuals = task.getResidualStatistic().getResiduals().stream()
				.map(doubleArray -> doubleArray[1]).mapToDouble(Double::doubleValue).toArray();
		return residuals;
	}
	
	public void calculateResiduals(SearchTask task) {
		HeatingCurve estimate = task.getProblem().getHeatingCurve();
		ExperimentalData reference = task.getExperimentalCurve();
		
		double timeInterval_1 = estimate.timeAt(1) - estimate.timeAt(0); 
		
		int cur;
		double b1, b2;
		double interpolated, diff;	
		
		/*Linear interpolation for the **solution**: 
		 * y* = y1*(x2-x*)/dx + y2*(x*-x1)/dx, 
		 * where y1,x1,y2,x2 are the calculated heating curve points, and 
		 * y* is the interpolated value.  
		*/ 
		
		final double zeroTime = estimate.timeAt(0);

		residuals = new LinkedList<double[]>(); 
		
		for (int startIndex = reference.getIndexRange().getLowerBound(),				
			 endIndex = reference.getIndexRange().getUpperBound(), 
			 i = startIndex; i <= endIndex; i++) {
			/*find the point on the calculated heating curve 
			which has the closest time value smaller than the experimental points' time value*/
			cur 		 = (int) ( (reference.timeAt(i) - zeroTime)/timeInterval_1);			
			
			b1			 = ( estimate.timeAt(cur + 1) - reference.timeAt(i)  ) / timeInterval_1; //(x2 -x*)/dx			
			b2  		 = ( reference.timeAt(i) 	  - estimate.timeAt(cur) ) / timeInterval_1; //(x* -x1)/dx
			interpolated = b1*estimate.temperatureAt(cur) + b2*estimate.temperatureAt(cur + 1);
			
			diff		 = reference.temperatureAt(i) - interpolated; //y_exp - y*
			
			residuals.add(new double[] { reference.timeAt(i), diff });
		
		}				

	}
	
	public List<double[]> getResiduals() {
		return residuals;
	}	
	
	public double residualUpperBound() {
		return residuals.stream().map(array -> array[1]).reduce((a, b) -> b > a ? b : a).get();			
	}
	
	public double residualLowerBound() {
		return residuals.stream().map(array -> array[1]).reduce((a, b) -> a < b ? a : b).get();
	}

	public static void setSelectedOptimiserDescriptor(String selectedTestDescriptor) {
		ResidualStatistic.selectedOptimiserDescriptor = selectedTestDescriptor;
	}
	
	public static String getSelectedOptimiserDescriptor() {
		return selectedOptimiserDescriptor;
	}

}