package com.example.occupines;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.occupines.activities.MainActivity;
import com.example.occupines.models.Reminders;

import java.util.Date;

public class NotifierAlarm extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        AppDatabase appDatabase = AppDatabase.getAppDatabase(context.getApplicationContext());
        RoomDAO roomDAO = appDatabase.getRoomDAO();
        Reminders reminder = new Reminders();
        reminder.setMessage(intent.getStringExtra("Message"));
        reminder.setRemindDate(new Date(intent.getStringExtra("RemindDate")));
        reminder.setId(intent.getIntExtra("id", 0));
        roomDAO.Delete(reminder);
        AppDatabase.destroyInstance();

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        Intent intent1 = new Intent(context, MainActivity.class);
        intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
        taskStackBuilder.addParentStack(MainActivity.class);
        taskStackBuilder.addNextIntent(intent1);

        PendingIntent intent2 = taskStackBuilder.getPendingIntent(1, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        //Vibration
        builder.setVibrate(new long[]{1000, 1000, 1000, 1000, 1000});
        //LED
        builder.setLights(Color.RED, 3000, 3000);

        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel("my_channel_01", "hello", NotificationManager.IMPORTANCE_HIGH);
        }

        Notification notification = builder.setContentTitle("Occupines")
                .setContentText(intent.getStringExtra("Message")).setAutoCancel(true)
                .setSound(alarmSound).setSmallIcon(R.mipmap.occupines)
                .setContentIntent(intent2)
                .setChannelId("my_channel_01")
                .build();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager.notify(1, notification);
    }
}
