/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package com.ettoremastrogiacomo.sktradingjava.starters;
 
import io.jenetics.DoubleChromosome;
import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.jenetics.Phenotype;
import io.jenetics.engine.Codec;
import io.jenetics.engine.Engine;
import static io.jenetics.engine.EvolutionResult.toBestPhenotype;
import io.jenetics.engine.EvolutionStatistics;
import io.jenetics.engine.Problem;
import io.jenetics.util.DoubleRange;
import io.jenetics.util.IntRange;
import java.util.ArrayList;
import java.util.function.Function;
 
/**
*
* @author a241448
* As we can see, we implement the Problem<T, G, C>, that has three parameters:
    <T> – the argument type of the problem fitness function, in our case an immutable, ordered, fixed sized Integer sequence ISeq<Integer>
    <G> – the gene type the evolution engine is working with, in this case, countable Integer genes EnumGene<Integer>
    <C> – the result type of the fitness function; here it is an Integer
*/
 
 
final class jenetics2 implements Problem<ArrayList<Integer>, IntegerGene, Double> {
    final IntRange v1Domain;
    final int elements,ngens;
    public jenetics2(IntRange ir,int elements, int ngens) {
        v1Domain=ir;
        this.elements=elements;
        this.ngens=ngens;
        this.run();
    }
    @Override
    public Function<ArrayList<Integer>, Double> fitness() {
        return ((subset) -> {
            return  Math.abs(subset.stream().mapToInt(Integer::intValue).sum())/(double)this.elements;            
        });
 
    }
// codifica da enumgene all'input della funzione di fitness ISeq<Integer>, che può essere qualsiasi classe xro
    @Override
    public Codec<ArrayList<Integer>, IntegerGene> codec() {
        //IntRange v1Domain=IntRange.of(0, 100);    
        ArrayList<IntegerChromosome> arr= new ArrayList<>();       
        for (int i=0;i<this.elements;i++) arr.add(IntegerChromosome.of(v1Domain));
        return Codec.of(
                                               Genotype.of(
                                                               /*IntegerChromosome.of(IntRange.of(v1Domain.getMin(), v1Domain.getMax())),
                                IntegerChromosome.of(IntRange.of(v1Domain.getMin(), v1Domain.getMax())),
                                IntegerChromosome.of(IntRange.of(v1Domain.getMin(), v1Domain.getMax()))*/
                                arr
                                               ),
                                               (gt) -> {
                            ArrayList<Integer> ret=new ArrayList<>();
                            for (int i=0;i<this.elements;i++) ret.add(gt.getChromosome(i).getGene().intValue());
                            return ret;
                            /*return new ArrayList<>(Arrays.asList(                               
                                gt.getChromosome(0).getGene().intValue(),
                                                               gt.getChromosome(1).getGene().intValue(),
                                gt.getChromosome(2).getGene().intValue()
                                )
                            );*/
                        }
                               );
    }  
    
    public void run() {
                               final Engine<IntegerGene, Double> engine = Engine
                                               .builder(this)
                                               .build();
            final EvolutionStatistics<Double, ?>
                                               statistics = EvolutionStatistics.ofNumber();
            final Phenotype<IntegerGene, Double> best = engine.stream()
                                               // Truncate the evolution stream after 7 "steady"
                                               // generations.
                                               //.limit(bySteadyFitness(10))
                                               // The evolution will stop after maximal 1000
                                               // generations.
                                               .limit(this.ngens)
                                               // Update the evaluation statistics after
                                               // each generation
                                               .peek(statistics)
                                               // Collect (reduce) the evolution stream to
                                               // its best phenotype.
                                               .collect(toBestPhenotype());
                               System.out.println(statistics);
                               System.out.println(best);           
    }
}
 
 
final class jenetics3 implements Problem<ArrayList<Double>, DoubleGene, Double> {
    final DoubleRange v1Domain;
    final int elements,ngens;
    public jenetics3(DoubleRange ir,int elements, int ngens) {
        v1Domain=ir;
        this.elements=elements;
        this.ngens=ngens;
        this.run();
    }
    @Override
    public Function<ArrayList<Double>, Double> fitness() {
        return ((subset) -> {
            return  Math.abs(subset.stream().mapToDouble(Double::doubleValue).sum())/(double)this.elements;            
        });
 
    }
// codifica da enumgene all'input della funzione di fitness ISeq<Integer>, che può essere qualsiasi classe xro
    @Override
    public Codec<ArrayList<Double>, DoubleGene> codec() {
        //IntRange v1Domain=IntRange.of(0, 100);    
        ArrayList<DoubleChromosome> arr= new ArrayList<>();       
        for (int i=0;i<this.elements;i++) arr.add(DoubleChromosome.of(v1Domain));
        return Codec.of(
                                               Genotype.of(
                                                               /*IntegerChromosome.of(IntRange.of(v1Domain.getMin(), v1Domain.getMax())),
                                IntegerChromosome.of(IntRange.of(v1Domain.getMin(), v1Domain.getMax())),
                                IntegerChromosome.of(IntRange.of(v1Domain.getMin(), v1Domain.getMax()))*/
                                arr
                                               ),
                                               (gt) -> {
                            ArrayList<Double> ret=new ArrayList<>();
                            for (int i=0;i<this.elements;i++) ret.add(gt.getChromosome(i).getGene().doubleValue());
                            return ret;
                            /*return new ArrayList<>(Arrays.asList(                               
                                gt.getChromosome(0).getGene().intValue(),
                                                               gt.getChromosome(1).getGene().intValue(),
                                gt.getChromosome(2).getGene().intValue()
                                )
                            );*/
                        }
                               );
    }  
    
