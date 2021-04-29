/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import static com.ettoremastrogiacomo.sktradingjava.starters.Rankings.logger;
import com.ettoremastrogiacomo.utils.DoubleArray;
import com.ettoremastrogiacomo.utils.UDate;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author sk
 */
public class SmartPortfolio {

    static public org.apache.log4j.Logger LOG = Logger.getLogger(SmartPortfolio.class);

    public static void main(String[] args) throws Exception {
        int minsamples = 500, maxsamples = 2500, stepsamples = 250, maxdaygap = 7, maxold = 30, minvol = 10000, minvoletf = 0, setmin = 20, setmax = 50, popsize = 20000, ngen = 2000;
        double maxpcgap = .2;
        Portfolio.optMethod optm = Portfolio.optMethod.MINCORREQUITYBH;
        boolean plot = false, plotlist = false;
        HashMap<String, Integer> list = new HashMap<>();
        //Set<String> listhash= new HashSet<>();
        //String filename="./hashlist";
        HashMap<Integer, Double> meaneq = new HashMap<>();
        HashMap<Integer, Double> meanmaxdd = new HashMap<>();
        JSONObject jsonAll = new JSONObject();
        JSONArray jsonArr = new JSONArray();

        jsonAll.put("date", (new UDate()).time);
        jsonAll.put("optmethod", optm.toString());
        jsonAll.put("minvol", minvol);
        jsonAll.put("setmin", setmin);
        jsonAll.put("setmax", setmax);

        for (int samples = minsamples; samples <= maxsamples; samples = samples + stepsamples) {
            JSONObject json = new JSONObject();

            json.put("samples", samples);

            //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.create_STOCK_NYSE_NASDAQ_Portfolio(Optional.of(samples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol));
            Portfolio ptf = com.ettoremastrogiacomo.sktradingjava.Portfolio.create_STOCK_EUR_Portfolio(Optional.of(samples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol));
            //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.create_ETF_INDICIZZATI_MLSE_Portfolio(Optional.of(samples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvoletf));
            //    Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.create_ETF_INDICIZZATI_AZIONARIO_MLSE_Portfolio(Optional.of(samples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvoletf));
            //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.create_ETF_INDICIZZATI_AZIONARIO_exCOMMODITIES_MLSE_Portfolio(Optional.of(samples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvoletf));
            //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.create_ETF_MLSE_Portfolio(Optional.of(samples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvoletf));
            //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.create_ETF_INDICIZZATI_AZIONARIO_GLOBALI_exCOMMODITIES_MLSE_Portfolio(Optional.of(samples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvoletf));
            //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.create_ETF_ATTIVI_MLSE_Portfolio(Optional.of(samples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvoletf));
            //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.create_ETF_BENCHAZIONARIO_MLSE_Portfolio(Optional.of(samples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvoletf));
            //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.create_ETF_NYSE_Portfolio(Optional.of(samples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvoletf));
            int SIZE = samples < ptf.getLength() ? samples : ptf.getLength() - 1;
            logger.info("************************ optimization GA " + optm.toString() + " ************************ ");
            logger.info("no sec " + ptf.getNoSecurities());
            logger.info("days gap " + ptf.closeER.getMaxDaysDateGap());
            logger.info("len " + ptf.getLength());
            logger.info("minvol " + minvol);
            logger.info("minvoletf " + minvoletf);
            logger.info("start date " + ptf.getDate(0) + "\tend date " + ptf.getDate(ptf.getLength() - 1));
            UDate train_enddate = ptf.dates.get(ptf.dates.size() - 1);
            UDate train_startdate = ptf.dates.get(ptf.dates.size() - SIZE);
            setmax = setmax > ptf.getNoSecurities() ? ptf.getNoSecurities() : setmax;
            json.put("nosec", ptf.getNoSecurities());
            json.put("startdate", train_startdate);
            json.put("enddate", train_enddate);

            Map.Entry<Double, ArrayList<Integer>> winner = ptf.opttrain(train_startdate, train_enddate, setmin, setmax, optm, plot, popsize, ngen);
            logger.info(train_startdate + "\tto\t" + train_enddate + "\tsamples " + ptf.closeER.Sub(train_startdate, train_enddate).getLength());
            logger.info("setmin " + setmin + "\tsetmax " + setmax);
            logger.info("BEST FITNESS " + winner.getKey());
            logger.info("BEST FITNESS INVERSE " + 1.0 / winner.getKey());
            logger.info("BEST " + winner.getValue());
            double[] w = new double[winner.getValue().size()];
            DoubleArray.fill(w, 1.0 / winner.getValue().size());
            logger.info("BEST LOG VAR " + ptf.closeERlog.SubSeries(winner.getValue()).head(SIZE).getWeightedCovariance(w));
            logger.info("BEST VAR check " + ptf.closeER.SubSeries(winner.getValue()).head(SIZE).getWeightedCovariance(w));
            logger.info("LOG VAR CAMPIONE " + Fints.ER(ptf.closeCampione, 100, true).head(SIZE).getCovariance()[0][0]);
            logger.info("BEST LEN " + winner.getValue().size());

            json.put("fitness", winner.getKey());
            json.put("variance", ptf.closeERlog.SubSeries(winner.getValue()).head(SIZE).getWeightedCovariance(w));
            //JSONArray hcodes=new JSONArray();
            //hcodes.put(ptf.hashcodes.get(x));
            //ptf.getName(ptf.hashcodes.get(x))
            HashMap<String, String> hcodes = new HashMap<>();
            winner.getValue().forEach((x) -> {
                String t1 = ptf.getName(ptf.hashcodes.get(x));
                try {
                    double d1 = ptf.getClose().getSerieCopy(x).getEquity().getLastValueInCol(0);
                    d1 = (d1 - 1) * 100;
                    logger.info(t1 + "\t" + String.format("%.2f", d1) + "%");
                } catch (Exception e) {
                    logger.info(t1);
                }
                hcodes.put(ptf.hashcodes.get(x), ptf.getName(ptf.hashcodes.get(x)));
                //json.put("fitness", ptf.hashcodes.get(x));
                //listhash.add(ptf.hashcodes.get(x));
                if (list.containsKey(t1)) {
                    list.replace(t1, list.get(t1) + 1);
                } else {
                    list.put(t1, 1);
                }
            });
            json.put("shares", hcodes);
            Fints f = ptf.opttest(winner.getValue(), train_startdate, train_enddate, Optional.empty(), Optional.empty());
            logger.info("MAXDD: " + f.getName(0) + "\t" + f.getMaxDD(0));
            logger.info("MAXDD: " + f.getName(1) + "\t" + f.getMaxDD(1));
            logger.info("Final EQ: " + f.getName(0) + "\t" + f.getLastValueInCol(0));
            logger.info("Final EQ: " + f.getName(1) + "\t" + f.getLastValueInCol(1));
            logger.debug("\n\n");
            json.put("maxdd", f.getMaxDD(0));
            json.put("maxddBH", f.getMaxDD(1));
            json.put("finaleq", f.getLastValueInCol(0));
            json.put("finaleqBH", f.getLastValueInCol(1));
            json.put("shares", hcodes);
            meaneq.put(samples, f.getLastValueInCol(0));
            meanmaxdd.put(samples, f.getMaxDD(0));
            f.plot(optm.toString() + " best=" + String.format("%.3f", winner.getKey()) + " size=" + SIZE + " maxdd=" + String.format("%.3f", f.getMaxDD(0)) + " finaleq=" + String.format("%.3f", f.getLastValueInCol(0)), "price");
            if (plotlist) {
                for (Integer x : winner.getValue()) {
                    ptf.getClose().getSerieCopy(x).plot(ptf.getName(ptf.hashcodes.get(x)), "price");
                }

            }
            jsonArr.put(json);
        }
        logger.info("LIST");
        list.keySet().forEach((x) -> logger.info(x + "\t" + list.get(x)));
        double tot = 0;
        HashMap<String, Double> whash = new HashMap<>();
        for (String x : list.keySet()) {
            tot += list.get(x);
        };
        for (String x : list.keySet()) {
            whash.put(x, 100.0*list.get(x) / tot);
        }
        jsonArr.put(whash);
        jsonAll.put("data", jsonArr);
        logger.info("MEAN EQUITY: " + meaneq.values().stream().mapToDouble(i -> i).average().orElse(Double.NaN));
        logger.info("MEAN MAXDD: " + meanmaxdd.values().stream().mapToDouble(i -> i).average().orElse(Double.NaN));
        jsonAll.put("meanequity", meaneq.values().stream().mapToDouble(i -> i).average().orElse(Double.NaN));
        jsonAll.put("meanmaxdd", meanmaxdd.values().stream().mapToDouble(i -> i).average().orElse(Double.NaN));
        try ( FileWriter file = new FileWriter(UDate.now().toYYYYMMDDHHmmss()+"."+optm.toString()+".smartportfolio.json")) {
            //We can write any JSONArray or JSONObject instance to the file
            file.write(jsonAll.toString());
            file.flush();
        } catch (IOException e) {
            LOG.warn(e);
        }
        //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.create_STOCK_EUR_Portfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol));
        //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.createETFSTOCKEURPortfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvoletf));
        //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.create_ETF_INDICIZZATI_AZIONARIO_MLSE_Portfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvoletf));
        //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.create_ETF_MLSE_Portfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvoletf));
        //Misc.writeObjToFile(listhash, filename);
        //StringBuilder tstr= new StringBuilder();tstr.append("Arrays.asList(");
        //((HashSet)Misc.readObjFromFile(filename)).forEach((x)->{tstr.append("\"");tstr.append(x);tstr.append("\"");});
        //tstr.append(")");
        //logger.debug(tstr.toString());
    }
}
