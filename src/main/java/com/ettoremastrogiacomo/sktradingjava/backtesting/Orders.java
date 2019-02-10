package com.ettoremastrogiacomo.sktradingjava.backtesting;

import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import com.ettoremastrogiacomo.utils.DoubleArray;



 public class Orders  {
     
     
    public static enum type {
        MARKET, CLOSE
    };
    public class Order implements Comparable<Order>{
        public final int dateidx;
        public final double[] weigths;
        public final type tp;
        public Order(int dateidx,double[] weights,type tp) {
            this.dateidx=dateidx;this.weigths=weights;this.tp=tp;
        }
        //public int getDateidx() {return dateidx;}
        @Override
        public int compareTo(Order o) {
            return dateidx-o.dateidx;
        }
    @Override
    public boolean equals(Object other){
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof Order)) return false;
        Order o=(Order) other;
        return (o.dateidx==this.dateidx && o.tp==o.tp);
    } 

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + (int) (this.dateidx ^ (this.dateidx >>> 16));
        return hash;
    }    
    
 }
    
    //public final int dateidx;
    //public final List<Double> weights;
    //public final type otype;
    //public final Date date;
    private final double eps=0.00001;
    private final Portfolio portfolio;
    public final java.util.List<Order> orders;
    public Orders(Portfolio portfolio) {this.portfolio=portfolio;orders=new java.util.ArrayList<>();}
    public void add(int dateidx,double[] weights,type tp) throws Exception{
        if (dateidx<0 || dateidx>=portfolio.getLength()) throw new Exception ("bad date idx "+dateidx);
        double dsum= DoubleArray.sum_abs(weights);
        if (weights.length!=portfolio.getNoSecurities()) throw new Exception ("bad weigts len="+weights.length+" <> "+portfolio.getNoSecurities());
        if ( dsum<0 || dsum>(1.0+eps)  )throw new Exception ("bad weigts sum="+dsum+"\n"+DoubleArray.toString(weights));
        Order atom=new Order(dateidx, weights, tp);
        if (!orders.isEmpty()) if (atom.compareTo(orders.get(orders.size()-1))<=0 ) throw new Exception("bad date added");
        orders.add(atom);
    }
    public void clearOrders() {orders.clear();}
      
    @Override
    public String toString() {
        String s="";
        for (Order o : this.orders)
            try {                
           s+=portfolio.getDate(o.dateidx)+"\t"+o.tp+DoubleArray.toString(o.weigths)+"\n";
            } catch (Exception e) {}
        return s;
    }
}
