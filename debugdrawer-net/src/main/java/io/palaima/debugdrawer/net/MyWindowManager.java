package io.palaima.debugdrawer.net;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;

import java.text.DecimalFormat;

public class MyWindowManager {

    private static final double TIME_SPAN = 1000d;
    private static final int OVERLAY_PERMISSION_REQ_CODE = 11;

    private static MyWindowManager instance;
    private Application application;
    private WindowManager mWindowManager;
    private WindowView mBigWindowView;
    private LayoutParams windowParams;
    private TextView tvMobileTx;
    private TextView tvMobileRx;
    private TextView tvWlanTx;
    private TextView tvWlanRx;
    private long mobileRecvSum = 0;
    private long mobileSendSum = 0;
    private long wlanRecvSum = 0;
    private long wlanSendSum = 0;
    private DecimalFormat showFloatFormat = new DecimalFormat("0.00");

    public static MyWindowManager getInstance() {
        if (instance == null) {
            instance = new MyWindowManager();
        }
        return instance;
    }

    public void createWindow(final Application application) {
        this.application = application;
        if (!hasOverlayPermission()) {
            startOverlaySettingActivity();
            return;
        }
        final WindowManager windowManager = getWindowManager(application);
        if (windowParams == null) {
            windowParams = getWindowParams(application);
        }
        if (mBigWindowView == null) {
            mBigWindowView = new BigWindowView(application);
            windowManager.addView(mBigWindowView, windowParams);
        }
        tvMobileRx = (TextView) mBigWindowView.findViewById(R.id.tvMobileRx);
        tvMobileTx = (TextView) mBigWindowView.findViewById(R.id.tvMobileTx);
        tvWlanRx = (TextView) mBigWindowView.findViewById(R.id.tvWlanRx);
        tvWlanTx = (TextView) mBigWindowView.findViewById(R.id.tvWlanTx);
    }

    public void initData() {
        mobileRecvSum = TrafficStats.getMobileRxBytes();
        mobileSendSum = TrafficStats.getMobileTxBytes();
        wlanRecvSum = TrafficStats.getTotalRxBytes() - mobileRecvSum;
        wlanSendSum = TrafficStats.getTotalTxBytes() - mobileSendSum;
    }

    private LayoutParams getWindowParams(Context context) {
        final WindowManager windowManager = getWindowManager(context);
        Point sizePoint = new Point();
        windowManager.getDefaultDisplay().getSize(sizePoint);
        int screenWidth = sizePoint.x;
        int screenHeight = sizePoint.y;
        LayoutParams windowParams = new LayoutParams();
        if (isOverlayApiDeprecated()) {
            windowParams.type = LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            windowParams.type = LayoutParams.TYPE_TOAST;
        }
        windowParams.format = PixelFormat.RGBA_8888;
        windowParams.flags = LayoutParams.FLAG_LAYOUT_IN_SCREEN | LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_NOT_TOUCH_MODAL;
        windowParams.gravity = Gravity.START | Gravity.TOP;
        windowParams.width = LayoutParams.WRAP_CONTENT;
        windowParams.height = LayoutParams.WRAP_CONTENT;
        windowParams.x = 0;
        windowParams.y = screenHeight;
        return windowParams;
    }

    private boolean isOverlayApiDeprecated() {
        return Build.VERSION.SDK_INT >= 26 /* FIXME: Android8.0 */;
    }

    private void removeWindow(Context context, WindowView windowView) {
        if (windowView != null) {
            WindowManager windowManager = getWindowManager(context);
            windowManager.removeView(windowView);
        }
    }

    public void removeAllWindow(Context context) {
        removeWindow(context, mBigWindowView);
        mBigWindowView = null;
    }

    public void updateViewData() {
        long tempMobileRx = TrafficStats.getMobileRxBytes();
        long tempMobileTx = TrafficStats.getMobileTxBytes();
        long tempWlanRx = TrafficStats.getTotalRxBytes() - tempMobileRx;
        long tempWlanTx = TrafficStats.getTotalTxBytes() - tempMobileTx;
        long mobileLastRecv = tempMobileRx - mobileRecvSum;
        long mobileLastSend = tempMobileTx - mobileSendSum;
        long wlanLastRecv = tempWlanRx - wlanRecvSum;
        long wlanLastSend = tempWlanTx - wlanSendSum;
        double mobileRecvSpeed = mobileLastRecv * 1000 / TIME_SPAN;
        double mobileSendSpeed = mobileLastSend * 1000 / TIME_SPAN;
        double wlanRecvSpeed = wlanLastRecv * 1000 / TIME_SPAN;
        double wlanSendSpeed = wlanLastSend * 1000 / TIME_SPAN;
        mobileRecvSum = tempMobileRx;
        mobileSendSum = tempMobileTx;
        wlanRecvSum = tempWlanRx;
        wlanSendSum = tempWlanTx;
        if (mBigWindowView != null) {
            if (mobileRecvSpeed >= 0d) {
                tvMobileRx.setText(showSpeed(mobileRecvSpeed));
            }
            if (mobileSendSpeed >= 0d) {
                tvMobileTx.setText(showSpeed(mobileSendSpeed));
            }
            if (wlanRecvSpeed >= 0d) {
                tvWlanRx.setText(showSpeed(wlanRecvSpeed));
            }
            if (wlanSendSpeed >= 0d) {
                tvWlanTx.setText(showSpeed(wlanSendSpeed));
            }
        }
    }

    private String showSpeed(double speed) {
        String speedString;
        if (speed >= 1048576d) {
            speedString = showFloatFormat.format(speed / 1048576d) + "MB/s";
        } else {
            speedString = showFloatFormat.format(speed / 1024d) + "KB/s";
        }
        return speedString;
    }

    public boolean isWindowShowing() {
        return mBigWindowView != null;
    }

    private WindowManager getWindowManager(Context context) {
        if (mWindowManager == null) {
            mWindowManager = (WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE);
        }
        return mWindowManager;
    }

    private boolean hasOverlayPermission() {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(application);
    }

    private void startOverlaySettingActivity() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            application.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + application.getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

}
