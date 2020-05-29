package pulse.search.math;

import java.util.Arrays;

import pulse.ui.Messages;

/**
 * <p>This is a general class for {@code Vector} operations useful for 
 * optimisation. Note it does not currently include cross or mixed products, as this
 * is not needed in {@code PULsE}.</p> 
 */

public class Vector { 
    
    private double[] x;
     
    public Vector(double[] x) {
    	this.x = new double[x.length];
    	System.arraycopy(x, 0, this.x, 0, x.length);
    }  
    
    /**
     * Creates a zero {@code Vector}.
     * @param n the dimension of the {@code Vector}.
     */
    
    public Vector(int n) {
    	x = new double[n];
    }
    
    /**
     * Creates a {@code Vector} consisting of {@code n} elements, each equal to {@code fill}.
     * @param n the dimension of this {@code Vector}
     * @param fill a fill value
     */
    
    public Vector(int n, double fill) {
    	x = new double[n];
    	Arrays.fill(x, fill);
    }
    
    /** Copy constructor. Uses {@code System.arraycopy()}.
    @param v The vector to be copied.
    */
    
    public Vector(Vector v) {
    	x = new double[v.x.length];
        System.arraycopy(v.x, 0, x, 0, v.x.length);
    }  
    
	/**
     * Creates a new {@code Vector} based on {@code this} one, all elements of 
     * which are inverted, i.e. <math><i>b</i><sub>i</sub> = -<i>a</i><sub>i</sub></math>. 
     * @return a generalised inversion of {@code this Vector}.
     */
   
    public Vector inverted() {
    	Vector v = new Vector(x.length);
    	
    	for(int i = 0; i < v.x.length; i++)
    		v.x[i] = -this.x[i];
    	
        return v;
    }
    
	/**
	 * The dimension is simply the number of elements in a {@code Vector}
	 * @return the integer dimension
	 */
	
	public int dimension() {
		return x.length;
	}
    
    /**
     * Performs an element-wise summation of {@code this} and {@code v}.  
     * @param v another {@code Vector} with the same number of elements. 
     * @return the result of the summation.
     * @throws IllegalArgumentException f the dimension of {@code this} and {@code v} are different. 
     */
        
    public Vector sum(Vector v) throws IllegalArgumentException {
    	if(v.x.length != x.length)
    		throw new IllegalArgumentException(Messages.getString("Vector.DimensionError1") + x.length + " != " + v.x.length);
    	
        Vector sum = new Vector(this);
        
        for(int i = 0; i < v.x.length; i++)
        	sum.x[i] += v.x[i];
        
        return sum; 
    }
    
    /**
     * Performs an element-wise subtraction of {@code v} from  {@code this}.  
     * @param v another {@code Vector} with the same number of elements. 
     * @return the result of subtracting {@code v} from {@code this}.
     * @throws IllegalArgumentException f the dimension of {@code this} and {@code v} are different. 
     */
    
    public Vector subtract(Vector v) throws IllegalArgumentException {
    	if(v.x.length != x.length)
    		throw new IllegalArgumentException(Messages.getString("Vector.DimensionError1") + x.length + " != " + v.x.length); //$NON-NLS-1$ //$NON-NLS-2$
        Vector diff = new Vector(this);
        
        for(int i = 0; i < v.x.length; i++)
        	diff.x[i] -= v.x[i];
        
        return diff; 
    
    }   
    
    /**
     * Performs an element-wise multiplication by {@code f}.  
     * @param f a double value.
     * @return a new {@code Vector}, all elements of which will be multiplied by {@code f}. 
     */
    
    public Vector multiply(double f) {
        Vector factor = new Vector(this);
        
        for(int i = 0; i < x.length; i++)
        	factor.x[i] *= f;
        
        return factor; 
    }    
    
    /**
     * Calculates the scalar product of {@code this} and {@code v}.
     * @param v another {@code Vector} with the same dimension.
     * @return the dot product of {@code this} and {@code v}.
     * @throws IllegalArgumentException f the dimension of {@code this} and {@code v} are different.
     */
    
