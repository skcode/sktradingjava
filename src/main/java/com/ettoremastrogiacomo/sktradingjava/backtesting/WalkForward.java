/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.backtesting;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import com.ettoremastrogiacomo.sktradingjava.system.TradingSystem;
import com.ettoremastrogiacomo.utils.UDate;
import java.util.Optional;
import org.apache.log4j.Logger;


/**
 *
 * @author sk
 */
public class WalkForward {

    private final UDate from, to;
    private final int training, test;
    private final TradingSystem ts;
    private final Portfolio ptf;
    private final MT_Optimizer.fitobjective fitness;
    private final Double initCapital, penalty_pc, penalty_abs;
    private final int startidx, endidx;
    static Logger LOG = Logger.getLogger(WalkForward.class);

    public WalkForward(TradingSystem ts, UDate from, UDate to, int training_window, int test_window, MT_Optimizer.fitobjective fo, Optional<Double> init_capital, Optional<Double> penalty_pc, Optional<Double> penalty_abs) throws Exception {
        this.from = from;
        this.to = to;
        this.training = training_window;
        this.test = test_window;
        this.ts = ts;
        this.fitness = fo;
        this.ptf = ts.getPortfolio();
        this.initCapital = init_capital.orElse(1000.0);
        this.penalty_pc = penalty_pc.orElse(0.0);
        this.penalty_abs = penalty_abs.orElse(0.0);
        this.startidx = ptf.getClose().getDateIdxFrom(from);
        this.endidx = ptf.getClose().getDateIdxTo(to);
        int len = this.endidx - this.startidx + 1;
        if (len < (this.training + this.test)) {
            throw new Exception("length too short for test : " + len);
        }
    }

    public Fints run(Optional<Integer> epochs) throws Exception {
        int tridx = this.startidx;
        int testidx = tridx + this.training;
        int lastidx = testidx + this.test - 1;
        Fints alleq = null;
        while (true) {
            if (lastidx >= this.ptf.getLength()) {
                break;
            }
            MT_Optimizer tsopt = new com.ettoremastrogiacomo.sktradingjava.backtesting.MT_Optimizer(ts, ptf.getDate(tridx), ptf.getDate(testidx - 1), epochs, Optional.of(this.initCapital), Optional.of(this.penalty_pc), Optional.of(this.penalty_abs));
            tsopt.setFitnessObjective(this.fitness);
            //logger.info("start optimizing");
            java.util.HashMap<int[],Double> map=new java.util.HashMap<>();
            int [] cr = tsopt.run(map);
            //this.ts.setParams(cr);
            double ic=alleq == null? this.initCapital:alleq.get(alleq.getLength()-1, 0);
            Backtest bk = new com.ettoremastrogiacomo.sktradingjava.backtesting.Backtest(this.ptf, Optional.of(ic), Optional.of(this.penalty_pc), Optional.of(this.penalty_abs));
            Statistics stats = bk.apply(this.ts.apply(ptf.getDate(testidx), ptf.getDate(lastidx),cr), ptf.getDate(testidx), ptf.getDate(lastidx));
            alleq = alleq == null ? stats.equity : Fints.append(alleq, stats.equity);
            LOG.debug(stats);
            tridx += this.test;
            testidx += this.test;
            lastidx += this.test;
        }
        return alleq;
    }
}
