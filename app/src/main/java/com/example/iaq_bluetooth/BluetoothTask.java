package com.example.iaq_bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothTask extends AsyncTask<Void, String, Boolean> {
  private final static String TAG = "BluetoothTask";
  private static UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
  private static BluetoothTask bluetoothTask = null;

  private Context mContext;
  private InputStream mInputStream = null;
  private OutputStream mOutputStream = null;

  private BluetoothSocket mBluetoothSocket = null;
  private String deviceName;

  private boolean connectStatus = false;
  private static boolean isConnectionError = false;
  private ArrayAdapter<String> mConversationArrayAdapter;

  BluetoothTask(BluetoothDevice pairedDevice) {
    try {
      mBluetoothSocket = pairedDevice.createInsecureRfcommSocketToServiceRecord(uuid);
      //this.toggleConnect(pairedDevice);

    } catch (IOException e) {
      Log.e(TAG, "socket not created", e);
    }

    //Log.d(TAG, "connected to " + mConnectedDeviceName);
    //mConnectionStatus.setText("connected to " + mConnectedDeviceName);
  }

  public static BluetoothTask getInstance(BluetoothDevice pairedDevice) {
    if(bluetoothTask == null) {
      bluetoothTask = new BluetoothTask(pairedDevice);
    }
    return bluetoothTask;
  }

  @Override
  protected Boolean doInBackground(Void... params) {
    byte[] readBuffer = new byte[1024];
    int readBufferPosition = 0;

    while (true) {
      if (isCancelled()) return false;
      try {
        int bytesAvailable = mInputStream.available();
        if (bytesAvailable > 0) {
          byte[] packetBytes = new byte[bytesAvailable];
          mInputStream.read(packetBytes);

          for (int i = 0; i < bytesAvailable; i++) {

            byte b = packetBytes[i];
            if (b == '\n') {
              byte[] encodedBytes = new byte[readBufferPosition];
              System.arraycopy(readBuffer, 0, encodedBytes, 0,
                  encodedBytes.length);
              String recvMessage = new String(encodedBytes, "UTF-8");

              readBufferPosition = 0;

              Log.d(TAG, "recv message: " + recvMessage);
              publishProgress(recvMessage);
            } else {
              readBuffer[readBufferPosition++] = b;
            }
          }
        }
      } catch (IOException e) {

        Log.e(TAG, "disconnected", e);
        return false;
      }
    }
  }

  @Override
  protected void onProgressUpdate(String... recvMessage) {
    Log.e(TAG, recvMessage[0]);
    //mConversationArrayAdapter.insert(recvMessage[0], 0); // 이 부분을 바꿔야 할 듯. 아니 걍 Task 두개 합치기.
  }

  @Override
  protected void onPostExecute(Boolean isSucess) {
    super.onPostExecute(isSucess);

    if (!isSucess) {

      closeSocket();
      Log.d(TAG, "Device connection was lost");
      isConnectionError = true;
      showErrorDialog("Device connection was lost");
    }
  }

  @Override
  protected void onCancelled(Boolean aBoolean) {
    super.onCancelled(aBoolean);
    closeSocket();
  }

  public void toggleConnect(BluetoothDevice bluetoothDevice) {
    //mBluetoothAdapter.cancelDiscovery();
    try {
      mBluetoothSocket.connect();
      mInputStream = mBluetoothSocket.getInputStream();
      mOutputStream = mBluetoothSocket.getOutputStream();

      connectStatus = true;
      deviceName = bluetoothDevice.getName();
      Log.e(TAG, "Socket Create " + deviceName);
    } catch (IOException e) {
      Toast.makeText(mContext, "Bluetooth Connect Failed", Toast.LENGTH_LONG).show();
      Log.e(TAG, "Socket Create Failed " + e.getMessage());
    }
  }

  void closeSocket() {
    try {
      mBluetoothSocket.close();
      Log.d(TAG, "close socket()");
    } catch (IOException e2) {
      Log.e(TAG, "unable to close() " +
          " socket during connection failure", e2);
    }
  }

  void write(String msg) {
    //msg += "\n";
    try {
      mOutputStream.write(msg.getBytes());
      mOutputStream.flush();
    } catch (IOException e) {
      Log.e(TAG, "Exception during send", e);
    }
    //mInputEditText.setText(" ");
  }

  /* Set Method 모음 */
  public void setContext(Context c) {
    mContext = c;
  }

  /* Get Method 모음 */

  /* Visual Method 모음 */
  public ArrayAdapter<String> getConversationArrayAdapter() {
    return mConversationArrayAdapter;
  }

  public void showErrorDialog(String message)
  {
    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
    builder.setTitle("Quit");
    builder.setCancelable(false);
    builder.setMessage(message);
    builder.setPositiveButton("OK",  new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
        if ( isConnectionError  ) {
          isConnectionError = false;
          //finish();
        }
      }
    });
    builder.create().show();
  }
}