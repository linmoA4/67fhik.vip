package com.linmo.injector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.widget.Toast;

/**
 * 音量键监听器
 * 监听音量+和音量-按键来控制悬浮窗的显示/隐藏
 */
public class VolumeKeyReceiver extends BroadcastReceiver {

    private static FloatingWindowService floatingWindowService = null;
    private static long lastVolumeChangeTime = 0;
    private static final long DEBOUNCE_TIME = 300; // 防抖时间（毫秒）

    /**
     * 设置悬浮窗服务引用
     */
    public static void setFloatingWindowService(FloatingWindowService service) {
        floatingWindowService = service;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) return;

        if (intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION")) {
            // 获取当前时间用于防抖
            long currentTime = System.currentTimeMillis();
            
            // 防抖处理，避免连续触发
            if (currentTime - lastVolumeChangeTime < DEBOUNCE_TIME) {
                return;
            }
            lastVolumeChangeTime = currentTime;

            // 获取音量类型
            int volumeType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
            
            // 只监听音乐/媒体音量（避免其他音量干扰）
            if (volumeType == AudioManager.STREAM_MUSIC) {
                
                // 判断是音量增加还是减少
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                
                // 通过额外数据判断方向
                // 注意：不同设备可能有差异，这里使用通用方法
                
                String extra = intent.getStringExtra("android.media.EXTRA_VOLUME_STREAM_VALUE");
                String prevExtra = intent.getStringExtra("android.media.EXTRA_PREVIOUS_VOLUME_STREAM_VALUE");
                
                if (extra != null && prevExtra != null) {
                    try {
                        int newVolume = Integer.parseInt(extra);
                        int prevVolume = Integer.parseInt(prevExtra);
                        
                        if (newVolume > prevVolume) {
                            // 音量增加 -> 显示悬浮窗
                            onVolumeUp(context);
                        } else if (newVolume < prevVolume) {
                            // 音量减少 -> 隐藏悬浮窗
                            onVolumeDown(context);
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 处理音量+ 事件：显示悬浮窗
     */
    private void onVolumeUp(Context context) {
        if (floatingWindowService != null) {
            floatingWindowService.showHiddenFloatingWindow();
            showToast(context, "悬浮窗已打开");
        }
    }

    /**
     * 处理音量- 事件：隐藏悬浮窗
     */
    private void onVolumeDown(Context context) {
        if (floatingWindowService != null) {
            floatingWindowService.hideFloatingWindow();
            showToast(context, "悬浮窗已关闭");
        }
    }

    /**
     * 显示Toast提示
     */
    private void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
