package ml.andong.vrmaze;

import static android.opengl.GLU.gluErrorString;

import android.content.Context;
import android.content.res.AssetManager;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Utility functions. */
/* package */ class Util {
    private static final String TAG = "Util";

    /** Debug builds should fail quickly. Release versions of the app should have this disabled. */
    private static final boolean HALT_ON_GL_ERROR = true;

    /** Class only contains static methods. */
    private Util() {}

    /**
     * Checks GLES30.glGetError and fails quickly if the state isn't GL_NO_ERROR.
     *
     * @param label Label to report in case of error.
     */
    public static void checkGlError(String label) {
        int error = GLES30.glGetError();
        int lastError;
        if (error != GLES30.GL_NO_ERROR) {
            do {
                lastError = error;
                Log.e(TAG, label + ": glError " + gluErrorString(lastError));
                error = GLES30.glGetError();
            } while (error != GLES30.GL_NO_ERROR);

            if (HALT_ON_GL_ERROR) {
                throw new RuntimeException("glError " + gluErrorString(lastError));
            }
        }
    }

    /**
     * Builds a GL shader program from vertex & fragment shader code. The vertex and fragment shaders
     * are passed as arrays of strings in order to make debugging compilation issues easier.
     *
     * @param vertexCode GLES30 vertex shader program.
     * @param fragmentCode GLES30 fragment shader program.
     * @return GLES30 program id.
     */
    public static int compileProgram(String vertexCode, String fragmentCode) {
        checkGlError("Start of compileProgram");
        // prepare shaders and OpenGL program
        int vertexShader = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER);
        GLES30.glShaderSource(vertexShader, vertexCode);
        GLES30.glCompileShader(vertexShader);
        Log.d(TAG, "Vertex shader info: " + GLES30.glGetShaderInfoLog(vertexShader));
        checkGlError("Compile vertex shader");

        int fragmentShader = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER);
        GLES30.glShaderSource(fragmentShader, fragmentCode);
        GLES30.glCompileShader(fragmentShader);
        Log.d(TAG, "Fragment shader info: " + GLES30.glGetShaderInfoLog(fragmentShader));
        checkGlError("Compile fragment shader");

        int program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragmentShader);

        // Link and check for errors.
        GLES30.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES30.GL_TRUE) {
            String errorMsg = "Unable to link shader program: \n" + GLES30.glGetProgramInfoLog(program);
            Log.e(TAG, errorMsg);
            if (HALT_ON_GL_ERROR) {
                throw new RuntimeException(errorMsg);
            }
        }
        checkGlError("End of compileProgram");

        return program;
    }


    /**
     * Computes the angle between two vectors; see
     * https://en.wikipedia.org/wiki/Vector_projection#Definitions_in_terms_of_a_and_b.
     */
    public static float angleBetweenVectors(float[] vec1, float[] vec2) {
        float cosOfAngle = dotProduct(vec1, vec2) / (vectorNorm(vec1) * vectorNorm(vec2));
        return (float) Math.acos(Math.max(-1.0f, Math.min(1.0f, cosOfAngle)));
    }

    private static float dotProduct(float[] vec1, float[] vec2) {
        return vec1[0] * vec2[0] + vec1[1] * vec2[1] + vec1[2] * vec2[2];
    }

    private static float vectorNorm(float[] vec) {
        return Matrix.length(vec[0], vec[1], vec[2]);
    }


    public static String loadFile(String filepath, Context context) {
        InputStream is = null;
        try {
            is = context.getAssets().open(filepath);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.e(TAG, "Load File Error: " + filepath + "\n" + Log.getStackTraceString(e));
            return null;
        } finally {
            if(is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(TAG, "Load File Error when closing file: " + filepath + "\n" + Log.getStackTraceString(e));
                }
            }
        }
    }

}

