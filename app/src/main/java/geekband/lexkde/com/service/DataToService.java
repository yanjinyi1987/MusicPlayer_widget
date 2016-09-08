package geekband.lexkde.com.service;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * Created by lexkde on 16-9-8.
 */

public class DataToService implements Parcelable {
    public String mParentDirName;
    public int fileList_count;

    public DataToService(String mParentDirName, int fileList_count) {
        this.mParentDirName = mParentDirName;
        this.fileList_count = fileList_count;
    }

    public DataToService() {
        this(null,0);
    }

    public static final ClassLoaderCreator<DataToService> CREATOR = new Parcelable.ClassLoaderCreator<DataToService>() {
        @Override
        public DataToService createFromParcel(Parcel source, ClassLoader loader) {
            Log.i("DataToService","ClassLoader");
            try {
                loader.loadClass("DataToService");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            Log.i("DataToService","createFromParcelwithLoader");
            DataToService dataToService = new DataToService();
            dataToService.mParentDirName = source.readString();
            dataToService.fileList_count = source.readInt();
            return dataToService;
        }

        @Override
        public DataToService createFromParcel(Parcel source) {
            Log.i("DataToService","createFromParcel");
            DataToService dataToService = new DataToService();
            dataToService.mParentDirName = source.readString();
            dataToService.fileList_count = source.readInt();
            return dataToService;
        }

        @Override
        public DataToService[] newArray(int size) {
            return new DataToService[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Log.i("DataToService","writeToParcel");
        dest.writeString(mParentDirName);
        dest.writeInt(fileList_count);
    }
}