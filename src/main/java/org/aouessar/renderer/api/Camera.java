package org.aouessar.renderer.api;

import org.joml.Matrix4f;
import org.joml.Vector3f;


public final class Camera {
    public final Vector3f position = new Vector3f(0, 90, 0);
    public float yaw = 0f;
    public float pitch = 0f;

    public Matrix4f viewMatrix() {
        Vector3f forward = forward();
        Vector3f center = new Vector3f(position).add(forward);
        return new Matrix4f().lookAt(position, center, new Vector3f(0, 1, 0));
    }

    public Matrix4f projMatrix(float aspect) {
        return new Matrix4f().perspective((float)Math.toRadians(70), aspect, 0.1f, 5000f);
    }

    public Vector3f forward() {
        float cy = (float)Math.cos(yaw);
        float sy = (float)Math.sin(yaw);
        float cp = (float)Math.cos(pitch);
        float sp = (float)Math.sin(pitch);
        return new Vector3f(sy * cp, -sp, cy * cp).normalize();
    }

    public Vector3f right() {
        return forward().cross(0, 1, 0, new Vector3f()).normalize();
    }
}