package run.wendster.wearinstaller;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.widget.Toast;

import run.wendster.wearinstaller.apk.APKCommander;
import run.wendster.wearinstaller.apk.ApkInfo;
import run.wendster.wearinstaller.apk.ICommanderCallback;

public class BackgroundInstallActivity extends WearableActivity implements ICommanderCallback {

    private APKCommander apkCommander;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getData() != null) {
            apkCommander = new APKCommander(this, getIntent().getData(), this);
        } else {
            showToast(getString(R.string.unable_to_install_apk));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        finish();
    }

    private void showToast(String text) {
        Toast.makeText(run.wendster.wearinstaller.BackgroundInstallActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStartParseApk(Uri uri) {
        showToast(getString(R.string.parsing));
    }

    @Override
    public void onApkParsed(ApkInfo apkInfo) {
        apkCommander.startInstall();
    }

    @Override
    public void onApkPreInstall(ApkInfo apkInfo) {
        showToast(getString(R.string.start_install, apkInfo.getApkFile().getPath()));
    }

    @Override
    public void onApkInstalled(ApkInfo apkInfo, int resultCode) {
        if (resultCode == 0) {
            showToast(getString(R.string.apk_installed, apkInfo.getAppName()));
        } else {
            showToast(getString(R.string.install_failed, apkInfo.getAppName()));
        }
        finish();
    }

    @Override
    public void onInstallLog(ApkInfo apkInfo, String logText) {

    }

}