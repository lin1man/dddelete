package com.my.ddfiledelete;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class DeleteService extends AccessibilityService {
    private String TAG = "DeleteService";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        LogUtil.d(TAG, event.toString());
        int eventType = event.getEventType();
        switch (eventType) {
            case AccessibilityEvent.TYPES_ALL_MASK:
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                HandlerService.startVithService(this);
                break;
        }
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {//界面更新时才去读取配置
            HandlerService.settingUpdate(this);
        }
    }

    @Override
    public void onInterrupt() {

    }
}
