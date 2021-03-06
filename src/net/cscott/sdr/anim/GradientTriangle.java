package net.cscott.sdr.anim;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jme.math.FastMath;
import com.jme.math.Vector2f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.TriMesh;
import com.jme.scene.batch.TriangleBatch;
import com.jme.util.geom.BufferUtils;

/**
 * {@link GradientTriangle} is a triangle shape with texture coordinates
 * suitable for a gradient giving an outline.  This triangle is actually
 * three triangles, so that texture interpolation works correctly.
 * @author C. Scott Ananian
 * @version $Id: GradientTriangle.java,v 1.2 2006-11-14 07:00:00 cananian Exp $
 */
public class GradientTriangle extends TriMesh {
    private final Vector2f p1,p2,p3; // just for use of "isInside()"
    GradientTriangle(String name,
            float x1, float y1,
            float x2, float y2,
            float x3, float y3) {
        super(name);
        this.p1=new Vector2f(x1,y1);
        this.p2=new Vector2f(x2,y2);
        this.p3=new Vector2f(x3,y3);

        TriangleBatch batch = getBatch(0);
        batch.setVertexCount(5);
        batch.setVertexBuffer(BufferUtils.createVector3Buffer(batch.getVertexCount()));
        batch.setNormalBuffer(BufferUtils.createVector3Buffer(batch.getVertexCount()));
        FloatBuffer tb = BufferUtils.createVector2Buffer(batch.getVertexCount());
        setTextureBuffer(0,tb);
        batch.setTriangleQuantity(3);
        batch.setIndexBuffer(BufferUtils.createIntBuffer(batch.getTriangleCount() * 3));
        
        // Vertices
        // vertex 0 is the centroid of the triangle.
        // vertex 4 is vertex 1, repeated with new texture coordinates.
        FloatBuffer vb = batch.getVertexBuffer();
        vb.put((x1+x2+x3)/3).put((y1+y2+y3)/3).put(0);
        vb.put(x1).put(y1).put(0);
        vb.put(x2).put(y2).put(0);
        vb.put(x3).put(y3).put(0);
        vb.put(x1).put(y1).put(0);
        
        // Normals
        FloatBuffer nb = batch.getNormalBuffer();
        for (int i=0; i<5; i++)
            nb.put(0).put(0).put(1);
        
        // Texture Coords
        tb.put(0).put(0);
        tb.put(0).put(1);
        tb.put(1/3f).put(1);
        tb.put(2/3f).put(1);
        tb.put(1).put(1);
        
        // Triangles
        IntBuffer ib = batch.getIndexBuffer();
        ib.put(0).put(1).put(2);
        ib.put(0).put(2).put(3);
        ib.put(0).put(3).put(4);

        setDefaultColor(ColorRGBA.white);
    }
    public boolean isInside(float x, float y) {
        testPoint.set(x,y);
        return 0 != FastMath.pointInsideTriangle(p1,p2,p3,testPoint);
    }
    private final Vector2f testPoint = new Vector2f();
}
