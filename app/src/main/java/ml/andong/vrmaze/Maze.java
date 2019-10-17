package ml.andong.vrmaze;

import android.content.Context;
import android.opengl.Matrix;
import android.util.Log;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;



/**
 * Maze 大小为sizeX * sizeY
 * 迷宫坐标系下，从(0, 0, 0)到(sizeY, 0, sizeX)
 * 注意：假设遍历时使用 i: 0~sizeX, j: 0~sizeY，
 * 那么i对应-z轴，j对应-x轴
 *
 * 可以设置mazeTransform将迷宫整体设置到世界坐标系的任意位置
 */
public class Maze {
    private static final String TAG = "Maze";

    private static final String mazeConfigFile = "maze.json";

    private static final String modelDir = "model/";
    private static final String blockObj = "block.obj";
    private static final String wallObj = "blockQuarter.obj";
    private static final String flowerObj = "flowers.obj";
    private static final String flagObj = "flag.obj";
    private static final String heartObj = "heart.obj";

    private GameObj block;
    private GameObj wall;
    private GameObj flower;
    private GameObj flag;
    private GameObj heart;
    private Set<Pair<Integer, Integer>> heartPos;

    private String[] maze;
    private int sizeX;
    private int sizeY;
    private int startX;
    private int startY;

    private float[] mazeTransform;
    private float[] invMazeTransform;
    private Map<GameObj, FloatBuffer> childTransform;

    // 临时变量提前申请空间，避免垃圾回收影响效率
    private float[] mazePos;
    private int[] intPos;
    private float[] distance;
    private float[] tmpMatrix;
    private Pair<Integer, Integer> ij;



    public float rotDeg;

    private float[] randomTable;


    public Maze() {

        mazeTransform = new float[16];
        invMazeTransform = new float[16];
        Matrix.setIdentityM(mazeTransform, 0);
        Matrix.setIdentityM(invMazeTransform, 0);

        childTransform = new HashMap<>();

        heartPos = new HashSet<>();
        rotDeg = 0;

        mazePos = new float[4];
        intPos = new int[3];
        distance = new float[3];
        ij = new Pair<>(0, 0);
        tmpMatrix = new float[16];


        Random random = new Random();
        randomTable = new float[100];
        for(int i=0; i<100; ++i) {
            randomTable[i] = (2.0f * random.nextFloat()) - 1.0f;
        }
    }

    public float[] getStartPos() {
        float[] pos = new float[] {
                - startY,
                1.6f,
                - startX,
                1.0f
        };
        float[] ret = new float[4];
        Matrix.multiplyMV(ret, 0, mazeTransform, 0, pos, 0);
        return ret;
    }

