package frb.axeron.server;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class AxeronConfig {

    /**
     * 3: FLAG_DENIED means "denied in the manager" and is enforced (no dialog). Before, it was
     * also written for every app that simply wasn't allowed, so loading an older config clears it.
     */
    public static final int LATEST_VERSION = 3;

    @SerializedName("version")
    public int version = LATEST_VERSION;

    @SerializedName("packages")
    public List<PackageEntry> packages = new ArrayList<>();

    public AxeronConfig() {
    }

    public AxeronConfig(@NonNull List<PackageEntry> packages) {
        this.version = LATEST_VERSION;
        this.packages = packages;
    }

    public static class PackageEntry extends ConfigPackageEntry {

        @SerializedName("uid")
        public final int uid;

        @SerializedName("flags")
        public int flags;

        @SerializedName("packages")
        public List<String> packages;

        public PackageEntry(int uid, int flags) {
            this.uid = uid;
            this.flags = flags;
            this.packages = new ArrayList<>();
        }

        @Override
        public boolean isAllowed() {
            return (flags & ConfigManager.FLAG_ALLOWED) != 0;
        }

        @Override
        public boolean isDenied() {
            return (flags & ConfigManager.FLAG_DENIED) != 0;
        }
    }
}
