package com.my.ddfiledelete;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.IntentService;
import android.content.Intent;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

public class HandlerService extends IntentService {
    private static final int Timeout = 800;
    private static String TAG = "HandlerService";
    private static boolean is_working = false;
    private static AccessibilityService mService = null;
    private static boolean is_done = false;
    private static List<String> rightMenuDesc = new ArrayList<>();
    private static List<String> chatNameFilter = new ArrayList<>();
    private static String strUserName = null;
    private static List<String> groupNameFilter = new ArrayList<>();
    private static boolean is_need_recycle = false;
    private static boolean deleteAllDone = false;
    private static int skip_recycle = 0;

    static {
        rightMenuDesc.add("聊天设置");  //单人聊天
        rightMenuDesc.add("群聊信息");  //群聊天
        chatNameFilter.add("钉盘");
    }

    public HandlerService() {
        super("DeleteHandler");
        groupNameFilter.clear();
    }

    public static void startVithService(AccessibilityService service) {
        if (is_working) {
            Log.d(TAG, "no working");
            return;
        }
        mService = service;
        Intent intent = new Intent(service, HandlerService.class);
        service.startService(intent);
    }

    private boolean nodeValid(List<AccessibilityNodeInfo> nodes) {
        if (nodes != null && nodes.size() > 0) {
            return true;
        }
        return false;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        is_working = true;
        strUserName = null;

        try {
            //handlerDelete();
            handlerMyGroupDelete();
        } catch (Throwable t) {
            Log.e(TAG, t.getMessage() + t.getStackTrace());
        }

        is_working = false;
    }

