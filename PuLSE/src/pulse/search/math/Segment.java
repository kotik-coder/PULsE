package pulse.search.math;

import java.util.Random;

public class Segment {
	private double a, b;
	
	public Segment(double a, double b) {		
		this.a = a < b ? a : b;
		this.b = b > a ? b : a;
	}
	
	public Segment(Segment segment) {		
		this.a = segment.a;
		this.b = segment.b;
	}
	
	public double getMinimum() {
		return a;
	}
	
	public double getMaximum() {
		return b;
	}
	
	public double length() {
		return (b - a);
	}
	
	public double lengthSq() {
		return Math.pow(b-a , 2);
	}

	public void setMinimum(double a) {
		this.a = a;
	}

	public void setMaximum(double b) {
		this.b = b;
	}
	
	public double mean() {
		return (a + b)*0.5;
	}
	
	public double randomValue() {
		return (new Random()).nextDouble()*length() + getMinimum();
	}
	
	public boolean contains(double x) {
		return x >= a ? (x <= b ? true : false) : false;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("["); 
		sb.append(a);
		sb.append(" ; "); 
		sb.append(b);
		sb.append("]"); 
		return sb.toString();
	}
	
}
