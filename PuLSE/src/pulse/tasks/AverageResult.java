package pulse.tasks;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;

public class AverageResult extends AbstractResult {
	
	private List<AbstractResult> results;
	
	public final static int SIGNIFICANT_FIGURES	= 1;

	public AverageResult(List<AbstractResult> res, ResultFormat resultFormat) {
		super(resultFormat);
		
		results = new ArrayList<AbstractResult>();
		
		for(AbstractResult r : res) {
			results.add(r);
			r.setParent(this);
		}
		
		average();
		
	}
	
	private void average() {
		/*
		 * Calculate average
		 */
		
		double[] av			= new double[getFormat().abbreviations().size()]; 
		
		int size = results.size();
				  
		for(int i = 0; i < av.length; i++) {
			for(AbstractResult r : results) 
				av[i] += ((Number)r.getProperty(i).getValue()).doubleValue();			
			av[i] 	/= size;
		}
		
		/*
		 * Calculate standard error std[j] for each column
		 */
		
		double[] std 		= new double[av.length];
		 
		for(int j = 0; j < av.length; j++) {
			for(AbstractResult r : results) 
				std[j] += Math.pow( 
						((Number) r.getProperty(j).getValue()).doubleValue() 
						- av[j], 2);
			std[j] /= size;
		}
		
		/*
		 * Round up
		 */

		BigDecimal resultAv, resultStd, stdBig, avBig;
		
		NumericProperty p;
		NumericPropertyKeyword key;
		
		for(int j = 0; j < av.length; j++) {
			key = getFormat().shortNames().get(j);
			
			if(!Double.isFinite(std[j])) 
				p = NumericProperty.derive(key, av[j]); //ignore error as the value is not finite			
			else {
				stdBig  = (new BigDecimal(std[j])).sqrt(MathContext.DECIMAL64);
				avBig	= new BigDecimal(av[j]);
				
				resultStd	= stdBig.setScale(SIGNIFICANT_FIGURES 
						- stdBig.precision() + stdBig.scale(), RoundingMode.HALF_UP);
				
				if(stdBig.precision() > 1)
					resultAv	= avBig.setScale(resultStd.scale(), RoundingMode.CEILING);
				else
					resultAv	= avBig; 
				
				p = NumericProperty.derive(	key, resultAv.doubleValue() );
				p.setError(resultStd.doubleValue());
				
			}
			
			addProperty(p);
				
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
