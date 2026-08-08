package com.android.nls.routine;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.android.nls.routine.activity.HomeActivity;
import com.android.nls.routine.utils.Common;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = Common.generateTag(MainActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Starting app");
        super.onCreate(savedInstanceState);
        launchHomeAct();
    }

    private void launchHomeAct() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}