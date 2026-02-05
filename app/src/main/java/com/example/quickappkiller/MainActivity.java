package com.example.quickappkiller;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private List<String> displayList = new ArrayList<>();
    private List<String> targetPackageList = new ArrayList<>();

    // 黑名单库
    private static final Map<String, String> BLACKLIST = new HashMap<>();
    static {
        BLACKLIST.put("com.huawei.fastapp", "华为快应用中心");
        BLACKLIST.put("com.miui.hybrid", "小米快应用服务框架");
        BLACKLIST.put("com.mi.quickapp", "小米快应用(旧版)");
        BLACKLIST.put("com.oppo.hybrid", "OPPO快应用");
        BLACKLIST.put("com.heytap.hybrid", "OPPO/一加快应用引擎");
        BLACKLIST.put("com.vivo.hybrid", "vivo快应用");
        BLACKLIST.put("com.meizu.flyme.direct", "魅族快应用");
        BLACKLIST.put("com.gionee.ami", "金立快应用");
        BLACKLIST.put("com.lenovo.hybrid", "联想快应用");
        BLACKLIST.put("com.nubia.hybrid", "努比亚快应用");
        //BLACKLIST.put("com.android.settings", "系统设置(仅供测试)");
    }

    private TextView tvStatus;
    private ListView lvEngines;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvStatus = findViewById(R.id.tv_status);
        lvEngines = findViewById(R.id.lv_list_new);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        lvEngines.setAdapter(adapter);

        // 点击事件：点谁杀谁
        lvEngines.setOnItemClickListener((parent, view, position, id) -> {
            String packageName = targetPackageList.get(position);
            showActionDialog(packageName);
        });

        // 启动扫描
        scanEngines();
    }

    private void scanEngines() {
        displayList.clear();
        targetPackageList.clear();

        PackageManager pm = getPackageManager();
        // 1. 获取手机里所有的软件
        List<PackageInfo> installedPackages = pm.getInstalledPackages(0);
        // 获取自己的包名
        String myPackageName = getPackageName();
        int count = 0;
        for (PackageInfo pkg : installedPackages) {
            String pkgName = pkg.packageName;
            // 如果是自己，直接跳过！
            if (pkgName.equals(myPackageName)) {
                continue;
            }
            // 获取应用的名称（比如 "华为快应用中心"）
            String appName = pkg.applicationInfo.loadLabel(pm).toString();

            // 转为小写方便匹配
            String lowerPkgName = pkgName.toLowerCase();
            String lowerAppName = appName.toLowerCase();

            // === 核心修改：判断逻辑 ===
            boolean isTarget = false;
            String matchReason = "";

            // 1. 命中已知黑名单
            if (BLACKLIST.containsKey(pkgName)) {
                isTarget = true;
                matchReason = "【已知引擎】" + BLACKLIST.get(pkgName);
            }
            // 2. 名字里包含 "快应用" (比如 "荣耀快应用")
            else if (appName.contains("快应用")) {
                isTarget = true;
                matchReason = "【疑似引擎】名称包含“快应用”";
            }
            // 3. 包名里包含关键词 (比如 com.new.fastapp)
            // 常见的快应用关键词：fastapp, quickapp, hybrid
            else if (lowerPkgName.contains("fastapp") ||
                    lowerPkgName.contains("quickapp") ||
                    lowerPkgName.contains("hybrid")) {
                // 排除一些误杀，比如 Settings 有时候会包含 hybrid 相关的组件，可以根据需要微调
                isTarget = true;
                matchReason = "【疑似引擎】包名包含关键字";
            }

            // 如果是目标，就加入显示列表
            if (isTarget) {
                displayList.add(matchReason + "\n应用名：" + appName + "\n包名：" + pkgName);
                targetPackageList.add(pkgName);
                count++;
            }
        }

        // 更新界面提示
        if (count > 0) {
            tvStatus.setText("扫描完成，共找出 " + count + " 个快应用相关软件");
            tvStatus.setTextColor(0xFFFF0000); // 红色
        } else {
            tvStatus.setText("非常干净，未发现任何快应用相关软件");
            tvStatus.setTextColor(0xFF00AA00); // 绿色
        }
        adapter.notifyDataSetChanged();
    }

    private void showActionDialog(String packageName) {
        String msg = "即将跳转到系统设置页。\n\n 请点击页面上的【停用】或【卸载】按钮来干掉它！";

        new AlertDialog.Builder(this)
                .setTitle("准备处理")
                .setMessage(msg)
                .setPositiveButton("立即跳转", (dialog, which) -> jumpToSettings(packageName))
                .setNegativeButton("取消", null)
                .show();
    }

    private void jumpToSettings(String packageName) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "跳转失败，请手动去设置搜索", Toast.LENGTH_SHORT).show();
        }
    }
    // 每次返回主界面时都重新扫描，确保状态最新
    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到页面时重新扫描，确保状态最新
        scanEngines();
    }


}