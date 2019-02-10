package com.ettoremastrogiacomo.utils;

import org.apache.log4j.Logger;


public class DoubleDoubleArray {
	static Logger logger = Logger.getLogger(DoubleDoubleArray.class);
	
	   static public boolean isRectangular(double[][] matrix) {
	        if (matrix.length == 0) {
	            return true;
	        }
	        for(int i=1; i < matrix.length; i++) {
	            if (matrix[i].length != matrix[0].length) {
	                return false;
	            }
	        }
	        return true;
	    }
	    
	    public static double[][] transpose(double[][] matrix) throws Exception{
	        if ( !isRectangular(matrix)) throw new Exception("Not rectangular matrix");
	        if (matrix.length == 0 || matrix[0].length == 0) {
	            return new double[0][];
	        }
	        double[][] result = new double[matrix[0].length][];
	        for(int i=0; i < result.length; i++) {
	            result[i] = new double[matrix.length];
	        }
	        for(int i=0; i < matrix.length; i++) {
	            for(int j=0; j < matrix[i].length; j++) {
	                result[j][i] = matrix[i][j];
	            }
	        }
	        return result;
	    }    
	    
	    static public  int nRows(double[][] m){
	    	return m.length;
	    }
	    
	    static public int nCols(double[][] m)throws Exception{
	    	if ( !isRectangular(m)) throw new Exception("Not rectangular matrix");
	    	if (m.length==0) return 0;
	    	return m[0].length;
	    }
	    static public double[][] copy(double [][] m){
	    	double[][] newm=new double[m.length][];
	    	for (int i=0;i<m.length;i++){
	    		newm[i]=new double[m[i].length];
            System.arraycopy(m[i], 0, newm[i], 0, m[i].length);
	    	}
	    	return newm;
	    }
	    
		static public void fill(double[][] m,double d){
            for (double[] m1 : m) {
                for (int j = 0; j < m1.length; j++) {
                    m1[j] = d;
                }
            }
		}
		static public boolean isSquare(double[][] m) {
			if (!isRectangular(m)) return false;
			if (m.length==0) return true;
			return m.length==m[0].length;
		}
		
	    static public double[] diag(double[][] m)throws Exception{
	    	if (!isSquare(m)) throw new Exception("not square matrix");
	    	double[] v=new double[m.length];
	    	for (int i=0;i<m.length;i++) v[i]=m[i][i];
	    	return v;
	    }
	    static public double[][] sub(double[][] m, int uli,int ulj,int lri,int lrj )throws Exception{
	    	if ( !isRectangular(m)) throw new Exception("Not rectangular matrix");	    	
	    	int nrows=lri-uli+1,ncols=lrj-ulj+1;
	    	double[][] newm=new double[nrows][ncols];
	    	for (int i=uli;i<=lri;i++)
	    		for (int j=ulj;j<=lrj;j++)
	    			newm[i-uli][j-ulj]=m[i][j];	    	
	    	return newm;
	    }

	    
	    static public double[][] minus(double[][] A,double[][] B) throws Exception{
	    	int rA=nRows(A),cA=nCols(A),rB=nRows(B),cB=nCols(B);
	        if (!isRectangular(A) || !isRectangular(B) || rA!=rB || cA!=cB) throw new Exception("Illegal matrix dimensions.");
	        double[][] C = new double[rA][cB];
	        for (int i = 0; i < rA; i++)
	            for (int j = 0; j < cB; j++)
	                C[i][j] = A[i][j] - B[i][j];
	        return C;
	    }
	    
	    static public double[][] plus(double[][] A,double[][] B) throws Exception{
	    	int rA=nRows(A),cA=nCols(A),rB=nRows(B),cB=nCols(B);
	        if (!isRectangular(A) || !isRectangular(B) || rA!=rB || cA!=cB) throw new Exception("Illegal matrix dimensions.");
	        double[][] C = new double[rA][cB];
	        for (int i = 0; i < rA; i++)
	            for (int j = 0; j < cB; j++)
	                C[i][j] = A[i][j] + B[i][j];
	        return C;
	    }
	    
	    static public double[][] times(double[][] A,double[][] B) throws Exception{
	    	int rA=nRows(A),cA=nCols(A),rB=nRows(B),cB=nCols(B);
	        if (!isRectangular(A) || !isRectangular(B) || cA!=rB) throw new Exception("Illegal matrix dimensions.");
	        double[][] C = new double[rA][cB];//Matrix(A.M, B.N);
	        for (int i = 0; i < rA; i++)
	            for (int j = 0; j < cB; j++)
	                for (int k = 0; k < cA; k++)
	                    C[i][j] += (A[i][k] * B[k][j]);
	        return C;
	    }
	    
