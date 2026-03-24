package com.aqa.cc.nodestatus;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView foundationScopeValue = findViewById(R.id.foundationScopeValue);
        foundationScopeValue.setText("VIRT_FUSION");
    }
}
