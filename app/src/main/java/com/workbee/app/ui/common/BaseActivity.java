package com.workbee.app.ui.common;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.workbee.app.R;
import com.workbee.app.data.repository.FirebaseRepository;

public abstract class BaseActivity extends AppCompatActivity {
    protected FirebaseRepository repository;
    private ProgressDialog progressDialog;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        String lang = com.workbee.app.utils.LanguageHelper.getLocalePreference(newBase);
        super.attachBaseContext(com.workbee.app.utils.LanguageHelper.wrapLocale(newBase, lang));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        android.content.SharedPreferences themePrefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDark = themePrefs.getBoolean("dark_theme", true);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            isDark ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES 
                   : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        );
        super.onCreate(savedInstanceState);
        repository = new FirebaseRepository(this);
    }

    protected void showProgress(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    protected void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    protected void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