    void handlerMyGroupDelete() {
        final int group_find_max = 10;
        int group_find_count = 0;
        Log.d(TAG, "handlerDelete");
        boolean is_file_deleted = false;
        int gesture_count = 0;
        while (!is_done) {
            AccessibilityNodeInfo root = mService.getRootInActiveWindow();
            AccessibilityNodeInfo buttonContactNode = findButtonContactNode(root);
            if (buttonContactNode != null) {//主界面聊系人
                performActionClick(buttonContactNode);
                utils.sleep(Timeout);
            }
            root = mService.getRootInActiveWindow();
            AccessibilityNodeInfo fragmentContactListViewNode = findFragmentContactListViewNode(root);
            if (fragmentContactListViewNode != null) {//聊系人界面list
                int timeoutCount = 0;
                while (true) {
                    AccessibilityNodeInfo myGroupLayoutNode = findMyGroupLayoutNode(fragmentContactListViewNode);//搜索我创建的群
                    if (myGroupLayoutNode != null) {
                        utils.sleep(Timeout);
                        performActionClick(myGroupLayoutNode);
                        break;
                    }
                    performActionForward(fragmentContactListViewNode);
                    utils.sleep(Timeout);
                    if (timeoutCount++ > 10) {
                        Log.d(TAG, "search my group timeout");
                        break;
                    }
                }
            }
            root = mService.getRootInActiveWindow();
            AccessibilityNodeInfo myGroupTitleNode = findMyGroupTitleNode(root);//我的群组
            if (myGroupTitleNode != null && myGroupTitleNode.getChildCount() == 1) {
                AccessibilityNodeInfo myGroupNode = recycle(myGroupTitleNode, "我创建的");
                if (myGroupNode != null) {
                    Log.d(TAG, "click my create group");
                    performActionClickParent(myGroupNode);  //递归点击，如果不能点击，点击父亲
                } else {
                    Log.d(TAG, "recycle my create group null");
                    AccessibilityNodeInfo child = myGroupTitleNode.getChild(0);
                    int childCount = child.getChildCount();
                    if (childCount == 2) {//必须为两个子节点  0为我创建的，1为我加入的
                        AccessibilityNodeInfo child1 = child.getChild(0);//我创建的
                        AccessibilityNodeInfo tvTextNode = findTvTextNode(child1);
                        if (tvTextNode != null) {
                            String nodeText = getNodeText(tvTextNode);
                            boolean selected = tvTextNode.isSelected();
                            if (nodeText.equals("我创建的")) {
                                if (!selected) {
                                    performActionClick(child1);//如果当前不是选中我创建的，则点击选中
                                    utils.sleep(Timeout);
                                }
                            }
                        }
                    }
                }
            }
            root = mService.getRootInActiveWindow();
            AccessibilityNodeInfo viewPagerNode = findViewPagerNode(root);//我的群组页面 会含viewPager
            if (viewPagerNode != null) {
                AccessibilityNodeInfo myGroupListNode = findMyGroupListNode(viewPagerNode);
                if (myGroupListNode != null) {
                    utils.sleep(Timeout * 3);
                    int childCount = myGroupListNode.getChildCount();
                    int idxChild;
                    for (idxChild = 0; idxChild < childCount; idxChild++) {//遍历所有群节点
                        AccessibilityNodeInfo child = myGroupListNode.getChild(idxChild);
                        String className = getClassName(child);
                        if (!className.equals("android.widget.RelativeLayout")) {//过滤如 常用群组  30天内无消息的群组
                            continue;
                        }
                        AccessibilityNodeInfo groupTitleNode = findGroupTitleNode(child);//获取群组名称和人数，用于过滤
                        AccessibilityNodeInfo groupCountNode = findGroupCountNode(child);
                        if (groupCountNode == null || groupTitleNode == null) {
                            continue;
                        }
                        String strGroupTitle = getNodeText(groupTitleNode);
                        String strGroupCount = getNodeText(groupCountNode);
                        String strGroupIndent = strGroupTitle + strGroupCount + idxChild;// 当前组名称 + 群成员数 + 索引 判断为是否遍历过
                        Log.d(TAG, "groupIndent:" + strGroupIndent);
                        if (groupNameFilter.contains(strGroupIndent)) {
                            continue;
                        }
                        Log.d(TAG, "groupIndent:next");
                        group_find_count = 0;
                        is_file_deleted = false;
                        performActionClick(child);
                        groupNameFilter.add(strGroupIndent);
                        utils.sleep(Timeout);
                        break;
                    }
                    if (idxChild == childCount) {
                        performActionForward(myGroupListNode);
                        group_find_count++;
                        if (group_find_count >= group_find_max) {
                            is_done = true;
                            Log.d(TAG, "My group visited end!");
                        }
                    }
                }
            }
            //聊天窗口 右上角...
            root = mService.getRootInActiveWindow();
            AccessibilityNodeInfo rightMenuNode = findRightMenuNode(root);
            if (rightMenuNode != null) {
                int childCount = rightMenuNode.getChildCount();
                Log.d(TAG, "child count " + childCount);
                if (childCount == 2) {
                    if (is_file_deleted) {
                        utils.sleep(Timeout);
                        globalActionBack();
                    } else {
                        performActionClick(rightMenuNode.getChild(1));
                        deleteAllDone = false;  //准备进入文件进行删除
                    }
                    utils.sleep(Timeout);
                }
            }
            //群设置
            root = mService.getRootInActiveWindow();
            AccessibilityNodeInfo scrollViewNode = findScrollViewNode(root);
            if (scrollViewNode != null) {
                AccessibilityNodeInfo functionNodes = findllFunctionNode(scrollViewNode);
                if (functionNodes != null) {
                    AccessibilityNodeInfo nodeInfo1 = findllFunctionRow1Node(scrollViewNode);
                    if (nodeInfo1 != null) {
                        if (is_file_deleted) {
                            utils.sleep(Timeout*2);
                            globalActionBack();
                        } else {
                            AccessibilityNodeInfo funcNameFileNode = findFuncNameFileNode(root);
                            if (funcNameFileNode != null) {//以文件搜索按键
                                performActionClick(funcNameFileNode.getParent());
                            } else {//遍历子控件
                                int childCount = nodeInfo1.getChildCount();
                                for (int idxchild = 0; idxchild < childCount; idxchild++) {
                                    AccessibilityNodeInfo child = nodeInfo1.getChild(idxchild);
                                    AccessibilityNodeInfo tvFunctionNameNode = findTvFunctionNameNode(child);
                                    if (tvFunctionNameNode != null) {
                                        String funcName = getNodeText(tvFunctionNameNode);
                                        if (funcName.equals("文件")) {
                                            performActionClick(child);//点击文件
                                            utils.sleep(Timeout);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            //文件界面
            root = mService.getRootInActiveWindow();
            AccessibilityNodeInfo h5PcContainerNode = findH5PcContainerNode(root);
            if (h5PcContainerNode != null) {
                //is_file_deleted = true;
                //globalActionBack();
                /*
                AccessibilityNodeInfo wv = getChildNode(h5PcContainerNode, 1);
                AccessibilityNodeInfo ay = getChildNode(wv, 0);
                AccessibilityNodeInfo wv2 = getChildNode(ay, 0);
                AccessibilityNodeInfo vreactContent = getChildNode(wv2, 0);
                */
                utils.sleep(Timeout*2);
                root = mService.getRootInActiveWindow();
                if (root == null) {
                    Log.d(TAG, "root is null???");
                    globalActionBack();
                    return;
                }
                AccessibilityNodeInfo titleNode = recycle(root, "回收站");
                if (titleNode == null) {
                    Log.d(TAG, "find 回收站 null");
                    titleNode = findTitleNode(root);
                    Log.d(TAG, "findTitleNode is null");
                }
                if (titleNode != null) {
                    Log.d(TAG, "TitleNode not null " + getNodeText(titleNode));
                    Log.d(TAG, "root" + root);
                }
                if (titleNode != null && getNodeText(titleNode).equals("回收站")) {
                    Log.d(TAG, "回收站页面");
                    if (is_need_recycle) {
                        utils.sleep(Timeout*2);
                        AccessibilityNodeInfo optionMenu1Node = findOptionMenu1Node(root);
                        if (optionMenu1Node != null) {
                            performActionClick(optionMenu1Node);
                            int timeoutCount = 0;
                            while (true) {
                                utils.sleep(Timeout*2);
                                root = mService.getRootInActiveWindow();
                                if (root == null) {
                                    globalActionBack();
                                    return;
                                }
                                AccessibilityNodeInfo text1Node = findText1Node(root);
                                if (text1Node != null) {
                                    performActionClick(text1Node);//清空回收站
                                    utils.sleep(Timeout);
                                    skip_recycle = 0;
                                    break;
                                }
                                if (timeoutCount++ > 10) {
                                    Log.d(TAG, "recycle timeout");
                                    globalActionBack();
                                    break;
                                }
                            }
                            timeoutCount = 0;
                            while (true) {
                                utils.sleep(Timeout);
                                root = mService.getRootInActiveWindow();
                                if (root == null) {
                                    globalActionBack();
                                    return;
                                }
                                AccessibilityNodeInfo button1Node = findButton1Node(root);//文件将彻底删除，是否清空回收站
                                if (button1Node != null) {
                                    performActionClick(button1Node);
                                    utils.sleep(Timeout);
                                    globalActionBack();
                                    setNeedRecycle(false);
                                    break;
                                }
                                if (timeoutCount++ > 10) {
                                    globalActionBack();
                                    break;
                                }
                            }
                        }
                    } else {
                        globalActionBack();
                    }
                } else {
                    Log.d(TAG, "文件管理");
                    if (!deleteAllDone) {
                        utils.sleep(Timeout*2);
                        /*
                        AccessibilityNodeInfo webview = getChildNode(h5PcContainerNode, new int[]{0, 0, 0});
                        if (webview == null) {
                            webview = getChildNode(h5PcContainerNode, new int[]{1, 0, 0});
                        }
                        if (webview != null) {
                            performActionBackward(webview);
                            utils.sleep(300);
                            Log.d(TAG, "performActionBackward");
                        }
                        */
                        AccessibilityNodeInfo notFileNode = recycle(h5PcContainerNode, "暂无文件");
                        AccessibilityNodeInfo notFile = notFileNode;
                        if (notFileNode == null) {
                            notFile = getChildNode(h5PcContainerNode, new int[]{1, 0, 0, 0, 0, 3, 1, 0, 1}); //寻找暂无文件
                            if (notFile == null) {
                                notFile = getChildNode(h5PcContainerNode, new int[]{0, 0, 0, 0, 0, 3, 1, 0, 1}); //寻找暂无文件
                            }
                        } else {
                            notFile = notFileNode;
                        }
                        if (notFile != null && getNodeText(notFile).equals("暂无文件")) {
                            deleteAllDone = true;
                            setNeedRecycle(true);
                        } else {//有文件需要进行删除
                            AccessibilityNodeInfo selectNode = getChildNode(h5PcContainerNode, new int[]{1, 0, 0, 0, 0, 4, 0, 1, 1}); //寻找...进行多选 删除
                            if (selectNode == null) {
                                selectNode = getChildNode(h5PcContainerNode, new int[]{0, 0, 0, 0, 0, 4, 0, 1, 1}); //寻找...进行多选 删除
                            }
                            if (selectNode == null) {
                                selectNode = getChildNode(h5PcContainerNode, new int[]{1, 0, 0, 0, 0, 3, 0, 1, 1});//没有文件，可能为3
                            }
                            if (selectNode == null) {
                                selectNode = getChildNode(h5PcContainerNode, new int[]{0, 0, 0, 0, 0, 3, 0, 1, 1});
                            }
                            Log.d(TAG, "select all 1");
                            int timeoutCount = 0;
                            if (selectNode != null) {
                                performActionClick(selectNode);
                                utils.sleep(Timeout);
                                boolean isMultipleSelectClick = false;
                                timeoutCount = 0;
                                while (!isMultipleSelectClick) {
                                    utils.sleep(Timeout);
                                    root = mService.getRootInActiveWindow();
                                    if (root == null) {
                                        globalActionBack();
                                        return;
                                    }
                                    AccessibilityNodeInfo text1Node = findText1Node(root);//多选
                                    if (text1Node != null) {
                                        performActionClick(text1Node);
                                        isMultipleSelectClick = true;
                                        utils.sleep(Timeout*2);
                                    } else {
                                        if (timeoutCount++ > 10) {
                                            break;
                                        }
                                    }
                                }

                                Log.d(TAG, "select all 2");
                                timeoutCount = 0;
                                while (true) {
                                    utils.sleep(Timeout);
                                    root = mService.getRootInActiveWindow();
                                    Log.d(TAG, "root:" + root);
                                    if (root == null) {
                                        globalActionBack();
                                        return;
                                    }
                                    h5PcContainerNode = findH5PcContainerNode(root);
                                    if (h5PcContainerNode != null) {
                                        AccessibilityNodeInfo selectAll = recycle(h5PcContainerNode, "全选");
                                        if (selectAll == null) {
                                            selectAll = getChildNode(h5PcContainerNode, new int[]{1, 0, 0, 0, 0, 4, 0, 1});//全选
                                            if (selectAll == null) {
                                                selectAll = getChildNode(h5PcContainerNode, new int[]{0, 0, 0, 0, 0, 4, 0, 1});//全选
                                            }
                                        }
                                        Log.d(TAG, "select all 22:" + selectAll);
                                        if (selectAll != null) {
                                            performActionClick(selectAll);  //点击全选按键
                                            utils.sleep(Timeout);
                                            Log.d(TAG, "select all 4");
                                        }
                                        AccessibilityNodeInfo deleteNode = recycle(h5PcContainerNode, "删除");
                                        if (deleteNode == null) {
                                            deleteNode = getChildNode(h5PcContainerNode, new int[]{1, 0, 0, 0, 0, 4, 0, 3});//删除
                                            if (deleteNode == null) {
                                                deleteNode = getChildNode(h5PcContainerNode, new int[]{0, 0, 0, 0, 0, 4, 0, 3});
                                            }
                                        }
                                        if (deleteNode != null) {
                                            performActionClick(deleteNode);
                                            Log.d(TAG, "select all 5");
                                            break;
                                        }
                                    }
                                    if (timeoutCount++ > 30) {
                                        break;
                                    }
                                }
                                Log.d(TAG, "select all 3");
                                timeoutCount = 0;
                                while (true) {
                                    utils.sleep(Timeout);
                                    root = mService.getRootInActiveWindow();
                                    AccessibilityNodeInfo button1Node = findButton1Node(root);//确认删除
                                    if (button1Node != null) {
                                        performActionClick(button1Node);
                                        Log.d(TAG, "select all 6");
                                        deleteAllDone = true;
                                        setNeedRecycle(true);
                                        break;
                                    }
                                    if (timeoutCount++ > 10) {
                                        deleteAllDone = true;
                                        setNeedRecycle(true);
                                        globalActionBack();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if (is_need_recycle) {
                        root = mService.getRootInActiveWindow();
                        if (root == null) {
                            globalActionBack();
                            return;
                        }
                        utils.sleep(Timeout);
                        AccessibilityNodeInfo optionMenu2Node = findOptionMenu2Node(root);//右上角...
                        if (optionMenu2Node != null) {
                            performActionClick(optionMenu2Node);
                            Log.d(TAG, "skip recycle " + skip_recycle);
                            skip_recycle++;
                            if (skip_recycle > 2) {
                                skip_recycle = 0;
                                Log.d(TAG, "skip recycle " + skip_recycle);
                                setNeedRecycle(false);
                            }
                            utils.sleep(Timeout);
                            int timeoutCount = 0;
                            while (true) {
                                utils.sleep(Timeout);
                                root = mService.getRootInActiveWindow();
                                AccessibilityNodeInfo text1Node = findText1Node(root);//回收站
                                if (text1Node != null) {
                                    performActionClick(text1Node);
                                    break;
                                }
                                if (timeoutCount++ >= 10) {
                                    globalActionBack();
                                    break;
                                }
                            }
                        }
                    }
                    if (deleteAllDone && !is_need_recycle) {
                        utils.sleep(Timeout);
                        is_file_deleted = true;
                        skip_recycle = 0;
                        globalActionBack();
                    }
                }
            }
        }
    }

    void setNeedRecycle(boolean b) {
        is_need_recycle = b;
    }

    boolean isNeedRecycle() {
        return is_need_recycle;
    }

    AccessibilityNodeInfo getChildNode(AccessibilityNodeInfo node, int[] idx) {
        if (node == null) {
            return null;
        }
        AccessibilityNodeInfo tmp = node;
        for (int i = 0; i < idx.length; i++) {
            tmp = getChildNode(tmp, idx[i]);
        }
        return tmp;
    }

    public AccessibilityNodeInfo recycle(AccessibilityNodeInfo info, String strNodeText) {
        if (info.getChildCount() == 0) {
            Log.i(TAG, "child widget----------------------------" + info.getClassName());
            Log.i(TAG, "showDialog:" + info.canOpenPopup());
            Log.i(TAG, "Text：" + info.getText());
            Log.i(TAG, "windowId:" + info.getWindowId());
            if (getNodeText(info).equals(strNodeText)) {
                return info;
            }
            return null;
        } else {
            for (int i = 0; i < info.getChildCount(); i++) {
                if(info.getChild(i)!=null){
                    AccessibilityNodeInfo tinfo = recycle(info.getChild(i), strNodeText);
                    if (tinfo != null) {
                        return tinfo;
                    }
                }
            }
        }
        return null;
    }

    AccessibilityNodeInfo getChildNode(AccessibilityNodeInfo node, int idx) {
        if (node == null) {
            return null;
        }
        if (node.getChildCount() > idx) {
            return node.getChild(idx);
        }
        return null;
    }

    boolean handlerFileDeletePage(AccessibilityNodeInfo h5_container) {
        int h5child = h5_container.getChildCount();
        if (h5child != 1) {
            return false;
        }
        return false;
    }

    void handlerDelete() {
        Log.d(TAG, "handlerDelete");
        while (!is_done) {
            getUserName();
            if (strUserName == null) {
                is_done = true;
                continue;
            }
            Log.d(TAG, "userName:" + strUserName);
            AccessibilityNodeInfo root = mService.getRootInActiveWindow();
            AccessibilityNodeInfo sessionListNode = findSessionListNode(root);
            if (sessionListNode != null) {
                handlerMsgView(sessionListNode);
                continue;
            }

            utils.sleep(100);
        }
        Log.d(TAG, "handlerDelete done");
    }

    void handlerMsgView(AccessibilityNodeInfo sessionNode) {
        int idx;
        AccessibilityNodeInfo viewGroup = null;
        for (idx = 0; idx < sessionNode.getChildCount(); idx++) {
            viewGroup = sessionNode.getChild(idx);
            CharSequence className = viewGroup.getClassName();
            if (className != null && className.toString().equals("android.view.ViewGroup")) {
                String strTitle = getNodeText(viewGroup);
                String strGmt = getNodeText(viewGroup);
                break;
            }
            viewGroup = null;
        }
        if (viewGroup != null) {
            AccessibilityNodeInfo sessionTitleNode = findSessionTitleNode(viewGroup);
            AccessibilityNodeInfo sessionGmtNode = findSessionGmtNode(viewGroup);
            if (sessionTitleNode != null) {
                performActionClick(viewGroup);
            }
        }
    }

    void getUserName() {
        if (strUserName != null) {
            return;
        }
        AccessibilityNodeInfo root = mService.getRootInActiveWindow();
        if (root == null) {
            Log.e(TAG, "getUserName root is null");
            return;
        }

        AccessibilityNodeInfo avatarNode = findAvatarNode(root);
        if (avatarNode != null) {
            performActionClick(avatarNode);
            int timeoutCount = 0;
            while (strUserName == null) {
                root = mService.getRootInActiveWindow();
                AccessibilityNodeInfo userNameNode = findUserNameNode(root);
                if (userNameNode != null) {
                    CharSequence text = userNameNode.getText();
                    if (text != null) {
                        strUserName = text.toString();
                    }
                }
                utils.sleep(500);
                if (timeoutCount++ > 50) {
                    break;
                }
            }
            globalActionBack();
        } else {
            Log.d(TAG, "find avatar node error!");
        }
    }

    boolean handlerRightMenu(AccessibilityNodeInfo rightMenuNode) {
        //群聊  群聊信息
        //单人聊天 聊天设置
        CharSequence contentDescription = rightMenuNode.getContentDescription();
        if (contentDescription != null) {
            if (rightMenuDesc.contains(contentDescription.toString())) {
                performActionClick(rightMenuNode);
                return true;
            }
        }
        return false;
    }

    void performActionClick(AccessibilityNodeInfo node) {
        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    void performActionClickParent(AccessibilityNodeInfo node) {//递归点击，如果不能点击，点击父亲
        if (node.isClickable()) {
            performActionClick(node);
        } else {
            performActionClickParent(node.getParent());
        }
    }

    void performActionForward(AccessibilityNodeInfo node) {
        node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
    }

    void performActionBackward(AccessibilityNodeInfo node) {
        node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
    }

    boolean findAndClickRightMenu(AccessibilityNodeInfo root) {
        AccessibilityNodeInfo msgRightMenu = findRightMenuNode(root);
        if (msgRightMenu != null) {
            return handlerRightMenu(msgRightMenu);
        }
        return false;
    }

    AccessibilityNodeInfo getListNode(List<AccessibilityNodeInfo> nodes) {
        return  getListNode(nodes, 0);
    }

    AccessibilityNodeInfo getListNode(List<AccessibilityNodeInfo> nodes, int index) {
        if (nodeValid(nodes) && nodes.size() > index) {
            return nodes.get(index);
        }
        return null;
    }

    String getNodeText(AccessibilityNodeInfo node) {
        if (node == null) {
            return "";
        }
        CharSequence text = node.getText();
        if (text == null) {
            return "";
        }
        return text.toString();
    }

    String getClassName(AccessibilityNodeInfo node) {
        if (node == null) {
            return "";
        }
        CharSequence text = node.getClassName();
        if (text == null) {
            return "";
        }
        return text.toString();
    }

    String getNodeContentDescription(AccessibilityNodeInfo node) {
        if (node == null) {
            return "";
        }
        CharSequence text = node.getContentDescription();
        if (text == null) {
            return "";
        }
        return text.toString();
    }

    List<AccessibilityNodeInfo> findAccessibilityNodeInfosByViewIds(AccessibilityNodeInfo node, String id) {
        if (node == null) {
            //Log.d(TAG, "node is null:" + utils.getStackTrace());
            return null;
        }
        try {
            return node.findAccessibilityNodeInfosByViewId(id);
        } catch (Throwable t) {
            Log.e(TAG, "findAccessibilityNodeInfosByViewIds:" + t.getMessage() + " " + utils.getStackTrace());
            return null;
        }
    }

    AccessibilityNodeInfo findAccessibilityNodeInfosByViewId(AccessibilityNodeInfo node, String id) {
        return findAccessibilityNodeInfosByViewId(node, id, 0);
    }

    AccessibilityNodeInfo findAccessibilityNodeInfosByViewId(AccessibilityNodeInfo node, String id, int index) {
        if (node == null) {
            //Log.d(TAG, "node is null:" + utils.getStackTrace());
            return null;
        } else {
            List<AccessibilityNodeInfo> nodes = node.findAccessibilityNodeInfosByViewId(id);
            return getListNode(nodes, index);
        }
    }

    AccessibilityNodeInfo findAccessibilityNodeInfosByText(AccessibilityNodeInfo node, String text) {
        return findAccessibilityNodeInfosByText(node, text, 0);
    }

    AccessibilityNodeInfo findAccessibilityNodeInfosByText(AccessibilityNodeInfo node, String text, int index) {
        if (node == null) {
            //Log.d(TAG, "node is null:" + utils.getStackTrace());
            return null;
        } else {
            List<AccessibilityNodeInfo> nodes = node.findAccessibilityNodeInfosByText(text);
            return getListNode(nodes, index);
        }
    }

    AccessibilityNodeInfo findSessionListNode(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "com.alibaba.android.rimet:id/session_list");
    }

    AccessibilityNodeInfo findSessionTitleNode(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "com.alibaba.android.rimet:id/session_title");
    }

    AccessibilityNodeInfo findSessionGmtNode(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "com.alibaba.android.rimet:id/session_gmt");
    }

    AccessibilityNodeInfo findRightMenuNode(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "com.alibaba.android.rimet:id/ll_right_menu");
    }

    AccessibilityNodeInfo findAvatarNode(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "com.alibaba.android.rimet:id/fl_avatar");
    }

    AccessibilityNodeInfo findUserNameNode(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "com.alibaba.android.rimet:id/my_user_info_name");
    }

    AccessibilityNodeInfo findButtonContactNode(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "com.alibaba.android.rimet:id/home_bottom_tab_button_contact");
    }

    AccessibilityNodeInfo findButtonMessageNode(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "com.alibaba.android.rimet:id/home_bottom_tab_button_message");
    }

    AccessibilityNodeInfo findFragmentContactListViewNode(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "com.alibaba.android.rimet:id/fragment_contact_listview");
    }

    AccessibilityNodeInfo findMyGroupLayoutNode(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "com.alibaba.android.rimet:id/rl_my_group_layout");
    }

    AccessibilityNodeInfo findMyGroupTitleNode(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "com.alibaba.android.rimet:id/ll_tab_title");
    }

    AccessibilityNodeInfo findMyGroupListNode(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "com.alibaba.android.rimet:id/group_list");
    }

    AccessibilityNodeInfo findTvTextNode(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "com.alibaba.android.rimet:id/tv_text");
    }

    AccessibilityNodeInfo findViewPagerNode(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "com.alibaba.android.rimet:id/view_pager");
    }

