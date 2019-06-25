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
    static java.util.Random rval; //generatore numeri casuali
    static {rval= new Random(System.currentTimeMillis()); }
    static double getRandom() {        //fornisce un numero da -1 a 1 uniformemente casuale
        return (rval.nextDouble()-.5)*2;//from -1 to 1
    }    
    public final ActivationFunction act;    
    double BIAS=getRandom(); //inizializzo il bias
    ArrayList<Double> outputs=new ArrayList<>();//output in modo temporale
    HashMap<Entry<Neuron,Integer>,Double> inputNeurons; //neuroni di input  <Neurone,Lag>,peso  
   // public ArrayList<Double> outputs=new ArrayList<>(); //neuroni di output
    /**
     * 
     * @param fun funzione di attivazione
     */
    public Neuron(ActivationFunction.FUNCTIONS fun) {
        this.act = new ActivationFunction(fun);        
        this.inputNeurons=new HashMap<>();
        
    }    
    /**
     * 
     * @param n entry <Neurone con lag>
     * @param w peso, opzionale, se non inserito viene messo casualmente
     */
    public void addInput(Entry<Neuron ,Integer> n,Optional<Double> w) {//neuron with lag, if lag = 0 no lag
        inputNeurons.put(n,w.orElse(getRandom()) );//inizializzo il peso random        
    }
    /**
     * 
     * @param n neurone con lag
     * @param weight peso aggiornato
     */
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
            z+=idx>=0? inputNeurons.get(n)*n.getKey().outputs.get(idx):0;//divero da zero se il lag è contenuto nell'array degli output
        }
        z+=BIAS;        
        double o=act.eval.apply(z);
        outputs.add(o);
        return o;
    }
  
}
