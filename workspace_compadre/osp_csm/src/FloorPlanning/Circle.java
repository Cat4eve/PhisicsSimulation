package FloorPlanning;
import java.awt.*;
    

class Circle {
    double x, y;        // position
    double vx, vy;      // velocity
    int radius;         // radius of the circle
    int mass;  // default mass (can be adjusted)
    
    // Add a constructor that allows setting the mass if needed
    public Circle(int radius, double x, double y, int mass) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.mass = mass;  // Optional: set mass during initialization
    }


    public void draw(Graphics g) {
        g.drawOval((int) x, (int) y, radius * 2, radius * 2);
    }

    public double centerX() {
        return x + radius;
    }

    public double centerY() {
        return y + radius;
    }
    
    public void capVelocity(double maxSpeed) {
        vx = Math.max(-maxSpeed, Math.min(maxSpeed, vx));
        vy = Math.max(-maxSpeed, Math.min(maxSpeed, vy));
    }

    public void updatePosition(double dt) {
        x += vx * dt;
        y += vy * dt;
    }
}
