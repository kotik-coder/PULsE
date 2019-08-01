package pulse.search.math;

public class Vector { 
    
    private double[] x;
     
    public Vector(double... args) {
    	x = new double[args.length];
    	
    	for(int i = 0; i < x.length; i++) 
    		x[i] = args[i];
    	
    }
    
    public Vector(int n, double fill) {
    	x = new double[n];
    	for(int i = 0; i < x.length; i++)
    		x[i] = fill;
    }
    
    /** Constructs a default Vector3D. 
    The vector's components value are set to zero.
    */
    
    public Vector(int n) { 
    	x = new double[n];
    }
    
    /** Copy constructor.
    @param v The vector to be copied.
    */
    
    public Vector(Vector v) {
    	x = new double[v.x.length];
        System.arraycopy(v.x, 0, x, 0, v.x.length);
    }    
   
    public Vector invert() {
    	Vector v = new Vector(this);
    	
    	for(int i = 0; i < v.x.length; i++)
    		v.x[i] = -v.x[i];
    	
        return v;
    }
        
    public Vector plus(Vector v) {
    	if(v.x.length != x.length)
    		throw new IllegalArgumentException(Messages.getString("Vector.DimensionError1") + x.length + " != " + v.x.length); //$NON-NLS-1$ //$NON-NLS-2$
        Vector sum = new Vector(this);
        
        for(int i = 0; i < v.x.length; i++)
        	sum.x[i] += v.x[i];
        
        return sum; 
    }  
    
    public Vector minus(Vector v) {
    	if(v.x.length != x.length)
    		throw new IllegalArgumentException(Messages.getString("Vector.DimensionError1") + x.length + " != " + v.x.length); //$NON-NLS-1$ //$NON-NLS-2$
        Vector diff = new Vector(this);
        
        for(int i = 0; i < v.x.length; i++)
        	diff.x[i] -= v.x[i];
        
        return diff; 
    
    }    
    
    public Vector times(double f) {
        Vector factor = new Vector(this);
        
        for(int i = 0; i < x.length; i++)
        	factor.x[i] *= f;
        
        return factor; 
    }    
    
    public double dot(Vector v) {
    	if(v.x.length != x.length)
    		throw new IllegalArgumentException(Messages.getString("Vector.DimensionError1") + x.length + " != " + v.x.length); //$NON-NLS-1$ //$NON-NLS-2$

    	double dotProduct = 0;
       	for(int i = 0; i < v.x.length; i++) {
       		dotProduct += this.x[i]*v.x[i];
       	}
       	return dotProduct;
    }
 
    /** Calculates the length of a vector.
    @return the calculated length.
    */
    
    public double length() {
       return Math.sqrt( lengthSq() );
    }
    
    /** Calculates the squared length of a vector
    @return the squared length.
    */
    
    public double lengthSq() {
       	return this.dot(this);
    }    
    
    public Vector normalize() {
        double l = length(); 
        return this.times(1.0/l);
    }    
    
    public double maxComponent() {
    	double max = -1;
    	
    	double cAbs, maxAbs;
    	
    	for(double c : x) {
    		cAbs	= Math.abs(c);
    		maxAbs	= Math.abs(max);
    		max = cAbs > maxAbs ? cAbs : maxAbs;
    	}
    	
        return max;
    }    
    
    public double sqDistanceTo(Vector v) {
       	if(v.x.length != x.length)
    		throw new IllegalArgumentException(Messages.getString("Vector.DimensionError1") + x.length + " != " + v.x.length); //$NON-NLS-1$ //$NON-NLS-2$

    	double distSq = 0;
    	
    	for(int i = 0; i < v.x.length; i++) 
    		distSq += Math.pow(this.x[i] - v.x[i], 2);
    	
    	
        return distSq;
    }
        
    public double distanceTo(Vector p) {
    	return Math.sqrt(this.sqDistanceTo(p));
    }    
    
    public double get(int index) {
    	return x[index];
    }
    
    public void set(int index, double value) {
    	x[index] = value;
    }
	 
	/** Defines the string representation of the current instance of the class.
	@return the string-equivalent of this object containing all it's field values.
	*/
	 
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
	
	public boolean outOfBounds(Vector min, Vector max) {
		if((min.x.length != x.length) || (max.x.length != x.length))
			throw new IllegalArgumentException(Messages.getString("Vector.DimensionError2") + x + " ; " + min + " ; " + max); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		
		for(int i = 0; i < x.length; i++) {
			if(x[i] < min.x[i]) 
				return true;
			if(x[i] > max.x[i])
				return true;
		}
		
		return false;
	
	}
	
	public int dimension() {
		return x.length;
	}

}