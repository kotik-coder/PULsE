package pulse.search.math;

import pulse.ui.Messages;

/**
 * <p>A class used to represent small matrices, generally used in 
 * optimisation. Hence, instead of implementing advanced decomposition
 * methods for calculating the determinant and the inverted matrix,
 * it relies on simpler techniques, such as the Laplace expansion.</p>
 */

public final class Matrix {
    
    private final double[][] x;
        
    /**
     * <p>Constructs a matrix (not necessarily square), where {@code args} 
     * are mapped to different <i>columns</i>, thus the {@code Vector} 
     * components are mapped into different <i>rows</i>. In case 
     * if the {@code args.length == 1}, the {@code Matrix} will represent 
     * a transposed {@code Vector}, i.e. a column vector.</p>  
     * @param args one or multiple vectors
     */
    
    public Matrix(Vector... args) {    	
    	int n = args[0].dimension();
    	int m = args.length;
    	
    	for(int i = 1; i < m; i++) 
    		if(args[i].dimension() != n)
    			throw new IllegalArgumentException(Messages.getString("Matrix.VectorDimensionError") + args[i] + " and " + n);
    	
    	x = new double [n][m];
    	
    	for(int i = 0; i < m; i++) 
    		for(int j = 0; j < n; j++) 
    			this.x[j][i] = args[i].get(j);
    		
    }
    
    /**
     * Constructs a square matrix of the size {@code n}.
     * @param n the size of the square matrix.
     */
    
    public Matrix(int n) {
    	this(n, n);
    }
    
    /**
     * Constructs a {@code Matrix} copying the contents of {@code args}.
     * @param args a two-dimensional array
     */
        
    public Matrix(double[][] args) {
    	int m = args.length;
    	int n = args[0].length;
    	
    	x = new double[m][n];
    	
    	for(int i = 0; i < m; i++) 
    		for(int j = 0; j < n; j++) 
    			x[i][j] = args[i][j];
    	
    }
    
    /**
     * Constructs a square {@code Matrix}, where the non-diagonal elements are zero,
     * and diagonal elements are each equal to {@code f}.
     * @param n the size of the square {@code Matrix}
     * @param f a numeric value
     */
      
    public Matrix(int n, double f) {
    	this.x = new double[n][n];
    	
    	for(int i = 0; i < n; i++) 
    		x[i][i] = f;
    	
    }

    /**
     * <p>Checks whether both {@code this} and {@code m} have matching dimensions.
     * If true, will perform an element-wise summation.</p>
     * @param m another {@code Matrix} of the same size as {@code this} one
     * @return a {@code Matrix} with elements formed as the result of summing up the respective elements in {@code this} and {@code m}
     * @throws IllegalArgumentException if the dimensions of the matrices do not match
     */
    
    public Matrix sum(Matrix m) throws IllegalArgumentException {
    	if ( !this.hasSameDimensions(m) )
    		throw new IllegalArgumentException(Messages.getString("Matrix.DimensionError") + this + " != " + m);
    	
        double[][] y = new double[this.x.length][this.x[0].length];
        
        for(int i = 0; i < x.length; i++) 
        	for(int j = 0; j < x[0].length; j++) 
        		y[i][j] = this.x[i][j] + m.x[i][j];
        	
        return new Matrix(y);     
    }
    
    /**
     * <p>Checks whether both {@code this} and {@code m} have matching dimensions.
     * If true, will perform an element-wise subtraction.</p>
     * @param m another {@code Matrix} of the same size as {@code this} one
     * @return a {@code Matrix} with elements formed as the result of subtracting an element of matrix {@code m} from the respective element in {@code this}
     * @throws IllegalArgumentException if the dimensions of the matrices do not match
     */
    
    public Matrix subtract(Matrix m) throws IllegalArgumentException {
    	if ( !this.hasSameDimensions(m))
    		throw new IllegalArgumentException(Messages.getString("Matrix.DimensionError") + this + " != " + m);
    	
        double[][] y = new double[this.x.length][this.x[0].length];
        
        for(int i = 0; i < x.length; i++) 
        	for(int j = 0; j < x[0].length; j++) 
        		y[i][j] = this.x[i][j] - m.x[i][j];
        	
        return new Matrix(y);
    }
    
