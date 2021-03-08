package pulse.problem.laser;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.PULSE_WIDTH;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;

import pulse.input.ExperimentalData;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.SearchTask;

public class NumericPulse extends PulseTemporalShape {

	private NumericPulseData pulseData;
	private UnivariateFunction interpolation;
	private double adjustedPulseWidth;
	
	public NumericPulse() {
		//intentionally blank
	}
	
	public NumericPulse(NumericPulse pulse) {
		super(pulse);
		this.pulseData = new NumericPulseData(pulseData);
	}
	
	@Override
	public void init(ExperimentalData data, DiscretePulse pulse) {
		pulseData = data.getMetadata().getPulseData();
		
		var problem = ((SearchTask)data.getParent()).getCurrentCalculation().getProblem();
		setPulseWidth(problem);
		
		double timeFactor = problem.getProperties().timeFactor();
				
		super.init(data, pulse);
			
		doInterpolation( timeFactor );
		
		normalise(problem);
	}
	
	public void normalise(Problem problem) {
		
		final double EPS = 1E-2;
		double timeFactor = problem.getProperties().timeFactor();

		for( double area = area() ; Math.abs(area - 1.0) > EPS; area = area() ) {
			pulseData.scale( 1.0 / area );
			doInterpolation( timeFactor );
		}
		
	}
	
	private void setPulseWidth(Problem problem) {
		var timeSequence = pulseData.getTimeSequence();
		double pulseWidth = timeSequence.get( timeSequence.size() - 1 );
		
		var pulseObject = problem.getPulse();
		pulseObject.setPulseWidth( derive(PULSE_WIDTH, pulseWidth) );

	}
	
	private void doInterpolation(double timeFactor) {
		var interpolator = new SplineInterpolator();
		
		var timeList = pulseData.getTimeSequence().stream().mapToDouble(d -> d / timeFactor).toArray();
		adjustedPulseWidth = timeList[timeList.length - 1];
		var powerList = pulseData.getSignalData();
		
		interpolation = interpolator.interpolate(timeList,
												 powerList.stream().mapToDouble(d -> d).toArray());
		
	}
	
	@Override
	public double evaluateAt(double time) {
		return time > adjustedPulseWidth ? 0.0 : interpolation.value(time);
	}

	@Override
	public PulseTemporalShape copy() {
		return new NumericPulse();
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		// TODO Auto-generated method stub
	}

	public NumericPulseData getData() {
		return pulseData;
	}

	public void setData(NumericPulseData pulseData) {
		this.pulseData = pulseData;
	}

	public UnivariateFunction getInterpolation() {
		return interpolation;
	}

}
