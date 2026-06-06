package com.linmo.injector;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView statusText;
    private Button startServiceBtn;
    private Button requestPermissionBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        startServiceBtn = findViewById(R.id.startServiceBtn);
        requestPermissionBtn = findViewById(R.id.requestPermissionBtn);

        // 检查悬浮窗权限
        checkOverlayPermission();

        // 启动服务按钮
        startServiceBtn.setOnClickListener(v -> {
            if (Settings.canDrawOverlays(this)) {
                startFloatingWindowService();
            } else {
                Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show();
                requestOverlayPermission();
            }
        });

        // 请求权限按钮
        requestPermissionBtn.setOnClickListener(v -> {
            requestAllPermissions();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkOverlayPermission();
    }

    /**
     * 检查悬浮窗权限状态
     */
    private void checkOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            statusText.setText("✓ 悬浮窗权限已授权");
            statusText.setTextColor(0xFF4CAF50);
            startServiceBtn.setEnabled(true);
        } else {
            statusText.setText("✗ 悬浮窗权限未授权");
            statusText.setTextColor(0xFFF44336);
            startServiceBtn.setEnabled(false);
        }
    }

    /**
     * 请求悬浮窗权限
     */
    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, 1001);
    }

    /**
     * 请求所有必要权限（包括存储权限）
     */
    private void requestAllPermissions() {
        // Android 11+ 需要请求所有文件访问权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                Toast.makeText(this, "请授予存储管理权限", Toast.LENGTH_LONG).show();
            }
        }
        
        // 请求悬浮窗权限
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission();
        }
    }

    /**
     * 启动悬浮窗服务
     */
    private void startFloatingWindowService() {
        Intent serviceIntent = new Intent(this, FloatingWindowService.class);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        Toast.makeText(this, "服务已启动\n音量+ 打开悬浮窗\n音量- 关闭悬浮窗", Toast.LENGTH_LONG).show();
        finish(); // 启动后关闭主界面
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            checkOverlayPermission();
        }
    }
}