    public double dot(Vector v) throws IllegalArgumentException {
    	if(v.x.length != x.length)
    		throw new IllegalArgumentException(Messages.getString("Vector.DimensionError1") + x.length + " != " + v.x.length); //$NON-NLS-1$ //$NON-NLS-2$

    	double dotProduct = 0;
       	for(int i = 0; i < v.x.length; i++) 
       		dotProduct += this.x[i]*v.x[i];
       	
       	return dotProduct;
    }
 
    /** Calculates the length, which is represented by the square-root of the 
     * squared length.
     * @return the calculated length.
     * @see lengthSq()
    */
    
    public double length() {
       return Math.sqrt( lengthSq() );
    }
    
    /** The squared length of this vector is the dot product of this vector by itself.
    @return the squared length.
    */
    
    public double lengthSq() {
       	return this.dot(this);
    }           
    
    /**
     * Performs normalisation, e.g. scalar multiplication of {@code this} by 
     * the multiplicative inverse of {@code this Vector}'s length.
     * @return a normalised {@code Vector} obtained from {@code this}.
     */
    
    public Vector normalise() {
        double l = length(); 
        return this.multiply(1.0/l);
    }      
    
    /**
     * Calculates the squared distance from {@code this Vector} to {@code v} (which is the squared length of the connecting {@code Vector}).
     * @param v another {@code Vector}.
     * @return the squared length of the connecting {@code Vector}.
     * @throws IllegalArgumentException f the dimension of {@code this} and {@code v} are different.
     */
    
    public double distanceToSq(Vector v) throws IllegalArgumentException {
       	if(v.x.length != x.length)
    		throw new IllegalArgumentException(Messages.getString("Vector.DimensionError1") + x.length + " != " + v.x.length); //$NON-NLS-1$ //$NON-NLS-2$

    	double distSq = 0;
    	
    	for(int i = 0; i < v.x.length; i++) 
    		distSq += Math.pow(this.x[i] - v.x[i], 2);    	
    	
        return distSq;
    }
    
    /**
     * Calculates distance from {@code this Vector} to {@code p} using square-root on the squared distance.
     * @param p another {@code Vector}.
     * @return the length of the connecting {@code Vector}.
     * @see distanceToSq(Vector)
     * @throws IllegalArgumentException f the dimension of {@code this} and {@code v} are different.
     */
        
    public double distanceTo(Vector p) {
    	return Math.sqrt(this.distanceToSq(p));
    }    
    
    /**
     * Gets the component of {@code this Vector} specified by {@code index}
     * @param index the index of the component
     * @return a double value, representing the value of the component 
     */
    
    public double get(int index) {
    	return x[index];
    }
    
    /**
     * Sets the component of {@code this Vector} specified by {@code index} to {@code value}.
     * @param index the index of the component.
     * @param value a new value that will replace the old one. 
     */
    
    public void set(int index, double value) {
    	x[index] = value;
    }
	 
	/** Defines the string representation of the current instance of the class.
	@return the string-equivalent of this object containing all it's field values.
	*/
	 
	@Override
	public String toString() {
		final String f = Messages.getString("Math.DecimalFormat"); //$NON-NLS-1$
	    StringBuilder sb = new StringBuilder().append("("); //$NON-NLS-1$
	    for(double c : x)
	    	sb.append(String.format(f, c) + " "); //$NON-NLS-1$
	    sb.append(")"); //$NON-NLS-1$
	    return sb.toString();
	 }
	 
	/** Checks if <code>o</code> is logically equivalent to an instance of this class. 
	@param o An object to compare with <code>this</code> vector.
	@return true if <code>o</code> equals <code>this</code>.
	*/
	 
	@Override
	public boolean equals(Object o) {
	    if(o == this)
	        return true;
	    
	    if(!(o instanceof Vector))
	        return false;
	    
	    Vector v = (Vector)o;
	    
	    if(v.x.length != x.length)
	    	return false;
	   
	    for(int i = 0; i < x.length; i++) {
	    	if( Double.doubleToLongBits(this.x[i]) 
	    			!= Double.doubleToLongBits(v.x[i]) )
	    			return false;
	    }

	    return true;
	    
	}
	
	public double[] getData() {
		return x;
	}

}