    AccessibilityNodeInfo findGroupTitleNode(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "com.alibaba.android.rimet:id/group_title");
    }

    AccessibilityNodeInfo findGroupCountNode(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "com.alibaba.android.rimet:id/group_count");
    }

    AccessibilityNodeInfo findScrollViewNode(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "com.alibaba.android.rimet:id/scroll_view");
    }

    AccessibilityNodeInfo findllFunctionNode(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "com.alibaba.android.rimet:id/ll_function");
    }

    AccessibilityNodeInfo findTvFunctionNameNode(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "com.alibaba.android.rimet:id/tv_function_name");
    }

    AccessibilityNodeInfo findllFunctionRow1Node(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "com.alibaba.android.rimet:id/ll_function_row1");
    }

    AccessibilityNodeInfo findH5PcContainerNode(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "com.alibaba.android.rimet:id/h5_pc_container");
    }

    AccessibilityNodeInfo findTitleNode(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "com.alibaba.android.rimet:id/title");
    }

    AccessibilityNodeInfo findText1Node(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "android:id/text1");
    }

    AccessibilityNodeInfo findButton1Node(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "android:id/button1");
    }

    AccessibilityNodeInfo findOptionMenu1Node(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "com.alibaba.android.rimet:id/option_menu1");
    }

    AccessibilityNodeInfo findOptionMenu2Node(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByViewId(node, "com.alibaba.android.rimet:id/option_menu2");
    }

    AccessibilityNodeInfo findFuncNameFileNode(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByText(node, "文件");
    }


    //findAccessibilityNodeInfosByText
    AccessibilityNodeInfo findNotFileNode(AccessibilityNodeInfo node) {
        return findAccessibilityNodeInfosByText(node, "暂无文件");
    }


    void globalActionBack() {
        mService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
    }

    void globalActionHome() {
        mService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
    }

    void globalGesture() {
        /*
        GestureDescription.Builder builder = new GestureDescription.Builder();
        Path path = new Path();
        path.moveTo(400, 200);
        path.lineTo(400, 800);
        GestureDescription gestureDescription = builder.addStroke(new GestureDescription.StrokeDescription(path, 200, 200)).build();
        mService.dispatchGesture(gestureDescription, null, null);
        */
        mService.performGlobalAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
    }
}
