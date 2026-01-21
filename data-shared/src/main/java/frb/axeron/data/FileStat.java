package frb.axeron.shared;

import android.os.Parcel;
import android.os.Parcelable;

public class FileStat implements Parcelable {
    public static final Creator<FileStat> CREATOR = new Creator<>() {
        @Override
        public FileStat createFromParcel(Parcel in) {
            return new FileStat(in);
        }

        @Override
        public FileStat[] newArray(int size) {
            return new FileStat[size];
        }
    };
    public String  path;
    public String  name;
    public String  parent;
    public boolean exists;
    public boolean isFile;
    public boolean isDirectory;
    public boolean isHidden;
    public long    length;
    public long    lastModified;
    public String  canonicalPath; // "" jika gagal resolve

    public FileStat() {}

    protected FileStat(Parcel in) {
        path = in.readString();
        name = in.readString();
        parent = in.readString();
        exists = in.readByte() != 0;
        isFile = in.readByte() != 0;
        isDirectory = in.readByte() != 0;
        isHidden = in.readByte() != 0;
        length = in.readLong();
        lastModified = in.readLong();
        canonicalPath = in.readString();
    }

    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeString(name);
        dest.writeString(parent);
        dest.writeByte((byte) (exists ? 1 : 0));
        dest.writeByte((byte) (isFile ? 1 : 0));
        dest.writeByte((byte) (isDirectory ? 1 : 0));
        dest.writeByte((byte) (isHidden ? 1 : 0));
        dest.writeLong(length);
        dest.writeLong(lastModified);
        dest.writeString(canonicalPath);
    }

    @Override public int describeContents() { return 0; }
}
