package FloorPlanning;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class CircleUI extends JFrame {
    private JPanel inputPanel, drawPanel;
    private JTextField numField;
    private ArrayList<JTextField> radiusFields = new ArrayList<>();
    private ArrayList<Circle> circles = new ArrayList<>();
    private Circle centerCircle;
    private boolean motionStopped = false;


    public CircleUI() {
        setTitle("Circle Radius Input");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel for input
        inputPanel = new JPanel(new GridLayout(0, 2));
        numField = new JTextField();
        JButton setNumBtn = new JButton("Set Number of Circles");

        inputPanel.add(new JLabel("Number of circles:"));
        inputPanel.add(numField);
        inputPanel.add(setNumBtn);

        add(inputPanel, BorderLayout.NORTH);

        // Drawing panel
        drawPanel = new JPanel() {
        	@Override
        	protected void paintComponent(Graphics g) {
        	    super.paintComponent(g);
        	    if (centerCircle == null) return;

        	    // Draw center circle
        	    g.setColor(Color.RED);
        	    centerCircle.draw(g);

        	    // Draw other circles
        	    g.setColor(Color.BLUE);
        	    for (Circle c : circles) {
        	        c.draw(g);
        	    }
        	    
        	    if (motionStopped) {
        	        int minX = Integer.MAX_VALUE;
        	        int minY = Integer.MAX_VALUE;
        	        int maxX = Integer.MIN_VALUE;
        	        int maxY = Integer.MIN_VALUE;

        	        // Calculate the bounding box that fits all circles
        	        for (Circle c : circles) {
        	            int left = (int)c.x;
        	            int top = (int)c.y;
        	            int right = (int)(c.x + 2 * c.radius);
        	            int bottom = (int)(c.y + 2 * c.radius);

        	            minX = Math.min(minX, left);
        	            minY = Math.min(minY, top);
        	            maxX = Math.max(maxX, right);
        	            maxY = Math.max(maxY, bottom);
        	        }

        	        // Draw the bounding box with padding
        	        Graphics2D g2d = (Graphics2D) g;
        	        g2d.setColor(Color.BLACK);
        	        g2d.setStroke(new BasicStroke(3));
        	        g2d.drawRect(minX - 5, minY - 5, (maxX - minX) + 10, (maxY - minY) + 10);

        	        // Calculate the area of the bounding box
        	        int width = maxX - minX + 10;
        	        int height = maxY - minY + 10;
        	        int area = width * height;

        	        // Draw the area near the top-left corner of the bounding box
        	        g2d.setColor(Color.BLACK);
        	        g2d.drawString("Area: " + area, minX - 5, minY - 10);  // Adjust position if necessary
        	    }
        	}
        };
        drawPanel.setPreferredSize(new Dimension(800, 300));
        add(drawPanel, BorderLayout.CENTER);

        // Button action
        setNumBtn.addActionListener(e -> {
            try {
                int n = Integer.parseInt(numField.getText());
                showRadiusInput(n);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid integer.");
            }
        });

        pack();
        setVisible(true);
        Timer timer = new Timer(30, e -> updatePhysics());
        timer.start();
    }
    
    private void updatePhysics() {
        if (centerCircle == null) return;

        for (Circle c : circles) {
            double dx = centerCircle.centerX() - c.centerX();
            double dy = centerCircle.centerY() - c.centerY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < centerCircle.radius + c.radius + 2) {
                // Close enough — snap into place and stop movement
                c.vx = 0;
                c.vy = 0;
                continue;
            }

            // Gravitational force (simple inverse square)
            double force = 10 * (1000.0 / (distance * distance)) * c.mass; 

            // Acceleration
            double ax = force * dx / distance / c.mass;  // Divide by mass to adjust acceleration
            double ay = force * dy / distance / c.mass;  // Divide by mass to adjust acceleration

            // Velocity update
            c.vx += ax;
            c.vy += ay;

            // Damping / friction to avoid buildup
            c.vx *= 0.95;
            c.vy *= 0.95;

            // Position update
            c.x += c.vx;
            c.y += c.vy;

            // Optional: Clamp within panel bounds
            int panelW = drawPanel.getWidth();
            int panelH = drawPanel.getHeight();
            c.x = Math.max(0, Math.min(c.x, panelW - c.radius * 2));
            c.y = Math.max(0, Math.min(c.y, panelH - c.radius * 2));
            
         // Collision handling between circles
            for (int i = 0; i < circles.size(); i++) {
                Circle a = circles.get(i);
                for (int j = i + 1; j < circles.size(); j++) {
                    Circle b = circles.get(j);

                    dx = b.centerX() - a.centerX();
                    dy = b.centerY() - a.centerY();
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    double minDist = a.radius + b.radius;

                    if (dist < minDist && dist > 0) {
                        // Overlap detected, push them apart
                        double overlap = 0.5 * (minDist - dist);

                        // Normalize direction
                        double nx = dx / dist;
                        double ny = dy / dist;

                        // Displace circles
                        a.x -= nx * overlap;
                        a.y -= ny * overlap;
                        b.x += nx * overlap;
                        b.y += ny * overlap;

                        // Simple elastic collision response (optional)
                        double vxTotal = a.vx - b.vx;
                        double vyTotal = a.vy - b.vy;
                        double dot = vxTotal * nx + vyTotal * ny;

                        if (dot > 0) {
                            double impulse = dot;
                            a.vx -= impulse * nx;
                            a.vy -= impulse * ny;
                            b.vx += impulse * nx;
                            b.vy += impulse * ny;
                        }
                    }
                }
            }
        }
        
        // Blue-red collision: prevent blue circles from entering the red one
        for (Circle c : circles) {
            double dx = c.centerX() - centerCircle.centerX();
            double dy = c.centerY() - centerCircle.centerY();
            double dist = Math.sqrt(dx * dx + dy * dy);
            double minDist = c.radius + centerCircle.radius;

            if (dist < minDist && dist > 0) {
                double overlap = minDist - dist;

                // Normalize direction
                double nx = dx / dist;
                double ny = dy / dist;

                // Push only blue circle out
                c.x += nx * overlap;
                c.y += ny * overlap;

                // Reflect velocity away from red center
                double dot = c.vx * nx + c.vy * ny;
                if (dot < 0) {
                    c.vx -= 2 * dot * nx;
                    c.vy -= 2 * dot * ny;
                }
            }
        }
        
        // Check if all velocities are below threshold
        motionStopped = true;
        for (Circle c : circles) {
            double speed = Math.sqrt(c.vx * c.vx + c.vy * c.vy);
            if (speed > 0.5) {
                motionStopped = false;
                break;
            }
        }

        drawPanel.repaint();
    }

    private void showRadiusInput(int n) {
        radiusFields.clear();
        JPanel panel = new JPanel(new GridLayout(n, 2));
        for (int i = 0; i < n; i++) {
            panel.add(new JLabel("Radius " + (i + 1) + ":"));
            JTextField field = new JTextField();
            radiusFields.add(field);
            panel.add(field);
        }

        int result = JOptionPane.showConfirmDialog(this, panel, "Enter Radii", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
        	circles.clear();
        	int width = drawPanel.getWidth();
        	int height = drawPanel.getHeight();
        	int maxRadius = -1;
        	int maxIndex = -1;
        	ArrayList<Integer> inputRadii = new ArrayList<>();

        	for (JTextField field : radiusFields) {
        	    inputRadii.add(Integer.parseInt(field.getText()));
        	}
        	for (int i = 0; i < inputRadii.size(); i++) {
        	    int r = inputRadii.get(i);
        	    if (r > maxRadius) {
        	        maxRadius = r;
        	        maxIndex = i;
        	    }
        	}

        	for (int i = 0; i < inputRadii.size(); i++) {
        	    int r = inputRadii.get(i);
        	    double x, y;

        	    if (i == maxIndex) {
        	        x = width / 2.0 - r;
        	        y = height / 2.0 - r;
        	        centerCircle = new Circle(r, x, y, r);
        	    } else {
        	        Circle newCircle;
        	        boolean valid;
        	        int attempts = 0;

        	        do {
        	            valid = true;
        	            x = Math.random() * (width - r * 2);
        	            y = Math.random() * (height - r * 2);
        	            newCircle = new Circle(r, x, y, r);

        	            // Check for overlap
        	            for (Circle existing : circles) {
        	                double dx = existing.centerX() - newCircle.centerX();
        	                double dy = existing.centerY() - newCircle.centerY();
        	                double dist = Math.sqrt(dx * dx + dy * dy);
        	                if (dist < existing.radius + newCircle.radius + 2) {
        	                    valid = false;
        	                    break;
        	                }
        	            }

        	            attempts++;
        	            if (attempts > 1000) break;

        	        } while (!valid);

        	        circles.add(newCircle);
        	    }
        	}
        }
    }
}