package geekband.lexkde.com.service.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import geekband.lexkde.com.service.FileDialogActivity;
import geekband.lexkde.com.service.R;
import geekband.lexkde.com.service.service.MusicPlayerService;

/**
 * Created by lexkde on 16-8-8.
 * 使用广播与后台通信
 */
public class MusicWidget extends AppWidgetProvider {

    public static final String NEXT_SONG = "MusicWidget.TO_SERVICE_NEXT_SONG"; //2
    public static final String PREV_SONG = "MusicWidget.TO_SERVICE_PREV_SONG"; //3
    public static final String STOP = "MusicWidget.TO_SERVICE_STOP"; //4
    public static final String START = "MusicWidget.TO_SERVICE_START"; //7
    public static final String GET_CURRENT_STATUS = "MusicWidget.GET_CURRENT_STATUS";
    public static final String STOP_FOREGROUND_SERVICE = "MusicWidget.STOP_FOREGROUND_SERVICE";
    public static final String IDENTIFY_ID = "IdentifyID";
    public static final int BROADCAST_FROM_WIDGET = 0;
    private RemoteViews remoteViews;

    @Override
    public void onEnabled(Context context) {
        Log.i("YJY1987MusicWidget","onEnable");
        //Enable看起来需要较长的一段时间
        super.onEnabled(context);
        Intent intent = new Intent();
        intent.putExtra(MusicWidget.IDENTIFY_ID,BROADCAST_FROM_WIDGET);
        intent.setAction(MusicWidget.GET_CURRENT_STATUS);
        context.sendBroadcast(intent);


    }

    @Override
    public void onReceive(Context context, Intent intent) {

        super.onReceive(context, intent);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.music_player_widget);
        if (intent!=null){
            //check AddressId
            if(TextUtils.equals(intent.getAction(),MusicPlayerService.CHANGE_PLAY_BUTTON_TEXT)) {
                String buttontext = intent.getStringExtra(MusicPlayerService.UPDATE_TEXT);
                Log.i("Change Text", buttontext);
                remoteViews.setTextViewText(R.id.PlayMusic, buttontext);
            }
            else if(TextUtils.equals(intent.getAction(),MusicPlayerService.CHANGE_DURATION_TEXT)) {
                String durationtext = intent.getStringExtra(MusicPlayerService.UPDATE_TEXT);
                Log.i("Change Text", durationtext);
                remoteViews.setTextViewText(R.id.MusicLength, durationtext);
            }
            else if(TextUtils.equals(intent.getAction(),MusicPlayerService.CHANGE_CURRENT_SONG_NAME)) {
                String songName = intent.getStringExtra(MusicPlayerService.UPDATE_TEXT);
                Log.i("Change Song name", songName);
                remoteViews.setTextViewText(R.id.SongName, songName);
            }
            else if(TextUtils.equals(intent.getAction(),MusicPlayerService.CHANGE_PROGRESS_BAR)){
                String progress = intent.getStringExtra(MusicPlayerService.UPDATE_TEXT);
                //Log.i("Progress", progress);
                remoteViews.setProgressBar(R.id.SongProgress,100,Integer.valueOf(progress),false);
            }
            else if(TextUtils.equals(intent.getAction(),MusicPlayerService.CHANGE_PROGRESS_TEXT)){
                String progress = intent.getStringExtra(MusicPlayerService.UPDATE_TEXT);
                //Log.i("Passed Time", progress);
                remoteViews.setTextViewText(R.id.MusicPlayTime,progress);
            }
            else {

            }
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName componentName = new ComponentName(context,MusicWidget.class);
            appWidgetManager.updateAppWidget(componentName,remoteViews);
        }

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.i("YJY1987MusicWidget","onUpdate");
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.music_player_widget);

        Intent intent = new Intent(context,FileDialogActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context
                ,1
                ,intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.openFiles,pendingIntent);


        Intent intent2 =new Intent();
        intent2.setAction(NEXT_SONG); //设定一个动作
        intent2.putExtra(IDENTIFY_ID, BROADCAST_FROM_WIDGET);
        PendingIntent pendingIntent2 = PendingIntent.getBroadcast(context,0,intent2,0); //用于未来执行
        remoteViews.setOnClickPendingIntent(R.id.NextSong,pendingIntent2); //点击以后发送广播

        Intent intent3 =new Intent();
        intent3.setAction(PREV_SONG); //设定一个动作
        intent3.putExtra(IDENTIFY_ID, BROADCAST_FROM_WIDGET);
        PendingIntent pendingIntent3 = PendingIntent.getBroadcast(context,0,intent3,0); //用于未来执行
        remoteViews.setOnClickPendingIntent(R.id.PreviousSong,pendingIntent3); //点击以后发送广播

        Intent intent4 =new Intent();
        intent4.setAction(STOP); //设定一个动作
        intent4.putExtra(IDENTIFY_ID, BROADCAST_FROM_WIDGET);
        PendingIntent pendingIntent4 = PendingIntent.getBroadcast(context,0,intent4,0); //用于未来执行
        remoteViews.setOnClickPendingIntent(R.id.StopMusic,pendingIntent4); //点击以后发送广播

        Intent intent7 =new Intent();
        intent7.setAction(START); //设定一个动作
        intent7.putExtra(IDENTIFY_ID, BROADCAST_FROM_WIDGET);
        PendingIntent pendingIntent7 = PendingIntent.getBroadcast(context,0,intent7,0); //用于未来执行
        remoteViews.setOnClickPendingIntent(R.id.PlayMusic,pendingIntent7); //点击以后发送广播
        appWidgetManager.updateAppWidget(appWidgetIds,remoteViews);
    }
}
