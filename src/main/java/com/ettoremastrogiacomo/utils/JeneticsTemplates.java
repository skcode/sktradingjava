/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.utils;

import io.jenetics.BitChromosome;
import io.jenetics.BitGene;
import io.jenetics.DoubleChromosome;
import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.jenetics.Phenotype;
import io.jenetics.engine.Codec;
import io.jenetics.engine.Engine;
import static io.jenetics.engine.EvolutionResult.toBestPhenotype;
import io.jenetics.engine.Problem;
import io.jenetics.util.DoubleRange;
import io.jenetics.util.IntRange;
import java.util.ArrayList;
import java.util.function.Function;

/**
 *
 * @author sk
 */
final class jeneticsDouble2Double implements Problem<ArrayList<Double>, DoubleGene, Double> {

    final DoubleRange v1Domain;
    final int elements, ngens;
    final Function<ArrayList<Double>, Double> f;

    public jeneticsDouble2Double(DoubleRange ir, int elements, int ngens, Function<ArrayList<Double>, Double> fitness) {
        v1Domain = ir;
        this.elements = elements;
        this.ngens = ngens;
        this.f = fitness;
    }

    @Override
    public Function<ArrayList<Double>, Double> fitness() {
        return this.f;
    }

    @Override
    public Codec<ArrayList<Double>, DoubleGene> codec() {
        //IntRange v1Domain=IntRange.of(0, 100);    
        ArrayList<DoubleChromosome> arr = new ArrayList<>();
        for (int i = 0; i < this.elements; i++) {
            arr.add(DoubleChromosome.of(v1Domain));
        }
        return Codec.of(
                Genotype.of(arr),
                (gt) -> {
                    ArrayList<Double> ret = new ArrayList<>();
                    gt.forEach((x) -> ret.add(x.getGene().doubleValue()));
                    return ret;
                }
        );
    }

}

final class jeneticsInteger2Double implements Problem<ArrayList<Integer>, IntegerGene, Double> {

    final IntRange v1Domain;
    final int elements, ngens;
    final Function<ArrayList<Integer>, Double> f;

    public jeneticsInteger2Double(IntRange ir, int elements, int ngens, Function<ArrayList<Integer>, Double> fitness) {
        v1Domain = ir;
        this.elements = elements;
        this.ngens = ngens;
        this.f = fitness;
    }

    @Override
    public Function<ArrayList<Integer>, Double> fitness() {
        return this.f;
    }

    @Override
    public Codec<ArrayList<Integer>, IntegerGene> codec() {
        //IntRange v1Domain=IntRange.of(0, 100);    
        ArrayList<IntegerChromosome> arr = new ArrayList<>();
        for (int i = 0; i < this.elements; i++) {
            arr.add(IntegerChromosome.of(v1Domain));
        }
        return Codec.of(
                Genotype.of(arr),
                (gt) -> {
                    ArrayList<Integer> ret = new ArrayList<>();
                    gt.forEach((x) -> ret.add(x.getGene().intValue()));
                    return ret;
                }
        );
    }
}


final class jeneticsBoolean2Double implements Problem<ArrayList<Boolean>, BitGene, Double> {


    final int elements, ngens;
    final Function<ArrayList<Boolean>, Double> f;

    public jeneticsBoolean2Double(int elements, int ngens, Function<ArrayList<Boolean>, Double> fitness) {
        this.elements = elements;
        this.ngens = ngens;
        this.f = fitness;
    }

    @Override
    public Function<ArrayList<Boolean>, Double> fitness() {
        return this.f;
    }

    @Override
    public Codec<ArrayList<Boolean>, BitGene> codec() {
        //IntRange v1Domain=IntRange.of(0, 100);    
        ArrayList<BitChromosome> arr = new ArrayList<>();
        for (int i = 0; i < this.elements; i++) {
            arr.add( BitChromosome.of(1) );
        }
        return Codec.of(
                Genotype.of(arr),
                (gt) -> {
                    ArrayList<Boolean> ret = new ArrayList<>();
                    gt.forEach((x) -> ret.add(x.getGene().booleanValue()));
                    return ret;
                }
        );
    }
}


public class JeneticsTemplates {

    public static Phenotype<DoubleGene, Double> Double2Double(DoubleRange dr, int elements, int generations, Function<ArrayList<Double>, Double> fitness) {
        Problem<ArrayList<Double>, DoubleGene, Double> prob = new jeneticsDouble2Double(dr, elements, generations, fitness);
        Engine<DoubleGene, Double> engine = Engine.builder(prob).build();
        return engine.stream().limit(generations).collect(toBestPhenotype());
    }

    public static Phenotype<IntegerGene, Double> Integer2Double(IntRange dr, int elements, int generations, Function<ArrayList<Integer>, Double> fitness) {
        Problem<ArrayList<Integer>, IntegerGene, Double> prob = new jeneticsInteger2Double(dr, elements, generations, fitness);
        Engine<IntegerGene, Double> engine = Engine.builder(prob).build();
        return engine.stream().limit(generations).collect(toBestPhenotype());
    }
    
    public static Phenotype<BitGene, Double> Boolean2Double( int elements, int generations, Function<ArrayList<Boolean>, Double> fitness) {
        Problem<ArrayList<Boolean>, BitGene, Double> prob = new jeneticsBoolean2Double(elements, generations, fitness);
        Engine<BitGene, Double> engine = Engine.builder(prob).build();
        return engine.stream().limit(generations).collect(toBestPhenotype());
    }
    
    static public void main(String[] args) {
        Function<ArrayList<Double>, Double> f = (i) -> {
            double d = 0;
            for (Double x : i) {
                d += x;
            }
            return d;
        };
        Function<ArrayList<Integer>, Double> f2 = (i) -> {
            double d = 0;
            for (Integer x : i) {
                d += x;
            }
            return d;
        };
        Function<ArrayList<Boolean>, Double> f3 = (i) -> {
            double d = 0;
            for (Boolean x : i) {
                d += x?1:0;
            }
            return d;
        };
                
        Phenotype<DoubleGene, Double> fen=Double2Double(DoubleRange.of(0, 100), 4, 1000, f);
        Phenotype<IntegerGene, Double> fen2=Integer2Double(IntRange.of(0, 100), 4, 1000, f2);
        Phenotype<BitGene, Double> fen3=Boolean2Double( 4, 1000, f3);
        System.out.println(fen);
        System.out.println(fen2);
        System.out.println(fen3);
    }

}
