package com.linmo.injector;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FloatingWindowService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private View islandView; // 灵动岛视图
    private LinearLayout expandedPanel; // 展开面板
    private boolean isExpanded = false;
    private boolean isVisible = false;
    
    // 灵动岛参数
    private int islandWidth = 120;
    private int islandHeight = 40;
    private int expandedWidth = 280;
    private int expandedHeight = 200;
    
    // 拖动参数
    private float initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isDragging = false;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // 创建通知渠道（前台服务必需）
        createNotificationChannel();
        
        // 启动前台服务
        startForeground(1, createNotification());
        
        // 设置音量监听器引用
        VolumeKeyReceiver.setFloatingWindowService(this);
        
        // 初始化悬浮窗
        initFloatingWindow();
    }

    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "floating_window_service",
                    "悬浮窗服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("保持悬浮窗服务运行");
            channel.setShowBadge(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * 创建前台服务通知
     */
    private Notification createNotification() {
        Notification.Builder builder = new Notification.Builder(this, "floating_window_service")
                .setContentTitle("林默注入器")
                .setContentText("悬浮窗服务运行中 - 音量+/- 控制")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true);
        
        return builder.build();
    }

    /**
     * 初始化悬浮窗
     */
    private void initFloatingWindow() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        floatingView = inflater.inflate(R.layout.floating_window_layout, null);
        
        // 获取视图引用
        islandView = floatingView.findViewById(R.id.islandContainer);
        expandedPanel = floatingView.findViewById(R.id.expandedPanel);
        Button btnImport = floatingView.findViewById(R.id.btnImport);
        Button btnImportOriginal = floatingView.findViewById(R.id.btnImportOriginal);
        Button btnClose = floatingView.findViewById(R.id.btnClose);
        TextView statusLabel = floatingView.findViewById(R.id.statusLabel);

        // 设置初始状态为灵动岛模式（收起）
        updateIslandMode();

        // 点击灵动岛 - 展开/收起
        islandView.setOnClickListener(v -> toggleExpand());

        // 拖动功能
        setupDraggable(islandView);

        // 导入功能按钮 - 注入pak文件
        btnImport.setOnClickListener(v -> {
            new Thread(() -> {
                boolean success = injectPakFiles(false);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> {
                    if (success) {
                        Toast.makeText(FloatingWindowService.this, 
                                "✓ 注入成功！已导入修改版PAK", Toast.LENGTH_LONG).show();
                        statusLabel.setText("✓ 已注入修改版");
                        statusLabel.setTextColor(0xFF4CAF50);
                    } else {
                        Toast.makeText(FloatingWindowService.this, 
                                "✗ 注入失败，请检查文件", Toast.LENGTH_LONG).show();
                        statusLabel.setText("✗ 注入失败");
                        statusLabel.setTextColor(0xFFF44336);
                    }
                    
                    // 2秒后自动收起
                    new Handler().postDelayed(() -> {
                        if (isExpanded) toggleExpand();
                    }, 2000);
                });
            }).start();
        });

        // 导入原版按钮 - 恢复原始pak文件
        btnImportOriginal.setOnClickListener(v -> {
            new Thread(() -> {
                boolean success = restoreOriginalPaks();
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> {
                    if (success) {
                        Toast.makeText(FloatingWindowService.this, 
                                "✓ 已恢复原版PAK！", Toast.LENGTH_LONG).show();
                        statusLabel.setText("✓ 已恢复原版");
                        statusLabel.setTextColor(0xFF2196F3);
                    } else {
                        Toast.makeText(FloatingWindowService.this, 
                                "✗ 恢复失败，请检查备份", Toast.LENGTH_LONG).show();
                        statusLabel.setText("✗ 恢复失败");
                        statusLabel.setTextColor(0xFFF44336);
                    }
                    
                    // 2秒后自动收起
                    new Handler().postDelayed(() -> {
                        if (isExpanded) toggleExpand();
                    }, 2000);
                });
            }).start();
        });

        // 关闭按钮
        btnClose.setOnClickListener(v -> toggleExpand());

        // 设置布局参数
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.TYPE_APPLICATION_OVERLAY :
                        WindowManager.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 200;

        try {
            windowManager.addView(floatingView, params);
            isVisible = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置拖动功能
     */
    private void setupDraggable(View view) {
        view.setOnTouchListener((v, event) => {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = ((WindowManager.LayoutParams) floatingView.getLayoutParams()).x;
                    initialY = ((WindowManager.LayoutParams) floatingView.getLayoutParams()).y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    isDragging = false;
                    return true;
                    
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - initialTouchX;
                    float dy = event.getRawY() - initialTouchY;
                    
                    // 判断是否在拖动（移动超过10像素才算拖动）
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true;
                        
                        WindowManager.LayoutParams params = (WindowManager.LayoutParams) floatingView.getLayoutParams();
                        params.x = (int) (initialX + dx);
                        params.y = (int) (initialY + dy);
                        windowManager.updateViewLayout(floatingView, params);
                    }
                    return true;
                    
                case MotionEvent.UP:
                    if (!isDragging) {
                        // 如果没有拖动，则视为点击事件
                        v.performClick();
                    }
                    return true;
            }
            return false;
        });
    }

    /**
     * 切换展开/收起状态（带丝滑动画）
     */
    private void toggleExpand() {
        isExpanded = !isExpanded;
        
        if (isExpanded) {
            animateExpand();
        } else {
            animateCollapse();
        }
    }

    /**
     * 展开动画 - 灵动岛展开效果
     */
    private void animateExpand() {
        islandView.setVisibility(View.GONE);
        expandedPanel.setVisibility(View.VISIBLE);
        
        // 使用属性动画实现丝滑效果
        expandedPanel.setScaleX(0.5f);
        expandedPanel.setScaleY(0.5f);
        expandedPanel.setAlpha(0f);
        
        expandedPanel.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                .start();
        
        // 更新窗口大小
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) floatingView.getLayoutParams();
        params.width = expandedWidth;
        params.height = expandedHeight;
        windowManager.updateViewLayout(floatingView, params);
    }

    /**
     * 收起动画 - 灵动岛收缩效果
     */
    private void animateCollapse() {
        expandedPanel.animate()
                .scaleX(0.5f)
                .scaleY(0.5f)
                .alpha(0f)
                .setDuration(250)
                .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    expandedPanel.setVisibility(View.GONE);
                    expandedPanel.setScaleX(1f);
                    expandedPanel.setScaleY(1f);
                    expandedPanel.setAlpha(1f);
                    islandView.setVisibility(View.VISIBLE);
                    
                    // 更新窗口大小
                    WindowManager.LayoutParams params = (WindowManager.LayoutParams) floatingView.getLayoutParams();
                    params.width = islandWidth;
                    params.height = islandHeight;
                    windowManager.updateViewLayout(floatingView, params);
                })
                .start();
    }

    /**
     * 更新为灵动岛模式
     */
    private void updateIslandMode() {
        islandView.setVisibility(View.VISIBLE);
        expandedPanel.setVisibility(View.GONE);
        
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) floatingView.getLayoutParams();
        params.width = islandWidth;
        params.height = islandHeight;
        windowManager.updateViewLayout(floatingView, params);
    }

    /**
     * 显示悬浮窗
     */
    public static void showFloatingWindow(Context context) {
        Intent intent = new Intent(context, FloatingWindowService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    /**
     * 隐藏悬浮窗
     */
    public void hideFloatingWindow() {
        if (isVisible && floatingView != null && windowManager != null) {
            try {
                windowManager.removeView(floatingView);
                isVisible = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 显示隐藏的悬浮窗
     */
    public void showHiddenFloatingWindow() {
        if (!isVisible) {
            initFloatingWindow();
        }
    }

    /**
     * 注入PAK文件（从林默目录到游戏目录）
     * @param isOriginal 是否是原版
     * @return 是否成功
     */
    private boolean injectPakFiles(boolean isOriginal) {
        String sourceDir;
        if (isOriginal) {
            sourceDir = "/storage/emulated/0/林默自动注入/原版/";
        } else {
            sourceDir = "/storage/emulated/0/林默自动注入/";
        }
        
        String targetDir = "/storage/emulated/0/Android/data/com.tencent.tmgp.pubgmhd/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Paks/";
        
        File sourceFolder = new File(sourceDir);
        File targetFolder = new File(targetDir);
        
        // 检查源目录是否存在
        if (!sourceFolder.exists()) {
            return false;
        }
        
        // 创建目标目录（如果不存在）
        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }
        
        // 获取所有.pak文件
        File[] pakFiles = sourceFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pak"));
        
        if (pakFiles == null || pakFiles.length == 0) {
            return false;
        }
        
        int successCount = 0;
        
        for (File pakFile : pakFiles) {
            // 跳过原版目录
            if (isOriginal && !pakFile.getParent().contains("原版")) {
                continue;
            }
            if (!isOriginal && pakFile.getParent().contains("原版")) {
                continue;
            }
            
            try {
                copyFile(pakFile, new File(targetFolder, pakFile.getName()));
                successCount++;
                
                // 小延迟避免卡顿
                Thread.sleep(50);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        return successCount > 0;
    }

    /**
     * 恢复原版PAK文件
     */
    private boolean restoreOriginalPaks() {
        return injectPakFiles(true);
    }

    /**
     * 复制文件
     */
    private void copyFile(File source, File dest) throws IOException {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        
        try {
            fis = new FileInputStream(source);
            fos = new FileOutputStream(dest);
            
            byte[] buffer = new byte[8192];
            int length;
            
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            
            fos.flush();
        } finally {
            if (fis != null) {
                fis.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideFloatingWindow();
    }
}
