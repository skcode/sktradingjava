/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.backtesting;
import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.utils.UDate;
import org.apache.log4j.Logger;

import static org.jenetics.engine.EvolutionResult.toBestPhenotype;
import static org.jenetics.engine.limit.bySteadyFitness;

import org.jenetics.*;
import org.jenetics.engine.Engine;
import org.jenetics.engine.EvolutionStatistics;
/**
 *
 * @author sk
 */
public class PortfolioOptimization {
 
    public PortfolioOptimization() throws Exception {
        
    }
	// This method calculates the fitness for a given genotype.
	private static Integer count(final Genotype<BitGene> gt) {
		return ((BitChromosome)gt.getChromosome()).bitCount();
	}
        private static Double fitness(final Genotype<DoubleGene> dg){
            Chromosome<DoubleGene> d=dg.getChromosome();
            int len=d.length();
            
            return 0.;}

	public static void main(String[] args) {
		// Configure and build the evolution engine.
                final Engine<DoubleGene,Double> fengine = Engine.builder(PortfolioOptimization::fitness,DoubleChromosome.of(0.0,1.0,100))
                        .populationSize(500).selector(new RouletteWheelSelector<>()).alterers(new Mutator<>(.55), new SinglePointCrossover<>(.06)).build();
                
		final Engine<BitGene, Integer> engine = Engine
			.builder(
				PortfolioOptimization::count,
				BitChromosome.of(20, 0.15))
			.populationSize(500)
			.selector(new RouletteWheelSelector<>())
			.alterers(
				new Mutator<>(0.55),
				new SinglePointCrossover<>(0.06))
			.build();

		// Create evolution statistics consumer.
		final EvolutionStatistics<Integer, ?>
			statistics = EvolutionStatistics.ofNumber();

		final Phenotype<BitGene, Integer> best = engine.stream()
			// Truncate the evolution stream after 7 "steady"
			// generations.
			.limit(bySteadyFitness(7))
				// The evolution will stop after maximal 100
				// generations.
			.limit(100)
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