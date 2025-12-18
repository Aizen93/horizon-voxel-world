package org.aouessar.platform.lwjgl.renderer;

public final class Frustum {
    // 6 planes, each plane: ax + by + cz + d >= 0 is inside
    private final float[][] p = new float[6][4];

    public void setFromMatrix(float[] m) {
        // m is column-major 4x4 (OpenGL)
        // Build row-major access helpers:
        float m00=m[0],  m01=m[4],  m02=m[8],  m03=m[12];
        float m10=m[1],  m11=m[5],  m12=m[9],  m13=m[13];
        float m20=m[2],  m21=m[6],  m22=m[10], m23=m[14];
        float m30=m[3],  m31=m[7],  m32=m[11], m33=m[15];

        // Left   = row4 + row1
        setPlane(0, m30+m00, m31+m01, m32+m02, m33+m03);
        // Right  = row4 - row1
        setPlane(1, m30-m00, m31-m01, m32-m02, m33-m03);
        // Bottom = row4 + row2
        setPlane(2, m30+m10, m31+m11, m32+m12, m33+m13);
        // Top    = row4 - row2
        setPlane(3, m30-m10, m31-m11, m32-m12, m33-m13);
        // Near   = row4 + row3
        setPlane(4, m30+m20, m31+m21, m32+m22, m33+m23);
        // Far    = row4 - row3
        setPlane(5, m30-m20, m31-m21, m32-m22, m33-m23);
    }

    private void setPlane(int i, float a, float b, float c, float d) {
        float invLen = 1.0f / (float)Math.sqrt(a*a + b*b + c*c);
        p[i][0] = a * invLen;
        p[i][1] = b * invLen;
        p[i][2] = c * invLen;
        p[i][3] = d * invLen;
    }

    public boolean aabbVisible(float minX,float minY,float minZ, float maxX,float maxY,float maxZ) {
        for (int i = 0; i < 6; i++) {
            float a=p[i][0], b=p[i][1], c=p[i][2], d=p[i][3];

            // “positive vertex” test (fast AABB-vs-plane)
            float px = (a >= 0) ? maxX : minX;
            float py = (b >= 0) ? maxY : minY;
            float pz = (c >= 0) ? maxZ : minZ;

            if (a*px + b*py + c*pz + d < 0) return false;
        }
        return true;
    }
}

