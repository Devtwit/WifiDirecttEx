package com.example.wifidirecttexmple.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.InetAddresses;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wifidirecttexmple.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    Button btnonoff, btnDiscover, btnSend;
    ListView listView;
    TextView read_msg_box, connetionStatus;
    EditText writemSG;
    //Object of wifi manager
    WifiManager wifiManager;
    WifiP2pManager mManager;

    BroadcastReceiver mReciver;
    IntentFilter mIntentFilter;
    WifiP2pManager.Channel mChannel;
List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
String[]  deviceNameArray;
WifiP2pDevice[] deviceArray;

static  final  int MASSAGE_READ =1;

ServerClass serverClass;
ClientClass clientClass;
SendReceive sendReceive;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        initialWork();
        esqListener();
    }


    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch(msg.what){
                case MASSAGE_READ:;
                byte[] reedBuff = (byte[]) msg.obj;
                String tempMsg =new String(reedBuff,0,msg.arg1);
                read_msg_box.setText(tempMsg);
                break;
            }
            return false;
        }
    });
    private void esqListener() {
        btnonoff.setOnClickListener(new View.OnClickListener() {
            //           Check Wifi enable
            @Override
            public void onClick(View v) {
                if (wifiManager.isWifiEnabled()) {
                    wifiManager.setWifiEnabled(false);
                    btnonoff.setText("ON");
                } else {
                    wifiManager.setWifiEnabled(true);
                    btnonoff.setText("OFF");
                }
            }
        });

        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
//                    // TODO: Consider calling
//                    //    ActivityCompat#requestPermissions
//                    // here to request the missing permissions, and then overriding
//                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                    //                                          int[] grantResults)
//                    // to handle the case where the user grants the permission. See the documentation
//                    // for ActivityCompat#requestPermissions for more details.
//                    return;
//                }
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        connetionStatus.setText(("Directory started"));
                    }

                    @Override
                    public void onFailure(int i) {
                        connetionStatus.setText(("Directory Failed"));
                    }
                });
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final WifiP2pDevice device= deviceArray[position];
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress=device.deviceAddress;
                mManager.connect(mChannel
                        , config, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(getApplicationContext(),"connected to "+device.deviceName,Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(int i) {
                                Toast.makeText(getApplicationContext(),"Not connected to "+device.deviceName,Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = writemSG.getText().toString();
                sendReceive.write(msg.getBytes());
            }
        });
    }

    private void initialWork() {
        btnonoff= findViewById(R.id.onOff);
        btnDiscover= findViewById(R.id.discover);
        btnSend= findViewById(R.id.sendButton);
        listView =findViewById(R.id.peerListView);
        read_msg_box =findViewById(R.id.readMsg);
        connetionStatus =findViewById(R.id.connectionStatus);
        writemSG =findViewById(R.id.writeMsg);

//        Intialse wifi manager
        wifiManager= (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this,getMainLooper(),null);


        mReciver = new WiFiDirectBroadcastReceiever(mManager,mChannel,this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

    }


    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
           if(!peerList.getDeviceList().equals(peers)){
                peers.clear();
                peers.addAll(peerList.getDeviceList());
                deviceNameArray= new String[peerList.getDeviceList().size()];
                deviceArray= new WifiP2pDevice[peerList.getDeviceList().size()];
                int index= 0;
                for(WifiP2pDevice device:peerList.getDeviceList()){
                    deviceNameArray[index]= device.deviceName;
                    deviceArray[index] = device;
                    index++;
                }
               ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(),android.R.layout.simple_list_item_1,deviceNameArray);
                listView.setAdapter(adapter);
            }
           if(peers.size()== 0){
               Toast.makeText(getApplicationContext(),"No device found",Toast.LENGTH_SHORT).show();
           }
        }
    };


    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;
            if(wifiP2pInfo.groupFormed &&  wifiP2pInfo.isGroupOwner){
                connetionStatus.setText("Host");
                serverClass= new ServerClass();
                serverClass.start();


            } else if(wifiP2pInfo.groupFormed){
                connetionStatus.setText("Client");
                clientClass= new ClientClass(groupOwnerAddress);
                clientClass.start();
            }
        }
    };
//    Register and Unregister

    @Override
    protected void onResume() {
        super.onResume();
        initialWork();
        esqListener();
        registerReceiver(mReciver,mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReciver);
    }

    //Server socket

    public class ServerClass extends  Thread{
        Socket socket;
        ServerSocket serverSocket;


        @Override
        public void run() {
//            super.run();
            try {
                serverSocket= new ServerSocket(8888);
                socket = serverSocket.accept();
                sendReceive = new SendReceive(socket);
                sendReceive.start();
            }catch (IOException e){
                e.printStackTrace();
            }

        }
    }
    private  class SendReceive extends Thread{
        private  Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceive(Socket skt){
            socket = skt;
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
//            super.run();
            byte[] buffer = new byte[1024];
            int bytes;
            while(socket!= null){
                try {
                    bytes = inputStream.read(buffer);
                    if(bytes>0){
                        handler.obtainMessage(MASSAGE_READ,bytes,-1,buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public  void write(byte[] bytes){
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public class ClientClass extends Thread{
        Socket socket;
        String hostadd;
        public ClientClass(InetAddress ha){
            hostadd=ha.getHostAddress();
            socket = new Socket();
        }

        @Override
        public void run() {
//            super.run();
            try {
                socket.connect(new InetSocketAddress(hostadd,8888),500);
                sendReceive= new SendReceive(socket);
                sendReceive.start();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

    }
}