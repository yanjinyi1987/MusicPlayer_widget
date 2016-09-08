package geekband.lexkde.com.service.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import geekband.lexkde.com.service.DataToService;
import geekband.lexkde.com.service.FileDialogActivity;
import geekband.lexkde.com.service.MainActivity;
import geekband.lexkde.com.service.R;
import geekband.lexkde.com.service.widget.MusicWidget;

/*
 * Binder是沟通Activity与Service的桥梁，在二者之间搭建了沟通的渠道，而start方式好像做不到。
 * 这中方式的生命周期中多了onBind与unBind两个节点
 */
public class MusicPlayerService extends Service {
    public static  final  String TAG = "YJY:MusicPlayerService";
    public static final String CHANGE_PLAY_BUTTON_TEXT = "MusicPlayerService.ChangeButtonText";
    public static final String CHANGE_DURATION_TEXT ="MusicPlayerService.ChangeBDurationText";
    public static final String CHANGE_PROGRESS_BAR ="MusicPlayerService.ChangeProgressBar";
    public static final String CHANGE_PROGRESS_TEXT ="MusicPlayerService.ChangeProgressText";
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
            int fromWhere = intent.getIntExtra(MusicWidget.IDENTIFY_ID,-1);
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
                case MusicWidget.GET_CURRENT_STATUS:
                    Log.i("YJY1986","action_GET_CURRENT_STATUS");
                    if(fromWhere==MainActivity.BROADCAST_FROM_ACTIVITY ||
                            fromWhere==MusicWidget.BROADCAST_FROM_WIDGET) {
                        if(canBePaused==true) {
                            updateWidgetAndActivityText(CHANGE_PLAY_BUTTON_TEXT,"Pause");
                        }
                        updateWidgetAndActivityText(CHANGE_DURATION_TEXT,currentDurationString);
                    }
                    break;
                case MusicWidget.STOP_FOREGROUND_SERVICE:
                    Log.i("YJY1986","action_STOP_FOREGROUND_SERVICE");
                    if(fromWhere==MainActivity.BROADCAST_FROM_NOTIFICATION) {
                        updateWidgetAndActivityText(CHANGE_CURRENT_SONG_NAME,"");
                        updateWidgetAndActivityText(CHANGE_PLAY_BUTTON_TEXT,
                                getResources().getString(R.string.play_music));
                        updateWidgetAndActivityText(CHANGE_PROGRESS_BAR,"0");
                        updateWidgetAndActivityText(CHANGE_DURATION_TEXT,"");
                        updateWidgetAndActivityText(CHANGE_PROGRESS_TEXT,"");
                        mMediaPlayer.reset();
                        //stopForeground(true);
                        stopSelf();
                    }
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
    private String currentDurationString;

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FileDialogActivity.DATA_FROM_FILE_DIALOG:
                    msg.getData().setClassLoader(DataToService.class.getClassLoader());
                    DataToService dataToService = msg.getData().getParcelable("DataToService");
                    mDirPath = dataToService.mParentDirName;
                    totalMusics = dataToService.fileList_count;
                    Toast.makeText(MusicPlayerService.this,mDirPath,Toast.LENGTH_SHORT).show();
                    if(mDirPath!=null) {
                        Log.i("YJY1987","Got Dir Path"+mDirPath);
                        getMusicFileList(mDirPath);
                    }
                    else {
                        Toast.makeText(MusicPlayerService.this,"请选择一个目录",Toast.LENGTH_SHORT).show();
                    }
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    //Notification
    private NotificationManager mNotificationManager;
    private Notification mNotification;
    private RemoteViews remoteViews;
    public static final int BROADCAST_FROM_NOTIFICATION=2;
    @Override
    public void onCreate() {
        super.onCreate();
        //添加IntentFilter
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MusicWidget.START);
        intentFilter.addAction(MusicWidget.STOP);
        intentFilter.addAction(MusicWidget.NEXT_SONG);
        intentFilter.addAction(MusicWidget.PREV_SONG);
        intentFilter.addAction(MusicWidget.GET_CURRENT_STATUS);
        intentFilter.addAction(MusicWidget.STOP_FOREGROUND_SERVICE);
//        intentFilter.addAction(ANDROID_NET_CONN_CONNECTIVITY_CHANGE);

        //注册receiver
        registerReceiver(receiver,intentFilter);
        Log.i(TAG,"onCreate");

