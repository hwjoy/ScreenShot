package com.redant.screenshot;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ScreenShotActivity extends AppCompatActivity {

    private static final String TAG = "ScreenShotActivity";

    private static final int MEDIA_PROJECTION_REQUEST_CODE = 1;
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 2;

    private MediaProjectionManager mMediaProjectionManager;
    private ImageReader mImageReader;

    private String mFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        } else {
            mFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/ScreenShots/";
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (mMediaProjectionManager != null) {
                startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), MEDIA_PROJECTION_REQUEST_CODE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case MEDIA_PROJECTION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        MediaProjection mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
                        DisplayMetrics displayMetrics = new DisplayMetrics();
                        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

                        mImageReader = ImageReader.newInstance(displayMetrics.widthPixels, displayMetrics.heightPixels, PixelFormat.RGBA_8888, 2);

                        mediaProjection.createVirtualDisplay(
                                "ScreenShot",
                                displayMetrics.widthPixels,
                                displayMetrics.heightPixels,
                                displayMetrics.densityDpi,
                                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                                mImageReader.getSurface(), null, null
                        );

                        new Handler().postDelayed(new Runnable() {
                            @TargetApi(Build.VERSION_CODES.KITKAT)
                            @Override
                            public void run() {
                                Image image = mImageReader.acquireLatestImage();

                                int width = image.getWidth();
                                int height = image.getHeight();

                                final Image.Plane[] planes = image.getPlanes();
                                final ByteBuffer buffer = planes[0].getBuffer();

                                int pixelStride = planes[0].getPixelStride();
                                int rowStride = planes[0].getRowStride();
                                int rowPadding = rowStride - pixelStride * width;

                                Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                                bitmap.copyPixelsFromBuffer(buffer);
                                bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
                                image.close();

                                if (bitmap != null) {
                                    try {
                                        File imageFile = new File( mFilePath + "Screenshot_" + System.currentTimeMillis() + ".png");
                                        Log.d(TAG, "imageFile URL: " + imageFile.getAbsolutePath());
                                        if (!imageFile.exists()) {
                                            Log.d(TAG, "File is not exists, create new one.");
                                            imageFile.createNewFile();
                                        }
                                        FileOutputStream fileOutputStream = new FileOutputStream(imageFile);
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                                        fileOutputStream.flush();
                                        fileOutputStream.close();

                                        Toast.makeText(ScreenShotActivity.this, "ScreenShot Succeeded.", Toast.LENGTH_SHORT).show();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } finally {
                                        Intent intent = new Intent(ScreenShotActivity.this, FloatWindowService.class);
                                        startService(intent);
                                        finish();
                                    }
                                }
                            }
                        }, 300);
                    }
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case WRITE_EXTERNAL_STORAGE_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/ScreenShots/";
                    File dirFile = new File(mFilePath);
                    if (!dirFile.exists()) {
                        boolean isSuccess = dirFile.mkdirs();
                        if (!isSuccess) {
                            mFilePath = getExternalFilesDir("screenshot").getPath() + "/";
                        }
                    }
                } else {
                    mFilePath = getExternalFilesDir("screenshot").getPath() + "/";
                }
                break;
        }
    }
}
