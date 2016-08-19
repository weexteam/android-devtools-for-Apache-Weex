package com.alibaba.weex;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.taobao.weex.WXEnvironment;
import com.taobao.weex.WXSDKEngine;

import java.util.Locale;

/**
 * 模拟器debug页面
 * @author Senyu email: senyu.ylj@alibaba-inc.com
 */
public class SimulatorDebugActivity extends AppCompatActivity {
    private ImageView mBackImageView;
    private EditText mDebugHostEditText;
    private EditText mDebugPortEditText;
    private EditText mServerEditText;
    private Button mConnectDevToolButton;
    private Button mConnectServerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simulator);
        initView();
        initListener();
    }

    /**
     * 初始化视图
     */
    private void initView(){
        mBackImageView = (ImageView) findViewById(R.id.simulator_back);
        mDebugHostEditText = (EditText) findViewById(R.id.simulator_debug_host);
        mDebugPortEditText = (EditText) findViewById(R.id.simulator_debug_port);
        mServerEditText = (EditText) findViewById(R.id.simulator_server);
        mConnectDevToolButton = (Button)findViewById(R.id.simulator_connect_devtool);
        mConnectServerButton = (Button)findViewById(R.id.simulator_connect_server);
    }

    /**
     * 初始化监听
     */
    private void initListener(){
        mConnectDevToolButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mDebugHostEditText.getText().toString().
                        matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")){
                    Toast.makeText(SimulatorDebugActivity.this, "请输入有效服务器Ip", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(!mDebugPortEditText.getText().toString().matches("\\d+")){
                    Toast.makeText(SimulatorDebugActivity.this, "请输入有效服务器端口", Toast.LENGTH_SHORT).show();
                    return;
                }

                //连接debug
                WXEnvironment.sRemoteDebugProxyUrl = String.format(Locale.CHINA, "ws://%s:%s/debugProxy/native",
                        mDebugHostEditText.getText().toString(),
                        mDebugPortEditText.getText().toString());
                WXSDKEngine.reload(getApplication(),true);
                finish();
            }
        });

        mConnectServerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent;
                    intent = new Intent(SimulatorDebugActivity.this, WXPageActivity.class);
                    intent.setData(Uri.parse(mServerEditText.getText().toString()));
                    startActivity(intent);
                }catch (Exception e){
                    Toast.makeText(SimulatorDebugActivity.this,
                            "请输入有效JSBundle地址", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });

        mBackImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
