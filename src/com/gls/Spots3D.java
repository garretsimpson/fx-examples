package com.gls;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

/**
 * @author Garret Simpson (gsimpson@gmail.com)
 */
public class Spots3D extends Application {

    private static final int WIDTH = 1024;
    private static final int HEIGHT = 800;
    private static final double FIELD_SIZE = 500;

    private static final int NUM_SPOTS = 30;
    private static final double SPOT_MIN_SIZE = 10;
    private static final double SPOT_MAX_SIZE = 50;
    private static final double MAX_SPEED = 5;

    Group world;
    List<Spot> spots = new ArrayList<>();
    int score = 0;
    boolean winner = false;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // primaryStage.setResizable(false);
        world = new Group();
        Scene scene = new Scene(world, WIDTH, HEIGHT, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.SILVER);
        Group content = new Group();

        // Create camera
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-2.0 * FIELD_SIZE);
        camera.setNearClip(0.1); // Setting this to zero disables the Z buffer.
        camera.setFarClip(5 * FIELD_SIZE);
        scene.setCamera(camera);

        // Create lights
        AmbientLight light1 = new AmbientLight(Color.DARKSLATEGRAY);
        PointLight light2 = new PointLight();
        light2.setTranslateX(-FIELD_SIZE);
        light2.setTranslateY(-FIELD_SIZE);
        light2.setTranslateZ(-FIELD_SIZE);
        content.getChildren().addAll(light1, light2);

        // Create border
        Group border = createBorder();
        border.setVisible(false);
        content.getChildren().add(border);

        // Create spots
        for (int i = 0; i < NUM_SPOTS; i++) {
            Spot spot = createSpot();
            spots.add(spot);
            content.getChildren().add(spot.getNode());
        }

        // Add content
        world.getChildren().add(content);

        // Display scene
        primaryStage.setScene(scene);
        primaryStage.show();

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                content.getTransforms().add(new Rotate(-0.3, Rotate.Y_AXIS));
                onUpdate();
            }
        };
        timer.start();

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
            case DIGIT1:
                winner = !winner;
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
        spots.forEach(Spot::update);
    }

    public Group createBorder() throws Exception {
        Group item = new Group();

        final PhongMaterial matBlack = new PhongMaterial(Color.BLACK);

        Box edge;
        double[] v = { -FIELD_SIZE / 2, FIELD_SIZE / 2 };

        for (double x : v) {
            for (double y : v) {
                edge = new Box(1, 1, FIELD_SIZE);
                edge.setTranslateX(x);
                edge.setTranslateY(y);
                edge.setMaterial(matBlack);
                item.getChildren().add(edge);
            }
        }
        for (double y : v) {
            for (double z : v) {
                edge = new Box(FIELD_SIZE, 1, 1);
                edge.setTranslateY(y);
                edge.setTranslateZ(z);
                edge.setMaterial(matBlack);
                item.getChildren().add(edge);
            }
        }
        for (double x : v) {
            for (double z : v) {
                edge = new Box(1, FIELD_SIZE, 1);
                edge.setTranslateX(x);
                edge.setTranslateZ(z);
                edge.setMaterial(matBlack);
                item.getChildren().add(edge);
            }
        }

        return item;
    }

    private Spot createSpot() {
        Spot spot = new Spot();
        spot.setVelocity(
            new Point3D(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5).multiply(MAX_SPEED));
        spot.setSize((SPOT_MAX_SIZE - SPOT_MIN_SIZE) * Math.random() + SPOT_MIN_SIZE);
        return spot;
    }

    private class Spot {
        private Sphere spot;
        private double radius = SPOT_MIN_SIZE;
        private Color color = Color.DARKSLATEGRAY;
        private Point3D velocity = new Point3D(0, 0, 0);

        public Spot() {
            spot = new Sphere(radius);
            final PhongMaterial mat = new PhongMaterial();
            mat.setDiffuseColor(color);
            mat.setSpecularColor(Color.GRAY);
            spot.setMaterial(mat);

            spot.setOnMouseClicked(e -> {
                if (color.equals(Color.DARKSLATEGRAY)) {
                    // scoreBox.incrementScore();
                    color = Color.hsb(360 * Math.random(), 1, 1, 0.7);
                    mat.setDiffuseColor(color);
                    mat.setSpecularColor(Color.WHITE);
                    spot.setMaterial(mat);
                    score++;
                    if (score == NUM_SPOTS) {
                        winner = true;
                    }
                }
            });
        }

        public Node getNode() {
            return spot;
        }

        public void setPosition(double x, double y, double z) {
            spot.setTranslateX(x);
            spot.setTranslateY(y);
            spot.setTranslateZ(z);
        }

        public void setVelocity(double dx, double dy, double dz) {
            velocity = new Point3D(dx, dy, dz);
        }

        public void setVelocity(Point3D velocity) {
            this.velocity = velocity;
        }

        public void setSize(double size) {
            radius = size;
            spot.setRadius(radius);
        }

        public void update() {
            double minX = -FIELD_SIZE / 2 + radius;
            double minY = -FIELD_SIZE / 2 + radius;
            double minZ = -FIELD_SIZE / 2 + radius;
            double maxX = FIELD_SIZE / 2 - radius;
            double maxY = FIELD_SIZE / 2 - radius;
            double maxZ = FIELD_SIZE / 2 - radius;
            double dx = velocity.getX();
            double dy = velocity.getY();
            double dz = velocity.getZ();

            double posX = spot.getTranslateX() + dx;
            double posY = spot.getTranslateY() + dy;
            double posZ = spot.getTranslateZ() + dz;

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
            spot.setTranslateX(posX);
            spot.setTranslateY(posY);
            spot.setTranslateZ(posZ);

            if (winner) {
                rotateColor();
                dy += 0.2; // Gravity
            }
            setVelocity(dx, dy, dz);
            // velocity = velocity.multiply(0.99); // Friction
        }

        private void rotateColor() {
            if (color.equals(Color.BLACK))
                return;
            double hue = color.getHue() + 1;
            if (hue > 360) {
                hue -= 360;
            }
            color = Color.hsb(hue, 1, 1, 0.7);
            final PhongMaterial mat = new PhongMaterial();
            mat.setDiffuseColor(color);
            mat.setSpecularColor(Color.WHITE);
            spot.setMaterial(mat);
        }
    }

}
