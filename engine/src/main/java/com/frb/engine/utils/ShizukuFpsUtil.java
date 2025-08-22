package com.frb.engine.utils;

//import com.fahrezone.engine.shizuku.client.AxeronNewProcess;

public class ShizukuFpsUtil {

    private static final String TAG = "ShizukuFpsUtil";

    private static final String FPS_COMMAND =
            "(dumpsys SurfaceFlinger --timestats -dump && dumpsys SurfaceFlinger --timestats -clear -enable) | grep \"averageFPS\" | head -n 1 | cut -d\"=\" -f2 | tr -d '[:space:]'";

    public interface Callback {
        void onResult(double fps);

        void onError(Exception e);
    }

//    public static synchronized void getCurrentFps(Callback callback) {
//        StringBuilder output = new StringBuilder();
//        try {
//            // Execute the shell command
//            Process process = AxeronNewProcess.exec(FPS_COMMAND);
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//
//            // Read the output of the command
//            String line;
//            while ((line = reader.readLine()) != null) {
//                output.append(line).append('\n');
//            }
//            reader.close();
//
//            process.waitFor();
//        } catch (Exception e) {
//            callback.onError(e);
//        }
//
//        if (!output.toString().trim().isEmpty()) {
//            callback.onResult(Double.parseDouble(output.toString().trim()));
//        }
//    }
}
