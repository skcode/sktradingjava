/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters.temp;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import com.ettoremastrogiacomo.sktradingjava.Security;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import static com.ettoremastrogiacomo.sktradingjava.data.EURONEXT_DataFetch.fetchEuroNext;
import com.ettoremastrogiacomo.sktradingjava.data.FetchData;
import static com.ettoremastrogiacomo.sktradingjava.data.FetchData.loadintoDB;
import com.ettoremastrogiacomo.utils.DoubleArray;
import org.apache.log4j.Logger;
import com.ettoremastrogiacomo.utils.UDate;
import java.io.FileReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import sun.security.util.ArrayUtil;

/**
 *
 * @author a241448
 */
public class Temp {

    static Logger LOG = Logger.getLogger(Temp.class);

    public static JSONArray tree2json(TreeMap<UDate, ArrayList<Double>> m) {

        JSONArray array = new JSONArray();
        for (UDate d : m.keySet()) {
            JSONObject jsonObject = new JSONObject();
            JSONObject jsonObject2 = new JSONObject();
            jsonObject2.put("open", m.get(d).get(0));
            jsonObject2.put("high", m.get(d).get(1));
            jsonObject2.put("low", m.get(d).get(2));
            jsonObject2.put("close", m.get(d).get(3));
            jsonObject2.put("volume", m.get(d).get(4));
            if (m.get(d).size() < 6) {
                jsonObject2.put("oi", 0);
            } else {
                jsonObject2.put("oi", m.get(d).get(5));
            }
            jsonObject.put("date", d.toYYYYMMDD());
            jsonObject.put("data", jsonObject2);
            array.put(jsonObject);
        }
        return array;
    }

    public static void main(String[] args) throws Exception {
        // com.ettoremastrogiacomo.sktradingjava.data.Database.getFintsQuotes(Optional.of("ENGI"),Optional.of("EURONEXT-XPAR") , Optional.empty());
        //LOG.debug(FetchData.fetchYahooQuotes("MSFT"));
        JSONArray arr;
        String mindd = "20210429100757.MINDD.smartportfolio.json";
        String mincorr = "20210429114943.MINCORREQUITYBH.smartportfolio.json";
        try ( FileReader f = new FileReader(mindd)) {
            JSONTokener tokener = new JSONTokener(f);
            JSONObject json = new JSONObject(tokener);
            arr = (JSONArray) json.get("data");
            LOG.debug(arr.get(arr.length() - 1));
        }
        JSONObject json = (JSONObject) arr.get(arr.length() - 1);

        Fints all = new Fints();
        ArrayList<String> hcodes = new ArrayList<>();
        ArrayList<Double> w = new ArrayList<>();

        json.toMap().keySet().forEach((x) -> {
            try {
                String[] s = x.split("\\.");
                String code = s[0];

                String market = s[1];
                w.add((Double) json.toMap().get(x));
                hcodes.add(Database.getHashcode(code, market));
            } catch (Exception e) {
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

        for (int i = 0; i < all.getNoSeries(); i++) {
            LOG.debug(all.getName(i) + "\t" + weights[i]);
        }
        LOG.debug("size :" + all.getNoSeries());
        LOG.debug("maxdd :" + eq.getMaxDD(0));
        LOG.debug("profit :" + eq.getLastValueInCol(0));
        eq.plot("equity", "val");
    }
}
