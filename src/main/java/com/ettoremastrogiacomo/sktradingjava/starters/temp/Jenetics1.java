/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters.temp;

/**
 *
 * @author sk
 */
import io.jenetics.*;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.util.Factory;
import java.util.stream.IntStream;
public class Jenetics1 {
    private static int eval(final Genotype<BitGene> gt) {
        int c=0;
        while (gt.iterator().hasNext()){
             Chromosome<BitGene> g=gt.iterator().next();             
             c+=g.stream().mapToInt((i) -> i.booleanValue()?1:0).sum();
             System.out.println(g);
        }
            System.out.println(c);
        
        return c;//gt.getChromosome().as(BitChromosome.class).bitCount();
    }
    public static void main(String[] args) {
        Factory<Genotype<BitGene>> gft= Genotype.of(BitChromosome.of(10, 0.5),BitChromosome.of(10, 0.5));
        
        final Engine<BitGene,Integer> engine = Engine.builder( Jenetics1::eval,gft). build () ;  
         final Genotype<BitGene> result = engine.stream ().limit(100).collect(EvolutionResult.toBestGenotype());
         System.out.println ( " Hello  World :\n\t " + result ) ;
    }
    
}
