package pulse;

import java.util.ArrayList;
import java.util.List;

import pulse.input.ExperimentalData;
import pulse.properties.EnumProperty;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.tasks.SearchTask;
import pulse.util.PropertyHolder;
import pulse.util.UpwardsNavigable;

public class Baseline extends PropertyHolder {

	private double slope, intercept;
	private BaselineType baselineType;
	
	private final static double ZERO_LEFT = -1E-5;
	
	public Baseline() {
		this.baselineType = BaselineType.CONSTANT;
	}
	
	public Baseline(BaselineType baselineType, double intercept, double slope) {
		this.intercept = intercept;
		this.slope = slope;
		this.baselineType = baselineType;
	}

	public double valueAt(double time) {
		return intercept + time*slope;
	}

	public NumericProperty getSlope() {
		return NumericProperty.derive(NumericPropertyKeyword.BASELINE_SLOPE, slope);
	}

	public void setSlope(NumericProperty slope) {
		this.slope = (double) slope.getValue();
	}

	public NumericProperty getIntercept() {
		return NumericProperty.derive(NumericPropertyKeyword.BASELINE_INTERCEPT, intercept); 
	}

	public void setIntercept(NumericProperty intercept) {
		this.intercept = (double) intercept.getValue();
	}
	
	public List<Property> listedParameters() {
		List<Property> list = new ArrayList<Property>();
		list.add(getSlope()); 
		list.add(getIntercept()); 
		list.add(BaselineType.CONSTANT);
		return list;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " = " + String.format("%3.2f", intercept) + " + t * " + String.format("%3.2f", slope);
	}

	public void fitTo(ExperimentalData data, double rangeMin, double rangeMax) {
		if(data.getFittingStartIndex() < 1)
			return;
		
		List<Double> x = new ArrayList<Double>();
		List<Double> y = new ArrayList<Double>();
		
		double t;
		
		int size = 0;
		
		for(int i = 0; i < data.getFittingStartIndex(); i++) {
			t = data.time.get(i);
			
			if(t < rangeMin)
				continue;
			
			if(t > rangeMax)
				break;	
			
			x.add(data.time.get(i));
			y.add(data.temperature.get(i));
			size++;
		}		
		
        // first pass: compute xbar and ybar
        double meanx = 0.0, meany = 0.0;
        double x1, y1;
        for(int i = 0; i < size; i++) {
        	x1 = x.get(i); y1 = y.get(i);
            meanx += x1; meany += y1;
        }
        meanx /= size; meany /= size;

        // second pass: compute summary statistics
        double xxbar = 0.0, xybar = 0.0;
        for (int i = 0; i < size; i++) {
        	x1 = x.get(i); y1 = y.get(i);
            xxbar += (x1 - meanx) * (x1 - meanx);
            xybar += (x1 - meanx) * (y1 - meany);
        }
        
        if(baselineType == BaselineType.LINEAR) {
        	slope		= xybar / xxbar;
        	intercept	= meany - slope * meanx;
		} else {
			slope		= 0;
        	intercept	= meany;
		}	
        
        x.clear(); x = null;
        y.clear(); y = null;
        
        setIntercept(NumericProperty.derive(NumericPropertyKeyword.BASELINE_INTERCEPT, intercept));
        setSlope(NumericProperty.derive(NumericPropertyKeyword.BASELINE_SLOPE, slope));
        
	}
	
	public double[] parameters() {
		return new double[] {intercept, slope};
	}
	
	public void setParameters(double[] parameters) {
		intercept = parameters[0];
		slope = parameters[1];
	}

	public void setParameter(int index, double parameter) {
		if(index == 0)
			intercept = parameter;
		else if(index == 1)
			slope = parameter;
		
	}
	
	public void fitTo(ExperimentalData data) {
		fitTo(data, Double.NEGATIVE_INFINITY, ZERO_LEFT);
	}

	public BaselineType getBaselineType() {
		return baselineType;
	}

	public void setBaselineType(BaselineType baselineType) {
		this.baselineType = baselineType;
		
		UpwardsNavigable ancestorTask = super.specificAncestor(SearchTask.class);
		if(ancestorTask == null)
			return;
		
		this.fitTo( ((SearchTask)ancestorTask).getExperimentalCurve() );		
	} 

	public enum BaselineType implements EnumProperty {
		LINEAR, CONSTANT;

		@Override
		public Object getValue() {
			return this;
		}

		@Override
		public String getDescriptor(boolean addHtmlTags) {
			return "Baseline type";
		}

		@Override
		public EnumProperty evaluate(String string) {
			return valueOf(string);
		}

	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch(type) {
		case BASELINE_INTERCEPT : setIntercept(property); break;
		case BASELINE_SLOPE : setSlope(property); break;
		}
	}
		
}