package com.ettoremastrogiacomo.sktradingjava;


import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.DoubleDoubleArray;

import com.ettoremastrogiacomo.utils.UDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
        static public enum SERIE {OPEN(0),HIGH(1),LOW(2),CLOSE(3),VOLUME(4),OI(5);           
            private final int value;
            SERIE(final int newValue) {
                value = newValue;
            }
            public int getValue() { return value; }                
        };

        static public  enum secType {STOCK("STOCK"), ETF("ETF"), ETCETN("ETCETN"), FUTURE("FUTURE"), BOND("BOND"), CURRENCY("CURRENCY"), INDEX("INDEX");
            private final String url;

            secType(String envUrl) {
                this.url = envUrl;
            }
            public static secType getEnum(String s) {
                if (s.equalsIgnoreCase("STOCK")) return secType.STOCK;
                else if (s.equalsIgnoreCase("ETF")) return secType.ETF;
                else if (s.equalsIgnoreCase("ETCETN")) return secType.ETCETN;
                else if (s.equalsIgnoreCase("FUTURE")) return secType.FUTURE;
                else if (s.equalsIgnoreCase("BOND")) return secType.BOND;
                else if (s.equalsIgnoreCase("CURRENCY")) return secType.CURRENCY;
                else if (s.equalsIgnoreCase("INDEX")) return secType.INDEX;
                else throw new IllegalArgumentException("type unknown : "+s);                
            }
        @Override
            public String toString() {
                return url;
            }            
        };
        /*
        static public final java.util.HashMap<String, secType> secMap = new java.util.HashMap<String, secType>() {
            {
                put("STOCK", secType.STOCK);
                put("ETF", secType.ETF);
                put("ETCETN", secType.ETCETN);
                put("FUTURE", secType.FUTURE);
                put("BOND", secType.BOND);
                put("CURRENCY", secType.CURRENCY);
                put("INDEX", secType.INDEX);
            }
        };*/         
        /*
        
        static public Fints changeFreq(Fints f,Fints.frequency newf) throws Exception {
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
                    case MINUTES3:
                        actual=UDate.roundXMinutes(actual,3);
                        break;
                    case MINUTES5:
                        actual=UDate.roundXMinutes(actual,5);
                        break;
                    case MINUTES10:
                        actual=UDate.roundXMinutes(actual,10);
                        break;
                    case MINUTES15:
                        actual=UDate.roundXMinutes(actual,15);
                        break;
                    case MINUTES30:
                        actual=UDate.roundXMinutes(actual,30);
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
                    previous=actual;
                } else {
                    if (actual.after(previous)) {
                        row=new java.util.ArrayList<>();
                        row.add(open);row.add(high);row.add(low);row.add(close);row.add(volume);row.add(oi);
                            map.put(previous, row);
                       previous=actual;
                    } else {
                        if (f.get(i, 1)>high) high=f.get(i, 1);
                        if (f.get(i, 2)<low) low=f.get(i, 2);
                        close=f.get(i, 3);
                        volume+=f.get(i, 4);oi+=f.get(i, 5);
                        if (i==(f.getLength()-1)) {
                            row=new java.util.ArrayList<>();
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
                for (int j=0;j<l.size();j++) {newmat[i][j]=l.get(j);}                
                i++;
            }
            return new Fints(newdates, f.getName(), newf, newmat);
        }*/
        static public Fints createContinuity (Fints f) throws Exception {
                //per i gap
            java.util.TreeMap<UDate, Double> mapvolume = new java.util.TreeMap<>();        
            java.util.TreeMap<UDate, Double> mapopen = new java.util.TreeMap<>();
            java.util.TreeMap<UDate, Double> maphigh = new java.util.TreeMap<>();
            java.util.TreeMap<UDate, Double> maplow = new java.util.TreeMap<>();
            java.util.TreeMap<UDate, Double> mapclose = new java.util.TreeMap<>();                
            java.util.TreeMap<UDate, Double> mapoi = new java.util.TreeMap<>();                
            
            f.getDate().forEach((d)->
                    
            {        
                mapvolume.put(d, f.get(d, Security.SERIE.VOLUME.getValue()));
                mapopen.put(d, f.get(d, Security.SERIE.OPEN.getValue()));
                maphigh.put(d, f.get(d, Security.SERIE.HIGH.getValue()));
                maplow.put(d, f.get(d, Security.SERIE.LOW.getValue()));
                mapclose.put(d, f.get(d, Security.SERIE.CLOSE.getValue()));                            
                mapoi.put(d, f.get(d, Security.SERIE.OI.getValue()));                            
            
            }
            
            );
            UDate[] tempd=mapvolume.keySet().stream().map(i->i).toArray(UDate[]::new);
            ArrayList<UDate> dates = new ArrayList<>();
            
            for (int i=1;i<tempd.length;i++){                    
            //    if (tempd[i].diffseconds(tempd[i-1])>1)                        
                //{
                    Calendar dt = Calendar.getInstance();
                    dt.setTimeInMillis(tempd[i-1].time);
                    switch (f.getFrequency()) {
                        case DAILY:dt.add(Calendar.HOUR, 24);break;
                        case MINUTE:dt.add(Calendar.MINUTE, 1);break;
                        case HOUR:dt.add(Calendar.HOUR, 1);break;
                        case MINUTES10:dt.add(Calendar.MINUTE, 10);break;
                        case MINUTES15:dt.add(Calendar.MINUTE, 15);break;
                        case MINUTES3:dt.add(Calendar.MINUTE, 3);break;
                        case MINUTES30:dt.add(Calendar.MINUTE, 30);break;
                        case MINUTES5:dt.add(Calendar.MINUTE, 5);break;
                        case MONTHLY:dt.add(Calendar.MONTH, 1);break;
                        case SECOND:dt.add(Calendar.SECOND, 1);break;
                        case WEEKLY:dt.add(Calendar.HOUR, 24*7);break;
                        case YEARLY:dt.add(Calendar.YEAR, 1);break;
                        default: throw new Exception ("not yet implemented "+f.getFrequency());                    
                    }
                    
                    while (!mapvolume.containsKey(new UDate(dt.getTimeInMillis()))){
                        UDate dt1=new UDate(dt.getTimeInMillis());
                        mapvolume.put(dt1, 0.);//no volume
                        mapopen.put(dt1, mapclose.get(tempd[i-1]));
                        maphigh.put(dt1, mapclose.get(tempd[i-1]));
                        maplow.put(dt1, mapclose.get(tempd[i-1]));
                        mapclose.put(dt1, mapclose.get(tempd[i-1]));                            
                        mapoi.put(dt1, mapoi.get(tempd[i-1]));                            
                        //dt.add(Calendar.SECOND, 1);
                        switch (f.getFrequency()) {
                            case DAILY:dt.add(Calendar.HOUR, 24);break;
                            case MINUTE:dt.add(Calendar.MINUTE, 1);break;
                            case HOUR:dt.add(Calendar.HOUR, 1);break;
                            case MINUTES10:dt.add(Calendar.MINUTE, 10);break;
                            case MINUTES15:dt.add(Calendar.MINUTE, 15);break;
                            case MINUTES3:dt.add(Calendar.MINUTE, 3);break;
                            case MINUTES30:dt.add(Calendar.MINUTE, 30);break;
                            case MINUTES5:dt.add(Calendar.MINUTE, 5);break;
                            case MONTHLY:dt.add(Calendar.MONTH, 1);break;
                            case SECOND:dt.add(Calendar.SECOND, 1);break;
                            case WEEKLY:dt.add(Calendar.HOUR, 24*7);break;
                            case YEARLY:dt.add(Calendar.YEAR, 1);break;
                            default: throw new Exception ("not yet implemented "+f.getFrequency());                    
                        }                        
                    }              
                //}                
            }
            dates.addAll(mapvolume.keySet());
            double[][] matrix = new double[dates.size()][6];
            for (int i = 0; i < matrix.length; i++) {
                matrix[i][0] = mapopen.get(dates.get(i));
                matrix[i][1] = maphigh.get(dates.get(i));
                matrix[i][2] = maplow.get(dates.get(i));
                matrix[i][3] = mapclose.get(dates.get(i));
                matrix[i][4] = mapvolume.get(dates.get(i));
                matrix[i][5] = mapoi.get(dates.get(i));
            }                    
            return new Fints(dates, f.getName(), f.getFrequency(), matrix);
        }
        
        static public Fints changeFreq(Fints f,Fints.frequency newf) throws Exception {
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
                    case MINUTES3:
                        actual=UDate.roundXMinutes(actual,3);
                        break;
                    case MINUTES5:
                        actual=UDate.roundXMinutes(actual,5);
                        break;
                    case MINUTES10:
                        actual=UDate.roundXMinutes(actual,10);
                        break;
                    case MINUTES15:
                        actual=UDate.roundXMinutes(actual,15);
                        break;
                    case MINUTES30:
                        actual=UDate.roundXMinutes(actual,30);
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
                    previous=actual;
                } else {
                    if (actual.after(previous)) {
                        row=new java.util.ArrayList<>();
                        row.add(open);row.add(high);row.add(low);row.add(close);row.add(volume);row.add(oi);
                        map.put(previous, row);
                        open=f.get(i, 0);low=f.get(i, 2);
                        high=f.get(i, 1);close=f.get(i, 3);
                        volume=f.get(i, 4);oi=f.get(i, 5);
                        previous=actual;
                    } else {
                        if (f.get(i, 1)>high) high=f.get(i, 1);
                        if (f.get(i, 2)<low) low=f.get(i, 2);
                        close=f.get(i, 3);
                        volume+=f.get(i, 4);oi+=f.get(i, 5);
                        if (i==(f.getLength()-1)) {
                            row=new java.util.ArrayList<>();
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
                for (int j=0;j<l.size();j++) {newmat[i][j]=l.get(j);}                
                i++;
            }
            return new Fints(newdates, f.getName(), newf, newmat);
        }


        
	public Security(String hashcode) throws Exception {
                this.hashcode=hashcode;
                infomap =Database.getRecords(Optional.of(Arrays.asList(hashcode)),Optional.empty(),Optional.empty() , Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()).get(0) ;                
		daily=Database.getFintsQuotes(Optional.of(infomap.get("code")), Optional.of(infomap.get("market")),Optional.empty());
                double[][] m=daily.getMatrixCopy();
                
                for (int i=0;i<m.length;i++){
                    for (int j=0;j<(m[i].length);j++){
                        if (!Double.isFinite(m[i][j]) ) throw new Exception("NAN or infinite value for "+infomap.toString());
                    //    if (m[i][j]<=0 ) throw new Exception("<=0 value for "+infomap.toString());
                    }                    
                    if (m[i][Security.SERIE.CLOSE.getValue()]<=0 ) throw new Exception(m[i][Security.SERIE.CLOSE.getValue()]+"<=0 close value for "+infomap.toString());                
                    if (m[i][Security.SERIE.VOLUME.getValue()]<0 ) throw new Exception(m[i][Security.SERIE.VOLUME.getValue()]+"<0 volume value for "+infomap.toString());                
                    if (m[i][Security.SERIE.OI.getValue()]<0 ) throw new Exception(m[i][Security.SERIE.OI.getValue()]+"<0 oi value for "+infomap.toString());                
                    if (m[i][Security.SERIE.OPEN.getValue()]<=0 ) throw new Exception(m[i][Security.SERIE.OPEN.getValue()]+"<=0 open value for "+infomap.toString());                
                    if (m[i][Security.SERIE.HIGH.getValue()]<=0 ) throw new Exception(m[i][Security.SERIE.HIGH.getValue()]+"<=0 high value for "+infomap.toString());                
                    if (m[i][Security.SERIE.LOW.getValue()]<=0 ) throw new Exception(m[i][Security.SERIE.LOW.getValue()]+"<=0 low value for "+infomap.toString());                                                        
                }
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
        public Fints getIntradayMinutes3(UDate date) throws Exception{
            return Security.changeFreq(Database.getIntradayFintsQuotes(hashcode, date), Fints.frequency.MINUTES3);
        }
        public Fints getIntradayMinutes5(UDate date) throws Exception{
            return Security.changeFreq(Database.getIntradayFintsQuotes(hashcode, date), Fints.frequency.MINUTES5);
        }
        public Fints getIntradayMinutes10(UDate date) throws Exception{
            return Security.changeFreq(Database.getIntradayFintsQuotes(hashcode, date), Fints.frequency.MINUTES10);
        }
        public Fints getIntradayMinutes15(UDate date) throws Exception{
            return Security.changeFreq(Database.getIntradayFintsQuotes(hashcode, date), Fints.frequency.MINUTES15);
        }
        public Fints getIntradayMinutes30(UDate date) throws Exception{
            return Security.changeFreq(Database.getIntradayFintsQuotes(hashcode, date), Fints.frequency.MINUTES30);
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
        public secType getTypeEnum() {return secType.getEnum(infomap.get("type"))  ;}
}
