# VR-Maze

 虚拟现实技术 小作业（一）



## 





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

- Google VR SDK: https://developers.google.com/vr/reference/android/com/google/vr/sdk/base/package-summary 
- OpenGL: https://learnopengl-cn.github.io/ 
- OpenGL ES for Android: https://developer.android.google.cn/reference/android/opengl/package-summary
- Wavefront OBJ file loader: https://github.com/javagl/Obj