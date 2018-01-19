package com.redant.screenshot;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
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
        final WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) {
            return null;
        }

        Button button = new Button(this);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

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
                        v.performClick();

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
        layoutParams.width = 200;
        layoutParams.height = 200;
        mLayoutParams = layoutParams;

        windowManager.addView(button, layoutParams);

        return button;
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
