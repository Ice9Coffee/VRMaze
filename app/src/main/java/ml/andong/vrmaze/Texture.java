package ml.andong.vrmaze;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.IOException;

/** A texture, meant for use with Mesh. */
/* package */ class Texture {
  private static final String TAG = "Texture";

  private final int[] textureId = new int[1];

  /**
   * Initializes the texture.
   *
   * @param context Context for loading the texture file.
   * @param texturePath Path to the image to use for the texture.
   */
  public Texture(Context context, String texturePath) {
    GLES30.glGenTextures(1, textureId, 0);
    bind();
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
    GLES30.glTexParameteri(
        GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_NEAREST);
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);

    try {
      Bitmap textureBitmap = BitmapFactory.decodeStream(context.getAssets().open(texturePath));
      GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, textureBitmap, 0);
      textureBitmap.recycle();
    } catch (IOException e) {
      Log.e(TAG, "Error when loading tex file: " + texturePath + "\n" + Log.getStackTraceString(e));
    }

    GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
  }

  /** Binds the texture to GL_TEXTURE0. */
  public void bind() {
    GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId[0]);
  }
}
