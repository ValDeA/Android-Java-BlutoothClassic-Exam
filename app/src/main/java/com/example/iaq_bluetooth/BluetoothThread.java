package com.example.iaq_bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothThread extends Thread {
  private final static String TAG = "BluetoothThread";

  // 스태틱
  private static UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
  private static BluetoothThread bluetoothThread = null;

  // 액티비티
  private Context mContext;
  private InputStream mInputStream = null;
  private OutputStream mOutputStream = null;

  // 블루투스
  private BluetoothSocket mBluetoothSocket = null;
  private BluetoothDevice mBluetoothDevice;
  private String deviceName;
  private String recvData;
  private String macAddress;

  // 이벤트
  private recvDataEventListener mRecvDataEventListener;
  private updateDeviceNameEventListener mUpdateDeviceNameEventListener;
  private updateConnectStateEventListener mUpdateConnectStateEventListener;

  // 스레드
  private boolean isRun = false;
  private boolean isConnect = false;
  private static boolean isConnectionError = false;
  private ArrayAdapter<String> mConversationArrayAdapter;

  BluetoothThread() {
  }

  public static BluetoothThread getInstance() {
    if (bluetoothThread == null) {
      bluetoothThread = new BluetoothThread();
    }
    return bluetoothThread;
  }

  @Override
  public void run() {
    byte[] readBuffer = new byte[1024];
    int readBufferPosition = 0;
    isRun = true;
    ByteStack stack = new ByteStack(3);

    while (isRunning()) {
      while (isConnected()) {
        try {
          int bytesAvailable = mInputStream.available();
          if (bytesAvailable > 0) {
            byte[] packetBytes = new byte[bytesAvailable];
            mInputStream.read(packetBytes);
            stack.clear();

            for (int i = 0; i < bytesAvailable; i++) {
              byte b = packetBytes[i];

              stack.push(b);
              String endPoint = byteArrayToHexString(stack.peekAllStack());
              System.out.println(endPoint);

              // tab으로 메시지 끝 판별
              if (endPoint.equals("FEFEFEFE")) {
                byte[] encodedBytes = new byte[readBufferPosition];
                System.arraycopy(readBuffer, 0, encodedBytes, 0,
                    encodedBytes.length);
                // 문자로 변환
                //String recvMessage = new String(encodedBytes, "UTF-8");
                String recvMessage = byteArrayToHexString(encodedBytes);
                // 버퍼 포지션 초기화
                readBufferPosition = 0;

                // 수신 받은 메시지 처리
                Log.e(TAG, "recv message: " + recvMessage);
                //recvData = parseStringtoJSON(recvMessage);
                recvData = recvMessage;
                mRecvDataEventListener.onRecvDataEvent();
              } else {
                readBuffer[readBufferPosition++] = b;
              }
            }
          }
        } catch (IOException | NullPointerException e) {
          Log.e(TAG, "disconnected", e);
          //Toast.makeText(mContext, "Recv Error()", Toast.LENGTH_SHORT).show();
          isConnect = false;
        }
      }
    }
  }

  // 블루투스를 통해 받은 데이터는 이 곳으로 넘어옴
  public String parseStringtoJSON(String recvData) {
    JSONObject jsonObject;
    String parseData;
    int dataType = -1;

    // JSON 데이터 처리
    try {
      jsonObject = new JSONObject(recvData);
      dataType = jsonObject.getInt("type");

      switch (dataType) {
        case 0:
          parseData = jsonObject.getString("mac");
          macAddress = parseData;
          break;
        case 1:
          parseData = jsonObject.getString("FanSpeed");
          break;
        default:
          parseData = "InCorrect DataType";
      }
      return parseData;
    } catch (JSONException e) {
      // 받은 데이터가 JSON 형식이 아닐 경우
      //parseData = "parseStringtoJSON() Error";
      //return parseData;
      e.printStackTrace();

      return recvData;
    }
  }

  public static byte[] hexStringToByteArray(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
          + Character.digit(s.charAt(i+1), 16));
    }
    return data;
  }
  public static String byteArrayToHexString(byte[] bytes){
    StringBuilder sb = new StringBuilder();
    for(byte b : bytes){
      sb.append(String.format("%02X", b&0xff));
    }
    return sb.toString();
  }

  public void connectSocket() {
    //mBluetoothAdapter.cancelDiscovery();
    try {
      mBluetoothSocket = mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
      mBluetoothSocket.connect();
      mInputStream = mBluetoothSocket.getInputStream();
      mOutputStream = mBluetoothSocket.getOutputStream();

      // 브로드캐스트 미동작으로 인한 Connect 설정 코드
      isConnect = true;

      deviceName = mBluetoothDevice.getName();
      macAddress = mBluetoothDevice.getAddress();
      mUpdateDeviceNameEventListener.onUpdateDeviceNameEvent(deviceName);

      Log.e(TAG, "Socket Create " + deviceName);
    } catch (IOException e) {
      mUpdateDeviceNameEventListener.onUpdateDeviceNameEvent("Bluetooth Connect Failed");
      Log.e(TAG, "Socket Create Failed " + e.getMessage());
    }
  }

  public void closeSocket() {
    //if(isConnect == false) return;
    try {
      write("{\nSocketClose\n}");
      mBluetoothSocket.close();
      isConnect = false;
      deviceName = null;
      mUpdateDeviceNameEventListener.onUpdateDeviceNameEvent("Not Connected");
      Log.d(TAG, "close socket()");
    } catch (IOException e) {
      Log.e(TAG, "unable to close() " +
          " socket during connection failure", e);
    }
  }

  public void showErrorDialog(String message) {
    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
    builder.setTitle("Quit");
    builder.setCancelable(false);
    builder.setMessage(message);
    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
        if (isConnectionError) {
          isConnectionError = false;
          //finish();
        }
      }
    });
    builder.create().show();
  }

  public void writeHex(byte[] hex) {
    try {
      mOutputStream.write(hex);
      mOutputStream.flush();
    } catch (IOException e) {
      Log.e(TAG, "Exception during send", e);
    }
  }

  public void write(String msg) {
    msg += "\n";
    try {
      mOutputStream.write(msg.getBytes());
      mOutputStream.flush();
    } catch (IOException e) {
      Log.e(TAG, "Exception during send", e);
    }
    //mInputEditText.setText(" ");
  }

  public boolean isRunning() {
    return isRun;
  }

  public boolean isConnected() {
    return isConnect;
  }

  /* Set Method 모음 */
  public void setContext(Context c) {
    mContext = c;
  }

  public void setConnect(boolean connect) {
    isConnect = connect;
    mUpdateConnectStateEventListener.onUpdateConnectStateEvent(connect);
  }

  public void setRunning(boolean run) {
    isRun = run;
  }

  public void setBluetoothDevice(BluetoothDevice device) {
    mUpdateDeviceNameEventListener.onUpdateDeviceNameEvent("Device Connecting...");
    mBluetoothDevice = device;
  }

  /* Get Method 모음 */
  public String getRecvData() {
    return recvData;
  }

  public String getDeviceName() {
    return deviceName;
  }

  public String getMacAddress() {
    return macAddress;
  }

  /* Event Listener 모음 */
  public interface recvDataEventListener {
    void onRecvDataEvent();
  }

  public void setRecvDataEventListener(recvDataEventListener listener) {
    mRecvDataEventListener = listener;
  }

  public interface updateDeviceNameEventListener {
    void onUpdateDeviceNameEvent(String deviceName);
  }

  public void setUpdateDeviceNameEventListener(updateDeviceNameEventListener listener) {
    mUpdateDeviceNameEventListener = listener;
  }

  public interface updateConnectStateEventListener {
    void onUpdateConnectStateEvent(boolean connectState);
  }

  public void setUpdateConnectStateEventListener(updateConnectStateEventListener listener) {
    mUpdateConnectStateEventListener = listener;
  }
}
