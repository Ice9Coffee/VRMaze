# VRMaze

Yet Another Na!ve VR Maze Game...

虚拟现实技术 小作业(一)

## 快速开始 - Quick Start

从[这里](https://github.com/Ice-Cirno/VRMaze/releases)下载apk并安装。

Download release version apk from [here](https://github.com/Ice-Cirno/VRMaze/releases) and install.

启动app，将手机放入[CardBoard](https://arvr.google.com/cardboard/get-cardboard/)，开始游戏！

Launch it, put your phone into [Cardboard](https://arvr.google.com/cardboard/get-cardboard/), and EMJOY!

按下磁扣前进（或直接点击屏幕）。

Press Cardboard trigger (or just your phone screen) to move.

收集❤❤并找到出口处的旗帜🏁！

Collect ❤Hearts❤ and find the Flag🏁!

*（迷路时可以尝试飞行🛫 ！）*

*( If you get stuck, try to fly🛫 ! )*



## 效果展示 - Preview





## 实现流程 - Workflow

首先，我参考Google VR SDK提供的样例项目`gvr-android-sdk`中的`sdk-hellovr`，熟悉VR项目的实现流程：继承`GvrActivity`实现`GvrView.StereoRenderer`接口即可创建一个简单的VR app。

其中需要实现的最主要的几个方法为：

- 



## 实现细节 - Details

### 环境



### 浏览



### 迷宫



### 交互









## 依赖包 - Dependencies

依赖包已包含在`build.gradle`中，直接在Android Studio中使用gradle构建即可。

```
dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation 'androidx.appcompat:appcompat:1.0.2'
    implementation 'androidx.legacy:legacy-support-v4:1.0.0'
    implementation 'androidx.constraintlayout:constraintlayout:1.1.3'
    testImplementation 'junit:junit:4.12'
    androidTestImplementation 'androidx.test:runner:1.1.1'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.1.1'

    implementation 'com.google.vr:sdk-base:1.190.0'
    implementation 'de.javagl:obj:0.3.0'
}
```

主要依赖包为：

- `com.google.vr:sdk-base` ：使用`GvrActivity`和`GvrView.StereoRenderer`创建VR主活动；由于`1.200`版本的库无法解析下载地址，根据StackOverflow的说法，更换至`1.190.0`即可解决。
- `de.javagl:obj`：用于导入`.obj`3D模型文件以及对应的`.mtl`材质文件；一个模型可能包含多种材质，需要使用` ObjSplitting.splitByMaterialGroups() `，而方法需要更新版本至`0.3.0`。



## 参考 - Reference

- Google VR SDK: https://developers.google.com/vr/develop/android/get-started 
- Google VR SDK Doc: https://developers.google.com/vr/reference/android/com/google/vr/sdk/base/package-summary 
- OpenGL: https://learnopengl-cn.github.io/ 
- OpenGL ES for Android: https://developer.android.google.cn/reference/android/opengl/package-summary
- Wavefront OBJ file loader: https://github.com/javagl/Obj