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
    private int stepCounter = 0;
    private final int MAX_STEPS_WITHOUT_EQUILIBRIUM = 300;
    private boolean centerSwitched = false;

    public CircleUI() {
        setTitle("Circle Radius Input");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        inputPanel = new JPanel(new GridLayout(0, 2));
        numField = new JTextField();
        JButton setNumBtn = new JButton("Set Number of Circles");

        inputPanel.add(new JLabel("Number of circles:"));
        inputPanel.add(numField);
        inputPanel.add(setNumBtn);

        add(inputPanel, BorderLayout.NORTH);

        drawPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (centerCircle == null) return;

                g.setColor(Color.RED);
                centerCircle.draw(g);

                g.setColor(Color.BLUE);
                for (Circle c : circles) {
                    c.draw(g);
                }

                if (motionStopped) {
                    int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
                    int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

                    ArrayList<Circle> allCircles = new ArrayList<>(circles);
                    allCircles.add(centerCircle);

                    for (Circle c : allCircles) {
                        int left = (int) c.x;
                        int top = (int) c.y;
                        int right = (int) (c.x + 2 * c.radius);
                        int bottom = (int) (c.y + 2 * c.radius);

                        minX = Math.min(minX, left);
                        minY = Math.min(minY, top);
                        maxX = Math.max(maxX, right);
                        maxY = Math.max(maxY, bottom);
                    }

                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setColor(Color.BLACK);
                    g2d.setStroke(new BasicStroke(3));
                    g2d.drawRect(minX - 5, minY - 5, (maxX - minX) + 10, (maxY - minY) + 10);

                    int width = maxX - minX + 10;
                    int height = maxY - minY + 10;
                    int area = width * height;

                    g2d.drawString("Area: " + area, minX - 5, minY - 10);
                    g2d.drawString("Steps: " + stepCounter, minX - 5, minY - 25);
                }
            }
        };

        drawPanel.setPreferredSize(new Dimension(800, 300));
        add(drawPanel, BorderLayout.CENTER);

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

        if (!motionStopped) stepCounter++;

        for (Circle c : circles) {
            double dx = centerCircle.centerX() - c.centerX();
            double dy = centerCircle.centerY() - c.centerY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < centerCircle.radius + c.radius + 2) {
                c.vx = 0;
                c.vy = 0;
                continue;
            }

            double force = 30 * (1000.0 / (distance * distance));
            double ax = force * dx / distance;
            double ay = force * dy / distance;

            c.vx += ax;
            c.vy += ay;
            c.vx *= 0.95;
            c.vy *= 0.95;

            c.x += c.vx;
            c.y += c.vy;

            int panelW = drawPanel.getWidth();
            int panelH = drawPanel.getHeight();
            c.x = Math.max(0, Math.min(c.x, panelW - c.radius * 2));
            c.y = Math.max(0, Math.min(c.y, panelH - c.radius * 2));
        }

        for (int i = 0; i < circles.size(); i++) {
            Circle a = circles.get(i);
            for (int j = i + 1; j < circles.size(); j++) {
                Circle b = circles.get(j);
                double dx = b.centerX() - a.centerX();
                double dy = b.centerY() - a.centerY();
                double dist = Math.sqrt(dx * dx + dy * dy);
                double minDist = a.radius + b.radius;

                if (dist < minDist && dist > 0) {
                    double overlap = 0.5 * (minDist - dist);
                    double nx = dx / dist;
                    double ny = dy / dist;

                    a.x -= nx * overlap;
                    a.y -= ny * overlap;
                    b.x += nx * overlap;
                    b.y += ny * overlap;

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

        for (Circle c : circles) {
            double dx = c.centerX() - centerCircle.centerX();
            double dy = c.centerY() - centerCircle.centerY();
            double dist = Math.sqrt(dx * dx + dy * dy);
            double minDist = c.radius + centerCircle.radius;

            if (dist < minDist && dist > 0) {
                double overlap = minDist - dist;
                double nx = dx / dist;
                double ny = dy / dist;

                c.x += nx * overlap;
                c.y += ny * overlap;

                double dot = c.vx * nx + c.vy * ny;
                if (dot < 0) {
                    c.vx -= 2 * dot * nx;
                    c.vy -= 2 * dot * ny;
                }
            }
        }

        motionStopped = true;
        for (Circle c : circles) {
            double speed = Math.sqrt(c.vx * c.vx + c.vy * c.vy);
            if (speed > 0.5) {
                motionStopped = false;
                break;
            }
        }

        // 🔁 Repeatedly switch center every N steps
        final int CENTER_UPDATE_INTERVAL = 300;

        if (!motionStopped && stepCounter > CENTER_UPDATE_INTERVAL) {
            Circle farthest = null;
            double maxDistance = -1;

            for (Circle c : circles) {
                double dx = c.centerX() - centerCircle.centerX();
                double dy = c.centerY() - centerCircle.centerY();
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > maxDistance) {
                    maxDistance = dist;
                    farthest = c;
                }
            }

            if (farthest != null) {
                circles.remove(farthest);
                circles.add(centerCircle);
                centerCircle = farthest;
                stepCounter = 0;

                for (Circle c : circles) {
                    c.vx = 0;
                    c.vy = 0;
                }

                drawPanel.repaint();
                return;
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
            stepCounter = 0;
            centerSwitched = false;

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
                    centerCircle = new Circle(r, x, y);
                } else {
                    Circle newCircle;
                    boolean valid;
                    int attempts = 0;

                    do {
                        valid = true;
                        x = Math.random() * (width - r * 2);
                        y = Math.random() * (height - r * 2);
                        newCircle = new Circle(r, x, y);

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
    
    public CircleUI(java.util.List<Integer> radii) {
        this(); // calls default constructor
        SwingUtilities.invokeLater(() -> showRadiusInput(radii));
    }

    private void showRadiusInput(java.util.List<Integer> radii) {
        int n = radii.size();
        radiusFields.clear();
        circles.clear();
        int width = drawPanel.getWidth();
        int height = drawPanel.getHeight();
        int maxRadius = -1;
        int maxIndex = -1;
        stepCounter = 0;
        centerSwitched = false;

        for (int i = 0; i < n; i++) {
            int r = radii.get(i);
            if (r > maxRadius) {
                maxRadius = r;
                maxIndex = i;
            }
        }

        for (int i = 0; i < n; i++) {
            int r = radii.get(i);
            double x, y;

            if (i == maxIndex) {
                x = width / 2.0 - r;
                y = height / 2.0 - r;
                centerCircle = new Circle(r, x, y);
            } else {
                Circle newCircle;
                boolean valid;
                int attempts = 0;

                do {
                    valid = true;
                    x = Math.random() * (width - r * 2);
                    y = Math.random() * (height - r * 2);
                    newCircle = new Circle(r, x, y);

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

        drawPanel.repaint();
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(CircleUI::new);
    }
}
