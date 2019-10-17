package ml.andong.vrmaze;

import android.opengl.GLES30;
import android.util.Log;

import de.javagl.obj.FloatTuple;
import de.javagl.obj.Mtl;

public class Material {
    private static final String TAG = "Material";

    public static final Material DEFAULT = new Material (
            new float[] {1.0f, 1.0f, 1.0f},
            new float[] {1.0f, 1.0f, 1.0f},
            new float[] {0.0f, 0.0f, 0.0f},
            100.0f
    );

    float[] ambient;
    float[] diffuse;
    float[] specular;
    float shininess;

    public Material(Mtl mtl) {
        ambient = tuple_to_float3(mtl.getKa());
        diffuse = tuple_to_float3(mtl.getKd());
        specular = tuple_to_float3(mtl.getKs());
        shininess = mtl.getNs();
        printLogInfo();
    }

    public Material(float[] ambient, float[] diffuse, float[] specular, float shininess) {
        this.ambient = ambient;
        this.diffuse = diffuse;
        this.specular = specular;
        this.shininess = shininess;
        printLogInfo();
    }

    public void bind(int glAmbient, int glDiffuse, int glSpecular, int glShininess) {
        GLES30.glUniform3f(glAmbient, ambient[0], ambient[1], ambient[2]);
        GLES30.glUniform3f(glDiffuse, diffuse[0], diffuse[1], diffuse[2]);
        GLES30.glUniform3f(glSpecular, specular[0], specular[1], specular[2]);
        GLES30.glUniform1f(glShininess, shininess);
    }

    private float[] tuple_to_float3(FloatTuple tuple) {
        return new float[] {
            tuple.getX(), tuple.getY(), tuple.getZ()
        };
    }

    public void printLogInfo() {
        Log.d(TAG, "ambient = (" + ambient[0] + ", " + ambient[1] + ", " + ambient[2] + ")"
                + " diffuse = (" + diffuse[0] + ", " + diffuse[1] + ", " + diffuse[2] + ")"
                + " specular = (" + specular[0] + ", " + specular[1] + ", " + specular[2] + ")"
                + " shininess = " + shininess );
    }

}
