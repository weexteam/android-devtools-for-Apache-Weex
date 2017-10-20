# Weex Devtools接入指南
[![GitHub release](https://img.shields.io/github/release/weexteam/weex_devtools_android.svg)](https://github.com/weexteam/weex_devtools_android/releases)   [![Codacy Badge](https://api.codacy.com/project/badge/Grade/af0790bf45c9480fb0ec90ad834b89a3)](https://www.codacy.com/app/weex_devtools/weex_devtools_android?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=weexteam/weex_devtools_android&amp;utm_campaign=Badge_Grade) 	[![GitHub issues](https://img.shields.io/github/issues/weexteam/weex_devtools_android.svg)](https://github.com/weexteam/weex_devtools_android/issues)  [ ![Download](https://api.bintray.com/packages/alibabaweex/maven/weex_inspector/images/download.svg) ](https://bintray.com/alibabaweex/maven/weex_inspector/_latestVersion)

Weex devtools是实现并扩展了[Chrome Debugging Protocol](https://developer.chrome.com/devtools/docs/debugger-protocol)专为weex定制的一款调试神器.其主要功能简介请点击[这里](https://yq.aliyun.com/articles/57651)查看.这篇文章重点介绍Android端的接入问题及注意事项.

- **Inspector**
 Inspector 用来查看运行状态如`Element` \ `Console log` \ `ScreenCast` \ `BoxModel` \ `DOM Tree` \ `Element Select` \ `DataBase` 等.

- **Debugger**
 Debugger 用来调试weex bundle和jsframework. 可以设置断点和查看调用栈.

## Android应用接入

#### 添加依赖
可以通过Gradle 或者 Maven添加对devtools aar的依赖, 也可以直接对源码依赖. 强烈建议使用最新版本, 因为weex sdk和devtools都在快速的迭代开发中, 新版本会有更多惊喜, 同时也修复老版本中一些问题. 最新的release版本可在[这里](https://github.com/weexteam/weex_devtools_android/releases)查看. 所有的release 版本都会发布到[jcenter repo](https://bintray.com/alibabaweex/maven/weex_inspector).

  * *Gradle依赖*.
  ```
  dependencies {
     compile 'com.taobao.android:weex_inspector:0.10.0.5'
  }
  ```
  
  或者
  * *Maven依赖*.
  ```
  <dependency>
    <groupId>com.taobao.android</groupId>
    <artifactId>weex_inspector</artifactId>
    <version>0.10.0.5</version>
    <type>pom</type>
  </dependency>
  ```
  
  或者
  * *源码依赖*.
  
  需要复制[inspector](https://github.com/weexteam/weex_devtools_android/tree/master/inspector)目录到你的app的同级目录, 然后在工程的 `settings.gradle` 文件下添加 `include ":inspector"`, 此过程可以参考playground源码的工程配置及其配置, 然后在app的`build.gralde`中添加依赖.
  ```
  dependencies {
     compile project(':inspector')
  }
```
 另外weex_inspector中有一部分包是以provided的方式引入, 接入方需要自行解决依赖和版本冲突.
 
 * **provided方式引用的包**
 ```
 dependencies {
     provided 'com.google.code.findbugs:jsr305:2.0.1'
     provided 'com.android.support:appcompat-v7:23.1.1'
     provided 'com.taobao.android:weex_sdk:0.10.0'
     provided 'com.alibaba:fastjson:1.1.45+'
     ...
 }
 ```
 
 * **反射引用的包(0.8.0.0以上版本)**
 ```
  dependencies {
     compile 'com.squareup.okhttp:okhttp:2.3.0'
     compile 'com.squareup.okhttp:okhttp-ws:2.3.0'
      ...
  }
 ```
 
 或者
  ```
   dependencies {
      compile 'com.squareup.okhttp3:okhttp:3.4.1'
      compile 'com.squareup.okhttp3:okhttp-ws:3.4.1'
       ...
   }
  ```

##### 版本兼容

| weex sdk | weex inspector | debug server |
|----------|----------------|--------------|
|0.9.5+    | 0.10.0.5+      |0.2.39+       |
|0.9.4+    | 0.9.4.0+       |0.2.39+       |
|0.8.0.1+  | 0.0.8.1+       |0.2.39+       |
|0.7.0+    | 0.0.7.13       |0.2.38        |
|0.6.0+    | 0.0.2.2        |              |


#### 添加Debug模式开关

控制调试模式的打开和关闭的关键点可以概括为三条规则.

**规则一 : 通过sRemoteDebugMode 和 sRemoteDebugProxyUrl和来设置开关和Debug Server地址.**

Weex SDK的WXEnvironment类里有一对静态变量标记了weex当前的调试模式是否开启分别是:

```
    public static boolean sDebugServerConnectable; // DebugServer是否可连通, 默认true
    public static boolean sRemoteDebugMode; // 是否开启debug模式, 默认关闭
    public static String sRemoteDebugProxyUrl; // DebugServer的websocket地址
```

无论在App中无论以何种方式设置Debug模式, 都必须在恰当的时机调用类似如下的方法来设置WXEnvironment.sRemoteDebugMode 和 WXEnvironment.sRemoteDebugProxyUrl.

```
  private void initDebugEnvironment(boolean connectable, boolean debuggable, String host) {
      WXEnvironment.sDebugServerConnectable = connectable;
      WXEnvironment.sRemoteDebugMode = debuggable;
      WXEnvironment.sRemoteDebugProxyUrl = "ws://" + host + ":8088/debugProxy/native";
  }
```

**规则二 :修改sRemoteDebugMode后一定要调用`WXSDKEngine.reload()`.**

一般來說，在修改了WXEnvironment.sRemoteDebugMode以后调用了`WXSDKEngine.reload()` 方法才能够使Debug模式生效. WXSDKEngine.reload() 用来重置Weex的运行环境上下文, 在切换调试模式时需要调用此方法来创建新的weex运行时和DebugBridge并将所有的JS调用桥接到调试服务器执行. 在reload过程中会调用launchInspector, 这就是SDK控制debug模式最核心一个方法, 其传入参数即为sRemoteDebugMode, 若为true则该方法中尝试以反射的方式获取DebugBridge用来在远端执行JS, 否则在本地运行. 

```
  private void launchInspector(boolean remoteDebug) {
    if (WXEnvironment.isApkDebugable()) {
      try {
        if (mWxDebugProxy != null) {
          mWxDebugProxy.stop();
        }
        HackedClass<Object> debugProxyClass = WXHack.into("com.taobao.weex.devtools.debug.DebugServerProxy");
        mWxDebugProxy = (IWXDebugProxy) debugProxyClass.constructor(Context.class, WXBridgeManager.class)
                .getInstance(WXEnvironment.getApplication(), WXBridgeManager.this);
        if (mWxDebugProxy != null) {
          mWxDebugProxy.start();
          if (remoteDebug) {
            mWXBridge = mWxDebugProxy.getWXBridge();
          } else {
            if (mWXBridge != null && !(mWXBridge instanceof WXBridge)) {
              mWXBridge = null;
            }
          }
        }
      } catch (HackAssertionException e) {
        WXLogUtils.e("launchInspector HackAssertionException ", e);
      }
    }
  }
```

 只要遵循上面的原理, 开启Debug模式的方式和时机可由接入方灵活实现. 从launchInspector可以看到, SDK对devtools的aar包并无强依赖, 我们的App只需要在Debug包中打包该aar即可, 这样多少可以缓解包大小问题和安全问题.
 
 **例外：** _若修改WXEnvironment.sRemoteDebugMode的时机在WXBridgeManager初始化和restart和之前则`WXSDKEngine.reload()`可忽略._

**规则三 : 通过响应ACTION_DEBUG_INSTANCE_REFRESH广播及时刷新.**

广播ACTION_DEBUG_INSTANCE_REFRESH在调试模式切换和Chrome调试页面刷新时发出, 主要用来通知当前的weex容器以Debug模式重新加载当前页. 在playground中的处理过程如下:
```
  public class RefreshBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (IWXDebugProxy.ACTION_DEBUG_INSTANCE_REFRESH.equals(intent.getAction())) {
        if (mUri != null) {
          if (TextUtils.equals(mUri.getScheme(), "http") || TextUtils.equals(mUri.getScheme(), "https")) {
            loadWXfromService(mUri.toString());
          } else {
            loadWXfromLocal(true);
          }
        }
      }
    }
  }
```
如果接入方的容器未对该广播做处理, 那么将不支持刷新和调试过程中编辑代码时的watch功能.

#### 接入示例
最简单方式就是复用Playground的相关代码,比如扫码和刷新等模块, 但是扫码不是必须的, 它只是与app通信的一种形式, 二维码里的包含DebugServer IP及bundle地址等信息,用于建立App和Debug Server之间的连接及动态加载bundle. 在Playground中给出了两种开启debug模式的范例.

* 范例1: 通过在XXXApplication中设置开关打开调试模式
```
public class MyApplication extends Application {
  public void onCreate() {
  super.onCreate();
  initDebugEnvironment(true, "xxx.xxx.xxx.xxx"/*"DEBUG_SERVER_HOST"*/);
  }
}
```
这种方式最直接, 在代码中直接hardcode了开启调试模式, 如果在SDK初始化之前调用甚至连`WXSDKEngine.reload()`都不需要调用, 接入方如果需要更灵活的策略可以将`initDebugEnvironment(boolean enable, String host)`和`WXSDKEngine.reload()`组合在一起在合适的位置和时机调用即可.

* 范例2:通过扫码打开调试模式 <br>
Playground中较多的使用扫码的方式传递信息, 不仅用这种方式控制Debug模式的开关,而且还通过它来传入bundle的url直接调试. 应当说在开发中这种方式是比较高效的, 省去了修改sdk代码重复编译和安装App的麻烦, 缺点就是调试工具这种方式接入需要App具有扫码和处理特定规则二维码的能力.除了Playground中的方式, 接入方亦可根据业务场景对Debugger和接入方式进行二次开发.

Playground集成的具体代码可参考如下两个文件:
* 开关控制 , 主要參考對二維碼的處理部分.詳見[WXApplication.java](https://github.com/weexteam/weex_devtools_android/blob/master/playground/app/src/main/java/com/alibaba/weex/WXApplication.java)

* 刷新控制 , 主要參考是容器對ACTION_DEBUG_INSTANCE_REFRESH的處理, 詳見[WXPageActivity.java](https://github.com/weexteam/weex_devtools_android/blob/master/playground/app/src/main/java/com/alibaba/weex/WXPageActivity.java)

#### 牛刀小试

##### 前置工作 
如果未安装Debug Server, 在命令行执行 `npm install -g weex-toolkit` 既可以安装调试服务器, 运行命令 `weex debug` 就会启动DebugServer并打开一个调试页面. 页面下方会展示一个二维码, 这个二维码用于向App传递Server端的地址建立连接.

##### 开始调试
如果你的App客户端完成了以上步骤那么恭喜你已经接入完毕, 可以愉快的调试weex bundle了, 调试体验和网页调试一致!建议新手首先用官方的playground体验一下调试流程. 只需要启动app扫描chrome调试页面下方的第一个二维码即可建立与DebugServer的通信, chorome的调试页面将会列出连接成功的设备信息.

![devtools-main](https://img.alicdn.com/tps/TB13fwSKFXXXXXDaXXXXXXXXXXX-887-828.png "connecting (multiple) devices")

##### 主要步骤如下:
  1. 如果你要加载服务器上bundle, 第一步就是要让你的bundle sever跑起来. 在playground中特别简单, 只需要你到weex源码目录下, 运行 `./start`即可.
  2. 命令行运行 `weex debug` 启动debug server, chrome 将会打开一个网页, 在网页下方有一个二维码和简单的介绍.
  3. 启动App并确认打开调试模式. 你将在上一步中打开的网页中看到一个设备列表, 每个设备项都有两个按钮,分别是`Debugger`和`Inspector`. 
  4. 点击`Inspector` chrome将创建Inspector网页; 点击 `Debugger` chrome hrome将创建Debugger网页; 二者是相互独立的功能, 不相互依赖.

---

## 背景知识

#### Devtools组件介绍
Devtools扩展了[Chrome Debugging Protocol](https://developer.chrome.com/devtools/docs/debugger-protocol), 在客户端和调试服务器之间的采用[JSON-RPC](https://en.wikipedia.org/wiki/JSON-RPC)作为通信机制, 本质上调试过程是两个进程间协同, 相互交换控制权及运行结果的过程. 更多细节还请阅读[Weex Devtools Debugger的技术选型实录](http://www.atatech.org/articles/59284)这篇文章.

* **客户端**
Devtools 客户端作为aar被集成App中, 它通过webscoket连接到调试服务器,此处并未做安全检查. 出于安全机制及包大小考虑, 强烈建议接入方只在debug版本中打包此aar.

* **服务器**
Devtools 服务器端是信息交换的中枢, 既连接客户端, 又连接Chrome, 大多数情况下扮演一个消息转发服务器和Runtime Manager的角色.

* **Web端**
Chrome的V8引擎扮演着bundle javascript runtime的角色. 开启debug模式后, 所有的bundle js 代码都在该引擎上运行. 另一方面我们也复用了Chrome前端的调试界面, 例如设置断点,  查看调用栈等, 调试页关闭则runtime将会被清理. 

调试的大致过程请参考如下时序图.
![debug sequence diagram](https://img.alicdn.com/tps/TB1igLoMVXXXXawapXXXXXXXXXX-786-1610.jpg "debug sequence diagram")


#### FAQ

在各业务接入过程中, 陆续发现一些问题, 对高频次的问题解答如下, 开发中以weex debug -V的方式启动Debug Server可以看到server端的log信息, 对照上文中的时序图对于定位问题还是非常有帮助, 建议调试中默认开启server端log.

1. **扫码App在DebugServerProxy中抛出class not found**
  已知的原因如下:
    * weex_inspector以provided方式引用的包是否引入成功, 如fastjson等.
    * weex_inspector以compile方式引用的包是否引入成功, 某些app重新引入com.squareup.okhttp:okhttp:2.3.0和com.squareup.okhttp:okhttp-ws:2.3.0则不再报错.
    * 混淆规则影响反射.
<br/>
2. **playground 扫码调试crash**
  已知的原因如下:
  * 系统为android 6+, 崩溃信息提示进程需要android.permission.READ_PHONE_STATE权限, 代码中未做权限检查, 在0.0.2.7版本以后已修复, 不再需要此权限.
<br/>
3. **扫码后设备列表页并没有出现我的设备信息.**
  已知的原因如下:
  * Debug Server和手机在不同网段, 被防火墙隔离.
  * 手机连接了PC端的代理, 当前尚不支持.
  * 多进程连接服务器端的同一端口, 比如在Application的onCreate中初始化sdk, 若多个进程连接服务器端的同一端口则报错, 在0.0.2.3版本以后已支持多进程无此问题.
<br/>
4. **调试过程中频繁刷新连接失败, Server端提示重新启动App, 非必现**
  已知的原因如下:
  * 多线程操作网络连接引起, 在频繁的即断即连时容易触发. 在0.0.7.1版本已修复.
