package ml.andong.vrmaze;

import androidx.core.math.MathUtils;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.view.MotionEvent;

import com.google.vr.sdk.audio.GvrAudioEngine;
import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import com.google.vr.ndk.base.Properties;

import javax.microedition.khronos.egl.EGLConfig;

public class MainActivity extends GvrActivity implements GvrView.StereoRenderer {
    private static final String TAG = "MainActivity";

    private GvrAudioEngine gvrAudioEngine;
    private Properties gvrProperties;


    // OpenGL
    private final String objVertexShaderFile = "shader/MyVert.glsl";
    private final String objFragmentShaderFile = "shader/MyFrag.glsl";
    private String objVertexShader;
    private String objFragmentShader;

    private int glProgram;

    private int glPosAttr;
    private int glUvAttr;
    private int glNormAttr;

    private int glModelAttr;
    private int glViewUniform;
    private int glProjectionUniform;

    private int glMtlAmbient;
    private int glMtlDiffuse;
    private int glMtlSpecular;
    private int glMtlShininess;

    private int glLightPos;
    private int glLightAmbient;
    private int glLightDiffuse;
    private int glLightSpecular;


    // Game Global Data
    private long lastFrameTime;
    private float frameRate;
    private float xMin;
    private float xMax;
    private float yMin;
    private float yMax;
    private float zMin;
    private float zMax;
    private float[] min;
    private float[] max;
    private final float DEFAULT_FLOOR_HEIGHT = -1.6f;


    // Game Camera
    private float[] camera;         // 相机 LookAtM
    private float[] view;           // 眼睛的视角
    private float[] headView;
    private float[] projectionM;    // 投影变换（透视）
    private static final float Z_NEAR = 0.01f;
    private static final float Z_FAR = 50.0f;

    // Player
    private float[] playerPos;
    private float[] playerPos2;     // buffer
    private float[] playerForward;
    private boolean playerIsWalking;
    private final float PLAYER_VELOCITY = 1.5f;

    // GameObjs
    private Maze maze;
    private GameObj room;
    private GameObj coin;
    // private float coinRotateDeg;
    private float[] coinTransform;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate");


        initGvrView();


        // OpenGL
        objVertexShader = Util.loadFile(objVertexShaderFile, this);
        objFragmentShader = Util.loadFile(objFragmentShaderFile, this);

        // Game Global Data
        lastFrameTime = SystemClock.elapsedRealtime();  // Create的时刻
        frameRate = 60.0f;  // 启动时假定为60fps，避免第一帧计算出错，实际帧率在onNewFrame内计算。
        xMin = -12.5f;
        xMax = +12.5f;
        yMin = DEFAULT_FLOOR_HEIGHT + 1.3f;
        yMax = +5.0f;
        zMin = -12.5f;
        zMax = +12.5f;
        min = new float[] {xMin, yMin, zMin};
        max = new float[] {xMax, yMax, zMax};

        // Game Camera
        camera = new float[16];
        view = new float[16];
        headView = new float[16];
        projectionM = new float[16];

        // Player
        playerPos = new float[] {0.0f, 0.0f, 0.0f, 1.0f};
        playerPos2 = new float[] {0.0f, 0.0f, 0.0f, 1.0f};
        playerForward = new float[] {0.0f, 0.0f, -1.0f};
        playerIsWalking = false;

        // GameObjs
        maze = new Maze();
        coinTransform = new float[16];