        //update progressBar
        mProgressUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                int progress = (int)((float)mCurrentTimePassed*100.0/mCurrentLength);
                //Log.i("ProgressBar Update",String.valueOf(progress));
                updateWidgetAndActivityText(CHANGE_PROGRESS_BAR,String.valueOf(progress));
                currentMinutes = mCurrentTimePassed/60;
                currentSeconds = mCurrentTimePassed%60;
                updateWidgetAndActivityText(CHANGE_PROGRESS_TEXT,String.format("%02d", currentMinutes)+"m"+
                        String.format("%02d",currentSeconds)+"s");
                mCurrentTimePassed+=1; //1s
                mHandler.postDelayed(mProgressUpdateRunnable,mUpdateInterval);
            }
        };
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                updateWidgetAndActivityText(CHANGE_PLAY_BUTTON_TEXT,getResources().getString(R.string.play_music));
                mHandler.removeCallbacks(mProgressUpdateRunnable);
            }
        });
        initNotification();
    }

    private void initNotification() {
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//        Parcel myParcel = Parcel.obtain();
//        myParcel.writeInt(R.mipmap.ic_launcher);
//        myParcel.writeString(getResources().getString(R.string.app_name));
//        myParcel.writeLong(System.currentTimeMillis());
//        mNotification = new Notification(myParcel);
        mNotification = new Notification(R.drawable.monkey,getResources().getString(R.string.app_name),
                System.currentTimeMillis());

        remoteViews = new RemoteViews(getPackageName(), R.layout.music_player_notification_layout);
        mNotification.bigContentView = remoteViews;
        mNotification.contentView = remoteViews;

        PendingIntent contentIntent = PendingIntent.getActivity(this,
                R.string.app_name,
                new Intent(this,MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        mNotification.contentIntent = contentIntent;
        mNotification.flags |= mNotification.FLAG_NO_CLEAR; //need change

        initRemoteViews(MusicPlayerService.this,remoteViews);
        //mNotificationManager.notify(1,mNotification);
        startForeground(1,mNotification);
    }

    private void initRemoteViews(Context context,RemoteViews remoteViews) {
        Intent intent = new Intent(context,FileDialogActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context
                ,1
                ,intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.openFiles,pendingIntent);


        Intent intent2 =new Intent();
        intent2.setAction(MusicWidget.NEXT_SONG); //设定一个动作
        intent2.putExtra(MusicWidget.IDENTIFY_ID, BROADCAST_FROM_NOTIFICATION);
        PendingIntent pendingIntent2 = PendingIntent.getBroadcast(context,0,intent2,0); //用于未来执行
        remoteViews.setOnClickPendingIntent(R.id.NextSong,pendingIntent2); //点击以后发送广播

        Intent intent3 =new Intent();
        intent3.setAction(MusicWidget.PREV_SONG); //设定一个动作
        intent3.putExtra(MusicWidget.IDENTIFY_ID, BROADCAST_FROM_NOTIFICATION);
        PendingIntent pendingIntent3 = PendingIntent.getBroadcast(context,0,intent3,0); //用于未来执行
        remoteViews.setOnClickPendingIntent(R.id.PreviousSong,pendingIntent3); //点击以后发送广播

        Intent intent4 =new Intent();
        intent4.setAction(MusicWidget.STOP); //设定一个动作
        intent4.putExtra(MusicWidget.IDENTIFY_ID, BROADCAST_FROM_NOTIFICATION);
        PendingIntent pendingIntent4 = PendingIntent.getBroadcast(context,0,intent4,0); //用于未来执行
        remoteViews.setOnClickPendingIntent(R.id.StopMusic,pendingIntent4); //点击以后发送广播

        Intent intent7 =new Intent();
        intent7.setAction(MusicWidget.START); //设定一个动作
        intent7.putExtra(MusicWidget.IDENTIFY_ID, BROADCAST_FROM_NOTIFICATION);
        PendingIntent pendingIntent7 = PendingIntent.getBroadcast(context,0,intent7,0); //用于未来执行
        remoteViews.setOnClickPendingIntent(R.id.PlayMusic,pendingIntent7); //点击以后发送广播

        Intent intent8 =new Intent();
        intent8.setAction(MusicWidget.STOP_FOREGROUND_SERVICE); //设定一个动作
        intent8.putExtra(MusicWidget.IDENTIFY_ID, BROADCAST_FROM_NOTIFICATION);
        PendingIntent pendingIntent8 = PendingIntent.getBroadcast(context,0,intent8,0); //用于未来执行
        remoteViews.setOnClickPendingIntent(R.id.close_button,pendingIntent8); //点击以后发送广播

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG,"Command started");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG,"onDestroy");
        unregisterReceiver(receiver);
        super.onDestroy();
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
        //return mLocalBinder;
        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG,"onUnbind");
        return super.onUnbind(intent);
    }

    void updateWidgetAndActivityText(String action, String text) {
        Intent intent = new Intent(action);
        intent.putExtra(UPDATE_TEXT,text);
        sendBroadcast(intent);

        //RemoteViews remoteViews = new RemoteViews(getPackageName(),R.layout.music_player_notification_layout);
        switch (action) {
            case MusicPlayerService.CHANGE_CURRENT_SONG_NAME :
                Log.i("YJY1986","CHANGE_CURRENT_SONG_NAME");
                String songName = intent.getStringExtra(MusicPlayerService.UPDATE_TEXT);
                Log.i("Change Song in Main", songName);
                remoteViews.setTextViewText(R.id.SongName,songName);
                break;
            case MusicPlayerService.CHANGE_DURATION_TEXT:
                Log.i("YJY1986","CHANGE_DURATION_TEXT");
                String durationText = intent.getStringExtra(MusicPlayerService.UPDATE_TEXT);
                Log.i("Change Duration in Main", durationText);
                remoteViews.setTextViewText(R.id.MusicLength,durationText);
                break;
            case MusicPlayerService.CHANGE_PLAY_BUTTON_TEXT:
                Log.i("YJY1986","CHANGE_PLAY_BUTTON_TEXT");
                String buttonText = intent.getStringExtra(MusicPlayerService.UPDATE_TEXT);
                Log.i("Change Button in Main", buttonText);
                remoteViews.setTextViewText(R.id.PlayMusic,buttonText);
                break;
            case MusicPlayerService.CHANGE_PROGRESS_BAR:
                //Log.i("YJY1986","CHANGE_PROGRESS_BAR");
                String progress = intent.getStringExtra(MusicPlayerService.UPDATE_TEXT);
                //Log.i("Progress", progress);
                remoteViews.setProgressBar(R.id.SongProgress,100,Integer.valueOf(progress),false);
                break;
            case MusicPlayerService.CHANGE_PROGRESS_TEXT:
                //Log.i("YJY1986","CHANGE_PROGRESS_TEXT");
                String progressText = intent.getStringExtra(MusicPlayerService.UPDATE_TEXT);
                //Log.i("Passed Time in Main", progressText);
                remoteViews.setTextViewText(R.id.MusicPlayTime,progressText);
                break;
            default:
                break;
        }
        mNotificationManager.notify(1,mNotification);
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
        currentDurationString = ""+minutes+"m"+seconds+"s";
        updateWidgetAndActivityText(CHANGE_DURATION_TEXT, currentDurationString);
        updateWidgetAndActivityText(CHANGE_CURRENT_SONG_NAME,"歌曲: "+musicFile.getName());
        mCurrentTimePassed=0;
        mHandler.postDelayed(mProgressUpdateRunnable,mUpdateInterval);
        return true;
    }

    public void playSong() {
        if(isStop==true) {
            //从第1首开始播放
            if(startPlayMusic(0)) {
                updateWidgetAndActivityText(CHANGE_PLAY_BUTTON_TEXT,"Pause");
                canBePaused = true;
                isStop = false;
                currentMusicIndex=0;
            }
        }
        else if(canBePaused==true) {
            //暂停
            pausePlay();
            canBePaused=false;
            updateWidgetAndActivityText(CHANGE_PLAY_BUTTON_TEXT,
                    getResources().getString(R.string.play_music));
        }
        else {
            //恢复
            resumePlay();
            canBePaused=true;
            updateWidgetAndActivityText(CHANGE_PLAY_BUTTON_TEXT,"Pause");

        }
    }

    public void playNextSong() {
        this.currentMusicIndex++;
        if(startPlayMusic(currentMusicIndex)) {
            updateWidgetAndActivityText(CHANGE_PLAY_BUTTON_TEXT,"Pause");
            canBePaused = true;
            isStop = false;
        }
    }

    public void playPreviousSong() {
        this.currentMusicIndex--;
        if(startPlayMusic(currentMusicIndex)) {
            updateWidgetAndActivityText(CHANGE_PLAY_BUTTON_TEXT,"Pause");
            canBePaused = true;
            isStop = false;
        }
    }

    public void stopSong() {
        if(isStop!=true) {
            stopPlay();
            isStop = true;
            canBePaused = false;
            updateWidgetAndActivityText(CHANGE_PLAY_BUTTON_TEXT,
                    getResources().getString(R.string.play_music));
            updateWidgetAndActivityText(CHANGE_PROGRESS_BAR,"0");
            updateWidgetAndActivityText(CHANGE_PROGRESS_TEXT,"00m00s");
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

//    public void onFinishFileDialog(String choosedPath,int totalMusics) {
//        mDirPath = choosedPath;
//        this.totalMusics = totalMusics;
//        Toast.makeText(this,mDirPath,Toast.LENGTH_SHORT).show();
//        if(mDirPath!=null) {
//            Log.i("YJY1987","Got Dir Path"+mDirPath);
//            getMusicFileList(mDirPath);
//        }
//        else {
//            Toast.makeText(this,"请选择一个目录",Toast.LENGTH_SHORT).show();
//        }
//    }


    public class LocalBinder extends Binder {
        public MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }
}