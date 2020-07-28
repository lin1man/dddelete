package com.my.ddfiledelete;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;

@SuppressLint("NewApi")
public class GestureUtil {
    private static final Object obj = new Object();
    private static final String TAG = "DDFileDelete_gesture";
    private static boolean bCompleted;

    public static boolean gesturePressRegion(AccessibilityService service, int x1, int y1, int x2, int y2) {
        Path path = new Path();

        path.moveTo(x1, y1);
        path.moveTo(x2, y2);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        GestureDescription gestureDescription = builder.addStroke(new GestureDescription.StrokeDescription(path, 100, 500)).build();
        bCompleted = true;
        synchronized (obj) {
            service.dispatchGesture(gestureDescription, new AccessibilityService.GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                    LogUtil.i(TAG, "onCompleted, gesturePressRegion------");
                    bCompleted = true;
                    synchronized (obj) {
                        obj.notify();
                    }
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    super.onCancelled(gestureDescription);
                    LogUtil.i(TAG, "onCancelled, gesturePressRegion------");
                    bCompleted = false;
                    synchronized (obj) {
                        obj.notify();
                    }
                }
            }, null);

            LogUtil.i(TAG, "gesturePressRegion locked------");
            try {
                obj.wait();
            } catch (Throwable t) {
                t.printStackTrace();
            }
            LogUtil.i(TAG, "gesturePressRegion, bCompleted:" + bCompleted);
        }

        path.reset();

        utils.sleep(500);
        return bCompleted;
    }
}
