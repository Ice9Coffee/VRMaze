# VRMaze

Yet Another Na!ve VR Maze Game...

虚拟现实技术 小作业(一)

## <span id="1">快速开始 - Quick Start</span>

从[这里](https://github.com/Ice-Cirno/VRMaze/releases)下载apk并安装。

Download release version apk from [here](https://github.com/Ice-Cirno/VRMaze/releases) and install.

启动app，将手机放入[Cardboard](https://arvr.google.com/cardboard/get-cardboard/)，开始游戏！

Launch it, put your phone into [Cardboard](https://arvr.google.com/cardboard/get-cardboard/), and EMJOY!

按下磁扣前进（或直接点击屏幕）。

Press Cardboard trigger (or just your phone screen) to move.

收集❤❤并找到出口处的旗帜🏁！

Collect ❤Hearts❤ and find the Flag🏁!

*（迷路时可以尝试飞行🛫 ！）*

*( If you get stuck, try to fly🛫 ! )*



## <span id="2">效果展示 - Preview</span>

*详见preview文件夹*

![起点](preview/preview1.gif)

![终点](preview/preview2.gif)



## <span id="3">依赖包 - Dependencies</span>

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

- `com.google.vr:sdk-base` ：使用`GvrActivity`和`GvrView.StereoRenderer`创建VR主活动；由于`1.200`版本的库无法解析下载地址，根据StackOverflow的说法，降级至`1.190.0`即可解决。
- `de.javagl:obj`：用于导入`.obj`3D模型文件以及对应的`.mtl`材质文件；一个模型可能包含多种材质，需要使用` ObjSplitting.splitByMaterialGroups() `，而该方法需要更新至`0.3.0`版本。





## <span id="4">实现流程 - Workflow</span>

首先，我参考Google VR SDK提供的样例项目`gvr-android-sdk`中的`sdk-hellovr`，熟悉VR项目的实现流程：继承`GvrActivity`、实现`GvrView.StereoRenderer`接口即可创建一个简单的双目立体视觉VR app。

其中需要实现的最主要的几个方法为：

- `onCreate`：进行`GvrView`、`gvrAudioEngine`以及其他变量的初始化。
- `onSurfaceCreated`：进行关于图形绘制的初始化，如编译链接shader、获取`attribution`和`uniform`的`location`、加载`obj`模型等。
- `onNewFrame`：进行绘制新帧前的准备工作，如根据`headTransform`处理视点变换、移动玩家位置、更新场景中动态物体的transform等。
- `onDrawEye`：进行渲染工作，由于VR程序需提供左右眼的视野，该函数每帧会被调用两次，这里根据`eyeView`完成从`head`到`eye`的视点变换，进行真正的渲染工作。
- `onCardboardTrigger`：当Carboard的磁扣被按下时，作出相应的响应。

-----

我先实现玩家在场景中的移动效果。`HeadTransform`提供了`getForwardVector`方法，可以获取到玩家面朝的方向；通过`SystemClock.elapsedRealtime`可以计算出游戏运行的帧率；`playerPos += playerForward[i] * PLAYER_VELOCITY / frameRate`即可更新玩家位置。但是，`onCardboardTrigger`仅在磁扣按下的瞬间被调用1次，不满足需求，我希望玩家按下磁扣时前进，松开时停下，使用基类`Activity`的`onTouchEvent`方法来实现。

-----

然后，我尝试向场景中添加新的模型并实现迷宫。

模型渲染方面，`sdk-hellovr`的原有实现中，一个模型由1个TexturedMesh和1张纹理贴图组成，但我在网上找到的模型大多数都具有多种材质信息而无纹理信息或只有局部带有纹理。于是我重构了原有的代码：增加了`Material`类实现材质效果，增加`GameObj`类对绘制的细节进行封装；采用组合的设计模式，一个`GameObj`由多个`Mesh`、相应的`Material`、`Texture`、以及其自身的`Transform`组合而成；同时，我需要重写Shader以增加对`Material`的支持，我将Shader的GLSL代码从原先的硬编码在主程序中调整为动态加载的独立文件，为了更好地优化程序运行效率，使用`#version 300 es`版本的GLSL编写。

迷宫实现方面，我使用json文件配置迷宫，迷宫的数据结构记录为一个二维字符数组，其中的字符代表该格是否可行走、是否有可拾取的道具等。迷宫整体相当于`GameObj`的一个`group`，它的`Transform`会影响其中所有的子`GameObj`。迷宫本身的实现并不复杂，但在渲染具有数百个方格的大型迷宫时，出现了严重的帧率下降。原先60fps仅剩15fps！在经过一番调查和研究后，我找到了问题所在：渲染函数调用需要大量的CPU和GPU之间的IO通信，而实例化绘制技术可以在一次渲染函数调用内绘制多个`Mesh`从而大幅减少IO时间。为使用实例化绘制，我修改了`Mesh`的绘制流程，使用`VAO`管理顶点，使用`VBO`将数据提前存入GPU，修改`draw`方法使其可以一次绘制多个对象。修改后，即便一帧内绘制上千个`Mesh`，也可稳定运行在60fps以上。解决方法及过程请参见[遇到的困难 - Challenges](#6)一节。

-----

最后，我为迷宫增加了碰撞检测，以阻止玩家穿墙 和 判定玩家对道具的拾取。将玩家位置坐标变换至maze坐标系后，用AABB包围盒判断玩家是否撞墙，用简单的球形碰撞体检测玩家对道具的拾取。



## <span id="5">细节 - Details</span>

### 环境

Android Studio 3.5

依赖包：com.google.vr:sdk-base:1.190.0, de.javagl:obj:0.3.0

测试机：HUAWEI SEA-AL10, samsung SM-N9200

### 浏览

通过继承`GvrActivity`、实现`GvrView.StereoRenderer`接口渲染双目立体视觉；`headTransform`和`eyeView`包装了陀螺仪信息，可以从中获取到两只眼睛的视点变换。

### 迷宫

`Maze`从配置文件`maze.json`中读取迷宫的大小、起点坐标、迷宫的结构信息，从`model/`文件夹读取需要的3D模型及其材质，创建相应的`GameObj`并为其设置`Transform`；在`draw`循环中，还需更新动态物件的`Transform`， 之后进行实例化绘制。

* `GameObj`是对游戏内物件的封装，它需要管理物件的本地坐标变换，一个物件可能具有多种材质、每种材质有相应的网格，还可以有纹理贴图、碰撞体等，采用组合的设计模式：`GameObj = Transform + Mesh + Texture + Material + Collider + α` 。

### 交互

通过`onTouchEvent`获取用户输入，通过`HeadTransform.getForwardVector`获取用户前进方向，用户按下磁扣（或按住屏幕）即可前进。



## <span id="6">遇到的困难 - Challenges</span>

#### 大量绘制模型时帧数严重下降

解决：实例化绘制。

样例项目的框架中，每次绘制一个Mesh都要向GPU发送顶点和变换数据，当需要绘制上百个Mesh时，大量的渲染函数调用需要大量的IO通信时间，而这其中有很多不必要的重复数据。例如，一个Mesh的顶点数据基本上是不变的，没有必要每次绘制都发送一遍；迷宫中需要绘制大量相同的Mesh，而它们仅仅是位置有所不同。

因此，可以进行以下两点改进：利用现代OpenGL的`VBO`缓存对象，将顶点数据在创建时一次性发送至GPU；利用实例化绘制，在一个渲染函数调用中使用多个不同的Transform（同样也需要使用`VBO`将数据存入GPU），绘制多个相同的Mesh。

相关代码见`Mesh`的构造函数（一次性发送顶点数据）、`GameObj.setTransform()`（更新amount个实例的transform）以及`Mesh.draw()`和`GameObj.draw()`（实例化绘制多个相同的Mesh）。

解决后，在麒麟980处理器上的帧率由~15fps提升至60fps以上。



#### 运行一段时间后帧数下降

解决：发现在渲染循环中有几处为临时变量申请内存的`new`操作，持续运行时会有大量重复的空间申请和释放，可能触发了JVM的垃圾回收机制导致运行效率下降，提前在构造函数中预分配好临时变量的空间即可解决。



#### 模型光照效果异常

解决：和助教讨论后，使用MeshLab查看模型的法线，发现模型提供的法线x轴和z轴均给反了，手动调整后得到了正确的光照效果。



*PS: 虽然已修过图形学基础，但该课仅对图形学的数学基础、光照模型等做了介绍，大作业是实现光线追踪算法（光子映射 etc.），对现代OpenGL编程以及GLSL介绍甚少，因此完成本项目时在学习OpenGL上花费了不少时间。*



## <span id="7">参考 - Reference</span>

- Google VR SDK: https://developers.google.com/vr/develop/android/get-started 
- Google VR SDK Doc: https://developers.google.com/vr/reference/android/com/google/vr/sdk/base/package-summary 
- Learn OpenGL: https://learnopengl-cn.github.io/ 
- OpenGL ES for Android: https://developer.android.google.cn/reference/android/opengl/package-summary
- Wavefront OBJ file loader: https://github.com/javagl/Obj
- Free 3D model:  https://creazilla.com/zh-CN/sections/3-3d
- Free 3D model:  https://free3d.com/3d-models