        // 3D audio engine
        gvrAudioEngine = new GvrAudioEngine(this, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);

    }

    public void initGvrView() {
        setContentView(R.layout.activity_main);

        GvrView gvrView = (GvrView) findViewById(R.id.gvr_view);
        // redSize, greenSize, blueSize, alphaSize, depthSize, stencilSize
        gvrView.setEGLConfigChooser(8,8,8,8,16,8);

        gvrView.setRenderer(this);

        // 显示插入CardBoard的提示信息
        gvrView.setTransitionViewEnabled(true);

        // 使用CardBoard API兼容DayDreaming
        gvrView.enableCardboardTriggerEmulation();

        if (gvrView.setAsyncReprojectionEnabled(true)) {
            // Async reprojection decouples the app framerate from the display framerate,
            // allowing immersive interaction even at the throttled clockrates set by
            // sustained performance mode.
            AndroidCompat.setSustainedPerformanceMode(this, true);
        }

        setGvrView(gvrView);
        gvrProperties = gvrView.getGvrApi().getCurrentProperties();
    }



    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {
        Log.i(TAG, "onSurfaceCreated");

        GLES30.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

        glProgram = Util.compileProgram(objVertexShader, objFragmentShader);

        glPosAttr = GLES30.glGetAttribLocation(glProgram, "a_Position");
        glUvAttr = GLES30.glGetAttribLocation(glProgram, "a_UV");
        glNormAttr = GLES30.glGetAttribLocation(glProgram, "a_Normal");
        glModelAttr = GLES30.glGetAttribLocation(glProgram, "a_Model");

        glViewUniform = GLES30.glGetUniformLocation(glProgram, "u_View");
        glProjectionUniform = GLES30.glGetUniformLocation(glProgram, "u_Projection");

        glMtlAmbient = GLES30.glGetUniformLocation(glProgram, "material.ambient");
        glMtlDiffuse = GLES30.glGetUniformLocation(glProgram, "material.diffuse");
        glMtlSpecular = GLES30.glGetUniformLocation(glProgram, "material.specular");
        glMtlShininess = GLES30.glGetUniformLocation(glProgram, "material.shininess");

        glLightPos = GLES30.glGetUniformLocation(glProgram, "light.position");
        glLightAmbient = GLES30.glGetUniformLocation(glProgram, "light.ambient");
        glLightDiffuse = GLES30.glGetUniformLocation(glProgram, "light.diffuse");
        glLightSpecular = GLES30.glGetUniformLocation(glProgram, "light.specular");


        GameObj.setGlConfig(glPosAttr, glNormAttr, glUvAttr,
                glModelAttr, glMtlAmbient, glMtlDiffuse, glMtlSpecular, glMtlShininess);

        Util.checkGlError("Object program params");


        // set static point light
        GLES30.glUseProgram(glProgram);
        GLES30.glUniform4f(glLightPos, 0.0f, 5.0f, 0.0f, 1.0f);
        GLES30.glUniform3f(glLightAmbient, 0.4f, 0.4f, 0.4f);
        GLES30.glUniform3f(glLightDiffuse, 1.0f, 0.95f, 0.9f);
        GLES30.glUniform3f(glLightSpecular, 0.5f, 0.5f, 0.5f);

        Util.checkGlError("Set Static Light");


        // TODO: new a Thread to play music


        Util.checkGlError("onSurfaceCreated");


        // load mesh and texture
        maze.load(this);

        room = new GameObj(this, "model/", "CubeRoom.obj", "CubeRoom_BakedDiffuse.png");
        coin = new GameObj(this, "model/", "coin.obj", "coin-texture.png");

        // set models' local transform
        float[] t;

        t = new float[16];
        Matrix.setIdentityM(t, 0);
        Matrix.translateM(t, 0, 12.0f, DEFAULT_FLOOR_HEIGHT, 12.0f);
        maze.setTransform(t);

        t = new float[16];
        Matrix.setIdentityM(t, 0);
        Matrix.translateM(t, 0, 0, DEFAULT_FLOOR_HEIGHT + 0.1f, 0);
        Matrix.scaleM(t, 0, 3.0f, 3.0f, 3.0f);
        room.setLocalTransform(t);
        room.setTransform(Const.IdentityMatrix, 0);

        t = new float[16];
        Matrix.setIdentityM(t, 0);
        Matrix.rotateM(t, 0, 90.0f, 1.0f, 0, 0);
        Matrix.scaleM(t, 0, 0.5f, 0.1f, 0.5f);
        coin.setLocalTransform(t);

        // Init models' world transform
        Matrix.setIdentityM(coinTransform, 0);
        Matrix.translateM(coinTransform, 0, 0.0f, 0.0f, 0.0f);
        coin.setTransform(coinTransform, 0);

        // set player
        playerPos = maze.getStartPos();

        Util.checkGlError("onSurfaceCreated end");
    }


    // 绘制新帧前的准备工作：
    //      处理视点变换
    //      移动玩家位置
    @Override
    public void onNewFrame(HeadTransform headTransform) {

        headTransform.getHeadView(headView, 0);
        headTransform.getForwardVector(playerForward, 0);

        long frameTime = SystemClock.elapsedRealtime();
        frameRate = 1000.0f / (frameTime - lastFrameTime);
        lastFrameTime = frameTime;

        // 计算player的位置，得到camera变换
        if(playerIsWalking) {
            // Update player position

            for(int i=0; i<3; ++i) {
                playerPos2[i] = playerPos[i] + playerForward[i] * PLAYER_VELOCITY / frameRate;
                if(!maze.checkIsInWall(playerPos2)) {
                    playerPos[i] = MathUtils.clamp(playerPos2[i], min[i], max[i]);
                }
            }

            Log.d(TAG, "Player Pos=(" + playerPos[0] + "," + playerPos[1] + "," + playerPos[2] + ")");
            Log.d(TAG, "Player Velocity=" + PLAYER_VELOCITY);
            Log.d(TAG, "Frame Rate=" + frameRate);

            // Maze.heart check eat
            if ( maze.checkEatHeart(playerPos) ) {
                playSE("audio/EatHeartSE.ogg");
            }
        }
        Matrix.setLookAtM(camera, 0,
                playerPos[0], playerPos[1], playerPos[2],
                playerPos[0], playerPos[1], playerPos[2] - 1.0f,
                0.0f, 1.0f, 0.0f);



        // Coin spin
        Matrix.rotateM(coinTransform, 0, 90.0f / frameRate, 0, 1.0f, 0);
        coin.setTransform(coinTransform, 0);

        // Maze.heart spin
        maze.increaseRotDeg(90.0f / frameRate);


        Util.checkGlError("onNewFrame");
    }


    // 绘制一只眼睛的视界（VR有两只眼睛，需要从不同位置绘制两次）
    @Override
    public void onDrawEye(Eye eye) {

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        // camera -> view
        Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera, 0);
        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);

        // 向着色器传递view和projection
        GLES30.glUseProgram(glProgram);
        GLES30.glUniformMatrix4fv(glViewUniform, 1, false, view, 0);
        GLES30.glUniformMatrix4fv(glProjectionUniform, 1, false, perspective, 0);


        room.draw();
        Util.checkGlError("drawRoom");
        coin.draw();
        Util.checkGlError("drawCoin");
        maze.draw();
        Util.checkGlError("drawMaze");
    }


    @Override
    public void onFinishFrame(Viewport viewport) {
        // do nothing
    }



    @Override
    public void onCardboardTrigger() {
        Log.i(TAG, "onCardboardTrigger");
        super.onCardboardTrigger();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if(MotionEvent.ACTION_MOVE == action) {
            playerIsWalking = true;
        }
        else if (MotionEvent.ACTION_UP == action) {
            playerIsWalking = false;
        }
        Log.d(TAG, "onTouchEvent, action=" + action + ", playerIsWalking=" + playerIsWalking);
        return super.onTouchEvent(event);
    }




    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");

        playerIsWalking = false;
        gvrAudioEngine.pause();
        super.onPause();
    }


    @Override
    protected void onResume() {
        super.onResume();
        gvrAudioEngine.resume();
    }


    @Override
    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
        playerIsWalking = false;
    }


    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged " + width + "," + height);
    }


    private void playSE(String filepath) {
        int id = gvrAudioEngine.createStereoSound(filepath);
        gvrAudioEngine.playSound(id, /*loop=*/false);
    }

}
