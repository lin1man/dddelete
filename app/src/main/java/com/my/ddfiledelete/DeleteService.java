package com.my.ddfiledelete;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

public class DeleteService extends AccessibilityService {
    private static boolean handler_content_change = false;
    private String TAG = "DeleteService";

    private static boolean is_over = false;
    private static int lastIndex = 1;
    private static String strLastTitle = "";
    private static List<String> listVisited = new ArrayList<>();
    private static int lastItemCount = 1;
    private static AccessibilityNodeInfo sourceNode = null;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(TAG, event.toString());
        int eventType = event.getEventType();
        String packageName = event.getPackageName().toString();
        AccessibilityNodeInfo rootInActiveWindow = null;
        switch (eventType) {
            case AccessibilityEvent.TYPES_ALL_MASK:
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                HandlerService.startVithService(this);
                break;
        }
    }

    //@Override
    public void onAccessibilityEvent1(AccessibilityEvent event) {
        Log.d(TAG, event.toString());
        int eventType = event.getEventType();
        String packageName = event.getPackageName().toString();
        AccessibilityNodeInfo rootInActiveWindow = null;
        switch (eventType) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                Intent intent = new Intent(this, HandlerService.class);
                startService(intent);
                rootInActiveWindow = getRootInActiveWindow();
                if (rootInActiveWindow != null) {
                    List<AccessibilityNodeInfo> backNode = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.alibaba.android.rimet:id/ll_left_menu");
                    if (backNode != null && backNode.size() > 0) {
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            Log.e(TAG, "Sleep exception " + e.getMessage());
                        }
                        backNode.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.d(TAG, "ll_left_menu clicked");
                        return;
                    }
                    backNode = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.alibaba.android.rimet:id/back_layout");
                    if (backNode != null && backNode.size() > 0) {
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            Log.e(TAG, "Sleep exception " + e.getMessage());
                        }
                        backNode.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.d(TAG, "back_layout clicked");
                        return;
                    }
                    List<AccessibilityNodeInfo> nodes = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.alibaba.android.rimet:id/session_list");
                    if (nodes != null && nodes.size() > 0) {
                        mainStateChange(nodes.get(0));
                        return;
                    }
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED://TYPE_WINDOW_CONTENT_CHANGED TYPE_WINDOW_STATE_CHANGED
                rootInActiveWindow = getRootInActiveWindow();
                if (rootInActiveWindow != null) {
                    List<AccessibilityNodeInfo> nodes = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.alibaba.android.rimet:id/session_list");
                    if (nodes != null && nodes.size() > 0) {
                        autoDelete(event);
                    } else {
                        Log.e(TAG, "not find ll_list_stick_container ???");
                    }
                }
                break;
        }
    }

    @Override
    public void onInterrupt() {

    }

    private void sleep(int mm) {
        try {
            Thread.sleep(mm);
        } catch (Throwable t) {
            Log.d(TAG, t.getStackTrace().toString());
        }
    }

    private void mainStateChange(AccessibilityNodeInfo sessionList) {
        sessionList.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        sleep(300);
    }

    private void autoDelete(AccessibilityEvent event) {
        if (is_over) {
            return;
        }
        if (event != null && event.getItemCount() > 0) {
            lastItemCount = event.getItemCount();
            sourceNode = event.getSource();
            if (sourceNode == null) {
                Log.d(TAG, "what source null??");
                return;
            }

            AccessibilityNodeInfo rootInActiveWindow = getRootInActiveWindow();
            List<AccessibilityNodeInfo> nodes = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.alibaba.android.rimet:id/session_list");
            if (nodes == null && nodes.size() < 1) {
                Log.e(TAG, "mainStateChange not find ll_list_stick_container ???");
                return;
            }
            Bundle arg = new Bundle();
            arg.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_ROW_INT,  lastIndex);
            sourceNode.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_TO_POSITION.getId(), arg);
            sleep(300);
            int idxViewGroup;
            AccessibilityNodeInfo child = null;
            for (idxViewGroup = 0; idxViewGroup < sourceNode.getChildCount(); idxViewGroup++) {
                child = sourceNode.getChild(idxViewGroup);
                if (child == null) {
                    continue;
                }
                if (child.getClassName().equals("android.view.ViewGroup")) {
                    List<AccessibilityNodeInfo> titleNode = child.findAccessibilityNodeInfosByViewId("com.alibaba.android.rimet:id/session_title");
                    if (titleNode == null || titleNode.size() < 1) {
                        Log.e(TAG, "session_title is null ???");
                        continue;
                    }
                    List<AccessibilityNodeInfo> gmtNode = child.findAccessibilityNodeInfosByViewId("com.alibaba.android.rimet:id/session_gmt");
                    String gmt = "";
                    if (gmtNode != null && gmtNode.size() > 0) {
                        gmt = gmtNode.get(0).getText().toString();
                    }
                    String title = titleNode.get(0).getText().toString() + gmt;
                    if (listVisited.contains(title)) {
                        continue;
                    }
                    listVisited.add(title);
                    break;
                } else {
                    Log.d(TAG, "className:" + child.getClassName());
                }
            }
            if ((idxViewGroup < sourceNode.getChildCount()) && child != null) {
                child.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                lastIndex ++;
            } else {
                Log.e(TAG, "Not find ViewGroup ???");
            }
        }
    }

    private void autoDelete1(AccessibilityEvent event) {
        if (event != null && event.getItemCount() > 0) {
            handler_content_change = true;
            Log.d(TAG, "getFromIndex:" + event.getFromIndex());
            Log.d(TAG, "getItemCount:" + event.getItemCount());
            Log.d(TAG, "getToIndex:" + event.getToIndex());
            Log.d(TAG, "getScrollX:" + event.getScrollX());
            Log.d(TAG, "getScrollY:" + event.getScrollY());
            int toidx = event.getToIndex();
            try {
                AccessibilityNodeInfo source = event.getSource();
                /*
                Bundle bundle = new Bundle();
                bundle.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_ROW_INT,  1);
                source.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, bundle);
                */
                Log.d(TAG, "source:" + source.toString());
                Log.d(TAG, "childcount:" + source.getChildCount());
                int itemcount = event.getItemCount();
                for (int i = 1; i < itemcount; i ++) {
                    int idxViewGroup = 0;
                    AccessibilityNodeInfo child = null;
                    for (idxViewGroup = 0; idxViewGroup < source.getChildCount(); idxViewGroup++) {
                        child = source.getChild(idxViewGroup);
                        if (child.getClassName().equals("android.view.ViewGroup")) {
                            Log.d(TAG, "Index:" + idxViewGroup);
                            break;
                        } else {
                            Log.d(TAG, "className:" + child.getClassName());
                        }
                    }
                    if (child != null) {
                        child.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        break;
                    }
                    Bundle bundle = new Bundle();
                    bundle.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_ROW_INT,  i);
                    source.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_TO_POSITION.getId(), bundle);
                    Log.d(TAG, "childcount:" + source.getChildCount());
                    Thread.sleep(1000);
                }
                Log.e(TAG, "scroll down");
            } catch (Exception e) {
                Log.e(TAG, "autoDelete except:" + e.getMessage());
            }
            handler_content_change = false;
        }
    }

    private void autoDelete() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> sessonNodes = nodeInfo.findAccessibilityNodeInfosByViewId("com.alibaba.android.rimet:id/session_list");
            if (sessonNodes != null && sessonNodes.size() > 0) {
                AccessibilityNodeInfo.CollectionInfo collectionInfo = sessonNodes.get(0).getCollectionInfo();
                if (collectionInfo != null) {
                    Log.d(TAG, "collectionInfo:" + collectionInfo.toString());
                }
                List<AccessibilityNodeInfo.AccessibilityAction> actionList = sessonNodes.get(0).getActionList();
                if (actionList != null) {
                    Log.d(TAG, "actionList:" + actionList.toString());
                }
                AccessibilityNodeInfo.CollectionItemInfo collectionItemInfo = sessonNodes.get(0).getCollectionItemInfo();
                if (collectionItemInfo != null) {
                    Log.d(TAG, "collectionItemInfo:" + collectionItemInfo.toString());
                }
                Log.d(TAG, "sessonNodes:" + sessonNodes.size() + "  " + sessonNodes.get(0).toString());
            }
        }
    }

    private void autoDelete1() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            while (true) {
                AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                if (rootNode != null) {
                    /*
                    List<AccessibilityNodeInfo> sessonNodes = rootNode.findAccessibilityNodeInfosByViewId("com.alibaba.android.rimet:id/session_item");
                    if (sessonNodes != null && sessonNodes.size() > 0) {
                        AccessibilityNodeInfo sessionNode = sessonNodes.get(0);
                        List<AccessibilityNodeInfo> context_tv = sessionNode.findAccessibilityNodeInfosByViewId("com.alibaba.android.rimet:id/session_content_tv");
                        if (context_tv != null && context_tv.size() > 0) {
                            //String gmt = gmtNode.get(0).getText().toString();
                            Log.d(TAG, "tv:" + context_tv.get(0).getText().toString());
                        } else {
                            Log.d(TAG, "tv error!");
                        }
                        sessionNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                    } else {
                        Log.e(TAG, "??? sessonNodes null ???");
                    }
                    */
                    List<AccessibilityNodeInfo> sessonNodes = rootNode.findAccessibilityNodeInfosByViewId("com.alibaba.android.rimet:id/session_item");
                    if (sessonNodes != null && sessonNodes.size() > 0) {
                        for (AccessibilityNodeInfo sessionNode : sessonNodes) {
                            List<AccessibilityNodeInfo> itemTitleNode = sessionNode.findAccessibilityNodeInfosByViewId("com.alibaba.android.rimet:id/session_title");
                            if (itemTitleNode != null && itemTitleNode.size() > 0) {
                                Log.d(TAG, "title:" + itemTitleNode.get(0).getText());
                            } else {
                                Log.e(TAG, "title is null???");
                            }
                        }
                        sessonNodes.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);

                    } else {
                        Log.e(TAG, "??? sessonNodes null ???");
                    }
                } else {
                    Log.e(TAG, "??? rootNode null ???");
                    break;
                }
            }
        } else {
            Log.e(TAG, "??? nodeInfo null ???");
        }
    }
}
