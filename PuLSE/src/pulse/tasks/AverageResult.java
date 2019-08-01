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
			r.parent = this;
		}
		
		double[] av 	= new double[resultFormat.labels().length];
		double[] std 	= new double[av.length];
		
		properties = new NumericProperty[av.length];
	
		for(int j = 0; j < av.length; j++) 
			properties[j] = new NumericProperty( 
					0.0,
					(NumericProperty) results.get(0).properties()[j]
					);
		
		
		int size = results.size();
		
		NumericProperty[] p;
		
		for(AbstractResult r : results) {
			p = r.properties();
			for(int i = 0; i < av.length; i++)
				av[i] += (double) p[i].getValue();
		}
		
		BigDecimal[] avBig = new BigDecimal[av.length];
		
		for(int j = 0; j < av.length; j++) { 
			av[j]	/= size;
			avBig[j] = new BigDecimal(av[j]);
		}
		
		/*
		 * Calculate standard error std[j] for each column
		 */
		
		for(AbstractResult r : results) {
			p = r.properties();
			for(int j = 0; j < av.length; j++) 
				std[j] += Math.pow( (double) p[j].getValue() - av[j], 2);
		}
		
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
			
			properties[j].setValue(resultAv.doubleValue());
			properties[j].setError(resultStd.doubleValue());		
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
