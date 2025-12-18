package org.aouessar.core.math;

public final class Vec3 {

    public float x, y, z;

    public Vec3() {}

    public Vec3(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
    }

    public Vec3 set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vec3 add(Vec3 o) {
        x += o.x;
        y += o.y;
        z += o.z;
        return this;
    }
    public Vec3 sub(Vec3 o) {
        x -= o.x;
        y -= o.y;
        z -= o.z;
        return this;
    }
    public Vec3 mul(float s) {
        x *= s;
        y *= s;
        z *= s;
        return this;
    }

    public Vec3 normalize() {
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len > 1e-8f) {
            x /= len;
            y /= len;
            z /= len;
        }
        return this;
    }

    public static Vec3 cross(Vec3 a, Vec3 b) {
        return new Vec3(
                a.y * b.z - a.z * b.y,
                a.z * b.x - a.x * b.z,
                a.x * b.y - a.y * b.x
        );
    }
}

