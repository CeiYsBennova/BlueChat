package com.example.bluechat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainActivityPlus extends AppCompatActivity {

    private static final int FIND_REQUEST = 3;

    AppCompatButton hostBtn, clientBtn;
    Intent intent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_plus);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestFindDevicesPermission();
        }
        hostBtn = findViewById(R.id.hostBtn);
        clientBtn = findViewById(R.id.clientBtn);
        intent = new Intent(MainActivityPlus.this, MainActivity.class);
    }

    public void HOSTBtn(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestFindDevicesPermission();
        } else {
            intent.putExtra("name", "Host");
            startActivity(intent);
        }
    }

    public void CLIENTBtn(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestFindDevicesPermission();
        } else {
            intent.putExtra("name", "Client");
            startActivity(intent);
        }
    }

    final void requestFindDevicesPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivityPlus.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(MainActivityPlus.this)
                    .setTitle("Yêu cầu quyền")
                    .setMessage("cho chép truy cập vị trí của bạn")
                    .setPositiveButton("ok", (dialog, which) -> ActivityCompat.requestPermissions(MainActivityPlus.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FIND_REQUEST))
                    .setNegativeButton("cancel", (dialog, which) -> {
                        tempToast();
                        dialog.dismiss();
                    }).create().show();
        } else {

            ActivityCompat.requestPermissions(MainActivityPlus.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FIND_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == FIND_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Có quyền để scan thiết bị", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Không có quyền để scan thiết bị", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void tempToast() {
        Toast.makeText(this, "Hủy quyền", Toast.LENGTH_LONG).show();
    } 

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        startActivity(new Intent(MainActivityPlus.this, AboutActivity.class));
        return true;
    }
}