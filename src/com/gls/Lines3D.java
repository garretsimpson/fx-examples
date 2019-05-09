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
    private static final double SPEED = 5;
    private static final double NUM_LINES = 30;

    Group world;
    List<Line> lines = new ArrayList<>();

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
        camera.setTranslateZ(-3.0 * FIELD_SIZE);
        camera.setNearClip(0.1); // Setting this to zero disables the Z buffer.
        camera.setFarClip(5 * FIELD_SIZE);
        scene.setCamera(camera);

        // Create lights
        AmbientLight light1 = new AmbientLight();
        PointLight light2 = new PointLight();
        light2.setTranslateX(-5 * FIELD_SIZE);
        light2.setTranslateY(-5 * FIELD_SIZE);
        light2.setTranslateZ(-5 * FIELD_SIZE);
        content.getChildren().addAll(light1, light2);

        // Create boarder
        content.getChildren().add(createBoarder());

        // Create content
        Group items = createContent();
        content.getChildren().add(items);
        world.getChildren().add(content);

        // Display scene
        primaryStage.setScene(scene);
        primaryStage.show();

        // Start animation
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
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
            case ESCAPE:
                Platform.exit();
                break;
            default:
                break;
            }
        });
    }

    private void onUpdate() {
        lines.forEach(Line::update);
    }

    private Group createContent() {
        Group items = new Group();

        Line line;
        for (int i = 0; i < NUM_LINES; i++) {
            line = createLine();
            lines.add(line);
            items.getChildren().add(line.getNode());
        }

        return items;
    }

    private Line createLine() {
        Line line = new Line();
        List<Spot> spots = line.getSpots();
        spots.forEach(spot -> {
            spot.setPosition(randomPoint(-FIELD_SIZE / 2, FIELD_SIZE / 2));
            spot.setVelocity(randomPoint(-SPEED, SPEED));
        });
        return line;
    }

    private Group createTestContent() {
        Group items = new Group();

        Point3D p1 = randomPoint(0, FIELD_SIZE / 2);
        Point3D p2 = randomPoint(-FIELD_SIZE / 2, 0);
        Point3D vec = p2.subtract(p1);
        double len = p1.distance(p2);
        double mag = vec.magnitude();
        Point3D mid = p1.midpoint(p2);
        double angle = Rotate.Y_AXIS.angle(vec);
        Point3D norm = Rotate.Y_AXIS.crossProduct(vec);

        System.out.println("p1:" + p1);
        System.out.println("p2:" + p2);
        System.out.println("v1:" + vec);
        System.out.println("len:" + len);
        System.out.println("mag:" + mag);
        System.out.println("mid:" + mid);
        System.out.println("ang:" + angle);

        Sphere s1 = new Sphere(10);
        s1.setMaterial(new PhongMaterial(Color.GREEN));
        s1.getTransforms().add(new Translate(p1.getX(), p1.getY(), p1.getZ()));

        Sphere s2 = new Sphere(10);
        s2.setMaterial(new PhongMaterial(Color.RED));
        s2.getTransforms().add(new Translate(p2.getX(), p2.getY(), p2.getZ()));

        Sphere s3 = new Sphere(10);
        s3.setMaterial(new PhongMaterial(Color.YELLOW));

        Sphere s4 = new Sphere(10);
        s4.setMaterial(new PhongMaterial(Color.BLUE));
        s4.getTransforms().add(new Translate(mid.getX(), mid.getY(), mid.getZ()));

        Box b1 = new Box(1, 1, 1);
        b1.setMaterial(new PhongMaterial(Color.BLACK));
        b1.getTransforms().add(new Translate(mid.getX(), mid.getY(), mid.getZ()));
        b1.getTransforms().add(new Rotate(angle, norm));
        b1.getTransforms().add(new Scale(10, len, 10));

        Box b2 = new Box(1, 1, 1);
        b2.getTransforms().add(new Scale(10, len, 10));
        b2.setMaterial(new PhongMaterial(Color.YELLOW));

        Box b3 = new Box(1, 1, 1);
        b3.setMaterial(new PhongMaterial(Color.BLUE));
        b3.getTransforms().add(new Translate(mid.getX(), mid.getY(), mid.getZ()));
        b3.getTransforms().add(new Scale(10, len, 10));

        items.getChildren().addAll(s1, s2, s3, s4, b1, b2, b3);
        return items;
    }

    private Point3D randomPoint(double min, double max) {
        double x = (max - min) * Math.random() + min;
        double y = (max - min) * Math.random() + min;
        double z = (max - min) * Math.random() + min;
        return new Point3D(x, y, z);
    }

    public Group createBoarder() throws Exception {
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

    private class Line {
        private Box box = new Box(1, 1, 1);
        final PhongMaterial mat = new PhongMaterial(Color.BLACK);
        private Spot[] spots = new Spot[2];

        public Line() {
            for (int i = 0; i < spots.length; i++) {
                spots[i] = new Spot();
            }
            Color color = Color.hsb(360 * Math.random(), 1, 1);
            mat.setDiffuseColor(color);
            mat.setSpecularColor(Color.WHITE);
            box.setMaterial(mat);
        }

        public Node getNode() {
            return box;
        }

        public List<Spot> getSpots() {
            return Arrays.asList(spots);
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
            box.getTransforms().clear();
            box.getTransforms().add(new Translate(mid.getX(), mid.getY(), mid.getZ()));
            box.getTransforms().add(new Rotate(angle, norm));
            box.getTransforms().add(new Scale(10, len, 10));
        }

        public void update() {
            getSpots().forEach(Spot::update);
            // rotateColor();
            draw();
        }

        private void rotateColor() {
            double hue = mat.getDiffuseColor().getHue() + 1;
            if (hue > 360) {
                hue -= 360;
            }
            mat.setDiffuseColor(Color.hsb(hue, 1, 1));
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
            this.position = position;
        }

        public void setVelocity(double dx, double dy, double dz) {
            velocity = new Point3D(dx, dy, dz);
        }

        public void setVelocity(Point3D velocity) {
            this.velocity = velocity;
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
