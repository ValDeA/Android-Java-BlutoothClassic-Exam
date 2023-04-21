package com.example.iaq_bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.app.AlertDialog;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.iaq_bluetooth.databinding.ActivityMainBinding;

import java.util.Set;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = "MainActivity";

  private AppBarConfiguration mAppBarConfiguration;
  private ActivityMainBinding binding;

  BluetoothAdapter bluetoothAdapter;
  BluetoothThread bluetoothThread = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // 브로드캐스트 필터 레지스트
    //IntentFilter filter = new IntentFilter();
    //filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
    //filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
    //registerReceiver(bluetoothBroadcastReceiver, filter);

    binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    setSupportActionBar(binding.appBarMain.toolbar);
    binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show();
      }
    });
    DrawerLayout drawer = binding.drawerLayout;
    NavigationView navigationView = binding.navView;
    // Passing each menu ID as a set of Ids because each
    // menu should be considered as top level destinations.
    mAppBarConfiguration = new AppBarConfiguration.Builder(
        R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
        .setDrawerLayout(drawer)
        .build();
    NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
    NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
    NavigationUI.setupWithNavController(navigationView, navController);

    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    if(bluetoothAdapter == null) return;

    Log.e(TAG, "MainActivity Create");

    if(bluetoothAdapter.isEnabled() == false) {
      // 블루투스 사용 요청하기
      //Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      //startActivityForResult(intent, REQUEST_BLUETOOTH_ENABLE);

      // 블루투스 강제 사용
      bluetoothAdapter.enable();
    }
    else {
      Log.d(TAG, "Initialisation successful.");
      showPairedDevicesListDialog();
    }
  }

  public void showPairedDevicesListDialog() {
    Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();

    // 기존에 연결됐던 디바이스와 같은 MAC 주소라면 바로 연결
    try {
      if(bluetoothThread.getMacAddress() != null) {
        for (BluetoothDevice device : devices) {
          if(device.getAddress().equals(bluetoothThread.getMacAddress())) {
            bluetoothThread = BluetoothThread.getInstance();
            bluetoothThread.setContext(getApplicationContext());
            bluetoothThread.setBluetoothDevice(device);
            bluetoothThread.connectSocket();

            if(bluetoothThread.isRunning() == false) bluetoothThread.start();

            return;
          }
        }
      }
    } catch (NullPointerException e) { e.printStackTrace(); }

    // 기존에 연결됐던 디바이스가 없다면
    final BluetoothDevice[] pairedDevices = devices.toArray(new BluetoothDevice[0]);
    // 페어링 된 디바이스가 하나도 없다면
    if(pairedDevices.length == 0) return;

    String[] items = new String[pairedDevices.length];
    for(int i=0; i<pairedDevices.length; i++) {
      items[i] = pairedDevices[i].getName();
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Select Device");
    builder.setCancelable(false);
    builder.setItems(items, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
        bluetoothThread = BluetoothThread.getInstance();
        bluetoothThread.setContext(getApplicationContext());
        bluetoothThread.setBluetoothDevice(pairedDevices[which]);
        bluetoothThread.connectSocket();

        if(bluetoothThread.isRunning() == false) bluetoothThread.start();
      }
    });
    builder.create().show();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onSupportNavigateUp() {
    NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
    return NavigationUI.navigateUp(navController, mAppBarConfiguration)
        || super.onSupportNavigateUp();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    bluetoothThread.setRunning(false);
  }

  /* 라즈베리파이에선 동작안함 */
  /*
  BroadcastReceiver bluetoothBroadcastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();

      switch (action) {
        case BluetoothDevice.ACTION_ACL_CONNECTED:
          bluetoothThread.setConnect(true);
          Log.e(TAG, "Broadcast Receive Bluetooth Connected");
          break;
        case BluetoothDevice.ACTION_ACL_DISCONNECTED:
          bluetoothThread.setConnect(false);
          bluetoothThread.closeSocket();
          Log.e(TAG, "Broadcast Receive Bluetooth Disconnected");
          break;
      }
    }
  };
  */
}