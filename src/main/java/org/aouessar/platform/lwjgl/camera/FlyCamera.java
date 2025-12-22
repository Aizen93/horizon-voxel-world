package org.aouessar.platform.lwjgl.camera;

import org.aouessar.core.math.Mat4;
import org.aouessar.core.math.Vec3;

import static org.lwjgl.glfw.GLFW.*;

public final class FlyCamera {
    public final Vec3 position = new Vec3(0, 140, 0);

    private float yaw = -90f;   // looking toward -Z
    private float pitch = -15f;

    private double lastMouseX, lastMouseY;
    private boolean firstMouse = true;

    public float moveSpeed = 40f;
    public float mouseSensitivity = 0.12f; // degrees per pixel

    public void handleMouse(long window) {
        double[] mx = new double[1];
        double[] my = new double[1];
        glfwGetCursorPos(window, mx, my);

        if (firstMouse) {
            lastMouseX = mx[0];
            lastMouseY = my[0];
            firstMouse = false;
            return;
        }

        double dx = mx[0] - lastMouseX;
        double dy = my[0] - lastMouseY;
        lastMouseX = mx[0];
        lastMouseY = my[0];

        yaw += (float)dx * mouseSensitivity;
        pitch -= (float)dy * mouseSensitivity;

        if (pitch > 89f) pitch = 89f;
        if (pitch < -89f) pitch = -89f;
    }

    public void updateMovement(long window, float dt) {
        float speed = moveSpeed * dt;
        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) speed *= 2.5f;

        Vec3 forward = getForward();
        Vec3 right = Vec3.cross(forward, new Vec3(0,1,0)).normalize();

        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) position.add(new Vec3(forward.x, forward.y, forward.z).mul(speed));
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) position.sub(new Vec3(forward.x, forward.y, forward.z).mul(speed));
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) position.add(new Vec3(right.x, right.y, right.z).mul(speed));
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) position.sub(new Vec3(right.x, right.y, right.z).mul(speed));
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) position.y += speed;
        if (glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS) position.y -= speed;
    }

    public Vec3 getForward() {
        float yawRad = (float)Math.toRadians(yaw);
        float pitchRad = (float)Math.toRadians(pitch);

        float fx = (float)(Math.cos(yawRad) * Math.cos(pitchRad));
        float fy = (float)(Math.sin(pitchRad));
        float fz = (float)(Math.sin(yawRad) * Math.cos(pitchRad));
        return new Vec3(fx, fy, fz).normalize();
    }

    public Mat4 viewMatrix() {
        Vec3 forward = getForward();
        Vec3 center = new Vec3(position.x + forward.x, position.y + forward.y, position.z + forward.z);
        return Mat4.lookAt(position, center, new Vec3(0,1,0));
    }
}