package com.my.ddfiledelete;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class DeleteService extends AccessibilityService {
    private String TAG = "DeleteService";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(TAG, event.toString());
        int eventType = event.getEventType();
        switch (eventType) {
            case AccessibilityEvent.TYPES_ALL_MASK:
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                HandlerService.startVithService(this);
                break;
        }
    }

    @Override
    public void onInterrupt() {

    }
}
