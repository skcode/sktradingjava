/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.nnet;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Optional;

/**
 *
 * @author a241448
 */
public class Nnet {

    final ArrayList<Neuron> input=new ArrayList<>();
    final ArrayList<Neuron> output=new ArrayList<>();
    double lr=0.01;
    public Nnet(int isize, int osize, Optional<ActivationFunction.FUNCTIONS> ifun,Optional<ActivationFunction.FUNCTIONS> ofun) {
        for (int i=0;i<isize;i++) input.add(new Neuron(ifun.orElse(ActivationFunction.FUNCTIONS.IDENTITY)));
        for (int i=0;i<osize;i++) output.add(new Neuron(ofun.orElse(ActivationFunction.FUNCTIONS.SIGMOID)));        
    }
    void addSinapse(Entry<Neuron,Integer> from, Neuron to){
        to.addInput(from);
    }
        
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Nnet n1= new Nnet(2, 1, Optional.empty(), Optional.empty());
       
        // TODO code application logic here
    }
    
}
