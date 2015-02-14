package au.id.bennettscash.photobeam;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class NotificationActivity extends Activity {
    public static String EXTRA_IMAGE = "photobeam_image";

    private Bitmap img;
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });
/*
        Intent intent = getIntent();
        if (intent != null) {
            final Asset asset = intent.getParcelableExtra(EXTRA_IMAGE);
            img = loadBitmapFromAsset(this, asset);
        }
*/
    }
/*
    public static Bitmap loadBitmapFromAsset(final Context context, final Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }
        final ArrayList<Bitmap> retVal = new ArrayList<Bitmap>(1);

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
                    retVal.set(0, bitmap);
                }
            }

        }.execute(asset);

        return retVal.get(0);
    }
    */

   // @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (action.equals("action_remove_timer")) {
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                notificationManager.cancel(OngoingNotificationListenerService.NOTIFICATION_ID);
            }
        }
    }
}
