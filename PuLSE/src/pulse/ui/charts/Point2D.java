package pulse.ui.charts;

public class Point2D {
	private double x,y;
	
	public Point2D(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	public double getX() {
		return x;
	}
	
	public double getY() {
		return y;
	}
	
	public void setX(double x) {
		this.x = x;
	}
	
	public void setY(double y) {
		this.y = y;
	}
	
	public Point2D plus(Point2D p) {
		return new Point2D(this.x + p.x, this.y + p.y);		
	}
	
	public Point2D subtract(Point2D p) {
		return new Point2D(this.x - p.x, this.y - p.y);		
	}
	
	public Point2D mult(double d) {
		return new Point2D(this.x*d, this.y*d);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		sb.append(getX());
		sb.append(" ; ");
		sb.append(getY());
		sb.append(")");
		return sb.toString();
	}
	
}