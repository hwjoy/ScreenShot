package com.redant.screenshot;

import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

public class FloatWindowService extends Service {

    private static final String TAG = "FloatWindowService";

    private Button mFloatButton;
    private WindowManager.LayoutParams mLayoutParams;

    private float mXTouchBeginInView;
    private float mYTouchBeginInView;

    public FloatWindowService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mFloatButton = createFloatView();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mFloatButton != null) {
            WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            if (windowManager == null) {
                return;
            }

            windowManager.removeView(mFloatButton);
        }
    }

    private Button createFloatView() {
        Log.i(TAG, "createFloatView");
        final WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) {
            return null;
        }

        Button button = new Button(this);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setStroke(37, Color.BLACK);
        drawable.setColor(Color.GRAY);
        drawable.setAlpha(127);
        drawable.setCornerRadius(37);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            button.setBackground(drawable);
        } else {
            button.setBackgroundDrawable(drawable);
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performScreenShot();
            }
        });
        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                performLock();
                return true;
            }
        });
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int statusBarHeight = getStatusBarHeight(FloatWindowService.this);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mXTouchBeginInView = event.getX();
                        mYTouchBeginInView = event.getY();
                        break;

                    case MotionEvent.ACTION_MOVE: {
                        int moveX = Math.round(event.getRawX() - mXTouchBeginInView);
                        int moveY = Math.round(event.getRawY() - mYTouchBeginInView - statusBarHeight);

                        mLayoutParams.x = moveX;
                        mLayoutParams.y = moveY;
                        windowManager.updateViewLayout(v, mLayoutParams);
                        break;
                    }

                    case MotionEvent.ACTION_UP:
//                        v.performClick();

                        mXTouchBeginInView = 0;
                        mYTouchBeginInView = 0;

                        int moveX = mLayoutParams.x;
                        int moveY = mLayoutParams.y;

                        Point point = new Point();
                        windowManager.getDefaultDisplay().getSize(point);
                        int screenWidth = point.x;
                        int screenHeight = point.y - statusBarHeight;

                        if (moveX > screenWidth / 2) {
                            if (moveY > screenHeight / 2) {
                                if (screenWidth - moveX > screenHeight - moveY) {
                                    moveY = screenHeight;
                                } else {
                                    moveX = screenWidth;
                                }
                            } else {
                                if (screenWidth - moveX > moveY) {
                                    moveY = 0;
                                } else {
                                    moveX = screenWidth;
                                }
                            }
                        } else {
                            if (moveY > screenHeight / 2) {
                                if (moveX > screenHeight - moveY) {
                                    moveY = screenHeight;
                                } else {
                                    moveX = 0;
                                }
                            } else {
                                if (moveX > moveY) {
                                    moveY = 0;
                                } else {
                                    moveX = 0;
                                }
                            }
                        }

                        mLayoutParams.x = moveX;
                        mLayoutParams.y = moveY;
                        windowManager.updateViewLayout(v, mLayoutParams);

                        break;
                }
                return false;
            }
        });

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        layoutParams.x = 0;
        layoutParams.y = 0;
        layoutParams.width = 150;
        layoutParams.height = 150;
        mLayoutParams = layoutParams;

        windowManager.addView(button, layoutParams);

        return button;
    }

    private void performHome() {
        Log.i(TAG, "performHome");
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }

    private void performLock() {
        Log.i(TAG, "performLock");
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, LockScreenReceiver.class);
        if (devicePolicyManager != null && devicePolicyManager.isAdminActive(admin)) {
            devicePolicyManager.lockNow();
        } else {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Lock Screen");
            startActivity(intent);
        }
    }

    private void performScreenShot() {
        Log.i(TAG, "performScreenShot");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (mediaProjectionManager == null) {
                Log.w(TAG, "performScreenShot error: mediaProjectionManager is null");
                return;
            }

            mFloatButton.setVisibility(View.INVISIBLE);

            Intent intent = new Intent(getBaseContext(), ScreenShotActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mFloatButton.setVisibility(View.VISIBLE);
                }
            }, 600);
        }
    }

    private int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