    public boolean checkIsInWall(float[] pos) {
        Matrix.multiplyMV(mazePos, 0, invMazeTransform, 0, pos, 0);
        int i = Math.round(-mazePos[2]);
        int j = Math.round(-mazePos[0]);
        float h = mazePos[1];
        try {
            return 0.0f <= h && h <= 2.1f && maze[i].charAt(j) == '#';
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    public boolean checkEatHeart(float[] pos) {
        Matrix.multiplyMV(mazePos, 0, invMazeTransform, 0, pos, 0);
        for(int i=0; i<3; ++i) {
            intPos[i] = Math.round(mazePos[i]);
        }
        ij = new Pair<>(-intPos[2], -intPos[0]);
        distance[0] = mazePos[0] - intPos[0];
        distance[1] = mazePos[1] - 1.5f;
        distance[2] = mazePos[2] - intPos[2];
        if(heartPos.contains(ij) && Matrix.length(distance[0], distance[1], distance[2]) < 0.5f) {
            heartPos.remove(ij);
            return true;
        }
        return false;
    }

    public void setTransform(float[] newTransform) {
        mazeTransform = newTransform;
        Matrix.invertM(invMazeTransform, 0, mazeTransform, 0);

        float[] worldTransform = mazeTransform.clone();
        float x, z, r;

        heartPos.clear();
        for(FloatBuffer buf: childTransform.values()) {
            buf.clear();
        }

        for(int i = 0; i < sizeX; ++i) {
            for(int j = 0; j < sizeY; ++j) {
                char code = maze[i].charAt(j);
                if('0' == code) {
                    heartPos.add(new Pair<>(i, j));
                }
                if('g' == code) {
                    childTransform.get(flag).put(worldTransform);
                }
                if(' ' == code || 'g' == code || '0' == code) {
                    for(int k = 0; k < 2; ++k) {
                        x = randomTable[((i + j     + 2 * k) * 6) % 100] * 0.45f;
                        z = randomTable[((i + j + 1 + 2 * k) * 6) % 100] * 0.45f;
                        r = randomTable[((i + j + 2 + 2 * k) * 6) % 100] * 180f;
                        Matrix.translateM(tmpMatrix, 0, worldTransform, 0, x, 0.0f, z);
                        Matrix.rotateM(tmpMatrix, 0, r, 0.0f, 1.0f, 0.0f);
                        childTransform.get(flower).put(tmpMatrix);
                    }
                    childTransform.get(block).put(worldTransform);
                }
                if('#' == code) {
                    childTransform.get(wall).put(worldTransform);
                }

                Matrix.translateM(worldTransform, 0, -1.0f, 0, 0);
                Util.checkGlError("draw Maze Block at (" + i + ", " + j + ")");
            }
            Matrix.translateM(worldTransform, 0, sizeY * 1.0f, 0, -1.0f);
        }

        for(Map.Entry<GameObj, FloatBuffer> e: childTransform.entrySet()) {
            GameObj obj = e.getKey();
            FloatBuffer buf = e.getValue();
            int amount = buf.position() / 16;
            if(amount > 0) {
                obj.setTransform(amount, buf.array(), 0);
            }
        }

    }


    public void load(Context ctx) {
        // 必须先loadMaze获取到sizeX sizeY后再loadObj
        loadMaze(ctx);
        loadObj(ctx);
    }

    private void loadMaze(Context ctx) {

        String json = Util.loadFile(mazeConfigFile, ctx);
        try {
            JSONObject mazeConfig = new JSONObject(json);
            sizeX = mazeConfig.getInt("sizeX");
            sizeY = mazeConfig.getInt("sizeY");
            startX = mazeConfig.getInt("startX");
            startY = mazeConfig.getInt("startY");
            maze = new String[sizeX];

            JSONArray mazeJSONArray = mazeConfig.getJSONArray("maze");
            for(int i=0; i<sizeX; ++i) {
                maze[i] = mazeJSONArray.getString(i);
            }
        } catch (JSONException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }


    private void loadObj(Context ctx) {
        block = new GameObj(ctx, modelDir, blockObj, null);
        wall = new GameObj(ctx, modelDir, wallObj, null);
        flower = new GameObj(ctx, modelDir, flowerObj, null);
        flag = new GameObj(ctx, modelDir, flagObj, null);
        heart = new GameObj(ctx, modelDir, heartObj, null);

        int childTransformSize = 16 * sizeX * sizeY;
        float[] t;

        t = new float[16];
        Matrix.setIdentityM(t, 0);
        Matrix.scaleM(t, 0, 0.9f, 1.0f, 0.9f);
        Matrix.translateM(t, 0, -0.5f, 0.0f, 0.5f);
        block.setLocalTransform(t);
        childTransform.put(block, FloatBuffer.allocate(childTransformSize));

        t = new float[16];
        Matrix.setIdentityM(t, 0);
        Matrix.scaleM(t, 0, 1.7f, 2.0f, 1.7f);
        Matrix.translateM(t, 0, -0.25f, 0.0f, 0.25f);
        wall.setLocalTransform(t);
        childTransform.put(wall, FloatBuffer.allocate(childTransformSize));

        t = new float[16];
        Matrix.setIdentityM(t, 0);
        Matrix.translateM(t, 0, 0.0f, 1.0f, 0.0f);
        Matrix.scaleM(t, 0, 0.25f, 0.25f, 0.25f);
        Matrix.translateM(t, 0, -0.5f, 0.0f, 0.5f);
        flower.setLocalTransform(t);
        childTransform.put(flower, FloatBuffer.allocate(childTransformSize * 2));


        t = new float[16];
        Matrix.setIdentityM(t, 0);
        Matrix.translateM(t, 0, 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(t, 0, -90.0f, 0.0f, 1.0f, 0.0f);
        Matrix.scaleM(t, 0, 1.0f, 1.5f, 1.0f);
        Matrix.translateM(t, 0, -0.5f, 0.0f, 0.5f);
        flag.setLocalTransform(t);
        childTransform.put(flag, FloatBuffer.allocate(childTransformSize));


        t = new float[16];
        Matrix.setIdentityM(t, 0);
        Matrix.translateM(t, 0, -0.5f, 1.0f, 0.5f);
        heart.setLocalTransform(t);
        childTransform.put(heart, FloatBuffer.allocate(childTransformSize));

    }


    public void draw() {

        FloatBuffer heartTrans = childTransform.get(heart);
        heartTrans.clear();

        float[] t = Const.IdentityMatrix.clone();
        Matrix.rotateM(t, 0, rotDeg, 0, 1.0f, 0);
        for(Pair<Integer, Integer> p: heartPos) {
            // 平移到maze的指定位置
            t[12] = -p.second;
            t[14] = -p.first;
            Matrix.multiplyMM(tmpMatrix, 0, mazeTransform, 0, t, 0);
            heartTrans.put(tmpMatrix);
        }
        heart.setTransform(heartTrans.position() / 16, heartTrans.array(), 0);
        // Log.d(TAG, "heart amount = " + (heartTrans.position() / 16));

        for(Map.Entry<GameObj, FloatBuffer> e: childTransform.entrySet()) {
            int amount = e.getValue().position() / 16;
            e.getKey().draw(amount);
        }

    }

    public void increaseRotDeg(float delta) {
        rotDeg += delta;
        rotDeg -= Math.floor(rotDeg / 360.0f) * 360.0f;
    }

}
