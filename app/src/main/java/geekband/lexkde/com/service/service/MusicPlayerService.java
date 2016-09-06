package geekband.lexkde.com.service.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import geekband.lexkde.com.service.widget.MusicWidget;

/*
 * Binder是沟通Activity与Service的桥梁，在二者之间搭建了沟通的渠道，而start方式好像做不到。
 * 这中方式的生命周期中多了onBind与unBind两个节点
 */
public class MusicPlayerService extends Service {
    public static  final  String TAG = "YJY:MusicPlayerService";
    public static final String CHANGE_PLAY_BUTTON_TEXT = "MusicPlayerService.ChangeButtonText";
    public static final String CHANGE_DURATION_TEXT ="MusicPlayerService.ChangeBDurationText";
    public static final String CHANGE_PROGRESS_TEXT ="MusicPlayerService.ChangeProgressText";
    public static final String CHANGE_PROGRESS_BAR ="MusicPlayerService.ChangeProgressBar";
    public static final String CHANGE_CURRENT_SONG_NAME = "MusicPlayerService.ChangeCurrentSongName";
    public static final String UPDATE_TEXT = "NEW_TEXT";
    //TextView mMusicLength;
    private String mDirPath;
    private boolean isStop=true;
    private boolean canBePaused=false;
    private int currentMusicIndex=-1, totalMusics =-1;
    private int mMusicDuration;
    private int currentSeconds = 0;
    private int currentMinutes = 0;
    String[] musicPostfix = {".mp3",".m4a",".wav"};

    MediaPlayer mMediaPlayer = new MediaPlayer();
    private Map<String, File> thePlayList;

    private String[] fileNameInMusicList;

    LocalBinder mLocalBinder = new LocalBinder();

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action  = intent.getAction();
            switch (action) {
                case MusicWidget.START :
                    Log.i("YJY1986","action_start");
                    playSong();
                    break;
                case MusicWidget.STOP:
                    Log.i("YJY1986","action_stop");
                    stopSong();
                    break;
                case MusicWidget.NEXT_SONG:
                    playNextSong();
                    Log.i("YJY1986","action_next");
                    break;
                case MusicWidget.PREV_SONG:
                    playPreviousSong();
                    Log.i("YJY1986","action_prev");
                    break;
                default:
                    break;
            }
        }
    };
    Handler mHandler = new Handler();
    private int mCurrentLength=0;
    private int mCurrentTimePassed=0;
    private int mUpdateInterval = 1000; //1000ms
    private Runnable mProgressUpdateRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        //添加IntentFilter
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MusicWidget.START);
        intentFilter.addAction(MusicWidget.STOP);
        intentFilter.addAction(MusicWidget.NEXT_SONG);
        intentFilter.addAction(MusicWidget.PREV_SONG);
