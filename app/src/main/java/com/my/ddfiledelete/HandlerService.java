package com.my.ddfiledelete;

import android.accessibilityservice.AccessibilityService;
import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
    private static int work_mode = utils.n_mode_my_group;

    static {
        rightMenuDesc.add("聊天设置");  //单人聊天
        rightMenuDesc.add("群聊信息");  //群聊天
        chatNameFilter.add("钉盘");
    }

    public static void flagClear() {
        is_working = false;
        strUserName = null;
        groupNameFilter.clear();
        is_need_recycle = false;
        deleteAllDone = false;
        skip_recycle = 0;
        is_done = false;
    }

    public HandlerService() {
        super("DeleteHandler");
        groupNameFilter.clear();
    }

    public static void settingUpdate(AccessibilityService service) {
        SharedPreferences sharedPreferences = service.getSharedPreferences(utils.strSettingsName, MODE_PRIVATE);
        boolean reest = sharedPreferences.getBoolean(utils.strResetConfigName, false);
        if (reest) {
            LogUtil.d(TAG, "reset flag and stop service");
            Intent intent = new Intent(service, HandlerService.class);
            service.stopService(intent);
            flagClear();
            sharedPreferences.edit().putBoolean(utils.strResetConfigName, false).apply();
        }
        LogUtil.logable = sharedPreferences.getBoolean(utils.strLogSwitchConfigName, false);
        work_mode = sharedPreferences.getInt(utils.strModeConfigName, utils.n_mode_my_group);
    }

    public static void startVithService(AccessibilityService service) {
        if (is_working) {
            LogUtil.d(TAG, "no working");
            return;
        }
        mService = service;
        Intent intent = new Intent(service, HandlerService.class);
        service.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.d(TAG, "HandlerService onCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.d(TAG, "HandlerService onDestroy");
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
            LogUtil.e(TAG, t.getMessage() + t.getStackTrace());
        }
        LogUtil.d(TAG, "onHandleIntent 一轮结束");
        is_working = false;
    }

    void handlerMyGroupDelete() {
        final int group_find_max = 10;
        int group_find_count = 0;
        LogUtil.d(TAG, "handlerMyGroupDelete");
        boolean is_file_deleted = false;
        int gesture_count = 0;
        while (!is_done) {
            if (isModeMyJoinGroup()) {
                getUserName();
                if (strUserName == null) {
                    LogUtil.e(TAG, "等待获取用户名称！！");
                    continue;
                }
            }
            //主界面聊系人 有发现则点击
            AccessibilityNodeInfo root = mService.getRootInActiveWindow();
            AccessibilityNodeInfo buttonContactNode = findButtonContactNode(root);
            if (buttonContactNode != null) {
                performActionClick(buttonContactNode);
                utils.sleep(Timeout);
            }
            //聊系人界面list
            root = mService.getRootInActiveWindow();
            AccessibilityNodeInfo fragmentContactListViewNode = findFragmentContactListViewNode(root);//通讯录界面
            if (fragmentContactListViewNode != null) {
                TimesCounter timesCounter = new TimesCounter(10);
                while (true) {
                    AccessibilityNodeInfo myGroupLayoutNode = findMyGroupLayoutNode(fragmentContactListViewNode);//搜索我创建的群
                    if (myGroupLayoutNode != null) {
                        utils.sleep(Timeout);
                        performActionClick(myGroupLayoutNode);//点击 通讯录  我的群组
                        break;
                    }
                    performActionForward(fragmentContactListViewNode);//通讯录  滑动
                    utils.sleep(Timeout);
                    if (timesCounter.check()) {
                        LogUtil.d(TAG, "查找我的群组超时");
                        break;
                    }
                }
            }
            root = mService.getRootInActiveWindow();
            AccessibilityNodeInfo myGroupTitleNode = findMyGroupTitleNode(root);//我的群组
            if (myGroupTitleNode != null && myGroupTitleNode.getChildCount() == 1) {
                AccessibilityNodeInfo myGroupNode = recursionFindNodeText(myGroupTitleNode, "我创建的");
                if (myGroupNode != null) {
                    LogUtil.d(TAG, "click my create group");
                    performActionClickParent(myGroupNode);  //递归点击，如果不能点击，点击父亲
                } else {
                    LogUtil.d(TAG, "recycle my create group null");
                    AccessibilityNodeInfo child = myGroupTitleNode.getChild(0);
                    int childCount = child.getChildCount();
                    if (childCount == 2) {//必须为两个子节点  0为我创建的，1为我加入的
                        if (isModeMyGroup()) {//我创建的模式
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
                        } else {//我加入的模式
                            AccessibilityNodeInfo child1 = child.getChild(1);//我加入的
                            AccessibilityNodeInfo tvTextNode = findTvTextNode(child1);
                            if (tvTextNode != null) {
                                String nodeText = getNodeText(tvTextNode);
                                boolean selected = tvTextNode.isSelected();
                                if (nodeText.equals("我加入的")) {
                                    if (!selected) {
                                        performActionClick(child1);//如果当前不是选中我加入的，则点击选中
                                        utils.sleep(Timeout);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            root = mService.getRootInActiveWindow();
            AccessibilityNodeInfo viewPagerNode = findViewPagerNode(root);//我的群组、我加入的 页面  会含viewPager
            if (viewPagerNode != null) {
                utils.sleep(Timeout * 3);
                AccessibilityNodeInfo myGroupListNode = findMyGroupListNode(viewPagerNode);
                if (myGroupListNode != null) {
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
                            LogUtil.e(TAG, "群组名称或人数为空？？？？");
                            continue;
                        }
                        String strGroupTitle = getNodeText(groupTitleNode);
                        String strGroupCount = getNodeText(groupCountNode);
                        String strGroupIndent = strGroupTitle + strGroupCount + idxChild;// 当前组名称 + 群成员数 + 索引 判断为是否遍历过
                        LogUtil.d(TAG, "groupIndent:" + strGroupIndent);
                        if (groupNameFilter.contains(strGroupIndent)) {
                            LogUtil.d(TAG, "群组 " + strGroupIndent + " 已遍历");
                            continue;
                        }
                        LogUtil.d(TAG, "正在删除群组：" + strGroupIndent);
                        group_find_count = 0;
                        is_file_deleted = false;
                        performActionClick(child);
                        groupNameFilter.add(strGroupIndent);
                        utils.sleep(Timeout);
                        break;
                    }
                    if (idxChild == childCount) {
                        group_find_count++;
                        if (group_find_count % 3 == 0) {
                            LogUtil.d(TAG, "群组列表滑动");
                            performActionForward(myGroupListNode);
                            utils.sleep(Timeout*3);
                        }
                        if (group_find_count >= group_find_max) {
                            is_done = true;
                            LogUtil.d(TAG, "群组遍历完成!");
                            /*
                            AlertDialog dialog = new AlertDialog.Builder(mService.getApplicationContext())
                                    .setTitle("钉钉文件删除")
                                    .setMessage("我的群组删除完成")
                                    .create();
                            dialog.show();
                            */
                            deleteServiceStop();
                        }
                    }
                }
            }
            //聊天窗口 右上角...
            root = mService.getRootInActiveWindow();
            AccessibilityNodeInfo rightMenuNode = findRightMenuNode(root);
            if (rightMenuNode != null) {
                int childCount = rightMenuNode.getChildCount();
                LogUtil.d(TAG, "child count " + childCount);
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
                AccessibilityNodeInfo functionNodes = findllFunctionNode(scrollViewNode);//ll_function布局
                if (functionNodes != null) {
                    AccessibilityNodeInfo nodeInfo1 = findllFunctionRow1Node(scrollViewNode);//ll_function_row1布局
                    if (nodeInfo1 != null) {
                        if (is_file_deleted) {//文件已删除，返回
                            utils.sleep(Timeout);
                            globalActionBack();
                        } else {
                            AccessibilityNodeInfo funcNameFileNode = findFuncNameFileNode(root);
                            if (funcNameFileNode != null) {//以文件搜索按键
                                LogUtil.d(TAG, "以 文件 搜索控件找到文件按键，并点击");
                                performActionClick(funcNameFileNode.getParent());
                            } else {//遍历子控件
                                AccessibilityNodeInfo fileNode = recursionFindNodeText(functionNodes, "文件");
                                if (fileNode != null) {
                                    LogUtil.d(TAG, "递归查找文件按键，并点击");
                                    performActionClickParent(fileNode);
                                } else {
                                    int childCount = nodeInfo1.getChildCount();
                                    for (int idxchild = 0; idxchild < childCount; idxchild++) {
                                        AccessibilityNodeInfo child = nodeInfo1.getChild(idxchild);
                                        AccessibilityNodeInfo tvFunctionNameNode = findTvFunctionNameNode(child);
                                        if (tvFunctionNameNode != null) {
                                            String funcName = getNodeText(tvFunctionNameNode);
                                            if (funcName.equals("文件")) {
                                                LogUtil.d(TAG, "查找tv_function_name找到文件点击");
                                                performActionClick(child);//点击文件
                                                break;
                                            }
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
            if (h5PcContainerNode != null) {//文件管理或回收站界面均有 h5PcContainerNode
                utils.sleep(Timeout);
                root = getRootInActiveWindowException(true);
                if (isPageRecycle()) {
                    LogUtil.d(TAG, "回收站页面 is_need_recycle:" + is_need_recycle);
                    if (is_need_recycle) {
                        utils.sleep(Timeout);
                        AccessibilityNodeInfo notFileNode = findNotFileNodeRecursion(h5PcContainerNode);
                        if (notFileNode != null && getNodeText(notFileNode).equals("暂无文件")) {
                            setNeedRecycle(false);
                        }
                        AccessibilityNodeInfo optionMenu1Node = findOptionMenu1Node(root);  //回收站右上角
                        if (is_need_recycle && optionMenu1Node != null) {
                            performActionClick(optionMenu1Node);    //点击
                            LogUtil.d(TAG, "点击右上角回收站");
                            TimesCounter timesCounterRec = new TimesCounter(10);
                            while (true) {
                                utils.sleep(Timeout);
                                root = getRootInActiveWindowException(true);
                                AccessibilityNodeInfo text1Node = findText1Node(root);
                                if (text1Node != null) {
                                    LogUtil.d(TAG, "点击清空回收站");
                                    performActionClick(text1Node);//清空回收站
                                    utils.sleep(Timeout);
                                    skip_recycle = 0;
                                    break;
                                }
                                if (timesCounterRec.check()) {
                                    LogUtil.e(TAG, "回收站超时");
                                    globalActionBack();
                                    break;
                                }
                            }
                            TimesCounter timesCounter = new TimesCounter(10);
                            while (true) {
                                utils.sleep(Timeout);
                                root = getRootInActiveWindowException(true);
                                AccessibilityNodeInfo button1Node = findButton1Node(root);//文件将彻底删除，是否清空回收站
                                if (button1Node != null) {
                                    performActionClick(button1Node);
                                    utils.sleep(Timeout);
                                    globalActionBack();
                                    setNeedRecycle(false);
                                    break;
                                }
                                if (timesCounter.check()) {
                                    globalActionBack();
                                    break;
                                }
                            }
                        }
                    } else {
                        globalActionBack();
                    }
                } else {
                    LogUtil.d(TAG, "文件管理deleteAllDone:" + deleteAllDone + " is_need_recycle:" + is_need_recycle);
                    if (!deleteAllDone) {
                        utils.sleep(Timeout*2);
                        LogUtil.d(TAG, "开始比较 已使用容量");
                        AccessibilityNodeInfo useNode = recursionFindNodeTextStartsWitch(h5PcContainerNode, "已使用容量");
                        if (useNode != null) {
                            String strUseInfo = getNodeText(useNode);
                            LogUtil.d(TAG, "文件管理：" + strUseInfo);
                            if (strUseInfo.startsWith("已使用容量: 0B")) {//找到已使用容量为0时直接返回
                                deleteAllDone = true;
                                setNeedRecycle(false);
                                is_file_deleted = true;
                                globalActionBack();
                                continue;
                            }
                        } else {
                            LogUtil.e(TAG, "比较 已使用容量 失败？？？");
                        }
                        AccessibilityNodeInfo notFileNode = findNotFileNodeRecursion(h5PcContainerNode);
                        if (notFileNode != null && getNodeText(notFileNode).equals("暂无文件")) {
                            deleteAllDone = true;
                            setNeedRecycle(true);
                        } else {//有文件需要进行删除
                            if (isModeMyGroup()) {//在我创建的模式下才会有多选按键
                                AccessibilityNodeInfo selectNode = findMutipleSelect(h5PcContainerNode);    //多选按键
                                LogUtil.d(TAG, "查找多选按键：" + selectNode);
                                if (selectNode != null) {
                                    performActionClick(selectNode);
                                    utils.sleep(Timeout);
                                    TimesCounter timesCounter = new TimesCounter(10);
                                    while (true) {
                                        utils.sleep(Timeout);
                                        root = getRootInActiveWindowException(true);
                                        AccessibilityNodeInfo text1Node = findText1Node(root);//多选
                                        if (text1Node != null) {
                                            performActionClick(text1Node);
                                            break;
                                        } else {
                                            if (timesCounter.check()) {
                                                LogUtil.d(TAG, "多选查找超时");
                                                break;
                                            }
                                        }
                                    }

                                    TimesCounter counterDelete = new TimesCounter(10);
                                    while (true) {
                                        LogUtil.d(TAG, "查找全选按键");
                                        root = getRootInActiveWindowException(true);
                                        h5PcContainerNode = findH5PcContainerNode(root);
                                        if (h5PcContainerNode != null) {
                                            AccessibilityNodeInfo selectAll = findSelectAllNode(h5PcContainerNode);//查找全选按键
                                            LogUtil.d(TAG, "全选按键:" + selectAll);
                                            if (selectAll != null) {
                                                LogUtil.d(TAG, "全选按键:" + getNodeText(selectAll));
                                                performActionClick(selectAll);  //点击全选按键
                                                utils.sleep(Timeout);
                                            }
                                            AccessibilityNodeInfo deleteNode = findDeleteNode(h5PcContainerNode);//查找删除按键
                                            if (deleteNode != null) {
                                                performActionClick(deleteNode);
                                                utils.sleep(Timeout);
                                                LogUtil.d(TAG, "点击删除");
                                                break;
                                            }
                                        }
                                        if (counterDelete.check()) {
                                            LogUtil.d(TAG, "全选删除超时");
                                            break;
                                        }
                                        utils.sleep(Timeout);
                                    }
                                    TimesCounter counter = new TimesCounter(10);
                                    while (true) {
                                        LogUtil.d(TAG, "查找确认删除");
                                        utils.sleep(Timeout);
                                        root = mService.getRootInActiveWindow();
                                        AccessibilityNodeInfo button1Node = findButton1Node(root);//确认删除
                                        if (button1Node != null) {
                                            performActionClick(button1Node);
                                            LogUtil.d(TAG, "点击确认删除");
                                            deleteAllDone = true;
                                            setNeedRecycle(true);
                                            globalActionBack();
                                            break;
                                        }
                                        if (counter.check()) {
                                            deleteAllDone = true;   //确认删除超时则认为删除完成，返回
                                            setNeedRecycle(true);
                                            globalActionBack();
                                            break;
                                        }
                                    }
                                }
                            } else if (isModeMyJoinGroup()) {//我加入的模式
                                LogUtil.d(TAG, "模式：我加入的");
                                boolean textNodeAndClickParent = findTextNodeAndClickParent(h5PcContainerNode, strUserName);//查
                            }
                        }
                    }
                    if (is_need_recycle) {//需要清除回收站
                        root = getRootInActiveWindowException(true);
                        utils.sleep(Timeout);
                        AccessibilityNodeInfo optionMenu2Node = findOptionMenu2Node(root);//右上角...
                        if (optionMenu2Node != null) {
                            performActionClick(optionMenu2Node);
                            LogUtil.d(TAG, "skip recycle " + skip_recycle);
                            skip_recycle++;
                            if (skip_recycle > 2) {
                                skip_recycle = 0;
                                LogUtil.d(TAG, "skip recycle " + skip_recycle);
                                setNeedRecycle(false);
                            }
                            utils.sleep(Timeout);
                            if (!isPageRecycle()) {    //有的群点击右上角会直接到回收站界面，并不会弹出框
                                TimesCounter counter = new TimesCounter(10, new Runnable() {
                                    @Override
                                    public void run() {
                                        globalActionBack();
                                    }
                                });
                                while (true) {
                                    utils.sleep(Timeout);
                                    root = mService.getRootInActiveWindow();
                                    AccessibilityNodeInfo text1Node = findText1Node(root);//回收站
                                    if (text1Node != null) {
                                        performActionClick(text1Node);
                                        break;
                                    }
                                    if (counter.check()) {
                                        LogUtil.e(TAG, "等待回收站弹框超时");
                                        break;
                                    }
                                }
                            }
                        } else {
                            LogUtil.e(TAG, "file manager findOptionMenu2Node is null");
                        }
                    }
                    if (deleteAllDone && !is_need_recycle) {
                        LogUtil.d(TAG, "file manager delete all done and return");
                        utils.sleep(Timeout);
                        is_file_deleted = true;
                        skip_recycle = 0;
                        globalActionBack();
                    }
                }
            }
        }
        LogUtil.d(TAG, "handlerMyGroupDelete end!!!!");
    }

    boolean isModeMyGroup() {
        return work_mode == utils.n_mode_my_group;
    }

    boolean isModeMyJoinGroup() {
        return work_mode == utils.n_mode_my_join_group;
    }

    boolean isModeMsg() {
        return work_mode == utils.n_mode_msg;
    }

    boolean findTextNodeAndClickParent(AccessibilityNodeInfo h5PcContainerNode, final String strNodeText) {//通过名称寻找节点，节点不能点击，点击父亲
        AccessibilityNodeInfo nodeInfo = recursionFindNodeCompare(h5PcContainerNode, new INodeCompare() {
            @Override
            public boolean compare(AccessibilityNodeInfo node) {
                if (!node.isClickable()) {
                    if (getNodeText(node).equals(strNodeText)) {
                        return true;
                    }
                }
                return false;
            }
        });
        if (nodeInfo != null) {
            performActionClickParent(nodeInfo);
            return true;
        }
        return false;
    }

    AccessibilityNodeInfo findNotFileNodeRecursion(AccessibilityNodeInfo h5PcContainerNode) {
        AccessibilityNodeInfo notFileNode = recursionFindNodeText(h5PcContainerNode, "暂无文件");
        if (notFileNode == null) {
            notFileNode = getChildNode(h5PcContainerNode, new int[]{1, 0, 0, 0, 0, 3, 1, 0, 1}); //寻找暂无文件
        }
        if (notFileNode == null) {
            notFileNode = getChildNode(h5PcContainerNode, new int[]{0, 0, 0, 0, 0, 3, 1, 0, 1}); //寻找暂无文件
        }
        return notFileNode;
    }
    AccessibilityNodeInfo findMutipleSelect(AccessibilityNodeInfo h5PcContainerNode) {
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
        if (selectNode == null) {
            selectNode = getChildNode(h5PcContainerNode, new int[]{1, 0, 0, 7, 0});
        }
        if (selectNode == null) {
            selectNode = getChildNode(h5PcContainerNode, new int[]{0, 0, 0, 7, 0});
        }
        return selectNode;
    }
    AccessibilityNodeInfo findSelectAllNode(AccessibilityNodeInfo h5PcContainerNode) {
        AccessibilityNodeInfo selectAll = recursionFindNodeText(h5PcContainerNode, "全选");
        if (selectAll == null) {
            selectAll = getChildNode(h5PcContainerNode, new int[]{1, 0, 0, 0, 0, 4, 0, 1});//全选
            if (selectAll == null) {
                selectAll = getChildNode(h5PcContainerNode, new int[]{0, 0, 0, 0, 0, 4, 0, 1});//全选
            }
        }
        return selectAll;
    }
    AccessibilityNodeInfo findDeleteNode(AccessibilityNodeInfo h5PcContainerNode) {
        AccessibilityNodeInfo deleteNode = recursionFindNodeText(h5PcContainerNode, "删除");
        if (deleteNode == null) {
            deleteNode = getChildNode(h5PcContainerNode, new int[]{1, 0, 0, 0, 0, 4, 0, 3});//删除
            if (deleteNode == null) {
                deleteNode = getChildNode(h5PcContainerNode, new int[]{0, 0, 0, 0, 0, 4, 0, 3});
            }
        }
        return deleteNode;
    }

    private void deleteServiceStop() {
        mService.stopSelf();    //停止监听事件服务
        stopSelf();             //停止本服务
        Intent intent = new Intent(this, HandlerService.class);
        stopService(intent);
    }

    AccessibilityNodeInfo getRootInActiveWindowException(boolean needGoBack) throws RuntimeException {
        AccessibilityNodeInfo root = mService.getRootInActiveWindow();
        if (root == null) {
            if (needGoBack) {
                globalActionBack();
            }
            throw new RuntimeException("root is null:" + utils.getStackTrace());
        }
        return root;
    }

    AccessibilityNodeInfo getRootInActiveWindowException() throws RuntimeException {
        return getRootInActiveWindowException(false);
    }

    boolean isPageRecycle() {
        AccessibilityNodeInfo root = mService.getRootInActiveWindow();
        AccessibilityNodeInfo h5PcContainerNode = findH5PcContainerNode(root);
        if (h5PcContainerNode != null) {
            AccessibilityNodeInfo titleNode = recursionFindNodeText(root, "回收站");
            if (titleNode == null) {
                LogUtil.d(TAG, "find 回收站 null");
                titleNode = findTitleNode(root);
                LogUtil.d(TAG, "findTitleNode is null");
            }
            if (titleNode != null) {
                LogUtil.d(TAG, "TitleNode not null " + getNodeText(titleNode));
                LogUtil.d(TAG, "root" + root);
            }
            if (titleNode != null && getNodeText(titleNode).startsWith("回收站")) {
                return true;
            }
        }
        return false;
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

    public AccessibilityNodeInfo recursionFindNodeTextStartsWitch(AccessibilityNodeInfo info, String strNodeText) {
        if (info.getChildCount() == 0) {
            LogUtil.d(TAG, "NodeTextStartsWitch ClassName:" + info.getClassName() + " Text：" + info.getText());
            if (getNodeText(info).startsWith(strNodeText)) {
                return info;
            }
            return null;
        } else {
            for (int i = 0; i < info.getChildCount(); i++) {
                if(info.getChild(i)!=null){
                    AccessibilityNodeInfo tinfo = recursionFindNodeTextStartsWitch(info.getChild(i), strNodeText);
                    if (tinfo != null) {
                        return tinfo;
                    }
                }
            }
        }
        return null;
    }

    public AccessibilityNodeInfo recursionFindNodeCompare(AccessibilityNodeInfo info, INodeCompare compare) {
        if (info.getChildCount() == 0) {
            LogUtil.d(TAG, "ClassName:" + info.getClassName() + " Text：" + info.getText());
            if (compare.compare(info)) {
                return info;
            }
            return null;
        } else {
            for (int i = 0; i < info.getChildCount(); i++) {
                if(info.getChild(i)!=null){
                    AccessibilityNodeInfo tinfo = recursionFindNodeCompare(info.getChild(i), compare);
                    if (tinfo != null) {
                        return tinfo;
                    }
                }
            }
        }
        return null;
    }

    public AccessibilityNodeInfo recursionFindNodeText(AccessibilityNodeInfo info, String strNodeText) {
        if (info.getChildCount() == 0) {
            //LogUtil.i(TAG, "child widget----------------------------" + info.getClassName());
            //LogUtil.i(TAG, "showDialog:" + info.canOpenPopup());
            LogUtil.d(TAG, "ClassName:" + info.getClassName() + " Text：" + info.getText());
            //LogUtil.i(TAG, "windowId:" + info.getWindowId());
            if (getNodeText(info).equals(strNodeText)) {
                return info;
            }
            return null;
        } else {
            for (int i = 0; i < info.getChildCount(); i++) {
                if(info.getChild(i)!=null){
                    AccessibilityNodeInfo tinfo = recursionFindNodeText(info.getChild(i), strNodeText);
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
        LogUtil.d(TAG, "handlerDelete");
        while (!is_done) {
            getUserName();
            if (strUserName == null) {
                is_done = true;
                continue;
            }
            LogUtil.d(TAG, "userName:" + strUserName);
            AccessibilityNodeInfo root = mService.getRootInActiveWindow();
            AccessibilityNodeInfo sessionListNode = findSessionListNode(root);
            if (sessionListNode != null) {
                handlerMsgView(sessionListNode);
                continue;
            }

            utils.sleep(100);
        }
        LogUtil.d(TAG, "handlerDelete done");
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
            LogUtil.e(TAG, "getUserName root is null");
            return;
        }

        AccessibilityNodeInfo avatarNode = findAvatarNode(root);
        if (avatarNode != null) {
            performActionClick(avatarNode);
            TimesCounter timesCounter = new TimesCounter(50);
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
                if (timesCounter.check()) {
                    break;
                }
            }
            globalActionBack();
        } else {
            LogUtil.d(TAG, "find avatar node error!");
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
        try {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        } catch (Throwable t) {
            LogUtil.e(TAG, "performActionClick except:" + utils.getStackTrace());
        }
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
        String s = text.toString();
        return new String(s.getBytes(), StandardCharsets.UTF_8);
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
            //LogUtil.d(TAG, "node is null:" + utils.getStackTrace());
            return null;
        }
        try {
            return node.findAccessibilityNodeInfosByViewId(id);
        } catch (Throwable t) {
            LogUtil.e(TAG, "findAccessibilityNodeInfosByViewIds:" + t.getMessage() + " " + utils.getStackTrace());
            return null;
        }
    }

    AccessibilityNodeInfo findAccessibilityNodeInfosByViewId(AccessibilityNodeInfo node, String id) {
        return findAccessibilityNodeInfosByViewId(node, id, 0);
    }

    AccessibilityNodeInfo findAccessibilityNodeInfosByViewId(AccessibilityNodeInfo node, String id, int index) {
        if (node == null) {
            //LogUtil.d(TAG, "node is null:" + utils.getStackTrace());
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
            //LogUtil.d(TAG, "node is null:" + utils.getStackTrace());
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
