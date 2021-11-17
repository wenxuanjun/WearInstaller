package run.wendster.wearinstaller;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import run.wendster.wearinstaller.apk.APKCommander;
import run.wendster.wearinstaller.apk.ApkInfo;
import run.wendster.wearinstaller.apk.ICommanderCallback;

public class MainActivity extends WearableActivity implements ICommanderCallback, View.OnClickListener {

    private TextView tvAppName;
    private LinearLayout layoutAppDetails;
    private ImageView imgAppIcon;
    private LinearLayout layoutTitleContainer;
    private LinearLayout layoutPermissionList;
    private LinearLayout layoutButtons;
    private TextView btnInstall;
    private TextView btnSilently;
    private TextView btnCancel;
    private APKCommander apkCommander;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        layoutAppDetails = findViewById(R.id.layout_app_details);
        tvAppName = findViewById(R.id.tv_app_name);
        layoutTitleContainer = findViewById(R.id.titleBar);
        imgAppIcon = findViewById(R.id.img_app_icon);

        btnInstall = findViewById(R.id.btn_install);
        btnSilently = findViewById(R.id.btn_silently);
        btnCancel = findViewById(R.id.btn_cancel);
        layoutButtons = (LinearLayout) btnInstall.getParent();
        btnInstall.setEnabled(true);
        btnInstall.setOnClickListener(this);
        btnSilently.setOnClickListener(this);
        btnCancel.setOnClickListener(this);

        loadSettings();
        if (getIntent().getData() == null) {
            finish();
        } else
            checkPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    apkCommander = new APKCommander(this, getIntent().getData(), this);
                } else {
                    Toast.makeText(MainActivity.this, R.string.no_permissions, Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            apkCommander = new APKCommander(this, getIntent().getData(), this);
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }

    private void loadSettings() {
        LinearLayout.LayoutParams marginParams = (LinearLayout.LayoutParams) btnInstall.getLayoutParams();
        btnInstall.setLayoutParams(marginParams);
        btnSilently.setLayoutParams(marginParams);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        if (apkCommander != null && apkCommander.getApkInfo() != null && apkCommander.getApkInfo().getApkFile() != null) {
            initDetails(apkCommander.getApkInfo());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        loadSettings();
    }

    private void initDetails(ApkInfo apkInfo) {
        layoutAppDetails.removeAllViews();
        tvAppName.setText(apkInfo.getAppName());
        imgAppIcon.setImageDrawable(apkInfo.getIcon());
        layoutAppDetails.addView(createAppInfoView(getString(R.string.info_pkg_name), apkInfo.getPackageName()));
        layoutAppDetails.addView(createAppInfoView(getString(R.string.info_apk_path), apkInfo.getApkFile().getPath()));
        layoutAppDetails.addView(createAppInfoView(getString(R.string.info_version), apkInfo.getVersion()));
        if (apkInfo.hasInstalledApp())
            layoutAppDetails.addView(createAppInfoView(getString(R.string.info_installed_version), apkInfo.getInstalledVersion()));
        if (apkInfo.getPermissions() != null && apkInfo.getPermissions().length > 0) {
            layoutPermissionList = new LinearLayout(this);
            layoutPermissionList.setOrientation(LinearLayout.VERTICAL);
            layoutPermissionList.addView(createAppInfoView(null, getString(R.string.app_permissions)));
            for (String perm : apkInfo.getPermissions()) {
                layoutPermissionList.addView(createAppPermissionView(perm));
            }
            layoutAppDetails.addView(layoutPermissionList);
        }
    }

    private LinearLayout createAppPermissionView(String perm) {
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.info_item_permission, null, false);
        TextView tv1 = (TextView) layout.getChildAt(0);
        tv1.setText(perm);
        return layout;
    }


    private LinearLayout createAppInfoView(String key, String value) {
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.info_item, null, false);
        TextView tv1 = (TextView) layout.getChildAt(0);
        TextView tv2 = (TextView) layout.getChildAt(1);
        tv1.setText(key);
        tv2.setText(value);
        if (TextUtils.isEmpty(value)) {
            layout.removeView(tv2);
            tv1.setTypeface(Typeface.MONOSPACE);
            tv1.setGravity(Gravity.START);
        }
        if (TextUtils.isEmpty(key)) {
            layout.removeView(tv2);
            tv1.setText(value);
        }
        return layout;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (apkCommander.getApkInfo() != null && apkCommander.getApkInfo().isFakePath())
            apkCommander.getApkInfo().getApkFile().delete();
    }


    @Override
    public void onStartParseApk(Uri uri) {
        TextView textView = new TextView(this);
        textView.setTextColor(Color.RED);
        textView.setText(getString(R.string.parsing) + " : " + uri.toString());
        layoutAppDetails.addView(textView);
        btnInstall.setVisibility(View.GONE);
    }

    @Override
    public void onApkParsed(ApkInfo apkInfo) {
        if (apkInfo != null && !TextUtils.isEmpty(apkInfo.getPackageName())) {
            initDetails(apkInfo);
            btnInstall.setVisibility(View.VISIBLE);
        } else {
            Uri uri = getIntent().getData();
            String s = null;
            if (uri != null)
                s = uri.toString();
            TextView textView = new TextView(this);
            textView.setTextColor(Color.RED);
            textView.setText(getString(R.string.parse_apk_failed, s));
            layoutAppDetails.addView(textView);
        }
    }

    @Override
    public void onApkPreInstall(ApkInfo apkInfo) {
        if (layoutPermissionList != null)
            layoutAppDetails.removeView(layoutPermissionList);
        tvAppName.setText(R.string.installing);
        btnInstall.setEnabled(false);
        btnSilently.setEnabled(false);
        layoutButtons.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onApkInstalled(ApkInfo apkInfo, int resultCode) {
        getString(R.string.install_finished_with_result_code, resultCode);
        btnInstall.setEnabled(false);
        btnSilently.setEnabled(false);
        if (resultCode == 0) {
            Toast.makeText(getApplicationContext(), getString(R.string.apk_installed, apkInfo.getAppName()), Toast.LENGTH_SHORT).show();
            tvAppName.setText(R.string.successful);
            btnInstall.setEnabled(true);
            btnInstall.setText(R.string.open_app);
        } else {
            tvAppName.setText(R.string.failed);
        }
        layoutButtons.setVisibility(View.VISIBLE);
    }

    @Override
    public void onInstallLog(ApkInfo apkInfo, String logText) {
        layoutAppDetails.addView(createAppInfoView(logText, null));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_install:
                if (btnInstall.getText().toString().equalsIgnoreCase(getString(R.string.open_app))) {
                    Intent intent = getPackageManager().getLaunchIntentForPackage(apkCommander.getApkInfo().getPackageName());
                    startActivity(intent);
                    finish();
                } else {
                    apkCommander.startInstall();
                }
                break;
            case R.id.btn_silently:
                Intent intent = new Intent(this, BackgroundInstallActivity.class);
                intent.setData(getIntent().getData());
                startActivity(intent);
                finish();
                break;
            case R.id.btn_cancel:
                finish();
                break;
        }
    }
}