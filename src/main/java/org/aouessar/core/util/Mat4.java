package org.aouessar.core.util;

public final class Mat4 {

    // column-major (OpenGL)
    public final float[] m = new float[16];

    public static Mat4 identity() {
        Mat4 r = new Mat4();
        r.m[0]=1;
        r.m[5]=1;
        r.m[10]=1;
        r.m[15]=1;
        return r;
    }

    public static Mat4 perspective(float fovYRad, float aspect, float zNear, float zFar) {
        Mat4 r = new Mat4();
        float f = 1.0f / (float) Math.tan(fovYRad * 0.5f);

        r.m[0] = f / aspect;
        r.m[5] = f;
        r.m[10] = (zFar + zNear) / (zNear - zFar);
        r.m[11] = -1.0f;
        r.m[14] = (2.0f * zFar * zNear) / (zNear - zFar);
        return r;
    }

    public static Mat4 lookAt(Vec3 eye, Vec3 center, Vec3 up) {
        Vec3 f = new Vec3(center.x - eye.x, center.y - eye.y, center.z - eye.z).normalize();
        Vec3 s = Vec3.cross(f, new Vec3(up.x, up.y, up.z).normalize()).normalize();
        Vec3 u = Vec3.cross(s, f);

        Mat4 r = identity();
        r.m[0] = s.x; r.m[4] = s.y; r.m[8]  = s.z;
        r.m[1] = u.x; r.m[5] = u.y; r.m[9]  = u.z;
        r.m[2] = -f.x; r.m[6] = -f.y; r.m[10] = -f.z;

        r.m[12] = -(s.x * eye.x + s.y * eye.y + s.z * eye.z);
        r.m[13] = -(u.x * eye.x + u.y * eye.y + u.z * eye.z);
        r.m[14] =  (f.x * eye.x + f.y * eye.y + f.z * eye.z);
        return r;
    }

    public static Mat4 mul(Mat4 a, Mat4 b) {
        Mat4 r = new Mat4();
        // r = a * b
        for (int c = 0; c < 4; c++) {
            for (int rRow = 0; rRow < 4; rRow++) {
                r.m[c*4 + rRow] =
                        a.m[0*4 + rRow] * b.m[c*4 + 0] +
                                a.m[1*4 + rRow] * b.m[c*4 + 1] +
                                a.m[2*4 + rRow] * b.m[c*4 + 2] +
                                a.m[3*4 + rRow] * b.m[c*4 + 3];
            }
        }
        return r;
    }
}