	    static public double[][] Vector2Matrix(double[] v,boolean row) {
	    	double[][] m;
	    	if (row) {
	    		m=new double[1][v.length];
            System.arraycopy(v, 0, m[0], 0, v.length);
	    	} else {
	    		m=new double[v.length][1];
	    		for (int i=0;i<v.length;i++)m[i][0]=v[i];	    		
	    	}
	    	return m;	    	
	    }
	    //osservazioni per righe, variabili per colonne
	    static public double[] mean(double[][]m)throws Exception{
	    	if ( !isRectangular(m)) throw new Exception("Not rectangular matrix");
	    	int n=nCols(m);
	    	double[] res=new double[n];
	    	for (int j=0;j<n;j++) {
	    		double s=0;
                        for (double[] m1 : m) {
                            s += m1[j];
                        }
	    		res[j]=s/m.length;
	    	}
	    	return res;
	    }
            
            static public double[] sum(double[][]m) throws Exception {
                double[] res=mean(m);
                for (int i=0;i<res.length;i++) res[i]=res[i]*m.length;
                return res;
            
            }

	    //osservazioni per righe, variabili per colonne
	    static public double[][] cov(double[][]m)throws Exception{
	    	//if ( !isRectangular(m)) throw new Exception("Not rectangular matrix");	    	
	    	int n=nCols(m);
	    	double[] mean=mean(m);
	    	double[][] res=new double[n][n];
	    	for (int j=0;j<n;j++) {
	    		for (int i=j;i<n;i++){
	    			double s=0;
                                for (double[] m1 : m) {
                                    s += (m1[j] - mean[j]) * (m1[i] - mean[i]);
                                }
	    			res[i][j]=s/(m.length-1);
	    		}	    		
	    	}
	    	for (int j=0;j<n;j++)
	    		for (int i=0;i<j;i++)
	    			res[i][j]=res[j][i];
	    	return res;
	    }
            //osservazioni per righe, variabili per colonne
	    static public double[][] corr(double[][] m)throws Exception {
	    	
	    	double[][] res=cov(m);
	    	double[][] newm=new double[res.length][res.length];
	    	for (int i=0;i<res.length;i++)
	    		for (int j=0;j<res.length;j++)
	    			newm[i][j]=res[i][j]/Math.sqrt(res[i][i]*res[j][j]);
	    	return newm;
	    }
	    //osservazioni per righe, variabili per colonne
	    static public double[] max(double[][] m)throws Exception{
	    	int n=nCols(m);
	    	double[] res=new double[n];
	    	for (int j=0;j<n;j++){
	    		double M=m[0][j];
	    		for (int i=1;i<m.length;i++)
	    			if (m[i][j]>M) M=m[i][j];
	    		res[j]=M;
	    	}
	    	return res;
	    }
	    //osservazioni per righe, variabili per colonne
	    static public double[] min(double[][] m)throws Exception{
	    	int n=nCols(m);
	    	double[] res=new double[n];
	    	for (int j=0;j<n;j++){
	    		double M=m[0][j];
	    		for (int i=1;i<m.length;i++)
	    			if (m[i][j]<M) M=m[i][j];
	    		res[j]=M;
	    	}
	    	return res;
	    }
            //osservazioni per righe, variabili per colonne
	    static public double[][] abs(double[][] m) throws Exception{
	    	int n=nCols(m);
	    	double[][] newm=new double[m.length][n];
	    	for (int i=0;i<m.length;i++)
	    		for (int j=0;j<n;j++)
	    			newm[i][j]=Math.abs(m[i][j]);
	    	return newm;
	    }
            static public double[] row(double[][] m,int i) throws Exception {
                int nrows=nRows(m);
                if (i>= nrows || i<0) throw new Exception("index out of range : "+i);
                double []r=new double[nCols(m)];
                System.arraycopy(m[i], 0, r, 0, r.length);
                return r;
            }

            static public double[] column(double[][] m,int i) throws Exception {
                int ncols=nCols(m);
                if (i>= ncols || i<0) throw new Exception("index out of range : "+i);
                double []r=new double[nRows(m)];
                for (int j=0;j<r.length;j++) r[j]=m[j][i];
                return r;
            }
            
	    static public String toString(double[][] m) throws Exception{
	    	//System.out.println("\nMatrix "+nRows(m)+"x"+nCols(m));
	    	String ret="";
	    	int n=nCols(m);
            for (double[] m1 : m) {
                for (int j = 0; j < n; j++) {
                    ret += m1[j] + "\t";
                }
                //System.out.printf("%9.4f ", m[i][j]);
                ret+="\n";//System.out.println();
            }
	        return ret;
	    }	    
	    
	    static public void show(double[][] m) throws Exception{
	    	System.out.println("\nMatrix "+nRows(m)+"x"+nCols(m));
	    	int n=nCols(m);
            for (double[] m1 : m) {
                for (int j = 0; j < n; j++) {
                    System.out.printf("%9.4f ", m1[j]);
                }
                System.out.println();	    	    	
            }
	    }
	    
}
