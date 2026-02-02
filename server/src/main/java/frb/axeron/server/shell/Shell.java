package frb.axeron.server.shell;

import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;

import frb.axeron.api.Axeron;
import frb.axeron.shared.AxeronApiConstant;
import rikka.rish.Rish;
import rikka.rish.RishConfig;

public class Shell extends Rish {

    public static void main(String[] args, String packageName, IBinder binder, Handler handler) {
        RishConfig.init(binder, AxeronApiConstant.server.BINDER_DESCRIPTOR, 30000);
        Axeron.onBinderReceived(binder, packageName);
        Axeron.addBinderReceivedListenerSticky(() -> {
            handler.post(() -> new Shell().start(args));
        });
    }

    @Override
    public void requestPermission(Runnable onGrantedRunnable) {
        if (Axeron.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            onGrantedRunnable.run();
        } else if (Axeron.shouldShowRequestPermissionRationale()) {
            System.err.println("Permission denied");
            System.err.flush();
            System.exit(1);
        } else {
            Axeron.addRequestPermissionResultListener(new Axeron.OnRequestPermissionResultListener() {
                @Override
                public void onRequestPermissionResult(int requestCode, int grantResult) {
                    Axeron.removeRequestPermissionResultListener(this);

                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        onGrantedRunnable.run();
                    } else {
                        System.err.println("Permission denied");
                        System.err.flush();
                        System.exit(1);
                    }
                }
            });
            Axeron.requestPermission(0);
        }
    }
}