    public void run() {
                               final Engine<DoubleGene, Double> engine = Engine
                                               .builder(this)
                                               .build();
            final EvolutionStatistics<Double, ?>
                                               statistics = EvolutionStatistics.ofNumber();
            final Phenotype<DoubleGene, Double> best = engine.stream()
                                               // Truncate the evolution stream after 7 "steady"
                                               // generations.
                                               //.limit(bySteadyFitness(10))
                                               // The evolution will stop after maximal 1000
                                               // generations.
                                               .limit(this.ngens)
                                               // Update the evaluation statistics after
                                               // each generation
                                               .peek(statistics)
                                               // Collect (reduce) the evolution stream to
                                               // its best phenotype.
                                               .collect(toBestPhenotype());
                               System.out.println(statistics);
                               System.out.println(best);           
    }
}
 
 
final class jenetics4 implements Problem<ArrayList<Double>, DoubleGene, Double> {
    final DoubleRange v1Domain;
    final int elements,ngens;
    final boolean duplicates;
    final Function<ArrayList<Double>,Double> f;
    public jenetics4(DoubleRange ir,int elements, int ngens,Function<ArrayList<Double>,Double> f,boolean duplicates) {
        v1Domain=ir;
        this.elements=elements;
        this.ngens=ngens;
        this.f=f;
        this.duplicates=duplicates;
        this.run();       
    }
    @Override
    public Function<ArrayList<Double>, Double> fitness() {
        return this.f;
    }
    @Override
    public Codec<ArrayList<Double>, DoubleGene> codec() {
        //IntRange v1Domain=IntRange.of(0, 100);    
        ArrayList<DoubleChromosome> arr= new ArrayList<>();       
        for (int i=0;i<this.elements;i++) arr.add(DoubleChromosome.of(v1Domain));       
        return Codec.of(
                                               Genotype.of(arr),
                                               (gt) -> {
                            ArrayList<Double> ret=new ArrayList<>(); 
                            gt.forEach((x)->ret.add(x.getGene().doubleValue()));                                                        
                            return ret;
                        }
                               );
    }  
    
    public void run() {
                               final Engine<DoubleGene, Double> engine = Engine
                                               .builder(this)
                                               .build();
            final EvolutionStatistics<Double, ?>
                                               statistics = EvolutionStatistics.ofNumber();
            final Phenotype<DoubleGene, Double> best = engine.stream()
                                               // Truncate the evolution stream after 7 "steady"
                                               // generations.
                                               //.limit(bySteadyFitness(10))
                                               // The evolution will stop after maximal 1000
                                               // generations.
                                               .limit(this.ngens)
                                               // Update the evaluation statistics after
                                               // each generation
                                               .peek(statistics)
                                               // Collect (reduce) the evolution stream to
                                               // its best phenotype.
                                               .collect(toBestPhenotype());
                               System.out.println(statistics);
                               System.out.println(best);           
    }
}
 
 
 
 
 
public class jenetics1  {
    public static void main(String[] args){
        final IntRange domain1 = IntRange.of(0, 100);
       final DoubleRange domain2 = DoubleRange.of(0, 100);
        Problem<ArrayList<Integer>, IntegerGene, Double> prob= new jenetics2(domain1,4,1000);        
        System.out.println("\n\n\n");
        Problem<ArrayList<Double>, DoubleGene, Double> prob2= new jenetics3(domain2,4,1000);        
        Function<ArrayList<Double>, Double> f= (i) -> {double d=0; for (Double x:i) d+=x;return d;};
        Problem<ArrayList<Double>, DoubleGene, Double> prob3= new jenetics4(domain2,4,1000,f,true);        
    }
}
 