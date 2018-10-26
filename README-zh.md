# Weex Devtools接入指南
[![GitHub release](https://img.shields.io/github/release/weexteam/weex_devtools_android.svg)](https://github.com/weexteam/weex_devtools_android/releases)   [![Codacy Badge](https://api.codacy.com/project/badge/Grade/af0790bf45c9480fb0ec90ad834b89a3)](https://www.codacy.com/app/weex_devtools/weex_devtools_android?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=weexteam/weex_devtools_android&amp;utm_campaign=Badge_Grade) 	[![GitHub issues](https://img.shields.io/github/issues/weexteam/weex_devtools_android.svg)](https://github.com/weexteam/weex_devtools_android/issues)  [ ![Download](https://api.bintray.com/packages/alibabaweex/maven/weex_inspector/images/download.svg) ](https://bintray.com/alibabaweex/maven/weex_inspector/_latestVersion)

Weex devtools是实现并扩展了[Chrome Debugging Protocol](https://developer.chrome.com/devtools/docs/debugger-protocol)专为weex定制的调试工具.其主要功能简介请点击[这里](https://yq.aliyun.com/articles/57651)查看.这篇文章重点介绍Android端的接入问题及注意事项.

- **Inspector**
 Inspector 用来查看运行状态如`Element` \ `Console log` \ `ScreenCast` \ `BoxModel` \ `DOM Tree` \ `Element Select` \ `DataBase` 等.

- **Debugger**
 Debugger 用来调试weex bundle和jsframework. 可以设置断点和查看调用栈.

## Android接入

#### 添加依赖
可以通过Gradle 或者 Maven添加对devtools aar的依赖, 也可以直接对源码依赖. 强烈建议使用最新版本, 因为weex sdk和devtools都在快速的迭代开发中, 新版本会有更多惊喜, 同时也修复老版本中一些问题. 最新的release版本可在[这里](https://github.com/weexteam/weex_devtools_android/releases)查看. 所有的release 版本都会发布到[jcenter repo](https://bintray.com/alibabaweex/maven/weex_inspector).

  * *Gradle依赖*.
  ```
  dependencies {
     compile 'com.taobao.android:weex_inspector:0.18.10'
  }
  ```
  
  或者
  * *Maven依赖*.
  ```
  <dependency>
    <groupId>com.taobao.android</groupId>
    <artifactId>weex_inspector</artifactId>
    <version>0.18.10</version>
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
 
 * **需要引用okhttp**
 ```
  dependencies {
     compile 'com.squareup.okhttp:okhttp:2.3.0'
     compile 'com.squareup.okhttp:okhttp-ws:2.3.0'
      ...
  }
 ```
 
#### 调试开关（扫码开启调试/手动开启调试）

最简单方式就是复用Playground的相关代码,比如扫码和刷新等模块, 但是扫码不是必须的, 它只是与app通信的一种形式, 二维码里的包含DebugServer IP及bundle地址等信息,用于建立App和Debug Server之间的连接及动态加载bundle. 在Playground中给出了两种开启debug模式的范例.

* 范例1: 通过在XXXApplication中设置开关打开调试模式 <br>
```
public class MyApplication extends Application {
  public void onCreate() {
  super.onCreate();
  initDebugEnvironment(true, "xxx.xxx.xxx.xxx"/*"DEBUG_SERVER_HOST"*/);
  //WXSDKEngine.reload();
  }
}
```
这种方式最直接, 在代码中直接hardcode了开启调试模式, 如果在SDK初始化之前调用甚至连`WXSDKEngine.reload()`都不需要调用, 接入方如果需要更灵活的策略可以将`initDebugEnvironment(boolean enable, String host)`和`WXSDKEngine.reload()`组合在一起在合适的位置和时机调用即可.（如果不是初始化之前调用，n那么每次调用initDebugEnvironment后必须调用WXSDKEngine.reload()刷新Weex引擎）

* 范例2:通过扫码打开调试模式 <br>
Playground中较多的使用扫描weex debugger生成的调试二维码的方式传递信息, 不仅用这种方式控制Debug模式的开关,而且还通过它来传入bundle的url直接调试. 应当说在开发中这种方式是比较高效的, 省去了修改sdk代码重复编译和安装App的麻烦.

拦截方式：
````
if (WXEnvironment.isApkDebugable()) {
  String devToolUrl = uri.getQueryParameter("_wx_devtool");
  if (!TextUtils.isEmpty(devToolUrl)) {
    WXEnvironment.sRemoteDebugProxyUrl = devToolUrl;
    WXEnvironment.sDebugServerConnectable = true;
    WXSDKEngine.reload(XXXXX.getApplication(), false);
  }
}
````

* 可选：调试刷新协议 <br>
广播WXSDKInstance.ACTION_DEBUG_INSTANCE_REFRESH在调试模式切换和Chrome调试页面刷新时发出, 如有需要可以监听
```
  public class RefreshBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (IWXDebugProxy.ACTION_DEBUG_INSTANCE_REFRESH.equals(intent.getAction())) {
        //Do something
      }
    }
  }
```

#### 版本匹配

| weex sdk | weex inspector | debug server |
|----------|----------------|--------------|
|0.9.5+    | 0.10.0.5+      |0.2.39+       |
|0.9.4+    | 0.9.4.0+       |0.2.39+       |
|0.8.0.1+  | 0.0.8.1+       |0.2.39+       |
|0.7.0+    | 0.0.7.13       |0.2.38        |
|0.6.0+    | 0.0.2.2        |              |


## 安装weex debugger [传送门](https://github.com/erha19/weex-debugger)

##### 前置工作 
如果未安装Debug Server, 在命令行执行 `npm install -g weex-toolkit` 既可以安装调试服务器, 运行命令 `weex debug` 就会启动DebugServer并打开一个调试页面. 页面下方会展示一个二维码, 这个二维码用于向App传递Server端的地址建立连接.

##### 开始调试
如果你的App客户端完成了以上步骤那么恭喜你已经接入完毕, 可以愉快的调试weex bundle了, 调试体验和网页调试一致!建议新手首先用官方的playground体验一下调试流程. 只需要启动app扫描chrome调试页面下方的第一个二维码即可建立与DebugServer的通信, chorome的调试页面将会列出连接成功的设备信息.

![devtools-main](https://img.alicdn.com/tps/TB13fwSKFXXXXXDaXXXXXXXXXXX-887-828.png "connecting (multiple) devices")

##### 主要步骤如下:
  1. 命令行运行 `weex debug` 启动debug server, chrome 将会打开一个网页, 在网页下方有一个二维码和简单的介绍.
  2. 启动App并确认打开调试模式（扫码或hardcode连接调试服务器. 
  3. 开始调试
---

## 科普

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
