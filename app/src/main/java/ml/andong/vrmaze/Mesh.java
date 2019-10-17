package ml.andong.vrmaze;

import android.opengl.GLES30;
import de.javagl.obj.Obj;
import de.javagl.obj.ObjData;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;


public class Mesh {
    private static final String TAG = "Mesh";

    public int[] VAO;
    public int[] VBO;
    public int[] EBO;
    public int indicesCount;


    public Mesh(Obj obj, int posAttr, int normAttr, int uvAttr, int modelAttr, int modelVBO) {

        VAO = new int[1];
        VBO = new int[1];
        EBO = new int[1];

        FloatBuffer pos = ObjData.getVertices(obj);
        FloatBuffer norm = ObjData.getNormals(obj);
        FloatBuffer uv = ObjData.getTexCoords(obj, 2, true);
        ShortBuffer indices = ObjData.convertToShortBuffer(ObjData.getFaceVertexIndices(obj, 3));
        indicesCount = indices.limit();

        GLES30.glGenVertexArrays(1, VAO, 0);
        GLES30.glGenBuffers(1, VBO, 0);
        GLES30.glGenBuffers(1, EBO, 0);

        GLES30.glBindVertexArray(VAO[0]);

        // Send vertex data to buffer and bind to VAO.
        // Vertex data are organized in batch layout, NOT interleave layout.
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, VBO[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, Const.sizeofFloat * (pos.limit() + norm.limit() + uv.limit()), null, GLES30.GL_STATIC_DRAW);

        int offset = 0;
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, offset, Const.sizeofFloat * pos.limit(), pos);
        GLES30.glEnableVertexAttribArray(posAttr);
        GLES30.glVertexAttribPointer(posAttr, 3, GLES30.GL_FLOAT, false, 0, offset);
        offset += Const.sizeofFloat * pos.limit();

        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, offset, Const.sizeofFloat * norm.limit(), norm);
        GLES30.glEnableVertexAttribArray(normAttr);
        GLES30.glVertexAttribPointer(normAttr, 3, GLES30.GL_FLOAT, false, 0, offset);
        offset += Const.sizeofFloat * norm.limit();

        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, offset, Const.sizeofFloat * uv.limit(), uv);
        GLES30.glEnableVertexAttribArray(uvAttr);
        GLES30.glVertexAttribPointer(uvAttr, 2, GLES30.GL_FLOAT, false, 0, offset);
        // offset += Const.sizeofFloat * uv.limit();  // For future use, DO NOT forget add offset!


        // Send fragments data to buffer.
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, EBO[0]);
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, Const.sizeofShort * indices.limit(), indices, GLES30.GL_STATIC_DRAW);


        // Bind model transform VBO
        int sizeofVec4 = 4 * Const.sizeofFloat;
        int stride = 4 * sizeofVec4;
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, modelVBO);
        for(int i=0; i<4; ++i) {
            int attr = modelAttr + i;
            GLES30.glEnableVertexAttribArray(attr);
            GLES30.glVertexAttribPointer(attr, 4, GLES30.GL_FLOAT, false, stride, i * sizeofVec4);
            GLES30.glVertexAttribDivisor(attr, 1);
        }


        // Unbind VAO
        GLES30.glBindVertexArray(0);

        Util.checkGlError("create Mesh");
    }


    public void draw() {
        draw(1);
    }


    public void draw(int amount) {
        GLES30.glBindVertexArray(VAO[0]);
        GLES30.glDrawElementsInstanced(GLES30.GL_TRIANGLES, indicesCount, GLES30.GL_UNSIGNED_SHORT, 0, amount);
        GLES30.glBindVertexArray(0);

        Util.checkGlError("Mesh.draw");
    }

}
