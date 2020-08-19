package io.palaima.debugdrawer.net;

import android.app.Application;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.util.Timer;
import java.util.TimerTask;

import io.palaima.debugdrawer.base.DebugModuleAdapter;

public class NetModule extends DebugModuleAdapter {

    private Application application;
    private Handler handler = new Handler();
    private Timer timer;
    private boolean isChecked;

    public NetModule(Application application) {
        this.application = application;
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        final View view = inflater.inflate(R.layout.dd_debug_drawer_item_net, parent, false);
        final Switch showSwitch = (Switch) view.findViewById(R.id.dd_debug_fps);
        showSwitch.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                        NetModule.this.isChecked = isChecked;
                        if (isChecked) {
                            startTimer();
                        } else {
                            stopTimer();
                        }
                    }
                });
        return view;
    }

    @Override
    public void onResume() {
        if (isChecked) {
            startTimer();
        }
    }

    @Override
    public void onPause() {
        if (isChecked) {
            stopTimer();
        }
    }

    private void stopTimer() {
        timer.cancel();
        timer = null;
        MyWindowManager.getInstance().removeAllWindow(application);
    }

    private void startTimer() {
        if (timer == null) {
            timer = new Timer();
            timer.scheduleAtFixedRate(new RefreshTask(), 0L, 1000);
        }
    }

    class RefreshTask extends TimerTask {

        @Override
        public void run() {
            // 当前没有悬浮窗显示，则创建悬浮窗。
            if (!MyWindowManager.getInstance().isWindowShowing()) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        MyWindowManager.getInstance().initData();
                        MyWindowManager.getInstance().createWindow(application);
                    }
                });
            }
            // 当前有悬浮窗显示，则更新内存数据。
            else {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        MyWindowManager.getInstance().updateViewData();
                    }
                });
            }
        }

    }

}
