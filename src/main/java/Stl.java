import com.github.servicenow.ds.stats.stl.SeasonalTrendLoess;

import java.util.ArrayList;
import java.util.Arrays;

public class Stl {
    public static double getAverage(ArrayList<String> aList) {
        double sum = 0;
        int num = aList.size();
        for (String str : aList) {
            sum += Double.valueOf(str);
        }
        return sum / num;
    }

    public static String replaceMissingValues(String line) {
        StringBuilder sb = new StringBuilder();
        int idx = line.lastIndexOf(",");
        if (idx <= 0)
            return null;

        String lastnum = line.substring(idx + 1);
        String subline = line.substring(0, idx);
        String[] subarr = subline.split(",");
        ArrayList<String> sublist = new ArrayList<>();
        for (String subnum : subarr) {
            if (!subnum.equals("0")) {
                sublist.add(subnum);
            }
        }

        double mean = getAverage(sublist);
        for (String subnum : subarr) {
            if (subnum.equals("0")) {
                sb.append(Math.round(mean));
            } else {
                sb.append(subnum);
            }
            sb.append(",");
        }

        sb.append(lastnum);

        return sb.toString();
    }

    public static void main(String[] args) {
        String dateCount = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,3159,0,389,0,5191";
        String goodDayCount = replaceMissingValues(dateCount);
        String[] totalArr = dateCount.split(",");
        String[] goodArr = goodDayCount.split(",");
        ArrayList<String> substituteMeanArray = new ArrayList<>(Arrays.asList(goodArr).subList(0, totalArr.length));
        //System.out.println(substituteMeanArray);
        double[] values = new double[substituteMeanArray.size()]; // Monthly time-series data
        for(int i = 0; i < substituteMeanArray.size(); ++i){
            values[i] = Double.valueOf(substituteMeanArray.get(i));
        }
        int pl = 7;
        int sw = substituteMeanArray.size() / pl;
        SeasonalTrendLoess.Builder builder = new SeasonalTrendLoess.Builder();
        SeasonalTrendLoess smoother = builder.
                setPeriodLength(pl).    // Data has a period of 12
                setSeasonalWidth(sw).   // Monthly data smoothed over 35 years
                setNonRobust().         // Not expecting outliers, so no robustness iterations
                buildSmoother(values);

        SeasonalTrendLoess.Decomposition stl = smoother.decompose();
        double[] seasonal = stl.getSeasonal();
        double[] trend = stl.getTrend();
        double[] residual = stl.getResidual();
        System.out.println(Arrays.toString(seasonal));
        System.out.println(Arrays.toString(trend));
        System.out.println(Arrays.toString(residual));
    }
}
