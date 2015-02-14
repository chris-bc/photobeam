package au.id.bennettscash.photobeam;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Random;


public class MyActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public static String KEY_IMAGE = "photobeam_image";
    public static String PATH = "/somePath";
    public static String PATH_KILL = "/photobeam/kill";

    private GoogleApiClient mGoogleApiClient;
    private final ArrayList<String> imagesPath = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        initImages();

        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onDestroy() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        super.onDestroy();
    }

    private void sendRequest() {
        if (mGoogleApiClient.isConnected()) {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH);

            // Add data to the request
            Bitmap img = getImage();
            Asset asset = createAssetFromBitmap(img);
            putDataMapRequest.getDataMap().putAsset(KEY_IMAGE, asset);

            PutDataRequest request = putDataMapRequest.asPutDataRequest();

            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            Log.d("TAG", "putDataItem status: " + dataItemResult.getStatus().toString());
                        }
                    });
        }
    }

    private void initImages() {
        String[] projection = new String[] {MediaStore.Images.Media.DATA};

        Uri images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Cursor cur = getContentResolver().query(images, projection, null, null, null);

        imagesPath.clear();

        if (cur.moveToFirst()) {
            int dataColumn = cur.getColumnIndex(MediaStore.Images.Media.DATA);
            do {
                imagesPath.add(cur.getString(dataColumn));
            } while (cur.moveToNext());
        }
        cur.close();
    }

    private Bitmap getImage() {
        final Random random = new Random();
        final int count = imagesPath.size();
        int number = random.nextInt(count);
        String path = imagesPath.get(number);
        return BitmapFactory.decodeFile(path);
    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }

    public void onConnectionSuspended(int cause) {
        // TODO: something
    }

    public void onConnected(Bundle connectionHint) {
        //TODO: something
        sendRequest();
    }

    public void onConnectionFailed(ConnectionResult result) {
        //TODO: something
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onButtonSelected(View v) {
        if (mGoogleApiClient.isConnected()) {
            sendRequest();
        }
    }

    public void onTerminateSelected(View v) {
        // Send a message to kill the notification
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH_KILL);

        PutDataRequest request = putDataMapRequest.asPutDataRequest();

        Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        Log.d("TAG", "putDataItem status: " + dataItemResult.getStatus().toString());
                    }
                });
    }
}