//        intentFilter.addAction(ANDROID_NET_CONN_CONNECTIVITY_CHANGE);

        //注册receiver
        registerReceiver(receiver,intentFilter);
        Log.i(TAG,"onCreate");

        //update progressBar
        mProgressUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                int progress = (int)((float)mCurrentTimePassed*100.0/mCurrentLength);
                Log.i("ProgressBar Update",String.valueOf(progress));
                updateWidgetText(CHANGE_PROGRESS_BAR,String.valueOf(progress));
                currentMinutes = mCurrentTimePassed/60;
                currentSeconds = mCurrentTimePassed%60;
                updateWidgetText(CHANGE_PROGRESS_TEXT,String.format("%010d", currentMinutes)+"m"+
                        String.format("%010d",currentSeconds)+"s");
                mCurrentTimePassed+=1; //1s
                mHandler.postDelayed(mProgressUpdateRunnable,mUpdateInterval);
            }
        };
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                updateWidgetText(CHANGE_PLAY_BUTTON_TEXT,"Start");
                mHandler.removeCallbacks(mProgressUpdateRunnable);
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG,"Command started");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG,"onDestroy");
        super.onDestroy();
        unregisterReceiver(receiver);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG,"onBind");
        if(mDirPath!=null) {
            Log.i("YJY1987","Enter onFinishFileDialog");
            getMusicFileList(mDirPath);
        }
        else {
            Toast.makeText(this,"请选择一个目录",Toast.LENGTH_SHORT).show();
        }
        return mLocalBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG,"onUnbind");
        return super.onUnbind(intent);
    }

    void updateWidgetText(String action, String text) {
        Intent intent = new Intent(action);
        intent.putExtra(UPDATE_TEXT,text);
        sendBroadcast(intent);
    }

    public void setDataResource(String uri) throws IOException {
        Log.i("YJY1987","enter setDataResource");
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
        Log.i("YJY1987","enter setDataResource");
        startPlay();
    }

    public void startPlay() {
        if(mMediaPlayer!=null) {
            Log.i("YJY1987","start play");
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

    public int getDuration() {
        if(mMediaPlayer!=null) {
            mMusicDuration=mMediaPlayer.getDuration(); //保存ms数，以便拖动控制
            return mMusicDuration/1000; //得到秒的数据
        }
        else {
            return 0;
        }
    }

    public boolean startPlayMusic(int index)  {
        if(fileNameInMusicList==null || fileNameInMusicList.length==0) {
            Toast.makeText(this,"请先选择歌曲",Toast.LENGTH_SHORT).show();
            return false;
        }
        if(index<0) {
            index=0;
            currentMusicIndex=0;
        }
        else if(index>=totalMusics) {
            index=0;
            currentMusicIndex=0;
        }
        String filename = fileNameInMusicList[index];
        File musicFile = thePlayList.get(filename);
        String uri = musicFile.getAbsolutePath(); //把这个uri发送给播放服务
        try {
            Log.i("YJY1987", "setDataResource:" + uri);
            if(isPlaying()||(canBePaused==false&&isStop==false)) {
                stopPlay();
            }
            setDataResource(uri);
        } catch (IOException e) {
            Log.i("YJY1987", "播放器找不到文件");
        }

        int minutes=0,seconds=0;
        minutes = getDuration()/60;
        seconds = getDuration()%60;
        mCurrentLength = getDuration();
        updateWidgetText(CHANGE_DURATION_TEXT,minutes+"m"+seconds+"s");
        updateWidgetText(CHANGE_CURRENT_SONG_NAME,"歌曲: "+musicFile.getName());
        mCurrentTimePassed=0;
        mHandler.postDelayed(mProgressUpdateRunnable,mUpdateInterval);
        return true;
    }

    public void playSong() {
        if(isStop==true) {
            //从第1首开始播放
            if(startPlayMusic(0)) {
                updateWidgetText(CHANGE_PLAY_BUTTON_TEXT,"Pause");
                canBePaused = true;
                isStop = false;
                currentMusicIndex=0;
            }
        }
        else if(canBePaused==true) {
            //暂停
            pausePlay();
            canBePaused=false;
            updateWidgetText(CHANGE_PLAY_BUTTON_TEXT,"Start");
        }
        else {
            //恢复
            resumePlay();
            canBePaused=true;
            updateWidgetText(CHANGE_PLAY_BUTTON_TEXT,"Pause");

        }
    }

    public void playNextSong() {
        this.currentMusicIndex++;
        if(startPlayMusic(currentMusicIndex)) {
            updateWidgetText(CHANGE_PLAY_BUTTON_TEXT,"Pause");
            canBePaused = true;
            isStop = false;
        }
    }

    public void playPreviousSong() {
        this.currentMusicIndex--;
        if(startPlayMusic(currentMusicIndex)) {
            updateWidgetText(CHANGE_PLAY_BUTTON_TEXT,"Pause");
            canBePaused = true;
            isStop = false;
        }
    }

    public void stopSong() {
        if(isStop!=true) {
            stopPlay();
            isStop = true;
            canBePaused = false;
            updateWidgetText(CHANGE_PLAY_BUTTON_TEXT,"Start");
            updateWidgetText(CHANGE_PROGRESS_BAR,"0");
            updateWidgetText(CHANGE_PROGRESS_TEXT,"00m00s");

            currentMusicIndex=0;
            mHandler.removeCallbacks(mProgressUpdateRunnable);
        }

    }


    public void getMusicFileList(String dirPath) {
        //Update ListView PlayList here!
        //使用HashMap来进行存储或者sqlite（）
        Log.i("YJY1987","enter getMusicFileList 1");
        thePlayList = new HashMap<>();
        File fMusicList = new File(dirPath);
        FilenameFilter musicFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                Log.i("YJY1987","start postfix");
                if(filename.lastIndexOf('.')==-1) { //要先check是不是有'.'，否则substring是不接受-1作为参数的
                    return false;
                }
                String postfix=filename.substring(filename.lastIndexOf('.'),filename.length());
                Log.i("YJY1987",postfix);
                for(int i=0;i<musicPostfix.length;i++) {
                    if(postfix.equals(musicPostfix[i])) {
                        return true;
                    }
                }
                return false;
            }
        };
        Log.i("YJY1987","enter getMusicFileList 2");
        File[] musicList = fMusicList.listFiles(musicFilter);
        Log.i("YJY1987","enter getMusicFileList 3");
        for(int i=0;i<musicList.length;i++) {
            Log.i("YJY1987",musicList[i].getName());
            thePlayList.put(musicList[i].getName(),musicList[i]);
        }
        Log.i("YJY1987","1");
        //注意这里的写法
        fileNameInMusicList = thePlayList.keySet().toArray(new String[0]);
    }

    public void onFinishFileDialog(String choosedPath,int totalMusics) {
        mDirPath = choosedPath;
        this.totalMusics = totalMusics;
        Toast.makeText(this,mDirPath,Toast.LENGTH_SHORT).show();
        if(mDirPath!=null) {
            Log.i("YJY1987","Got Dir Path"+mDirPath);
            getMusicFileList(mDirPath);
        }
        else {
            Toast.makeText(this,"请选择一个目录",Toast.LENGTH_SHORT).show();
        }
    }


    public class LocalBinder extends Binder {
        public MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }
}

