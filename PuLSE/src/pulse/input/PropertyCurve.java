package pulse.input;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class PropertyCurve {

	private List<Double> property;
	private List<Double> temperature;
	private double maxTemp;
	private double minTemp;
	
	public PropertyCurve() throws IOException {				
		property = new LinkedList<Double>();
		temperature = new LinkedList<Double>();
	}
	
	public double valueAt(double temperature) {
		if((temperature > maxTemp) || (temperature < minTemp))
			throw new IllegalArgumentException("Temperature must be in range: " + minTemp + " to " + maxTemp + ". Received: " + temperature); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		double floor = 0;
		int floorIndex = -1;
		
		for(double temp : this.temperature) {
			if(temperature > temp) {
				floor = temp;
				floorIndex = this.temperature.indexOf(temp);
			} else 
				break;				
		}
		
		double roof = this.temperature.get(floorIndex + 1);
		
		double k = ( property.get(floorIndex + 1) - property.get(floorIndex) ) /
				   ( roof - floor );
		
		double b = ( property.get(floorIndex)*roof - property.get(floorIndex + 1)*floor ) /
				   ( roof - floor );
		
		double result = k*temperature + b;
		result *= 100;
		result = (double)Math.round(result)/100;
		
		return result;
		
	}
	
	public int size() {
		return temperature.size();
	}
	
	public double getMaxTemperature() {
		return maxTemp;
	}
	
	public double getMinTemperature() {
		return minTemp;
	}

	public void setMinTemperature(double d) {
		minTemp = d;
		
	}
	
	public void setMaxTemperature(double d) {
		maxTemp = d;
		
	}
	
	public void addTemperature(double t) {
		temperature.add(t);
	}
			
	public void addProperty(double t) {
		property.add(t);
	}
		
}
	

