package pulse.util.geom;

public class Rectangle {
	private double xLower, xUpper, yLower, yUpper;
	private double xLowerLimit, xUpperLimit, yLowerLimit, yUpperLimit;
	
	public double getXLower() {
		return xLower;
	}

	public void setXLower(double xLower) {
		this.xLower = xLower;
	}

	public double getXUpper() {
		return xUpper;
	}

	public void setXUpper(double xUpper) {
		this.xUpper = xUpper;
	}

	public double getYLower() {
		return yLower;
	}

	public void setYLower(double yLower) {
		this.yLower = yLower;
	}

	public double getYUpper() {
		return yUpper;
	}

	public void setYUpper(double yUpper) {
		this.yUpper = yUpper;
	}

	public Rectangle(double xLower, double xUpper, double yLower, double yUpper) {
		this.xLower = xLower;
		this.xUpper = xUpper;
		this.yLower = yLower;
		this.yUpper = yUpper;
		xLowerLimit = Double.NEGATIVE_INFINITY;
		yLowerLimit = Double.NEGATIVE_INFINITY;
		xUpperLimit = Double.POSITIVE_INFINITY;
		yUpperLimit = Double.POSITIVE_INFINITY;
	}
	
	public Rectangle(Rectangle rect) {
		this(rect.xLower, rect.xUpper, rect.yLower, rect.yUpper); 
	}
	
	public Point2D center() {
		return new Point2D(0.5*(xUpper - xLower), 0.5*(yUpper - yLower));
	}
	
	public void bind(Rectangle r) {
		xLowerLimit = r.xLower;
		xUpperLimit = r.xUpper;
		yLowerLimit = r.yLower;
		yUpperLimit = r.yUpper;
	}
	
	public void checkBounds() {
		if(xLower < xLowerLimit) 
			xLower = xLowerLimit;
			
		if(xUpper > xUpperLimit) 
			xUpper = xUpperLimit;
				
		if(yLower < yLowerLimit) 
			yLower = yLowerLimit;
			
		if(yUpper > yUpperLimit) 
			yUpper = yUpperLimit;
	}
	
	public void moveTo(Point2D newCenter) {
		Point2D currentCenter = center();
		double dx = newCenter.getX() - currentCenter.getX();
		double dy = newCenter.getY() - currentCenter.getY();
		
		xLower += dx;				
		xUpper += dx;		
		yLower += dy;				
		yUpper += dy;		
		
		checkBounds();
		
	}
	
	public boolean contains(Point2D point) {
		return (point.getX() > this.getXLower() && point.getX() < this.getXUpper())
			&& (point.getY() > this.getYLower() && point.getY() < this.getYUpper());
	}
	
	public void scale(double factor) {
		double xDimension = xUpper - xLower;
		double yDimension = yUpper - yLower;
		
		xLower += xDimension*(1.0 - factor);		
		yLower += yDimension*(1.0 - factor);		
		xUpper -= xDimension*(1.0 - factor);		
		yUpper -= yDimension*(1.0 - factor);	
		
		checkBounds();
		
	}
	
}