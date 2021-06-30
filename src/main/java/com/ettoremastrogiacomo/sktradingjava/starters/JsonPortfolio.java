/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Security;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.DoubleArray;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 *
 * @author sk
 */
public class JsonPortfolio {
        static Logger LOG = Logger.getLogger(JsonPortfolio.class);
        public static void main(String[] args) throws Exception {
        // com.ettoremastrogiacomo.sktradingjava.data.Database.getFintsQuotes(Optional.of("ENGI"),Optional.of("EURONEXT-XPAR") , Optional.empty());
        //LOG.debug(FetchData.fetchYahooQuotes("MSFT"));
        JSONArray arr;
        String mindd = "20210501182015.MINDD.smartportfolio.json";
        String mincorr = "20210622150020.MINCORREQUITYBH.smartportfolio.json";
        try ( FileReader f = new FileReader(mincorr)) {
            JSONTokener tokener = new JSONTokener(f);
            JSONObject json = new JSONObject(tokener);
            arr = (JSONArray) json.get("data");
            LOG.debug(arr.get(arr.length() - 1));
        }
        JSONObject json = (JSONObject) arr.get(arr.length() - 1);

        Fints all = new Fints();
        ArrayList<String> hcodes = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();        
        ArrayList<Double> w = new ArrayList<>();

        json.toMap().keySet().forEach((String x) -> {
            try {
                String[] s = x.split("\\.");
                names.add(x);
                String code = s[0];
                String market = s[1];             
                Object o=  json.toMap().get(x);
                double d=0;
                if (o instanceof Integer) {
                    d=((Integer) o).intValue();
                } else if (o instanceof Double) {
                    d= ((Double) o).doubleValue();
                } else throw new Exception("unknow instance");
                w.add(d);
                hcodes.add(Database.getHashcode(code, market));
            } catch (Exception e) {
                LOG.error(e);
                System.exit(1);
            }
        });
        LOG.debug(hcodes.size() + "\t" + w.size());
        for (String x : hcodes) {
            all = all.isEmpty() ? Database.getFintsQuotes(x).getSerieCopy(Security.SERIE.CLOSE.getValue()) : all.merge(Database.getFintsQuotes(x).getSerieCopy(Security.SERIE.CLOSE.getValue()));
        }
        LOG.debug(all.toString());
        double[][] cer = all.getMatrixCopy();
        double[][] meq = new double[cer.length][1];
        meq[0][0] = 1.0;
        double[] weights = w.stream().mapToDouble(d -> d).toArray();
        if (weights.length != cer[0].length) {
            throw new RuntimeException(" wrong weights len ");
        }
        double sw = DoubleArray.sum(weights);
        for (int i = 0; i < weights.length; i++) {
            weights[i] /= sw;
        }
        for (int i = 1; i < cer.length; i++) {
            double cv = 0;
            for (int j = 0; j < cer[i].length; j++) {
                cv += (cer[i][j] - cer[i - 1][j]) * weights[j] / cer[i - 1][j];
            }
            meq[i][0] = meq[i - 1][0] * (1 + cv);
        }
        Fints eq = new Fints(all.getDate(), Arrays.asList("Weighted Equity"), all.getFrequency(), meq);
        double reccsize=(2000/DoubleArray.min(weights));
        for (int i = 0; i < all.getNoSeries(); i++) {
            LOG.debug(all.getName(i) + "\t"+names.get(i)+"\t" + String.format("%.4f", weights[i])+"\t"+String.format("%.2f",reccsize*weights[i]));
        }
        
        LOG.debug("size :" + all.getNoSeries());
        LOG.debug("maxdd :" + eq.getMaxDD(0));
        LOG.debug("profit :" + eq.getLastValueInCol(0));
        LOG.debug("profit/maxdd ratio : "+eq.getLastValueInCol(0)/(Math.abs(eq.getMaxDD(0))));
        LOG.debug("recommended ptf size € : "+String.format("%.2f",reccsize));
        eq.plot("equity", "val");
    }
}
