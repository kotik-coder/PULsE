/*
 * Copyright 2021 Artem Lunev <artem.v.lunev@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pulse.search.statistics;

import org.apache.commons.math3.distribution.FDistribution;
import pulse.tasks.Calculation;

/**
 * A static class for testing two calculations based on the Fischer test (F-Test)
 * implemented in Apache Commons Math.
 * @author Artem Lunev <artem.v.lunev@gmail.com>
 */
public class FTest {
    
    /**
     * False-rejection probability for the F-test, equal to {@value FALSE_REJECTION_PROBABILITY}
     */
    
    public final static double FALSE_REJECTION_PROBABILITY = 0.05;
    
    private FTest() {
        //intentionall blank
    }
    
    /**
     * Tests two models to see which one is better according to the F-test
     * @param a a calculation
     * @param b another calculation
     * @return {@code null} if the result is inconclusive, otherwise the 
     * best of two calculations.
     * @see FTest.evaluate()
     */
     
    public static Calculation test(Calculation a, Calculation b) {
    
        double[] data = evaluate(a, b);
        
        Calculation best = null; 
        
        if(data != null) {
        
            //Under the null hypothesis the general model does not provide 
            //a significantly better fit than the nested model
            
            Calculation nested = findNested(a, b);
            
            //if the F-statistic is greater than the F-critical, reject the null hypothesis.
            
            if(nested == a)
                best = data[0] > data[1] ? b : a;
            else
                best = data[0] > data[1] ? a : b;
        
        }
        
        return best;
       
    }
       
    /**
     * Evaluates the F-statistic for two calculations. 
     * @param a a calculation
     * @param b another calculation
     * @return {@code null} if the test is inconclusive, i.e., if models are not 
     * nested, or if the model selection criteria are based on a statistic different
     * from least-squares, or if the calculations refer to different data ranges.
     * Otherwise returns an double array, consisting of two elements {@code [fStatistic, fCritical] }
     */
    
    public static double[] evaluate(Calculation a, Calculation b) {
        
        Calculation nested = findNested(a, b);
        
        double[] result = null;
        
        //if one of the models is nested into the other
        if(nested != null) {            
            Calculation general = nested == a ? b : a;
            
            ResidualStatistic nestedResiduals = nested.getModelSelectionCriterion().getOptimiserStatistic();
            ResidualStatistic generalResiduals = general.getModelSelectionCriterion().getOptimiserStatistic();            

            final int nNested = nestedResiduals.getResiduals().size(); //sample size
            final int nGeneral = generalResiduals.getResiduals().size(); //sample size
            
            //if both models use a sum-of-square statistic for the model selection criteria
            //and if both calculations refer to the same calculation range
            if(nestedResiduals.getClass() == generalResiduals.getClass() 
                    && nestedResiduals.getClass() == SumOfSquares.class 
                    && nNested == nGeneral) {
            
                double rssNested  = ( (Number) ((SumOfSquares)nestedResiduals).getStatistic().getValue() ).doubleValue();
                double rssGeneral = ( (Number) ((SumOfSquares)generalResiduals).getStatistic().getValue() ).doubleValue();
                
                int kGeneral    = general.getModelSelectionCriterion().getNumVariables();
                int kNested     = nested.getModelSelectionCriterion().getNumVariables();
                
                double fStatistic = (rssNested - rssGeneral)
                                    /(kGeneral - kNested)
                                    /(rssGeneral/(nGeneral - kGeneral)); 
                
                var fDistribution = new FDistribution(kGeneral - kNested, nGeneral - kGeneral);
                
                double fCritical = fDistribution.inverseCumulativeProbability(1.0 - FALSE_REJECTION_PROBABILITY);
                
                result = new double[]{fStatistic, fCritical};
                
            }
        
        }
        
        return result;
        
    }
    
    /**
     * Tests two models to see which one is nested in the other. A model is 
     * considered nested if it refers to the same class of problems but has 
     * fewer parameters.
     * @param a a calculation
     * @param b another calculation
     * @return {@code null} if the models refer to different problem classes.
     * Otherwise returns the model that is nested in the second model.
     */
    
    public static Calculation findNested(Calculation a, Calculation b) {
      if(a.getProblem().getClass() != b.getProblem().getClass())
          return null;
      
      int aParams = a.getModelSelectionCriterion().getNumVariables();
      int bParams = b.getModelSelectionCriterion().getNumVariables();
      
      return aParams > bParams ? b : a;
    }
    
}