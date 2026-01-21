package frb.axeron.shared;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Environment implements Parcelable {

    public static final Creator<Environment> CREATOR = new Creator<>() {
        @Override
        public Environment createFromParcel(Parcel in) {
            return new Environment(in);
        }

        @Override
        public Environment[] newArray(int size) {
            return new Environment[size];
        }
    };
    private final Map<String, String> envMap;
    private boolean newEnv = false;

    public Environment(Map<String, String> envMap, boolean newEnv) {
        this.newEnv = newEnv;
        this.envMap = Map.copyOf(envMap);
    }

    // Parcelable
    protected Environment(Parcel in) {
        int size = in.readInt();
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String k = in.readString();
            String v = in.readString();
            map.put(k, v);
        }
        this.envMap = Collections.unmodifiableMap(map);
    }

    public boolean isNewEnv() {
        return newEnv;
    }

    public String[] getEnv() {
        if (envMap.isEmpty()) return null;
        return envMap.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .toArray(String[]::new);
    }

    public Map<String, String> getEnvMap() {
        return envMap;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(envMap.size());
        for (Map.Entry<String, String> e : envMap.entrySet()) {
            dest.writeString(e.getKey());
            dest.writeString(e.getValue());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // --------- Builder ----------
    public static class Builder {
        private final Map<String, String> systemEnvMap = new HashMap<>(System.getenv());
        private final Map<String, String> envMap;

        private final boolean newEnv;

        public Builder(boolean newEnv) {
            this.newEnv = newEnv;
            this.envMap = newEnv ? new HashMap<>() : systemEnvMap;
        }

        public Builder put(String key, String value) {
            if (value != null) {
                Pattern pattern = Pattern.compile("\\$([A-Za-z0-9_]+)");
                Matcher matcher = pattern.matcher(value);
                StringBuffer sb = new StringBuffer();
                while (matcher.find()) {
                    String var = matcher.group(1);
                    String replacement = envMap.getOrDefault(var, systemEnvMap.getOrDefault(var, ""));
                    if (replacement != null) {
                        matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                    }
                }
                matcher.appendTail(sb);
                value = sb.toString();
            }
            envMap.put(key, value);
            return this;
        }

        public Builder putAll(String[] envArray) {
            if (envArray != null) {
                for (String s : envArray) {
                    int idx = s.indexOf('=');
                    if (idx > 0) {
                        String key = s.substring(0, idx);
                        String val = s.substring(idx + 1);
                        put(key, val); // gunakan put() biar support substitusi $KEY
                    }
                }
            }
            return this;
        }

        public Builder putAll(Map<String, String> map) {
            for (Map.Entry<String, String> e : map.entrySet()) {
                put(e.getKey(), e.getValue());
            }
            return this;
        }

        public Builder remove(String key) {
            envMap.remove(key);
            return this;
        }

        public Builder clear() {
            envMap.clear();
            return this;
        }

        public Environment build() {
            if (envMap == null || envMap.isEmpty()) return new Environment(systemEnvMap, newEnv);
            return new Environment(envMap, newEnv);
        }
    }
}

