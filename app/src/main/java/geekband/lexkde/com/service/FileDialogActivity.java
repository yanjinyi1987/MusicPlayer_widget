package geekband.lexkde.com.service;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
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

/**
 * Created by lexkde on 16-8-6.
 */
public class FileDialogActivity extends AppCompatActivity implements View.OnClickListener{
    Button mToFather,mOK,mCancel;
    ListView mlistView;
    private String mParentDirName,mStoragePath;
    private String[] mFileList;
    private ArrayAdapter<String> stringArrayAdapter;
    private TextView mTextView;
    private MusicPlayerService mMusicPlayerService;
    //ReturnMusicContainerPathListener mFatherActivity;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        //service由onBind返回
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.LocalBinder localBinder = (MusicPlayerService.LocalBinder) service;
            //getService必须declare为public
            mMusicPlayerService = localBinder.getService();
            mMusicPlayerService.onFinishFileDialog(null,0);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i("YJY1987","FileDialogActivity-onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_dialog);
        initViews();
        startAndBindMusicService();
    }

    void startAndBindMusicService() {
        Intent intent_service = new Intent(this,MusicPlayerService.class);
        startService(intent_service);
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
                    if (mFileList != null) {
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
                    mMusicPlayerService.onFinishFileDialog(mParentDirName, mFileList.length);
                }
                else {
                    mMusicPlayerService.onFinishFileDialog(null,0);
                }
                unbindService(mServiceConnection);
                finish();
                break;
            case R.id.cancel_button:
                //向host返回一个null
                mMusicPlayerService.onFinishFileDialog(null,0);
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
