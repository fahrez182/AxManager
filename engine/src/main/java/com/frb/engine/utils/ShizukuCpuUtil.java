package com.frb.engine.utils;

public class ShizukuCpuUtil {

//    public static void getCpuUsagePercent(@Nullable Callback callback) {
//        try {
//            String command = "PREV=$(cat /proc/stat | awk '/^cpu /{for(i=2;i<=8;i++) t+=$i; print t,$5}'); sleep 0.1; CUR=$(cat /proc/stat | awk '/^cpu /{for(i=2;i<=8;i++) t+=$i; print t,$5}'); PREV_T=$(echo $PREV | cut -d' ' -f1); PREV_I=$(echo $PREV | cut -d' ' -f2); CUR_T=$(echo $CUR | cut -d' ' -f1); CUR_I=$(echo $CUR | cut -d' ' -f2); DIFF_T=$((CUR_T - PREV_T)); DIFF_I=$((CUR_I - PREV_I)); echo $(( (1000*(DIFF_T - DIFF_I)/DIFF_T +5)/10 ))";
//
//            Process process = AxeronNewProcess.exec(
//                    new String[]{"sh", "-c", command},
//                    null, null);
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            String line = reader.readLine();
//            reader.close();
//
//            if (line != null) {
//                float cpuPercent = Float.parseFloat(line.trim());
//                if (callback != null) {
//                    callback.onResult(cpuPercent);
//                }
//            } else {
//                if (callback != null) {
//                    callback.onError("Empty output");
//                }
//            }
//        } catch (IOException | NumberFormatException e) {
//            if (callback != null) {
//                callback.onError(e.getMessage());
//            }
//        }
//    }

    public interface Callback {
        void onResult(float cpuUsagePercent);
        void onError(String error);
    }
}
