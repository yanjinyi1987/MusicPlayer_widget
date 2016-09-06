package geekband.lexkde.com.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import geekband.lexkde.com.service.service.MusicPlayerService;
import geekband.lexkde.com.service.widget.MusicWidget;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public static final int BROADCAST_FROM_ACTIVITY = 1;
    private Button mStartButton,mStopButton,mNextButton,mPrevButton,mChooseFileButton;
    private ProgressBar mProgressBar;
    private TextView mMusicLength,mMusicPlayTime,mSongName;
    private ListView mListView;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action  = intent.getAction();
            switch (action) {
                case MusicPlayerService.CHANGE_CURRENT_SONG_NAME :
                    Log.i("YJY1986","CHANGE_CURRENT_SONG_NAME");
                    String songName = intent.getStringExtra(MusicPlayerService.UPDATE_TEXT);
                    Log.i("Change Song name", songName);
                    mSongName.setText(songName);
                    break;
                case MusicPlayerService.CHANGE_DURATION_TEXT:
                    Log.i("YJY1986","CHANGE_DURATION_TEXT");
                    String durationText = intent.getStringExtra(MusicPlayerService.UPDATE_TEXT);
                    Log.i("Change Text", durationText);
                    mMusicLength.setText(durationText);
                    break;
                case MusicPlayerService.CHANGE_PLAY_BUTTON_TEXT:
                    Log.i("YJY1986","CHANGE_PLAY_BUTTON_TEXT");
                    String buttonText = intent.getStringExtra(MusicPlayerService.UPDATE_TEXT);
                    Log.i("Change Text", buttonText);
                    mStartButton.setText(buttonText);
                    break;
                case MusicPlayerService.CHANGE_PROGRESS_BAR:
                    Log.i("YJY1986","CHANGE_PROGRESS_BAR");
                    String progress = intent.getStringExtra(MusicPlayerService.UPDATE_TEXT);
                    Log.i("Progress", progress);
                    mProgressBar.setProgress(Integer.valueOf(progress));
                    break;
                case MusicPlayerService.CHANGE_PROGRESS_TEXT:
                    Log.i("YJY1986","CHANGE_PROGRESS_TEXT");
                    String progressText = intent.getStringExtra(MusicPlayerService.UPDATE_TEXT);
                    Log.i("Passed Time", progressText);
                    mMusicPlayTime.setText(progressText);
                    break;
                default:
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_button);

        initViews();
    }

    private void initViews() {
        mStartButton = (Button) findViewById(R.id.PlayMusic);
        mStopButton = (Button) findViewById(R.id.StopMusic);
        mNextButton = (Button) findViewById(R.id.NextSong);
        mPrevButton = (Button) findViewById(R.id.PreviousSong);
        mChooseFileButton = (Button) findViewById(R.id.openFiles);

        mMusicLength = (TextView) findViewById(R.id.MusicLength);
        mMusicPlayTime = (TextView) findViewById(R.id.MusicPlayTime);
        mListView = (ListView) findViewById(R.id.play_list);

        mStartButton.setOnClickListener(this);
        mStopButton.setOnClickListener(this);
        mNextButton.setOnClickListener(this);
        mPrevButton.setOnClickListener(this);
        mChooseFileButton.setOnClickListener(this);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //TODO
            }
        });
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
            default:
                break;
        }

    }
}
