package pulse.search.math;


public final class Matrix {
    
    private final double[][] x;
    
    
    public Matrix(int n) {
    	this(n, n);
    }
    
    public Matrix(int m, int n) {   
    	x = new double[m][n];
    }
    
    public Matrix(Vector... args) {    	
    	int dim = args[0].dimension();
    	
    	for(int i = 1; i < args.length; i++) {
    		if(args[i].dimension() != dim)
    			throw new IllegalArgumentException(Messages.getString("Matrix.VectorDimensionError") + args[i] + " and " + dim);  //$NON-NLS-1$ //$NON-NLS-2$
    	}
    		
    	int m = args.length;
    	int n = args[0].dimension();
    	
    	x = new double [n][m];
    	
    	for(int i = 0; i < m; i++) 
    		for(int j = 0; j < n; j++) 
    			this.x[j][i] = args[i].get(j);
    		
    }
    
    public Matrix(double[][] args) {
    	int m = args.length;
    	int n = args[0].length;
    	
    	x = new double[m][n];
    	
    	for(int i = 0; i < m; i++) 
    		for(int j = 0; j < n; j++) 
    			x[i][j] = args[i][j];
    	
    }
      
    public Matrix(int n, double f) {
    	this.x = new double[n][n];
    	
    	for(int i = 0; i < n; i++) 
    		x[i][i] = f;
    	
    }

    public Matrix plus(Matrix m) {
    	if ( !this.hasSameDimensions(m))
    		throw new IllegalArgumentException(Messages.getString("Matrix.DimensionError") + this + " != " + m); //$NON-NLS-1$ //$NON-NLS-2$
    	
        double[][] y = new double[this.x[0].length][this.x[0].length];
        
        for(int i = 0; i < x[0].length; i++) 
        	for(int j = 0; j < x[0].length; j++) 
        		y[i][j] = this.x[i][j] + m.x[i][j];
        	
        return new Matrix(y);     
    }
    
    public Matrix subtract(Matrix m) {
    	if ( !this.hasSameDimensions(m))
    		throw new IllegalArgumentException(Messages.getString("Matrix.DimensionError") + this + " != " + m); //$NON-NLS-1$ //$NON-NLS-2$
    	
        double[][] y = new double[this.x[0].length][this.x[0].length];
        
        for(int i = 0; i < x[0].length; i++) 
        	for(int j = 0; j < x[0].length; j++) 
        		y[i][j] = this.x[i][j] - m.x[i][j];
        	
        return new Matrix(y);
    }
        
    public Matrix multiply(Matrix m) {
    	if ( this.x[0].length != m.x.length)
    		throw new IllegalArgumentException(Messages.getString("Matrix.MultiplicationError") + this + " and " + m); //$NON-NLS-1$ //$NON-NLS-2$
    	
    	int mm = this.x.length;
    	int nn = m.x[0].length;
    	
        double[][] y = new double[mm][nn];
        
        for(int i = 0; i < mm; i++) 
        	for(int j = 0; j < nn; j++) 
        		for(int k = 0; k < this.x[0].length; k++)
        			y[i][j] += this.x[i][k] * m.x[k][j];
        	
        return new Matrix(y);
    }
    
    public Matrix multiply(double f) {
        double[][] y = new double[this.x[0].length][this.x[0].length];
        
        for(int i = 0; i < x[0].length; i++) 
        	for(int j = 0; j < x[0].length; j++) 
        		y[i][j] = this.x[i][j] * f;
        	
        return new Matrix(y);
    }
      
    public Vector multiply(Vector v) {
    	Matrix vm	  = new Matrix(v);
    	
    	Matrix result = this.multiply(vm);      

    	Vector vv = new Vector(v.dimension());
        
        for(int i = 0; i < vv.dimension(); i++) 
        	vv.set(i, result.x[i][0]);
     
        return vv;
    }
 
    public Matrix transpose() {
    	int m = x.length;
    	int n = x[0].length;
    	double[][] y = new double[n][m];
    	    	
    	for(int i = 0; i < m; i++) {
    		for(int j = 0; j < n; j++) {
    			y[j][i] = x[i][j];
    		}
    	}
    	
    	
    	
    	return new Matrix(y);
    	
    }
     
    public double det() {
    	   int i,j,j1,j2;
    	   double det = 0;
    	   double[][] m;
    	   int n = x[0].length;
    	   
    	   if (n < 1) 
    		   throw new IllegalStateException(Messages.getString("Matrix.ZeroDimensionError")); //$NON-NLS-1$
    	   else if (n == 1)  
    	      det = x[0][0];
    	   else if (n == 2) {
    	      det = x[0][0] * x[1][1] - x[1][0] * x[0][1];
    	   } else {
    	      det = 0;
    	      for (j1=0; j1 < n; j1++) {
    	    	 m = new double[n-1][n-1];
    	         for (i=1; i<n; i++) {
    	            j2 = 0;
    	            for (j=0; j<n; j++) {
    	               if (j == j1)
    	                  continue;
    	               m[i-1][j2] = x[i][j];
    	               j2++;
    	            }
    	         }
    	         det += Math.pow(-1.0, 1.0 + j1 + 1.0) * x[0][j1] * (new Matrix(m)).det();
    	      }
    	   }
    	   return det;
    }    
    
    public Matrix invert() {
        double[][] inverse = new double[x[0].length][x[0].length];

        int n = x[0].length;
        
        // minors and cofactors
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                inverse[i][j] = Math.pow(-1, i + j)
                        * this.minor(i, j).det();

        // adjugate and determinant
        double det = 1.0 / this.det();
        for (int i = 0; i < inverse.length; i++) {
            for (int j = 0; j <= i; j++) {
                double temp = inverse[i][j];
                inverse[i][j] = inverse[j][i] * det;
                inverse[j][i] = temp * det;
            }
        }

        return new Matrix(inverse);
    }

    private Matrix minor(int row, int column) {
        double[][] minor = new double[x.length - 1][x[0].length - 1];

        for (int i = 0; i < x.length; i++)
            for (int j = 0; i != row && j < x[0].length; j++)
                if (j != column)
                    minor[i < row ? i : i - 1][j < column ? j : j - 1] = x[i][j];
        return new Matrix(minor);
    }

    public static Matrix multiplyAsMatrices(Vector a, Vector b) {
    	return (new Matrix(a)).multiply( (new Matrix(b)).transpose() );
    }
    
    /** Defines the string representation of the current instance of the class.
    @return the string-equivalent of this object containing all it's field values.
    */
 
    public String toString() {
    	int m = x.length;
    	int n = x[0].length;
		final String f = Messages.getString("Math.DecimalFormat"); //$NON-NLS-1$
    	
        StringBuilder sb = new StringBuilder(m + "x" + n + " matrix: "); //$NON-NLS-1$ //$NON-NLS-2$
        for(int i = 0; i < m; i++) {
        	sb.append( System.lineSeparator()  ); //$NON-NLS-1$
            for(int j = 0; j < n; j++) {
                sb.append(" "); //$NON-NLS-1$
                sb.append(String.format(f, x[i][j]));
            }
    	}
        
        return sb.toString();
    }
    
    public boolean hasSameDimensions(Matrix m) {
    	
    	return this.x.length != m.x.length ? false :
    		(this.x[0].length != m.x[0].length ? false : true); 
    	
    }
    
}