    /**
     * <p>Performs {@code Matrix} multiplication. Checks whether the dimensions of 
     * each matrix are appropriate (number of columns in {@code this} matrix should be equal to
     * the number of rows in {@code m}.</p>
     * @param m another {@code Matrix} suitable for multiplication
     * @return a {@code Matrix}, which is the result of multiplying {@code this} by {@code m}
     * @throws IllegalArgumentException if the number of columns in {@code this} matrix is not equal to
     * the number of rows in {@code m}
     */
        
    public Matrix multiply(Matrix m) throws IllegalArgumentException {
    	if ( this.x[0].length != m.x.length)
    		throw new IllegalArgumentException(Messages.getString("Matrix.MultiplicationError") + this + " and " + m); 
    	
    	int mm = this.x.length;
    	int nn = m.x[0].length;
    	
        double[][] y = new double[mm][nn];
        
        for(int i = 0; i < mm; i++) 
        	for(int j = 0; j < nn; j++) 
        		for(int k = 0; k < this.x[0].length; k++)
        			y[i][j] += this.x[i][k] * m.x[k][j];
        	
        return new Matrix(y);
    }

    /**
     * Scales this {@code Matrix} by {@code f}, which results in element-wise multiplication by {@code f}.
     * @param f a numeric value
     * @return the scaled {@code Matrix}
     */
    
    public Matrix multiply(double f) {
        double[][] y = new double[this.x[0].length][this.x[0].length];
        
        for(int i = 0; i < x[0].length; i++) 
        	for(int j = 0; j < x[0].length; j++) 
        		y[i][j] = this.x[i][j] * f;
        	
        return new Matrix(y);
    }
    
    /**
     * <p>Multiplies this {@code Matrix} by the vector {@code v}, which
     * is represented by a {@code n x 1} {@code Matrix}, where {@code n}
     * is the dimension of {@code v}. Note {@code n} should be equal to 
     * the number of rows in this {@code Matrix}.</p>
     * @param v a {@code Vector}.
     * @return the result of multiplication, which is a {@code Vector}.
     */
      
    public Vector multiply(Vector v) {
    	Matrix vm	  = new Matrix(v);
    	
    	Matrix result = this.multiply(vm);      

    	Vector vv = new Vector(v.dimension());
        
        for(int i = 0; i < vv.dimension(); i++) 
        	vv.set(i, result.x[i][0]);
     
        return vv;
    }
    
    /**
     * Transposes this {@code Matrix}, i.e. reflects it over the main
     * diagonal. 
     * @return a transposed {@code Matrix}
     */
 
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
    
    /**
     * Calculates the determinant for an <i>n</i>-by-<i>n</i> square matrix.
     * <p>If the matrix dimension is less than 3, the calculation is straightforward 
     * according to simplified formulae. Otherwise, a Laplace expansion will 
     * be used.</p>
     * @return a double, representing the determinant
     * @see detLaplace()
     */
     
    public double det() {
    	   int n = x[0].length;
    	   
    	   switch(n) {
    	   case 0 : throw new IllegalArgumentException("Illegal matrix size: " + n);
    	   case 1 : return x[0][0];
    	   case 2 : return x[0][0] * x[1][1] - x[1][0] * x[0][1];
    	   default: return detLaplace(); 	     	   
    	   }

    }
    
    /**
     * Calculates the determinant from the first row of this {@code Matrix}
     * using the value of cofactors.
     * @return the determinant. 
     */
    
    public double detLaplace() {
 	   double det = 0;
 	   int n = x[0].length;
 	   
 	   for(int j = 0; j < n; j++) 
 		   det += x[0][j]*cofactor(0,j);
 	   
 	   return det;

    }
    
    /**
     * Inverts the n-by-n {@code Matrix} using an adjugate matrix and the 
     * determinant of this matrix.
     * @return the inverted {@Code Matrix}.
     * @see det
     * @see adjugate
     */
    
    public Matrix inverse() {
    	return adjugate().multiply(1.0/det());
    }
    
    /**
     * Calculates a submatrix by eliminating the {@code row} and {@code column}
     * from this {@code Matrix}
     * @param row the row number, starting from 0
     * @param column the column number, starting from 0
     * @return a submatrix of {@code this} one
     */

