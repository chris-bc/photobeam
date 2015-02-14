package au.id.bennettscash.photobeam;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.tagmanager.TagManager;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OngoingNotificationListenerService extends WearableListenerService {
    public static  String PATH = "/somePath";
    public static String PATH_KILL = "/photobeam/kill";
    public static int NOTIFICATION_ID = 1;

    GoogleApiClient mGoogleApiClient;

    public OngoingNotificationListenerService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d("TAG", "message");
        if (messageEvent.getPath().equals(PATH_KILL)) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();

        if (!mGoogleApiClient.isConnected()) {
            ConnectionResult connectionResult = mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);
            if (!connectionResult.isSuccess()) {
                Log.e("TAG", "Service failed to connect to GoogleApiClient.");
                return;
            }
        }

        for (DataEvent event : events) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if (PATH.equals(path)) {
                    // Get data out
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    Asset asset = dataMapItem.getDataMap().getAsset(NotificationActivity.EXTRA_IMAGE);

                    loadBitmapFromAsset(this, asset);
//                    Bitmap img = BitmapFactory.decodeStream(Wearable.DataApi.getFdForAsset(mGoogleApiClient, asset).await().getInputStream());

                } else {
                    Log.d("TAG", "unrecognised path: " + path);
                }
            }
        }
    }

    private static void displayNotification(Context context, Asset asset, Bitmap img) {
        // Build the intent
        Intent notificationIntent = new Intent(context, NotificationActivity.class);
        notificationIntent.putExtra(NotificationActivity.EXTRA_IMAGE, asset);
        PendingIntent notificationPendingIntent = PendingIntent.getService(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent removeIntent = new Intent("action_remove_timer", null, context, NotificationActivity.class);
        PendingIntent pendingRemoveIntent = PendingIntent.getService(context, 0, removeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Create the ongoing notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(img)
                .setContentTitle("Photo")
                .setStyle(new NotificationCompat.BigPictureStyle().bigPicture(img))
                .setOngoing(true)
                .addAction(android.R.drawable.ic_delete, "Remove", pendingRemoveIntent)
                .extend(new NotificationCompat.WearableExtender().setDisplayIntent(notificationPendingIntent));

        // Build the notification and show it
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private static void loadBitmapFromAsset(final Context context, final Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }

        new AsyncTask<Asset, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Asset... assets) {
                GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                        .addApi(Wearable.API)
                        .build();
                ConnectionResult result = googleApiClient.blockingConnect(1000, TimeUnit.MILLISECONDS);
                if (!result.isSuccess()) {
                    return null;
                }

                // Convert asset into a file descriptor and block until it's ready
                InputStream assetInputStream = Wearable.DataApi.getFdForAsset(googleApiClient, assets[0]).await().getInputStream();
                googleApiClient.disconnect();

                if (assetInputStream == null) {
                    Log.w("TAG", "Requested an unknown asset.");
                    return null;
                }

                // Decode the stream into a bitmap
                return BitmapFactory.decodeStream(assetInputStream);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    displayNotification(context, asset, bitmap);
                }
            }

        }.execute(asset);
    }
}
