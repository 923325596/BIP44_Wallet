package com.lv.wallet;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void createWallet(View view){
        startActivity(new Intent(this,CreateWalletActivity.class));
    }

    public void importWallet(View view){
        startActivity(new Intent(this,ImportWalletActivity.class));
    }
}




