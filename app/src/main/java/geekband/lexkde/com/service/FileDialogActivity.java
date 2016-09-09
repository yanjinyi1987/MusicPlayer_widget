package geekband.lexkde.com.service;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Arrays;

import geekband.lexkde.com.service.service.MusicPlayerService;
import geekband.lexkde.com.service.widget.MusicWidget;

/**
 * Created by lexkde on 16-8-6.
 */
public class FileDialogActivity extends AppCompatActivity implements View.OnClickListener{
    public static final int DATA_FROM_FILE_DIALOG = 1;
    public static final int BROADCAST_FROM_FILE_DIALOGACTIVITY = 4;
    Button mToFather,mOK,mCancel;
    ListView mlistView;
    private String mParentDirName,mStoragePath;
    private String[] mFileList;
    private ArrayAdapter<String> stringArrayAdapter;
    private TextView mTextView;
    //ReturnMusicContainerPathListener mFatherActivity;
    private Messenger mMusicPlayerService = null;

    /** Flag indicating whether we have called bind on the service. */
    boolean mBound;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        //service由onBind返回
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
//            MusicPlayerService.LocalBinder localBinder = (MusicPlayerService.LocalBinder) service;
//            //getService必须declare为public
//            mMusicPlayerService = localBinder.getService();
//            mMusicPlayerService.onFinishFileDialog(null,0);
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mMusicPlayerService = new Messenger(service);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mMusicPlayerService = null;
            mBound = false;
        }
    };
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i("YJY1987","FileDialogActivity-onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_dialog);
        initViews();
        startAndBindMusicService();
        sendPrivateBroadcast(MusicWidget.PAUSE);
    }

    void sendPrivateBroadcast(String Action) {
        Intent intent = new Intent();
        intent.putExtra(MusicWidget.IDENTIFY_ID,BROADCAST_FROM_FILE_DIALOGACTIVITY);
        intent.setAction(Action);
        sendBroadcast(intent);
    }
    void startAndBindMusicService() {
        if(!MainActivity.isServiceRunning(FileDialogActivity.this,"MusicPlayerService")) {
            Intent intent_service = new Intent(this, MusicPlayerService.class);
            startService(intent_service);
        }
        Intent intent = new Intent(this,MusicPlayerService.class);
        bindService(intent,mServiceConnection,BIND_AUTO_CREATE);
    }
    void initViews()
    {
        mToFather= (Button) findViewById(R.id.return_to_father);
        mOK= (Button) findViewById(R.id.ok_button);
        mCancel= (Button) findViewById(R.id.cancel_button);
        mTextView = (TextView) findViewById(R.id.show_file_path);

        mlistView = (ListView) findViewById(R.id.filelist);
        mParentDirName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mStoragePath=mParentDirName;
        mFileList = getFileList(Environment.getExternalStorageDirectory());
        Arrays.sort(mFileList);
        if (mFileList != null) {
            Log.i("YJY1987", "mFileList is not null");
            mTextView.setText(mParentDirName);
            stringArrayAdapter = new ArrayAdapter<String>(FileDialogActivity.this
                    , android.R.layout.simple_list_item_1
                    , mFileList);
            mlistView.setAdapter(stringArrayAdapter);

            mlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String filename = mFileList[position];
                    Log.i("YJY1987", mParentDirName + "/" + filename);
                    File dir = new File(mParentDirName + "/" + filename);
                    if (dir!= null) {
                        //Log.i("YJY1987","GetParent"+dir.getParent());
                        if (dir.isFile() == true) {
                            Toast.makeText(FileDialogActivity.this
                                    , "这是一个文件"
                                    , Toast.LENGTH_SHORT).show();
                            return;
                        }
                        mFileList = getFileList(dir);
                        Arrays.sort(mFileList);
                        Log.i("YJY1987", "mFileList 2");
                        if (mFileList != null) {
                            Log.i("YJY1987", "mFileList is not null");
                            stringArrayAdapter = new ArrayAdapter<String>(FileDialogActivity.this,
                                    android.R.layout.simple_list_item_1,
                                    mFileList);
                            mlistView.setAdapter(stringArrayAdapter);
                            mParentDirName += "/";
                            mParentDirName += filename;
                            mTextView.setText(mParentDirName);
                            Log.i("YJY1987", mParentDirName);
                        }
                    }
                    else {
                        Toast.makeText(FileDialogActivity.this
                                ,"你不能Access这个目录或文件"
                                ,Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        mToFather.setOnClickListener(this);
        mOK.setOnClickListener(this);
        mCancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Message msg = new Message();
        msg.what = DATA_FROM_FILE_DIALOG;
        switch (v.getId()) {
            case R.id.return_to_father:
                File dir = new File(mParentDirName);
                String parentName=dir.getParent();
                if(!dir.getAbsolutePath().equals(mStoragePath)) {
                    mTextView.setText(parentName);
                    File parentDir = new File(parentName);
                    mFileList = getFileList(parentDir);
                    Arrays.sort(mFileList);
                    Log.i("YJY1987", "mFileList 2");
                    if (mFileList!= null) {
                        Log.i("YJY1987", "mFileList is not null");
                        stringArrayAdapter = new ArrayAdapter<String>(FileDialogActivity.this
                                ,android.R.layout.simple_list_item_1
                                ,mFileList);
                        mlistView.setAdapter(stringArrayAdapter);
                        mParentDirName = parentName;
                        Log.i("YJY1987", mParentDirName);
                    }
                }
                else {
                    Toast.makeText(FileDialogActivity.this
                    ,"就到这里了，OK？"
                    ,Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.ok_button:
                //向host返回一个确定的目录来作为uri
                if(mFileList!=null) {
                    Log.i("DJB",""+mFileList.length);
                    //mMusicPlayerService.onFinishFileDialog(mParentDirName, mFileList.length);
                    //msg.obj = new DataToService(mParentDirName,mFileList.length);
                    msg.getData().putParcelable("DataToService",new DataToService(mParentDirName,mFileList.length));
                    try {
                        mMusicPlayerService.send(msg);
                        sendPrivateBroadcast(MusicWidget.RESTART);
                    } catch (RemoteException e) {
                        Log.d("FileDialogActivity","ok error");
                        e.printStackTrace();
                    }
                }
                else {
                    //http://stackoverflow.com/questions/15005615/class-not-found-when-unmarshalling-when-passing-parcelable-through-messenger-to

                    //mMusicPlayerService.onFinishFileDialog(null,0);
                    msg.getData().putParcelable("DataToService",new DataToService(null,0));
                    try {
                        mMusicPlayerService.send(msg);
                    } catch (RemoteException e) {
                        Log.d("FileDialogActivity","ok error null");
                        e.printStackTrace();
                    }
                    sendPrivateBroadcast(MusicWidget.RESUME);
                }
                unbindService(mServiceConnection);
                finish();
                break;
            case R.id.cancel_button:
                //向host返回一个null
                //when using remoteService
                msg.getData().putParcelable("DataToService",new DataToService(null,0));
                sendPrivateBroadcast(MusicWidget.RESUME);
                try {
                    mMusicPlayerService.send(msg);
                } catch (RemoteException e) {
                    Log.d("FileDialogActivity","cancle error");
                    e.printStackTrace();
                }
                unbindService(mServiceConnection);
                finish();
                break;
            default:
                break;
        }
    }

    public String[] getFileList(File fDir) { //需要在AndroidManifest.xml中添加读写STORAGE权限
        return fDir.list();
    }
}