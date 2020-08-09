/*
package com.jd.isvsecurity.businessbackground;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jd.isvsecurity.IsvLoginAddLocation;
import com.jd.isvsecurity.IsvLoginAddLocationType;
import com.jd.isvsecurity.IsvlogLogin;
import com.jd.tools.ToolsClass;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;

import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.orc.TypeDescription;
import org.apache.orc.mapred.OrcStruct;
import org.apache.orc.mapreduce.OrcInputFormat;
import org.apache.orc.mapreduce.OrcOutputFormat;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddLocationField extends Configured implements Tool {
    public static final String TAB = "\t";
    public static int requestFailCount = 0;

    //二次排序类
    static class SecondCompared implements WritableComparable<SecondCompared> {
        private String ip;
        private String flag;

        public SecondCompared() {
            this.ip = "";
            this.flag = "";
        }

        public SecondCompared(String ip, String flag) {
            this.ip = ip;
            this.flag = flag;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getFlag() {
            return flag;
        }

        public String toString() {
            return this.ip + "\t" + this.flag;
        }

        @Override
        public void readFields(DataInput in) throws IOException {
            ip = in.readUTF();
            flag = in.readUTF();
        }

        @Override
        public void write(DataOutput out) throws IOException {
            out.writeUTF(ip);
            out.writeUTF(flag);
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass())
                return false;

            SecondCompared secondCompared = (SecondCompared) obj;
            return ip.equals(secondCompared.ip) &&
                    flag.equals(secondCompared.flag);
        }

        @Override
        public int compareTo(SecondCompared q) {
            int minus = this.getIp().compareTo(q.getIp());
            if (minus != 0) {
                return minus;
            }
            return this.flag.compareTo(q.flag);
        }
    }

    //
    public static class KeyPartitioner extends Partitioner<SecondCompared, Text> {
        @Override
        public int getPartition(SecondCompared key, Text value, int numPartitions) {
            return (key.getIp().hashCode() & 0x7FFFFFFF) % numPartitions;
        }
    }

    //按key-pair中的ip分组
    public static class GroupComparator extends WritableComparator {
        protected GroupComparator() {
            super(SecondCompared.class, true);
        }

        public int compare(WritableComparable wc1, WritableComparable wc2) {
            SecondCompared a = (SecondCompared) wc1;
            SecondCompared b = (SecondCompared) wc2;
            return a.getIp().compareTo(b.getIp());
        }
    }


    //ip归属地 mapper
    public static class ReadLocationMapper extends Mapper<LongWritable, Text, SecondCompared, Text> {
        private String counterName = this.getClass().getSimpleName();

        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            String[] lparts = line.split(TAB);
            if (lparts.length != 5) {
                context.getCounter(counterName, "Number of error location lines").increment(1);
                return;
            }

            String ip = lparts[0];
            String fregion = lparts[1];
            String fprovince = lparts[2];
            String fcity = lparts[3];
            String fdistrict = lparts[4];
            context.write(new SecondCompared(ip, "10"), new Text(fregion + TAB + fprovince + TAB + fcity + TAB + fdistrict));
            context.getCounter(counterName, "Number of location lines").increment(1);
        }
    }

    //订单 mapper
    private static class BusinessOrderMapper extends Mapper<NullWritable, OrcStruct, SecondCompared, Text> {
        public String counterName = this.getClass().getSimpleName();

        public void map(NullWritable key, OrcStruct value, Context context) throws IOException, InterruptedException {
            try {
                String h_time = value.getFieldValue(0) == null ? "" : value.getFieldValue(0).toString();
                String date = h_time.split(" ")[0];
//                String date = getDate(context);
                if (!date.equals(context.getConfiguration().get("date"))) {
                    context.getCounter(counterName, "not target date").increment(1);
                    return;
                }
                String app_name = value.getFieldValue(3) == null ? "" : value.getFieldValue(3).toString();
                if (!app_name.equals("export.man")) {
                    context.getCounter(counterName, "not need").increment(1);
                    return;
                }
                String client_ip = value.getFieldValue(7) == null ? "" : value.getFieldValue(7).toString();
                if (isNull(client_ip) || !client_ip.contains(".")) {
                    context.getCounter(counterName, "ip error").increment(1);
                    return;
                }
                String req_pin = value.getFieldValue(11) == null ? "" : value.getFieldValue(11).toString();
                String resp_order_id = value.getFieldValue(30) == null ? "" : value.getFieldValue(30).toString();
                String reqinfo = value.getFieldValue(38) == null ? "" : value.getFieldValue(38).toString();

                Pattern p = Pattern.compile("additionParams=\\{(.+?)\\}");
                Matcher m = p.matcher(reqinfo);

                String exportType = null,startTime=null,endTime=null;
                if (m.find()) {
                    if (m.groupCount() > 1) {
                        context.getCounter(counterName, "additionParams too many").increment(1);
                    }

                    for (String pair : m.group(1).split(",")) {
                        int dot = pair.indexOf(":");
                        String name = pair.substring(0, dot);
                        String var = pair.substring(dot + 1);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        switch (name.substring(1, name.length() - 1)) {
                            case "startTime":
                                startTime = String.valueOf(sdf.parse(var.substring(1, var.length() - 1)).getTime());
                                break;
                            case "endTime":
                                endTime = String.valueOf(sdf.parse(var.substring(1, var.length() - 1)).getTime());
                                break;
                            case "exportType":
                                exportType = var.substring(1, var.length() - 1);
                                break;
                        }
                    }
                }

                String venderId = null;
                p = Pattern.compile("venderId=(.+?)\\}");
                m = p.matcher(reqinfo);
                if (m.find()) {
                    if (m.groupCount() > 1) {
                        context.getCounter(counterName, "venderId too many").increment(1);
                    }
                    venderId = m.group(1);
                }
                if (isNull(req_pin) || isNull(venderId) || isNull(resp_order_id) || isNull(exportType)) {
                    context.getCounter(counterName, "pin,venderid,order_id,exportType null").increment(1);
                    return;
                }
                context.write(new SecondCompared(client_ip, "20"), new Text(req_pin
                        + TAB + venderId
                        + TAB + exportType
                        + TAB + startTime
                        + TAB + endTime
                        + TAB + client_ip));
                context.getCounter(counterName, "raw map").increment(1);

            } catch (Exception e) {
                e.printStackTrace();
                context.getCounter(counterName, "Number of parse Exception lines").increment(1);
            }
        }
    }

    //判断为空
    public static boolean isNull(String item) {
        if (item == null || item.length() == 0)
            return true;
        return false;
    }

    //添加ip归属地
    public static class IpLocationReduce extends Reducer<SecondCompared, Text, Text, Text> {
        private String counterName = this.getClass().getSimpleName();
        public void reduce(SecondCompared key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            String fprovince = "";
            String fcity = "";
            HashSet<String> locations = new HashSet();
            for (Text val : values) {
                if (key.getFlag().equals("10")) {
                    String location = val.toString();
                    String[] lparts = location.split(TAB);
                    if (4 == lparts.length) {
                        fprovince = lparts[1];
                        fcity = lparts[2];
                    }
                } else if (key.getFlag().equals("20")) {
                    String[] timeSeries = val.toString().split(TAB);
                    String req_pin = timeSeries[0];
                    String venderId = timeSeries[1];
                    String exportType = timeSeries[2];
                    String startTime = timeSeries[3];
                    String endTime = timeSeries[4];
                    String client_ip = timeSeries[5];
                    String location = fprovince + fcity;

                    context.getCounter(counterName, "number of ip has added location").increment(1);
                    context.write(new Text(req_pin), new Text(venderId + TAB + exportType + TAB + startTime
                            + TAB + endTime + TAB + client_ip + TAB + location));
                }
            }
        }
    }

    @Override
    public int run(String[] args) throws Exception {
        Job job = Job.getInstance(getConf(), "BusinessesOrderIpAddLocation");
        job.setJarByClass(AddLocationField.class);
        job.setReducerClass(IpLocationReduce.class);

        job.setMapOutputKeyClass(SecondCompared.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.setGroupingComparatorClass(GroupComparator.class);
        job.setPartitionerClass(KeyPartitioner.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        String proVal = getConf().get("location", null);
        if (proVal != null) {
            for (String p : proVal.split(","))
                MultipleInputs.addInputPath(job, new Path(p), TextInputFormat.class, ReadLocationMapper.class);
        } else {
            System.out.println("location path not set yet");
        }
        proVal = getConf().get("BusinessesOrder", null);
        if (proVal != null) {
            for (String p : proVal.split(","))
                MultipleInputs.addInputPath(job, new Path(p), OrcInputFormat.class, BusinessOrderMapper.class);
        } else {
            System.out.println("BusinessesOrder path not set yet");
        }
        TextOutputFormat.setOutputPath(job, new Path(args[0]));
        job.setNumReduceTasks(50);
        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {
        ToolRunner.run(new Configuration(), new AddLocationField(), args);
    }
}

 */
