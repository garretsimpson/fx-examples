package com.gls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

/**
 * @author Garret Simpson (gsimpson@gmail.com)
 */
public class Lines3D extends Application {

    private static final int WIDTH = 1024;
    private static final int HEIGHT = 800;
    private static final double FIELD_SIZE = 512;
    private static final double MAX_SPEED = 4;
    private static final double LINE_SIZE = 1;
    private static final int FAN_SIZE = 150;
    private static final int NUM_FANS = 5;

    Group world = new Group();
    Group lineGroup = new Group();
    List<Fan> fans = new ArrayList<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // primaryStage.setResizable(false);
        Scene scene = new Scene(world, WIDTH, HEIGHT, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.BLACK);
        Group content = new Group();

        // Create camera
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-1 * FIELD_SIZE);
        camera.setNearClip(0.1); // Setting this to zero disables the Z buffer.
        camera.setFarClip(5 * FIELD_SIZE);
        scene.setCamera(camera);

        // Create lights
        AmbientLight light1 = new AmbientLight();
        PointLight light2 = new PointLight();
        light2.setTranslateX(-5 * FIELD_SIZE);
        light2.setTranslateY(-5 * FIELD_SIZE);
        light2.setTranslateZ(-5 * FIELD_SIZE);
        world.getChildren().addAll(light1, light2);

        // Create border
        Group border = createBorder();
        border.setVisible(false);
        content.getChildren().add(border);

        // Create content
        content.getChildren().add(createFans());
        world.getChildren().add(content);

        // Display scene
        primaryStage.setScene(scene);
        primaryStage.show();

        // Start animation
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                content.getTransforms().add(new Rotate(0.2, Rotate.Y_AXIS));
                onUpdate();
            }

        };
        timer.start();

        // Handle key press events
        primaryStage.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()) {
            case UP:
                content.getTransforms().add(new Rotate(-10, Rotate.X_AXIS));
                break;
            case DOWN:
                content.getTransforms().add(new Rotate(10, Rotate.X_AXIS));
                break;
            case LEFT:
                content.getTransforms().add(new Rotate(10, Rotate.Y_AXIS));
                break;
            case RIGHT:
                content.getTransforms().add(new Rotate(-10, Rotate.Y_AXIS));
                break;
            case W:
                camera.setTranslateZ(camera.getTranslateZ() + 50);
                break;
            case S:
                camera.setTranslateZ(camera.getTranslateZ() - 50);
                break;
            case B:
                border.setVisible(!border.isVisible());
                break;
            case ESCAPE:
                Platform.exit();
                break;
            default:
                break;
            }
        });
    }

    private void onUpdate() {
        fans.forEach(Fan::update);
    }

    private Group createFans() {
        for (int i = 0; i < NUM_FANS; i++) {
            createFan();
        }
        return lineGroup;
    }

    private Group createFan() {
        Fan fan = new Fan();
        Line line = fan.getLine();
        List<Spot> spots = line.getSpots();
        spots.forEach(spot -> {
            spot.setPosition(randomPoint(-FIELD_SIZE / 2, FIELD_SIZE / 2));
            spot.setVelocity(randomPoint(-MAX_SPEED, MAX_SPEED));
        });
        line.draw();
        lineGroup.getChildren().add(line);
        fans.add(fan);

        return lineGroup;
    }

    private Point3D randomPoint(double min, double max) {
        double x = (max - min) * Math.random() + min;
        double y = (max - min) * Math.random() + min;
        double z = (max - min) * Math.random() + min;
        return new Point3D(x, y, z);
    }

    public Group createBorder() throws Exception {
        Group item = new Group();

        final PhongMaterial mat = new PhongMaterial(Color.SILVER);

        Box edge;
        double[] v = { -FIELD_SIZE / 2, FIELD_SIZE / 2 };

        for (double x : v) {
            for (double y : v) {
                edge = new Box(1, 1, FIELD_SIZE);
                edge.setTranslateX(x);
                edge.setTranslateY(y);
                edge.setMaterial(mat);
                item.getChildren().add(edge);
            }
        }
        for (double y : v) {
            for (double z : v) {
                edge = new Box(FIELD_SIZE, 1, 1);
                edge.setTranslateY(y);
                edge.setTranslateZ(z);
                edge.setMaterial(mat);
                item.getChildren().add(edge);
            }
        }
        for (double x : v) {
            for (double z : v) {
                edge = new Box(1, FIELD_SIZE, 1);
                edge.setTranslateX(x);
                edge.setTranslateZ(z);
                edge.setMaterial(mat);
                item.getChildren().add(edge);
            }
        }

        return item;
    }

    private class Fan {
        // Need a circular queue.
        int size = FAN_SIZE;
        Line[] lines = new Line[size];
        int pos = 0;

        public Fan() {
            Line line = new Line();
            lines[pos] = line;
        }

        // Note: Cannot use getParent() to remove the line and add a new one.
        public void update() {
            if (size == 1) {
                lines[pos].update();
                return;
            }

            // Copy the current line
            Line curLine = lines[pos];
            Line newLine = new Line();
            List<Spot> curSpots = curLine.getSpots();
            List<Spot> newSpots = newLine.getSpots();
            for (int i = 0; i < curSpots.size(); i++) {
                newSpots.get(i).setPosition(curSpots.get(i).getPosition());
                newSpots.get(i).setVelocity(curSpots.get(i).getVelocity());
            }
            newLine.setColor(curLine.getColor());

            // Update the new line
            newLine.update();

            // Add the new line
            int next = (pos + 1) % size;
            Line oldLine = lines[next];
            if (oldLine != null) {
                lineGroup.getChildren().remove(oldLine);
            }
            lineGroup.getChildren().add(newLine);
            lines[next] = newLine;
            pos = next;
        }

        public Line getLine() {
            return lines[pos];
        }
    }

    private class Line extends Box {
        private Spot[] spots = new Spot[2];
        private final PhongMaterial mat = new PhongMaterial(Color.BLACK);

        public Line() {
            super(1, 1, 1);
            for (int i = 0; i < spots.length; i++) {
                spots[i] = new Spot();
            }
            Color color = Color.hsb(360 * Math.random(), 0.6, 1);
            mat.setDiffuseColor(color);
            // mat.setSpecularColor(Color.SILVER);
            setMaterial(mat);
        }

        private List<Spot> getSpots() {
            return Arrays.asList(spots);
        }

        private Color getColor() {
            return mat.getDiffuseColor();
        }

        private void setColor(Color color) {
            mat.setDiffuseColor(color);
        }

        private void draw() {
            Point3D p0 = spots[0].getPosition();
            Point3D p1 = spots[1].getPosition();
            Point3D vec = p1.subtract(p0);
            Point3D mid = p1.midpoint(p0);
            double len = vec.magnitude();
            double angle = Rotate.Y_AXIS.angle(vec);
            Point3D norm = Rotate.Y_AXIS.crossProduct(vec);

            // Note: Order of transforms is important
            getTransforms().clear();
            getTransforms().add(new Translate(mid.getX(), mid.getY(), mid.getZ()));
            getTransforms().add(new Rotate(angle, norm));
            getTransforms().add(new Scale(LINE_SIZE, len, LINE_SIZE));
        }

        public void update() {
            getSpots().forEach(Spot::update);
            rotateColor();
            draw();
        }

        private void rotateColor() {
            double hue = getColor().getHue() + 0.6;
            if (hue > 360) {
                hue -= 360;
            }
            setColor(Color.hsb(hue, 0.6, 1));
        }

    }

    private class Spot {
        private Point3D position = new Point3D(0, 0, 0);
        private Point3D velocity = new Point3D(0, 0, 0);

        public Point3D getPosition() {
            return position;
        }

        public void setPosition(double x, double y, double z) {
            position = new Point3D(x, y, z);
        }

        public void setPosition(Point3D position) {
            this.position = new Point3D(0, 0, 0).add(position);
        }

        public Point3D getVelocity() {
            return velocity;
        }

        public void setVelocity(double dx, double dy, double dz) {
            velocity = new Point3D(dx, dy, dz);
        }

        public void setVelocity(Point3D velocity) {
            this.velocity = new Point3D(0, 0, 0).add(velocity);
        }

        public void update() {
            double minX = -FIELD_SIZE / 2;
            double minY = -FIELD_SIZE / 2;
            double minZ = -FIELD_SIZE / 2;
            double maxX = FIELD_SIZE / 2;
            double maxY = FIELD_SIZE / 2;
            double maxZ = FIELD_SIZE / 2;
            double dx = velocity.getX();
            double dy = velocity.getY();
            double dz = velocity.getZ();
            double posX = position.getX() + dx;
            double posY = position.getY() + dy;
            double posZ = position.getZ() + dz;

            if (posX < minX) {
                posX = minX + (minX - posX);
                dx = -dx;
            } else if (posX > maxX) {
                posX = maxX - (posX - maxX);
                dx = -dx;
            }
            if (posY < minY) {
                posY = minY + (minY - posY);
                dy = -dy;
            } else if (posY > maxY) {
                posY = maxY - (posY - maxY);
                dy = -dy;
            }
            if (posZ < minZ) {
                posZ = minZ + (minZ - posZ);
                dz = -dz;
            } else if (posZ > maxZ) {
                posZ = maxZ - (posZ - maxZ);
                dz = -dz;
            }
            setPosition(posX, posY, posZ);
            // dy += 0.1; // Gravity
            setVelocity(dx, dy, dz);
            // velocity = velocity.multiply(0.99); // Friction
        }

    }
}
