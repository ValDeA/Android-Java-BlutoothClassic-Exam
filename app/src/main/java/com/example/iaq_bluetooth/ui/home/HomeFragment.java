package com.example.iaq_bluetooth.ui.home;

import static com.example.iaq_bluetooth.BluetoothThread.hexStringToByteArray;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.iaq_bluetooth.BluetoothThread;
import com.example.iaq_bluetooth.MainActivity;
import com.example.iaq_bluetooth.R;
import com.example.iaq_bluetooth.databinding.FragmentHomeBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class HomeFragment extends Fragment {
  public static String TAG = "HomeFragment";

  private HomeViewModel homeViewModel;
  private FragmentHomeBinding binding;

  BluetoothThread bluetoothThread;

  TextView textView, textViewDeviceName;
  EditText sendText;
  Button sendBtn, setFanIncrese, setFanDecrese, toggleConnect;

  int fanSpeed = 3;

  public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
    bluetoothThread = BluetoothThread.getInstance();

    homeViewModel =
        new ViewModelProvider(this).get(HomeViewModel.class);

    binding = FragmentHomeBinding.inflate(inflater, container, false);
    View root = binding.getRoot();

    textView = binding.textHome;
    sendText = binding.editTextSendMsg;
    sendBtn = binding.buttonSendMsg;
    setFanIncrese = binding.buttonFanIncrese;
    setFanDecrese = binding.buttonFanDecrese;
    textViewDeviceName = binding.textViewDeviceName;
    toggleConnect = binding.buttonToggleConnect;

    homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
      @Override
      public void onChanged(@Nullable String s) {
        textView.setText(s);

        String deviceName = bluetoothThread.getDeviceName();
        if(deviceName != null) {
          textViewDeviceName.setText(deviceName);
        }
      }
    });

    sendBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        String msg = sendText.getText().toString();
        bluetoothThread.write(msg);
        setRecvData(textView);
      }
    });

    setFanIncrese.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        fanSpeed = setFanSpeed(fanSpeed + 1);

        // JSON 데이터 파싱
        //String msg = makeJsonForm(1, String.valueOf(fanSpeed));

        // String 데이터 파싱
        String hex = "FEFEFE";
        byte[] msg = hexStringToByteArray(hex);
        bluetoothThread.writeHex(msg);
      }
    });
    setFanDecrese.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        fanSpeed = setFanSpeed(fanSpeed - 1);

        // JSON 데이터 파싱
        //String msg = makeJsonForm(1, String.valueOf(fanSpeed));

        // String 데이터 파싱
        String msg = "fanspeed:" + fanSpeed;
        bluetoothThread.write(msg);
      }
    });

    // 브로드캐스트 반응 이벤트
    toggleConnect.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (bluetoothThread.isConnected() == true) {
          bluetoothThread.closeSocket();
        } else {
          /* 프래그먼트에서 메인 액티비티에 접근하기 */
          ((MainActivity) requireActivity()).showPairedDevicesListDialog();
        }
        //toggleConnect.setEnabled(false);
      }
    });

    // 이벤트 처리
    bluetoothThread.setUpdateDeviceNameEventListener(new BluetoothThread.updateDeviceNameEventListener() {
      @Override
      public void onUpdateDeviceNameEvent(String deviceName) {
        Log.e(TAG, deviceName);
        textViewDeviceName.setText(deviceName);
      }
    });
    bluetoothThread.setRecvDataEventListener(new BluetoothThread.recvDataEventListener() {
      @Override
      public void onRecvDataEvent() {
        refreshFragment();
      }
    });
    // 브로드캐스트 반응 이벤트
    bluetoothThread.setUpdateConnectStateEventListener(new BluetoothThread.updateConnectStateEventListener() {
      @Override
      public void onUpdateConnectStateEvent(boolean connectState) {
        if(connectState == true) {
          toggleConnect.setText("DisConnect");
        } else {
          toggleConnect.setText("Connect");
        }
        //toggleConnect.setEnabled(true);
      }
    });

    return root;
  }

  @Override
  public void onResume() {
    super.onResume();

    Log.e(TAG, "REsume Event");
  }

  // GUi 업데이트 쓰레드
  private void refreshFragment() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        requireActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            setRecvData(textView);
          }
        });
      }
    }).start();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  // 받은 메시지를 화면에 뿌려주는 메소드
  private void setRecvData(View v) {
    TextView textView = (TextView) v;
    textView.setText(bluetoothThread.getRecvData());
    //textView.setText("Connected");
  }

  // JSON Type으로 파싱하는 메소드
  private String makeJsonForm(int type, String data) {
    String key;
    JSONObject jsonObject = new JSONObject();

    switch (type) {
      case 1:
        key = "FanSpeed";
        break;
      case 2:
        key = "Sensor";
        break;
      default:
        type = -1;
        key = "Error";
        data = "makeJsonForm()";
    }

    try {
      jsonObject.put("type", type);
      jsonObject.put(key, data);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return jsonObject.toString();
  }

  private int setFanSpeed(int fan) {
    if (fan >= 5) return 5;
    else return Math.max(fan, 0);
  }
}