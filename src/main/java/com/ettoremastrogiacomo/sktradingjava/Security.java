package com.ettoremastrogiacomo.sktradingjava;


import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.UDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.log4j.Logger;





public final class Security {
	static Logger logger = Logger.getLogger(Security.class);
	private final String hashcode;
        private final Fints daily;
        private final Fints weekly;
        private final Fints monthly;
        private final java.util.Map<String, String> infomap;
        
        static private Fints changeFreq(Fints f,Fints.frequency newf) throws Exception {
            if (newf.ordinal()<f.getFrequency().ordinal()) throw new Exception("bad input :"+f.getFrequency()+" to "+newf);
            if (newf.ordinal()==f.getFrequency().ordinal()) return f;
            java.util.TreeMap<UDate,java.util.ArrayList<Double>> map=new java.util.TreeMap<>();
            UDate previous=new UDate(0),actual;
            java.util.ArrayList<Double> row;//=new java.util.ArrayList<>(6);
            double high=0,low=0,open=0,close=0,volume=0,oi=0;
            for (int i=0;i<f.getLength();i++){
                actual=f.getDate(i);
                switch (newf) {
                    case MINUTE:
                        actual=UDate.roundMinute(actual);
                        break;
                    case HOUR:
                        actual=UDate.roundHour(actual);
                        break;
                    case DAILY :
                        actual=UDate.roundDayOfMonth(actual);
                        break;         
                    case WEEKLY :
                        actual=UDate.roundWeek(actual);
                        break;                                 
                    case MONTHLY:
                        actual=UDate.roundMonth(actual);
                        break;                        
                    default:
                        throw new Exception("not implemented yet "+newf);
                }
                if (i==0) {
                    open=f.get(i, 0);low=f.get(i, 2);
                    high=f.get(i, 1);close=f.get(i, 3);
                    volume=f.get(i, 4);oi=f.get(i, 5);
                } else {
                    if (actual.after(previous)) {
                        row=new java.util.ArrayList<>();
                        /*row.ensureCapacity(6);
                        row.set(0, open);row.set(1, high);row.set(2, low);
                        row.set(3, close);row.set(4, volume);
                        row.set(5, oi);*/
                        row.add(open);row.add(high);row.add(low);row.add(close);row.add(volume);row.add(oi);
                       map.put(actual, row);
                    } else {
                        if (f.get(i, 1)>high) high=f.get(i, 1);
                        if (f.get(i, 2)<low) low=f.get(i, 2);
                        close=f.get(i, 3);
                        volume+=f.get(i, 4);oi+=f.get(i, 5);
                        if (i==(f.getLength()-1)) {
                            row=new java.util.ArrayList<>();
                            //row.ensureCapacity(6);
                            /*row.set(0, open);row.set(1, high);row.set(2, low);
                            row.set(3, close);row.set(4, volume);
                            row.set(5, oi);*/
                            row.add(open);row.add(high);row.add(low);row.add(close);row.add(volume);row.add(oi);
                            map.put(actual, row);                            
                        }
                    }
                }
            }
            //Fints second=Database.getIntradayFintsQuotes(isin, date);
            double[][] newmat=new double[map.size()][6];
            int i=0;
            java.util.ArrayList<UDate> newdates=new java.util.ArrayList<>();
            for (UDate d : map.keySet()){
                List<Double> l=map.get(d);
                newdates.add(d);
                for (int j=0;j<l.size();j++) newmat[i][j]=l.get(j);
                i++;
            }
            return new Fints(newdates, f.getName(), newf, newmat);
        }
	public Security(String hashcode) throws Exception {
                this.hashcode=hashcode;
                infomap =Database.getRecords(Optional.of(Arrays.asList(hashcode)),Optional.empty(),Optional.empty() , Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()).get(0) ;                
		daily=Database.getFintsQuotes(Optional.of(infomap.get("code")), Optional.of(infomap.get("market")),Optional.empty());
                //weekly=Fints.changeFrequency(daily, Fints.frequency.DAILY);// .daily2weekly(daily);
                weekly=changeFreq(daily,Fints.frequency.WEEKLY);
                monthly=changeFreq(daily,Fints.frequency.MONTHLY);
	}

        public Fints getDaily() {return daily;}
        public Fints getWeekly() {return weekly;}
        public Fints getMonthly() {return monthly;}
        public Fints getIntradaySecond(UDate date) throws Exception{
            return Database.getIntradayFintsQuotes(hashcode, date);
        }
        public Fints getIntradayMinute(UDate date) throws Exception{
            return Security.changeFreq(Database.getIntradayFintsQuotes(hashcode, date), Fints.frequency.MINUTE);
        }
        public Fints getIntradayHour(UDate date) throws Exception{
            return Security.changeFreq(Database.getIntradayFintsQuotes(hashcode, date), Fints.frequency.HOUR);
        }
        public String getHashcode() {return infomap.get("hashcode");}        
        public String getIsin() {return infomap.get("isin");}        
        public String getName() {return infomap.get("name");}
        public String getCode() {return infomap.get("code");}
        public String getMarket() {return infomap.get("market");}
        public String getCurrency() {return infomap.get("currency");}
        public String getSector() {return infomap.get("sector");}
        public String getType() {return infomap.get("type");}
}
