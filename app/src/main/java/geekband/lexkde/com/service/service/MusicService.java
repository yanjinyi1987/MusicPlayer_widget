package geekband.lexkde.com.service.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.util.List;

import geekband.lexkde.com.service.R;

/**
 * Created by lexkde on 16-8-5.
 * Service也必须像Activity Fragment那样放在AndroidManifest.xml的<application>...</application>中
 * 有start与bind两种启动方式。
 *
 * service, activity都继承了Context，这个Context是个啥呢？
 */
public class MusicService extends Service {
    public static final String TAG = "MusicService";
    private MediaPlayer mMediaPlayer;
    private LocalBinder mIBinder = new LocalBinder();
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"onCreate");
    }


    public void setDataResource(String uri) throws IOException {
        mMediaPlayer.setDataSource(uri);
        /*
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                startPlay();
            }
        });
        mMediaPlayer.prepareAsync();
        */
        mMediaPlayer.prepare();
        startPlay();
    }

    public void startPlay() {
        if(mMediaPlayer!=null) {
            mMediaPlayer.start();
        }
    }

    public void pausePlay() {
        if(mMediaPlayer!=null) {
            mMediaPlayer.pause();
        }
    }

    public void resumePlay() {
        startPlay();
    }

    public void stopPlay() {
        if(mMediaPlayer!=null) {
            mMediaPlayer.stop();
            //mMediaPlayer.release();
            mMediaPlayer.reset(); //清空以便播放下一首歌曲
        }
    }

    public boolean isPlaying() {
        if(mMediaPlayer!=null) {
            return mMediaPlayer.isPlaying();
        }
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG,"onStartCommand");
        return super.onStartCommand(intent, flags, startId); //需要返回START_NOT_STICKY吗？
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"onDestroy");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        mMediaPlayer = new MediaPlayer();
        return mIBinder; //Java的多态，Binder为IBinder的子类
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG,"onUnbind");
        stopPlay();
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.i(TAG,"onRebind");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.i(TAG,"onTaskRemoved");
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Log.i(TAG,"onTrimMemory");
    }

    public void getCurrentMusicInfo() {
        //音乐时长
        int musiclength = mMediaPlayer.getDuration(); //check if it is -1
        //音乐名称
    }




    public class LocalBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }
}