    public Matrix submatrix(int row, int column) {
        double[][] sub = new double[x.length - 1][x[0].length - 1];

        for (int i = 0; i < row; i++) {
        	
	       for (int j = 0; j < column; j++)
	    	   sub[i][j] = x[i][j];
	            
	       for (int j = column + 1; j < x[0].length; j++)
	    	   sub[i][j - 1] = x[i][j];
	       
        }
        
        for (int i = row + 1; i < x.length; i++) {
        	
            for (int j = 0; j < column; j++)
                sub[i - 1][j] = x[i][j];
            
            for (int j = column + 1; j < x[0].length; j++)
                sub[i - 1][j - 1] = x[i][j];
        
        }
        
        return new Matrix(sub);
        
    }
    
    /**
     * Constructs an adjugate {@code Matrix}, the elements of which
     * are <math><i>a</i><sub>ij</sub>=<i>A</i><sub>ji</sub></math>,
     * where <math><i>A</i><sub>ij</sub></math> are the cofactors of 
     * this {@code Matrix}
     * @return an adjugate {@code Matrix}
     */
    
    public Matrix adjugate() {
    	int n = x.length;
    	int m = x[0].length;
    	
    	if(n != m)
    		throw new IllegalArgumentException("Cannot invert this matrix: " + n + " by " + m);
    	
    	double y[][] = new double[n][n];
    	
    	for(int i = 0; i < n; i++) 
    		for(int j = 0; j < n; j++) 
    			y[j][i] = cofactor(i,j);

    	return new Matrix(y);
    	
    }
    
    /**
     * Calculates the minor <math><i>M</i><sub>ij</sub></math> of this matrix using {@code row = i} and {@code column = j}.
     * @param row the row number, starting from 0 
     * @param column the column number, starting from 0
     * @return the <math><i>M</i><sub>ij</sub></math> value 
     */
    
    public double minor(int row, int column) {
    	Matrix sub = submatrix(row,column);
    	if(sub.x.length < 3)
    		return sub.det();
    	else
    		return sub.detLaplace();
    }
    
    /**
     * Calculates the cofactor <math><i>A</i><sub>ij</sub>=(-1)<sup>(i+1)+(j+1)</sup><i>M</i><sub>ij</sub></math> 
     * of this matrix using {@code row = i} and {@code column = j}.
     * @param row the row number, starting from 0 
     * @param column the column number, starting from 0
     * @return the cofactor value 
     */
    
    public double cofactor(int row, int column) {
    	return Math.pow(-1.0, (row+1) + (column+1))*minor(row,column);
    }
    
    /**
     * Calculates the outer product of two vectors, which is represented as 
     * <math><b>a</b> * <b>b</b><sup>T</sup></math>
     * @param a a Vector
     * @param b a Vector
     * @return the outer product of {@code a} and {@code b}
     */

    public static Matrix outerProduct(Vector a, Vector b) {
    	return (new Matrix(a)).multiply( (new Matrix(b)).transpose() );
    }
    
    /**
     * Prints out matrix dimensions and all the elements contained in it. 
     */
    
    @Override
    public String toString() {
    	int m = x.length;
    	int n = x[0].length;
		final String f = Messages.getString("Math.DecimalFormat");
    	
        StringBuilder sb = new StringBuilder(m + "x" + n + " matrix: ");
        for(int i = 0; i < m; i++) {
        	sb.append( System.lineSeparator()  );
            for(int j = 0; j < n; j++) {
                sb.append(" ");
                sb.append(String.format(f, x[i][j]));
            }
    	}
        
        return sb.toString();
    }
    
    /**
     * Checks if the dimension of {@code this Matrix} and {@code m} match,
     * i.e. if the number of rows is the same and the number of columns 
     * is the same 
     * @param m another {@code Matrix}
     * @return {@code true} if the dimensions match, {@code false} otherwise.
     */
    
    public boolean hasSameDimensions(Matrix m) {
    	
    	if(this.x.length != m.x.length)
    		return false;
    	
    	if(this.x[0].length != m.x[0].length)
    		return false; 
    	
    	return true; 
    	
    }
    
}