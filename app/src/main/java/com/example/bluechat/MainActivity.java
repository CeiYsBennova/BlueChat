package com.example.bluechat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.companion.CompanionDeviceManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int BLUETOOTH_REQUEST_CODE = 1;
    private static final int SELECT_DEVICE_REQUEST_CODE = 2;
    private static final int FIND_REQUEST = 3;
    private static final int REQUEST_DISCOVERABILITY = 4;


    private static String APP_NAME;// =
    private static final UUID APP_UUID = UUID.fromString("ab0e7500-9205-11ec-b909-0242ac120002");

    ChatDataBase chatDataBase = null;

    String name;
    AppCompatButton discoverBtn;
    TextView status;
    TextView notFoundText;
    ListView listView;
    ArrayList<String> bluetoothList = null;
    BluetoothDevice pairingDevice = null;

    final Handler handler = new Handler();


    ArrayList<BluetoothDevice> bluetoothDevices = null;
    ListAdapter listAdapter = null;
    private BluetoothAdapter bluetoothAdapter = null;


    ClientSocket clientSocket = null;
    ServerSocket serverSocket = null;
    IntentFilter intentFilter = null, intentFilter2 = null, intentFilter3 = null, intentFilter4 = null;
    Intent discoveryIntent = null;


    ConstraintLayout chatLayout = null, commandCenterLayout;
    TextView clientName;
    EditText enteredMsg;
    AppCompatButton sendMsgBtn, disconnectBtn;
    SendReceive sendReceive = null;
    ArrayList<String> chatList = null;
    ArrayList<Byte> chatPosition = null;
    ListView chatListView = null;
    ChatListAdapter chatListAdapter = null;
    String targetName;

    private static final int MESSAGE_READ = 1;
    private static final int MESSAGE_WRITE = 2;
    private static final int MESSAGE_TOAST = 3;

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        name = getIntent().getStringExtra("name");
        Objects.requireNonNull(getSupportActionBar()).setTitle(name);

        initializeVariables();
        setListeners();

        // kiểm tra xem thiết bị có bật bluetooth hay không
        if (checkBluetoothCompatibility()) {
            requestEnableBluetooth();
        } else {
            return;
        }

        registerBroadcastReceivers();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestFindDevicesPermission();
        }
        // strart ServerSocket
        getBoundedDevices();

        stopEverything();
        if (name.equals("Client")) {
            discoverBtn.setVisibility(View.VISIBLE);
            enableDiscoverability(null);
            try {
                serverSocket.interrupt();
            } catch (Exception ignored) {
            }

            startServerSocket();
        } else {
            startDiscoveryOfDevices();
        }


    }


    public void enableDiscoverability(View view) {
        discoveryIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoveryIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivityForResult(discoveryIntent, REQUEST_DISCOVERABILITY);
    }


    private void startServerSocket() {

        serverSocket = new ServerSocket();
        serverSocket.start();

    }
    // khởi tạo các biến
    private void initializeVariables() {
        APP_NAME = getString(R.string.app_name);

        discoverBtn = findViewById(R.id.discoverBtn);
        listView = findViewById(R.id.boundedDevicesList);
        status = findViewById(R.id.status);
        notFoundText = findViewById(R.id.notFoundText);


        bluetoothList = new ArrayList<>();
        bluetoothDevices = new ArrayList<>();
        listAdapter = new ListAdapter(bluetoothList);
        listView.setAdapter(listAdapter);


        chatLayout = findViewById(R.id.chatLayout);
        commandCenterLayout = findViewById(R.id.commandCenterView);
        clientName = findViewById(R.id.chatClientName);
        sendMsgBtn = findViewById(R.id.sendMsg);
        disconnectBtn = findViewById(R.id.disconnectBtn);
        enteredMsg = findViewById(R.id.enteredMsg);
        chatListView = findViewById(R.id.chatListView);

        chatList = new ArrayList<>();
        chatPosition = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(chatList);
        chatListView.setAdapter(chatListAdapter);


        chatLayout.setVisibility(View.GONE);
        commandCenterLayout.setVisibility(View.VISIBLE);
        discoverBtn.setVisibility(View.GONE);


    }

    private void setListeners() {
        listView.setOnItemClickListener(listViewListener);
        listView.setOnItemLongClickListener(listViewLongListener);
    }


    AdapterView.OnItemClickListener listViewListener = (parent, view, position, id) -> {

        if (name.equals("Host")) {
            if (checkBluetoothCompatibility()) {
                requestEnableBluetooth();
            } else {
                return;
            }

            if (checkDiscoverState()) {
                tempToast("Scanning...", 0);
                return;
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                if (bluetoothAdapter.isEnabled()) {

                    clientSocket = new ClientSocket(bluetoothDevices.get(position));
                    clientSocket.start();

                }
            } else {
                requestFindDevicesPermission();
            }
        } else {
            tempToast("Chỉ có HOST mới chọn được thiết bị", 1);
        }


    }; 

    AdapterView.OnItemLongClickListener listViewLongListener = (parent, view, position, id) -> {

        PopupMenu popupMenu = new PopupMenu(MainActivity.this, view);
        popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            clearChat(bluetoothList.get(position));
            return true;
        });

        popupMenu.show();
        return true;
    };


    private boolean checkDiscoverState() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestFindDevicesPermission();
            return true;
        }
        return bluetoothAdapter.isDiscovering();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == FIND_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                tempToast("Yêu cầu quyền để scan thiết bị!", 1);
                startDiscoveryOfDevices();
            } else {
                tempToast("Không có quyền để scan thiết bị!", 1);
            }
        }
    }

    final void requestFindDevicesPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Yêu cầu quyền")
                    .setMessage("Cho phép ứng dụng truy cập vào vị trí của bạn")
                    .setPositiveButton("ok", (dialog, which) -> ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FIND_REQUEST))
                    .setNegativeButton("cancel", (dialog, which) -> {
                        tempToast("Không cho phép quyền", 1);
                        dialog.dismiss();
                    }).create().show();
        } else {

            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FIND_REQUEST);
        }
    }


    private void requestEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent bluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(bluetoothIntent, BLUETOOTH_REQUEST_CODE);
            }
        }
    }

    private boolean checkBluetoothCompatibility() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            tempToast("Lỗi: Không support thiết bị này!", 1);
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_DISCOVERABILITY) {

            if (resultCode == RESULT_CANCELED) {
                tempToast("Vui lòng để thiết bị không ẩn", 0);
            } else {
                tempToast("Không ẩn trong 5 phút", 1);

            }
        }

        if (requestCode == BLUETOOTH_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                getBoundedDevices();
            } else {
                tempToast("Vui lòng bật Bluetooth", 1);
            }
        }

        if (requestCode == SELECT_DEVICE_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                pairingDevice = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);
            }

            if (pairingDevice != null) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    tempToast("Không có quyền connect", 1);
                    return;
                }
                pairingDevice.createBond();
            }
        }


    }

    // khám phá thiết bị xung quanh
    private void startDiscoveryOfDevices() {

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (bluetoothAdapter.startDiscovery()) {
                getBoundedDevices();
                tempToast("Scanning vị trí hiện tại", 1);
                setTheStatus("Scanning...");
            }
        }
    }

    private void setTheStatus(String msg) {
        String txt = "Status: " + msg;
        status.setText(txt);
    }

    private void tempToast(String message, int time) {
        if (time == 0) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }
    // 
    private void getBoundedDevices() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Set<BluetoothDevice> bt = bluetoothAdapter.getBondedDevices();

            if (bt.size() > 0) {
                for (BluetoothDevice b : bt) {
                    if (bluetoothList.contains(b.getName())) continue;
                    bluetoothList.add(b.getName());
                    bluetoothDevices.add(b);
                }
                listAdapter.notifyDataSetChanged();
                notFoundText.setVisibility(View.GONE);

            } else {
                notFoundText.setVisibility(View.VISIBLE);
            }
        }
    }

    // list Apdater
    public class ListAdapter extends BaseAdapter {

        ArrayList<String> list;

        public ListAdapter(ArrayList<String> list) {
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_view_model, parent, false);
            }

            TextView textView = convertView.findViewById(R.id.deviceName);
            textView.setText((String) getItem(position));
            return convertView;
        }
    }

    public class ChatListAdapter extends BaseAdapter {

        ArrayList<String> list;


        public ChatListAdapter(ArrayList<String> list) {
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.message_list_view_model, parent, false);
            }

            TextView textView = convertView.findViewById(R.id.messageText);

            LinearLayout.LayoutParams textParams = (LinearLayout.LayoutParams) textView.getLayoutParams();


            if (chatPosition.get(position) == (byte) 1) {
                textParams.gravity = Gravity.CENTER | Gravity.START;
                textView.setBackgroundResource(R.drawable.host_chat_background);
            } else {
                textParams.gravity = Gravity.CENTER | Gravity.END;
                textView.setBackgroundResource(R.drawable.client_chat_background);
            }

            textView.setLayoutParams(textParams);
            textView.setText((String) getItem(position));

            return convertView;
        }
    }

    private void registerBroadcastReceivers() {
        intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        intentFilter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter3 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter4 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(foundReceiver, intentFilter);
        registerReceiver(deviceDisconnectReceiver, intentFilter2);
        registerReceiver(discoveryReceiver, intentFilter3);
        registerReceiver(stateChangeReceiver, intentFilter4);
    }

    private final BroadcastReceiver foundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                pairingDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    sendMessageToUi("Không có quyền để lấy tên thiết bị");
                    return;
                }

                String name = pairingDevice.getName();

                if (name != null && !bluetoothList.contains(name)) {
                    bluetoothList.add(name);
                    bluetoothDevices.add(pairingDevice);
                    listAdapter.notifyDataSetChanged();
                    notFoundText.setVisibility(View.GONE);
                }

            }
        }
    };


    private final BroadcastReceiver deviceDisconnectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                int st = intent.getIntExtra(BluetoothDevice.ACTION_ACL_DISCONNECTED, -1);

                if (st != BluetoothDevice.ERROR) {

                    handler.post(() -> {

                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            if (bluetoothAdapter.isDiscovering()) {
                                setTheStatus("Scanning...");
                            }
                        } else {
                            setTheStatus("Active");
                        }


                    });

                    chatLayout.setVisibility(View.GONE);
                    commandCenterLayout.setVisibility(View.VISIBLE);

                }
            }
        }
    };

    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                handler.post(() -> {
                    if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                        tempToast("Hoàn thành Scan...", 1);
                        setTheStatus("Active");
                    }
                });
            }
        }
    };

    private final BroadcastReceiver stateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            String action = intent.getAction();
            String msg = null, toastMsg = null;

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int ste = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);

                switch (ste) {
                    case BluetoothAdapter.STATE_OFF:
                        // use a handler to send info as Inactive
                        msg = "Inactive";
                        toastMsg = "Bluetooth đang TẮT";

                        break;
                    case BluetoothAdapter.STATE_ON:
                        // user a handler to send info Active|neutral
                        msg = "Active";
                        toastMsg = "Bluetooth đang BẬT";

                        break;
                }

                if (msg != null) {
                    final String finalMsg = toastMsg;
                    final String finalStatus = msg;
                    handler.post(() -> {
                        tempToast(finalMsg, 1);
                        setTheStatus(finalStatus);
                    });
                }
            }
        }
    };

    // server socket
    private class ServerSocket extends Thread {
        private final BluetoothServerSocket serverSocket;

        public ServerSocket() {

            BluetoothServerSocket tmp = null;

            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestFindDevicesPermission();
            }
            try {

                if (bluetoothAdapter.isEnabled()) {
                    tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID);
                } else {
                    sendMessageToUi("Vui lòng bật Bluetooth");
                }
            } catch (Exception e) {
                e.printStackTrace();

            }
            serverSocket = tmp;
        }


        public void run() {
            BluetoothSocket socket = null;

            while (true) {
                try {
                    if (bluetoothAdapter.isEnabled() && serverSocket != null) {
                        socket = serverSocket.accept();
                        sendReceive = new SendReceive(socket);
                        sendReceive.start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (socket != null) {
                    BluetoothSocket finalSocket = socket;
                    handler.post(() -> {
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            targetName = finalSocket.getRemoteDevice().getName();

                            startChatting();
                        }
                    });
                    cancel();
                    break;
                }
            }

        }

        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                sendMessageToUi("Không thể đóng server socket");
            }
        }
    }


    // client socket
    private class ClientSocket extends Thread {

        private final BluetoothSocket bluetoothSocket;

        public ClientSocket(BluetoothDevice bluetoothDevice) {
            handler.post(() -> tempToast("Connecting...", 1));
            BluetoothSocket tmp = null;

            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestFindDevicesPermission();

            }

            targetName = bluetoothDevice.getName();

            try {
                if (bluetoothAdapter.isEnabled()) {
                    tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(APP_UUID);
                } else {
                    sendMessageToUi("Vui lòng bật Bluetooth");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendMessageToUi("Không thể tạo socket từ client");
            }
            bluetoothSocket = tmp;

        }

        public void run() {

            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestFindDevicesPermission();
            }
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
            } else {
                sendMessageToUi("Không có quyền truy cập vi trí");
            }

            try {
                bluetoothSocket.connect();

                handler.post(() -> {
                    setTheStatus("Đã kết nối");
                    startChatting();
                });

                sendReceive = new SendReceive(bluetoothSocket);
                sendReceive.start();

            } catch (Exception e) {
                e.printStackTrace();
                sendMessageToUi("Restart Host và thử lại");
                if (serverSocket != null) {
                    serverSocket.cancel();
                }
                cancel();

            }

        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                sendMessageToUi("Không thể đóng socket từ client");
            }
        }
    }

    Handler messageHandler = new Handler(msg -> {
        String message;
        byte[] bytes;

        switch (msg.what) {
            case MESSAGE_READ:
                bytes = (byte[]) msg.obj;
                message = new String(bytes, 0, msg.arg1);
                chatList.add(message);
                chatPosition.add((byte) 1);
                break;
            case MESSAGE_WRITE:
                break;
            case MESSAGE_TOAST:
                break;
        }

        chatListView.setSelection(chatListAdapter.getCount() - 1);
        chatListAdapter.notifyDataSetChanged();
        return true;
    });

    private void startChatting() {
        commandCenterLayout.setVisibility(View.GONE);
        chatLayout.setVisibility(View.VISIBLE);
        setTheChats();

        if (targetName == null || targetName.equals("")) {
            targetName = "username không hợp lệ";
        }
        if (targetName.length() > 15) {
            targetName = targetName.substring(0, 15);
        }
        String tempName = "Target : " + targetName;
        clientName.setText(tempName);
    }

    public void sendMsgToUserBtn(View view) {
        String msgTxt;
        try {
            msgTxt = enteredMsg.getText().toString();
        } catch (Exception e) {
            e.printStackTrace();
            sendMessageToUi("Enter message");
            return;
        }

        msgTxt = msgTxt.trim();
        if (msgTxt.equals("")) {
            sendMessageToUi("Enter message");
            return;
        }

        chatList.add(msgTxt);
        byte[] bytes = msgTxt.getBytes();

        sendReceive.writeMessage(bytes);
        enteredMsg.setText("");
        chatPosition.add((byte) 2);
    }

    public void disconnectTheChat(View view) {
        if (sendReceive != null) {
            sendReceive.cancel();
            chatLayout.setVisibility(View.GONE);
            commandCenterLayout.setVisibility(View.VISIBLE);
            stopEverything();
            finish();
        }
    }

    private void sendMessageToUi(String msg) {
        handler.post(() -> tempToast(msg, 1));
    }

    private class SendReceive extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        private byte[] buffer;

        public SendReceive(BluetoothSocket bluetoothSocket) {
            this.bluetoothSocket = bluetoothSocket;

            InputStream tempIn = null;
            OutputStream tempOut = null;
            try {
                tempIn = bluetoothSocket.getInputStream();
            } catch (Exception e) {
                e.printStackTrace();
                sendMessageToUi("Lỗi!");
            }

            try {
                tempOut = bluetoothSocket.getOutputStream();
            } catch (Exception e) {
                e.printStackTrace();
                sendMessageToUi("Lỗi!");
            }

            inputStream = tempIn;
            outputStream = tempOut;
        }

        public void run() {
            buffer = new byte[1024];
            int numOfBytes;

            while (true) {
                try {
                    numOfBytes = inputStream.read(buffer);
                    Message message = messageHandler.obtainMessage(MESSAGE_READ, numOfBytes, -1, buffer);
                    message.sendToTarget();
                } catch (Exception e) {
                    e.printStackTrace();
                    handler.post(() -> {
                        tempToast("User disconnected...", 1);
                        setTheStatus("Active");
                        commandCenterLayout.setVisibility(View.VISIBLE);
                        chatLayout.setVisibility(View.GONE);
                    });
                    if (name.equals("Client")) {
                        startServerSocket();
                    }
                    saveTheChats();
                    stopEverything();

                    break;
                }
            }
        }

        public void writeMessage(byte[] bytes) {
            try {
                outputStream.write(bytes);
                Message message = messageHandler.obtainMessage(MESSAGE_WRITE, bytes.length, -1, buffer);
                message.sendToTarget();

            } catch (Exception e) {
                e.printStackTrace();
                sendMessageToUi("Lỗi: không thể gửi tin nhắn...");
            }
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
                sendMessageToUi("Lỗi: không thể đóng socket...");
            }
        }
    }

    private void stopEverything() {
        try {
            serverSocket.cancel();
            serverSocket.interrupt();
            clientSocket.cancel();
            clientSocket.interrupt();
        } catch (Exception ignored) {
        }
    }

    private void saveTheChats() {
        chatDataBase = new ChatDataBase(MainActivity.this);
        StringBuilder totalChats = new StringBuilder();
        String tempChat = "";

        for (int i = 0; i < chatList.size(); i++) {
            tempChat = chatPosition.get(i) + chatList.get(i) + "₧";
            totalChats.append(tempChat);
        }

        if (chatDataBase.insertData(targetName, totalChats.toString())) {
            sendMessageToUi("Đã lưu chat...");
        } else {
            if (chatDataBase.updateData(targetName, totalChats.toString())) {
                sendMessageToUi("Đã cập nhật chat...");
            } else {
                sendMessageToUi("Lỗi: không thể lưu chat...");
            }
        }

        chatDataBase.close();
    }

    private void setTheChats() {
        chatDataBase = new ChatDataBase(MainActivity.this);
        Cursor cursor = chatDataBase.getData();

        String totalChats = null;
        String[] totalChatArray;

        if (cursor.getCount() == 0) return;

        chatList.clear();
        chatPosition.clear();

        try {
            while (cursor.moveToNext()) {
                if (cursor.getString(0).equals(targetName)) {
                    totalChats = cursor.getString(1);
                    break;
                }
            }
            if (totalChats != null) {

                totalChatArray = totalChats.split("₧");

                for (String s : totalChatArray) {
                    int position = -1;
                    try {
                        String p = s.charAt(0) + "";
                        position = Integer.parseInt(p);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (position != -1) {
                        chatPosition.add((byte) position);
                        chatList.add(s.substring(1));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        chatDataBase.close();
    }

    private void clearChat(String name) {
        chatDataBase = new ChatDataBase(MainActivity.this);

        if (chatDataBase.deleteSpecific(name)) {
            tempToast("Đã xóa chat...", 1);
        } else {
            tempToast("Lỗi: không thể xóa chat...", 1);
        }
    }

    @Override
    public void onBackPressed() {

        unregisterAll();
        stopEverything();

        super.onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        unregisterAll();
        stopEverything();
        super.onDestroy();
    }

    private void unregisterAll() {
        try {
            sendReceive.cancel();
            unregisterReceiver(foundReceiver);
            unregisterReceiver(discoveryReceiver);
            unregisterReceiver(stateChangeReceiver);
            unregisterReceiver(discoveryReceiver);
        } catch (Exception ignored) {
        }
    }
}