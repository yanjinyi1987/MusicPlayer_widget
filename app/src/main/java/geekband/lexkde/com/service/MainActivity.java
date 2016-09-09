package geekband.lexkde.com.service;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import geekband.lexkde.com.service.service.MusicPlayerService;
import geekband.lexkde.com.service.widget.MusicWidget;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public static final int BROADCAST_FROM_ACTIVITY = 1;
    public static final int BROADCAST_FROM_NOTIFICATION=2;
    public static final String WHICH_TO_PLAY = "WhichToPlay";
    public static final int SEND_MESSENGER_TO_SERVICE = 2;
    public static final String HightColor = "#8f0000";
    public static final String CommonColor = "#ffffff";

    private Button mStartButton,mStopButton,mNextButton,mPrevButton,mChooseFileButton;
    private ProgressBar mProgressBar;
    private TextView mMusicLength,mMusicPlayTime,mSongName;
    private ListView mListView;

    //got Playlist from service
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    private Messenger mServiceMessenger;
    /** Flag indicating whether we have called bind on the service. */
    boolean mBound;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            Log.i("MainActivity","onServiceConnected");
            mServiceMessenger = new Messenger(service);
            mBound = true;

            Message msg = new Message();
            msg.what = SEND_MESSENGER_TO_SERVICE;
            msg.obj = mMessenger;
            try {
                mServiceMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mBound = false;
        }
    };
    private int mCurrentMusicIndex;
    private ImageView mImageView;

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MusicPlayerService.PLAYLIST_TO_MAINACTIVITY:
                    Log.i("MainActivity", "got playlist");
                    Bundle playlistBundle = msg.getData().getBundle("PlaylistBundle");
                    String[] playlist = playlistBundle.getStringArray("Playlist");
                    mCurrentMusicIndex = playlistBundle.getInt("MusicIndex");

                    try {
                        mPlayList.clear();
                        for (int i = 0; i < playlist.length; i++) {
                            if (i == mCurrentMusicIndex) {
                                mPlayList.add(new MyPlayList(playlist[i], HightColor));
                            }
                            else {
                                mPlayList.add(new MyPlayList(playlist[i], CommonColor));
                            }
                        }
                    } catch (Exception e) {
                        Log.i("MainActivity", "Array to ArrayList failed");
                        e.printStackTrace();
                    }
                    mArrayAdapter.notifyDataSetChanged();

                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action  = intent.getAction();
            int fromWhere = intent.getIntExtra(MusicWidget.IDENTIFY_ID,-1);
            //RemoteViews remoteViews = new RemoteViews(getPackageName(),R.layout.music_player_notification_layout);
            switch (action) {
                case MusicPlayerService.CHANGE_CURRENT_SONG_NAME :
                    Log.i("YJY1986","CHANGE_CURRENT_SONG_NAME");
                    String songName = intent.getStringExtra(MusicPlayerService.UPDATE_TEXT);
                    Log.i("Change Song in Main", songName);
                    mSongName.setText(songName);
                    //remoteViews.setTextViewText(mSongName.getId(),songName);
                    break;
                case MusicPlayerService.CHANGE_DURATION_TEXT:
                    Log.i("YJY1986","CHANGE_DURATION_TEXT");
                    String durationText = intent.getStringExtra(MusicPlayerService.UPDATE_TEXT);
//                    Log.i("Change Duration in Main", durationText);
                    mMusicLength.setText(durationText);
                    //remoteViews.setTextViewText(mMusicLength.getId(),durationText);
                    break;
                case MusicPlayerService.CHANGE_PLAY_BUTTON_TEXT:
                    Log.i("YJY1986","CHANGE_PLAY_BUTTON_TEXT");
                    String buttonText = intent.getStringExtra(MusicPlayerService.UPDATE_TEXT);
                    Log.i("Change Button in Main", buttonText);
                    mStartButton.setText(buttonText);
                    //remoteViews.setTextViewText(mStartButton.getId(),buttonText);
                    break;
                case MusicPlayerService.CHANGE_PROGRESS_BAR:
                    //Log.i("YJY1986","CHANGE_PROGRESS_BAR");
                    String progress = intent.getStringExtra(MusicPlayerService.UPDATE_TEXT);
                    //Log.i("Progress", progress);
                    mProgressBar.setProgress(Integer.valueOf(progress));
                    //remoteViews.setProgressBar(mProgressBar.getId(),100,Integer.valueOf(progress),false);
                    break;
                case MusicPlayerService.CHANGE_PROGRESS_TEXT:
                    //Log.i("YJY1986","CHANGE_PROGRESS_TEXT");
                    String progressText = intent.getStringExtra(MusicPlayerService.UPDATE_TEXT);
                    //Log.i("Passed Time in Main", progressText);
                    mMusicPlayTime.setText(progressText);
                    //remoteViews.setTextViewText(mMusicPlayTime.getId(),progressText);
                    break;
                case MusicPlayerService.SEND_CURRENT_MUSIC_INDEX:
                    int mCurrentMusicIndex_old = mCurrentMusicIndex;
                    mCurrentMusicIndex = intent.getIntExtra(MusicPlayerService.UPDATE_INT,0);
                    if(mPlayList.size()>0) {
                        mPlayList.get(mCurrentMusicIndex_old).color = CommonColor;
                        mPlayList.get(mCurrentMusicIndex).color = HightColor;
                        mArrayAdapter.notifyDataSetChanged();
                    }
                    break;
                case MusicWidget.STOP_FOREGROUND_SERVICE:
                    if(fromWhere!=BROADCAST_FROM_ACTIVITY) {
                        finish();
                    }
                    break;
                default:
                    break;
            }
            //mNotificationManager.notify(1,mNotification);
        }
    };
    private List<MyPlayList> mPlayList;
    private ArrayAdapter<MyPlayList> mArrayAdapter;

    /*
	 * 判断服务是否启动,context上下文对象 ，className服务的name
	 */
    public static boolean isServiceRunning(Context mContext, String className) {

        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager
                .getRunningServices(30);

        if (!(serviceList.size() > 0)) {
            return false;
        }

        for (int i = 0; i < serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().equals(className) == true) {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_button);
        //添加IntentFilter
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MusicPlayerService.CHANGE_CURRENT_SONG_NAME);
        intentFilter.addAction(MusicPlayerService.CHANGE_PLAY_BUTTON_TEXT);
        intentFilter.addAction(MusicPlayerService.CHANGE_PROGRESS_BAR);
        intentFilter.addAction(MusicPlayerService.CHANGE_DURATION_TEXT);
        intentFilter.addAction(MusicPlayerService.CHANGE_PROGRESS_TEXT);
        intentFilter.addAction(MusicPlayerService.SEND_CURRENT_MUSIC_INDEX);
        intentFilter.addAction(MusicWidget.STOP_FOREGROUND_SERVICE);

        //注册receiver
        registerReceiver(mBroadcastReceiver,intentFilter);
        Log.i("MainActivity","onCreate");

        initViews();

        if(!isServiceRunning(MainActivity.this,"MusicPlayerService")) {
            Intent intent_service = new Intent(this, MusicPlayerService.class);
            startService(intent_service); //service can be started multiple times, closed only once
        }
        Intent intenttoService = new Intent(this,MusicPlayerService.class);
        bindService(intenttoService,mServiceConnection,BIND_AUTO_CREATE);

        Intent intent = new Intent();
        intent.putExtra(MusicWidget.IDENTIFY_ID,BROADCAST_FROM_ACTIVITY);
        intent.setAction(MusicWidget.GET_CURRENT_STATUS);
        sendBroadcast(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent();
        intent.putExtra(MusicWidget.IDENTIFY_ID, BROADCAST_FROM_ACTIVITY);
        intent.setAction(MusicWidget.GET_CURRENT_STATUS);
        sendBroadcast(intent);
    }

    @Override
    public void onTrimMemory(int level) {
        Log.i("MainActivity","onTrimMemory");
//        //STOP_FOREGROUND_SERVICE
//        Intent intent = new Intent();
//        intent.putExtra(MusicWidget.IDENTIFY_ID,BROADCAST_FROM_ACTIVITY);
//        intent.setAction(MusicWidget.STOP_FOREGROUND_SERVICE);
//        sendBroadcast(intent);
        super.onTrimMemory(level);
    }

    @Override
    protected void onStop() {
        Log.i("MainActivity","onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.i("MainActivity","onDestroy");
        unregisterReceiver(mBroadcastReceiver);
        unbindService(mServiceConnection);
        StopPlayWhenExit();
        super.onDestroy();
    }

    private void StopPlayWhenExit() {
        //Intent newIntent = new Intent();
        //newIntent.setAction(MusicWidget.STOP);
        //newIntent.setAction(MusicWidget.STOP_FOREGROUND_SERVICE);
        //newIntent.putExtra(MusicWidget.IDENTIFY_ID, BROADCAST_FROM_ACTIVITY);
        //sendBroadcast(newIntent);
    }

    private void initViews() {
        mStartButton = (Button) findViewById(R.id.PlayMusic);
        mStopButton = (Button) findViewById(R.id.StopMusic);
        mNextButton = (Button) findViewById(R.id.NextSong);
        mPrevButton = (Button) findViewById(R.id.PreviousSong);
        mChooseFileButton = (Button) findViewById(R.id.openFiles);
        mImageView = (ImageView) findViewById(R.id.close_button);

        mMusicLength = (TextView) findViewById(R.id.MusicLength);
        mMusicPlayTime = (TextView) findViewById(R.id.MusicPlayTime);
        mSongName = (TextView) findViewById(R.id.SongName);
        mProgressBar = (ProgressBar) findViewById(R.id.SongProgress);



        mStartButton.setOnClickListener(this);
        mStopButton.setOnClickListener(this);
        mNextButton.setOnClickListener(this);
        mPrevButton.setOnClickListener(this);
        mChooseFileButton.setOnClickListener(this);
        mImageView.setOnClickListener(this);

        //Playlist
        mPlayList = new ArrayList<>();
        mArrayAdapter = new PlaylistArrayAdapter(MainActivity.this,
                R.layout.playlist_item,
                mPlayList);
        mListView = (ListView) findViewById(R.id.play_list);
        mListView.setAdapter(mArrayAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //TODO
                Intent newIntent = new Intent();
                newIntent.setAction(MusicWidget.PLAY_RANDOM);
                newIntent.putExtra(MusicWidget.IDENTIFY_ID, BROADCAST_FROM_ACTIVITY);
                newIntent.putExtra(WHICH_TO_PLAY,position);
                sendBroadcast(newIntent);
            }
        });
    }

    class PlaylistArrayAdapter extends ArrayAdapter<MyPlayList> {
        List<MyPlayList> playLists;
        int resourceId;
        public PlaylistArrayAdapter(Context context, int resource, List<MyPlayList> objects) {
            super(context, resource, objects);
            playLists = objects;
            resourceId=resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MyPlayList currentPlaylistItem = playLists.get(position);
            TextView textView;
            ViewHolder viewHolder;
            View view;
            if(convertView==null) {
                view = getLayoutInflater().inflate(resourceId, null);
                textView = (TextView) view.findViewById(R.id.music_name_playlist);
                view.setTag(new ViewHolder(textView));

            }
            else {
                view = convertView;
                viewHolder = (ViewHolder) view.getTag();
                textView = viewHolder.mTextView;
            }
            textView.setBackgroundColor(Color.parseColor(currentPlaylistItem.color));
            textView.setText(currentPlaylistItem.musicName);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP,20);
            return view;
        }

        class ViewHolder {
            public TextView mTextView;

            public ViewHolder(TextView mTextView) {
                this.mTextView = mTextView;
            }
        }
    }

    class MyPlayList {
        public String musicName;
        public String color;

        public MyPlayList(String musicName, String color) {
            this.musicName = musicName;
            this.color = color;
        }
    }
    @Override
    public void onClick(View v) {
        Intent newIntent = new Intent();
        switch (v.getId()) {
            case R.id.PlayMusic:
                //send broadcast
                newIntent.setAction(MusicWidget.START);
                newIntent.putExtra(MusicWidget.IDENTIFY_ID, BROADCAST_FROM_ACTIVITY);
                sendBroadcast(newIntent);
                break;
            case R.id.StopMusic:
                //send broadcast
                newIntent.setAction(MusicWidget.STOP);
                newIntent.putExtra(MusicWidget.IDENTIFY_ID, BROADCAST_FROM_ACTIVITY);
                sendBroadcast(newIntent);
                break;
            case R.id.NextSong:
                //send broadcast
                newIntent.setAction(MusicWidget.NEXT_SONG);
                newIntent.putExtra(MusicWidget.IDENTIFY_ID, BROADCAST_FROM_ACTIVITY);
                sendBroadcast(newIntent);
                break;
            case R.id.PreviousSong:
                //send broadcast
                newIntent.setAction(MusicWidget.PREV_SONG);
                newIntent.putExtra(MusicWidget.IDENTIFY_ID, BROADCAST_FROM_ACTIVITY);
                sendBroadcast(newIntent);
                break;
            case R.id.openFiles:
                startActivity(new Intent(MainActivity.this,FileDialogActivity.class));
                break;
            case R.id.close_button:
                finish();
                break;
            default:
                break;
        }

    }
}
