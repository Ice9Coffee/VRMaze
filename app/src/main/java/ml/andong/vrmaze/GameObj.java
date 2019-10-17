package ml.andong.vrmaze;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.javagl.obj.Mtl;
import de.javagl.obj.MtlReader;
import de.javagl.obj.Obj;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjUtils;
import de.javagl.obj.ObjSplitting;

/**
 * GameObj =
 *      Transform   // Local坐标 -> World坐标
 *    + Mesh        // 顶点 面元 法线 纹理UV 等信息
 *    + Texture     // 纹理图片数据
 *    + Material    // 材质信息
 *    + Collider    // TODO: 物理引擎碰撞体
 *
 *    + others
 */
public class GameObj {
    private static final String TAG = "GameObj";

    private static int posAttr;
    private static int normAttr;
    private static int uvAttr;
    private static int modelAttr;
    private static int glAmbient;
    private static int glDiffuse;
    private static int glSpecular;
    private static int glShininess;


    private float[] myTransform;
    private float[] worldTransform;     // this is just a placeholder, GameObj DO NOT store it!
                                        // worldTrans = parentTrans * myTrans

    private int[] modelTransformVBO;
    private int maxDrawAmount;          // 指示modelTransformVBO的大小

    private Map<Material, Mesh> objParts;
    private Texture tex;

    public static void setGlConfig(int posAttr, int normAttr, int uvAttr, int modelAttr,
                              int glAmbient, int glDiffuse, int glSpecular, int glShininess) {
        GameObj.posAttr = posAttr;
        GameObj.normAttr = normAttr;
        GameObj.uvAttr = uvAttr;
        GameObj.modelAttr = modelAttr;
        GameObj.glAmbient = glAmbient;
        GameObj.glDiffuse = glDiffuse;
        GameObj.glSpecular = glSpecular;
        GameObj.glShininess = glShininess;
    }

    public GameObj(Context context, String modelDir, String objFilename, String textureFilename) {

        objParts = new HashMap<>();
        myTransform = Const.IdentityMatrix.clone();

        modelTransformVBO = new int[1];
        GLES30.glGenBuffers(1, modelTransformVBO, 0);
        maxDrawAmount = 0;

        Obj obj = loadObjFile(context, modelDir + objFilename);

        List<Mtl> allMtls = new ArrayList<Mtl>();
        for(String mtlFilename: obj.getMtlFileNames()) {
            List<Mtl> mtls = loadMtlFile(context, modelDir + mtlFilename);
            allMtls.addAll(mtls);
        }

        if(0 == allMtls.size()) {
            Mesh mesh = new Mesh(obj, posAttr, normAttr, uvAttr, modelAttr, modelTransformVBO[0]);
            objParts.put(Material.DEFAULT, mesh);
        }
        else {
            Map<String, Obj> mtlGroups = ObjSplitting.splitByMaterialGroups(obj);

            for (Map.Entry<String, Obj> e : mtlGroups.entrySet()) {
                String mtlName = e.getKey();
                Mtl mtl = findMtlByName(allMtls, mtlName);
                Material mmtl;
                if (null == mtl) {
                    Log.e(TAG, "mtl<" + mtlName + "> Not Found! when loading " + objFilename);
                    mmtl = Material.DEFAULT;
                }
                else {
                    mmtl = new Material(mtl);
                }
                Obj part = e.getValue();
                Mesh mesh = new Mesh(part, posAttr, normAttr, uvAttr, modelAttr, modelTransformVBO[0]);
                objParts.put(mmtl, mesh);
            }
        }

        if(textureFilename != null) {
            tex = new Texture(context, modelDir + textureFilename);
        }
        else {
            tex = new Texture(context, modelDir + "defaultTexture.png");
        }

    }



    public void setTexture(Texture tex) {
        this.tex = tex;
    }

    public void setLocalTransform(float[] newTransform) {
        myTransform = newTransform;
    }


    /*
    public void draw(float[] parentTransform) {

        Matrix.multiplyMM(worldTransform, 0, parentTransform, 0, myTransform, 0);

        // send model transform
        // GLES30.glUniformMatrix4fv(glModelUniform, 1, false, worldTransform, 0);
        for(int i=0; i<4; ++i) {
            int attr = modelAttr + i;
            GLES30.glVertexAttrib4fv(attr, worldTransform, 4 * i);
        }

        // bind texture
        tex.bind();

        for(Map.Entry<Material, Mesh> e: objParts.entrySet()) {
            // bind material
            Material mtl = e.getKey();
            mtl.bind(glAmbient, glDiffuse, glSpecular, glShininess);
            // render mesh
            Mesh mesh = e.getValue();
            mesh.draw();
        }

    }
    */


    // 更新amount个实例的transform
    public void setTransform(int amount, float[] parentTransform, int offset) {

        // 准备一批modelTransform
        if(amount > maxDrawAmount) {
            worldTransform = new float[amount * 16];
        }
        for(int i=0; i<amount; ++i) {
            Matrix.multiplyMM(worldTransform, i * 16, parentTransform, offset + i * 16, myTransform, 0);
        }
        FloatBuffer modelTransBuf = FloatBuffer.wrap(worldTransform);

        // 更新GPU上的modelTransform缓存
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, modelTransformVBO[0]);
        if(amount > maxDrawAmount) {
            maxDrawAmount = amount;
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, Const.sizeofFloat * modelTransBuf.limit(), modelTransBuf, GLES30.GL_DYNAMIC_DRAW);
        }
        else {
            GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, Const.sizeofFloat * modelTransBuf.limit(), modelTransBuf);
        }

        Util.checkGlError("GameObj.setTransform");
    }


    public void setTransform(float[] parentTransform, int offset) {
        setTransform(1, parentTransform, offset);
    }


    public void draw(int amount) {
        // bind texture
        tex.bind();

        for(Map.Entry<Material, Mesh> e: objParts.entrySet()) {
            // bind material
            Material mtl = e.getKey();
            mtl.bind(glAmbient, glDiffuse, glSpecular, glShininess);
            // render mesh
            Mesh mesh = e.getValue();
            mesh.draw(amount);
        }
    }

    public void draw() {
        draw(1);
    }


    public static Obj loadObjFile(Context ctx, String filePath) {
        InputStream in = null;
        Obj obj = null;
        try {
            in = ctx.getAssets().open(filePath);
            obj = ObjUtils.convertToRenderable(ObjReader.read(in));
        } catch (IOException e) {
            Log.e(TAG, "Error when loading obj file: " + filePath + "\n" + Log.getStackTraceString(e));
        } finally {
            if(in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error when closing obj file: " + filePath + "\n" + Log.getStackTraceString(e));
                }
            }
        }
        return obj;
    }

    public static List<Mtl> loadMtlFile(Context ctx, String filePath) {
        InputStream in = null;
        List<Mtl> ret = new ArrayList<Mtl>();
        try {
            in = ctx.getAssets().open(filePath);
            ret = MtlReader.read(in);
        } catch (IOException e) {
            Log.e(TAG, "Error when loading mtl file: " + filePath + "\n" + Log.getStackTraceString(e));
        } finally {
            if(in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error when closing mtl file: " + filePath + "\n" + Log.getStackTraceString(e));
                }
            }
        }
        return ret;
    }

    public static Mtl findMtlByName(Iterable<? extends Mtl> mtls, String name) {
        for(Mtl mtl: mtls) {
            if(mtl.getName().equals(name)) {
                return mtl;
            }
        }
        return null;
    }



}
