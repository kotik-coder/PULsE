package pulse.tasks;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import pulse.properties.NumericProperty;

public class AverageResult extends AbstractResult {
	
	private List<AbstractResult> results;

	public AverageResult(List<AbstractResult> res, ResultFormat resultFormat) {
		super(resultFormat);
		
		results = new ArrayList<AbstractResult>();
		
		for(AbstractResult r : res) {
			results.add(r);
			r.setParent(this);
		}
		
		double[] av 	= new double[resultFormat.abbreviations().size()];
		double[] std 	= new double[av.length];
	
		for(int j = 0; j < av.length; j++) 
			addProperty(new NumericProperty( 
					0.0,
					(NumericProperty) results.get(0).getProperty(j)
					));
		
		
		int size = results.size();
		
		for(AbstractResult r : results) 
			for(int i = 0; i < av.length; i++)
				av[i] += ((Number)r.getProperty(i).getValue()).doubleValue();
		
		BigDecimal[] avBig = new BigDecimal[av.length];
		
		for(int j = 0; j < av.length; j++) { 
			av[j]	/= size;
			avBig[j] = new BigDecimal(av[j]);
		}
		
		/*
		 * Calculate standard error std[j] for each column
		 */
		
		for(AbstractResult r : results) 
			for(int j = 0; j < av.length; j++)  
				std[j] += Math.pow( 
						((Number) r.getProperty(j).getValue()).doubleValue() 
						- av[j], 2);
		
		BigDecimal[] stdBig = new BigDecimal[av.length];
		int numFigures		= 2;
		int power 			= 0;
		
		BigDecimal resultAv, unitStd, resultStd;
		
		for(int j = 0; j < stdBig.length; j++) {
			std[j] 		= Math.sqrt( std[j] / ( (av.length - 1)*av.length ) );
			stdBig[j]	= new BigDecimal(std[j]);
			
			power		= stdBig[j].precision() - numFigures;
			
			unitStd		= stdBig[j].ulp().scaleByPowerOfTen(power);
			resultStd	= stdBig[j].divideToIntegralValue(unitStd).multiply(unitStd);
			resultAv	= avBig[j].setScale(unitStd.scale(), RoundingMode.CEILING);
			
			getProperty(j).setValue(resultAv.doubleValue());
			getProperty(j).setError(resultStd.doubleValue());		
		}
		
	}
	
	public List<AbstractResult> getIndividualResults() {
		List<AbstractResult> indResults = new ArrayList<AbstractResult>();
		
		for(AbstractResult r : results) {
			if(r instanceof AverageResult) {
				AverageResult ar = (AverageResult)r;
				indResults.addAll(ar.getIndividualResults());
			}
			else 
				indResults.add(r);
		}
		
		return indResults;
	}

}
