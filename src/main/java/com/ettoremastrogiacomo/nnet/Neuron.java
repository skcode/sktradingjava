/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.nnet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import org.apache.log4j.Logger;

/**
 *
 * @author a241448
 */
public class Neuron {
    static Logger logger = Logger.getLogger(Neuron.class);    
    
    static java.util.Random rval;
    static {rval= new Random(System.currentTimeMillis()); }
    static double getRandom() {        
        return (rval.nextDouble()-.5)*2;//from -1 to 1
    }    
    public final ActivationFunction act;    
    double BIAS=getRandom();
    HashMap<Entry<Neuron,Integer>,Double> inputNeurons;
    
    public ArrayList<Double> outputs=new ArrayList<>();
    public Neuron(ActivationFunction.FUNCTIONS fun) {
        this.act = new ActivationFunction(fun);        
        this.inputNeurons=new HashMap<>();
        
    }    
    public void addInput(Entry<Neuron ,Integer> n) {//neuron with lag, if lag = 0 no lag
        inputNeurons.put(n,getRandom());
        
    }
    public void setWeight(Entry<Neuron ,Integer> n,double weight) {
        if (inputNeurons.containsKey(n)){            
            inputNeurons.replace(n,weight);        
        }        
        else throw new IllegalArgumentException("Neuron "+n+" not found");
    }    
    public void setBIAS(double d) {this.BIAS=d;}
    public void removeInput(Entry<Neuron ,Integer> n) {inputNeurons.remove(n);}
    public void removeAllInput() {inputNeurons.clear();}
    public double output() {
        double z=0;
        for (Entry<Neuron,Integer> n: inputNeurons.keySet()) {            
            int lag=n.getValue();
            int idx=outputs.size()-1-lag;
            z+=idx>=0? inputNeurons.get(n)*n.getKey().outputs.get(idx):0;            
        }
        z+=BIAS;        
        double o=act.eval.apply(z);
        outputs.add(o);
        return o;
    }
  
}
