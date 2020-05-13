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
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

/**
 * @author Garret Simpson (gsimpson@gmail.com)
 */
public class Boids3D extends Application {

    private static final int WIDTH = 1600;
    private static final int HEIGHT = 900;
    private static final double FIELD_SIZE_X = 2400.0;
    private static final double FIELD_SIZE_Y = 1350.0;
    private static final double FIELD_SIZE_Z = 2400.0;

    private static final int NUM_BOIDS = 400;
    private static final double BOID_SIZE = 1.2;
    private static final double MIN_SPEED = 0.0;
    private static final double MAX_SPEED = 3.0;
    private static final double MAX_DELTA = 1.0;
    private static final double MAX_VIEW = 100.0;
    private static final double PUSH_SCALE = 1.0;
    private static final double PULL_SCALE = 0.8;
    private static final double MATCH_SCALE = 0.1;

    private static final Color FILL_COLOR = Color.LIGHTSKYBLUE;
    private static final Color BOID_COLOR = Color.LIGHTSLATEGRAY;

    boolean pause = false;
    boolean up = true;

    Group world = new Group();
    Boid[] boids = new Boid[NUM_BOIDS];
    Point3D[][] vects = new Point3D[NUM_BOIDS][NUM_BOIDS];

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Group content = new Group();

        // primaryStage.setResizable(false);
        Scene scene = new Scene(world, WIDTH, HEIGHT, true, SceneAntialiasing.BALANCED);
        scene.setFill(FILL_COLOR);

        // Create camera
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-1.5 * FIELD_SIZE_Z);
        camera.setNearClip(0.1); // Setting this to zero disables the Z buffer.
        camera.setFarClip(5.0 * FIELD_SIZE_Z);
        scene.setCamera(camera);

        // Create lights
        AmbientLight light1 = new AmbientLight(Color.GRAY);
        PointLight light2 = new PointLight(Color.WHITE);
        light2.setTranslateX(-FIELD_SIZE_X);
        light2.setTranslateY(-FIELD_SIZE_Y);
        light2.setTranslateZ(-FIELD_SIZE_Z);
        world.getChildren().addAll(light1, light2);

        // Create border
        Group border = createBorder();
        border.setVisible(true);
        content.getChildren().add(border);

        // Create boids
        for (int i = 0; i < NUM_BOIDS; i++) {
            Boid boid = createBoid(i);
            boids[i] = boid;
            content.getChildren().add(boid.getNode());
        }

        // content.getChildren().add(colorTest());

        // Add content
        world.getChildren().add(content);

        // Display scene
        primaryStage.setScene(scene);
        primaryStage.show();

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (pause) {
                    content.getTransforms().add(new Rotate(-0.3, Rotate.Y_AXIS));
                }
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
                up = !up;
                break;
            case DIGIT2:
                scramble();
                break;
            case SPACE:
                pause = !pause;
                break;
            case ESCAPE:
                Platform.exit();
                break;
            default:
                break;
            }
        });
    }

    // Create a color test pattern
    // Hue - range is 0.0 to 360.0
    // - 0.0 RED
    // - 60.0 YELLOW
    // - 120.0 GREEN
    // - 180.0 CYAN
    // - 240.0 BLUE
    // - 300.0 MAGENTA
    private Group colorTest() {
        Group items = new Group();
        Color color;
        int NUM_ITEMS = 360;

        for (int i = 0; i < NUM_ITEMS; i++) {
            double hue = 1.0 * i;
            color = Color.hsb(hue, 1.0, 1.0);
            Sphere s = new Sphere(50);
            PhongMaterial mat = new PhongMaterial();
            mat.setDiffuseColor(color);
            s.setMaterial(mat);
            double x = (1.0 * i * FIELD_SIZE_X / NUM_ITEMS) - (FIELD_SIZE_X / 2.0);
            s.setTranslateX(x);
            items.getChildren().add(s);
        }
        return items;
    }

    private void onUpdate() {
        computeVects();
        if (pause) {
            return;
        }
        for (int i = 0; i < NUM_BOIDS; i++) {
            boids[i].update();
        }
    }

    private void scramble() {
        for (int i = 0; i < NUM_BOIDS; i++) {
            Point3D vec = new Point3D(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5).normalize();
            boids[i].setVelocity(vec.multiply(MIN_SPEED + (MAX_SPEED - MIN_SPEED) / 2.0));
        }
    }

    private void computeVects() {
        for (int i = 0; i < NUM_BOIDS; i++) {
            for (int j = i; j < NUM_BOIDS; j++) {
                if (i == j) {
                    vects[i][j] = Point3D.ZERO;
                    continue;
                }
                Point3D vec = boids[j].getPosition().subtract(boids[i].getPosition());
                vects[i][j] = vec;
                vects[j][i] = vec.multiply(-1.0);
                if (vec.magnitude() < BOID_SIZE / 2.0) {
                    System.out.println("BOOM!");
                }
            }
        }
    }

    private List<Boid> getBoidsInRange(int index, double range) {
        ArrayList<Boid> res = new ArrayList<>();
        for (int i = 0; i < NUM_BOIDS; i++) {
            if (i == index) {
                continue;
            }
            double dist = vects[index][i].magnitude();
            if (dist <= range) {
                res.add(boids[i]);
            }
        }
        return res;
    }

    public Group createBorder() throws Exception {
        Group item = new Group();

        final PhongMaterial matBlack = new PhongMaterial(Color.BLACK);

        Box edge;
        double[] vx = { -FIELD_SIZE_X / 2, FIELD_SIZE_X / 2 };
        double[] vy = { -FIELD_SIZE_Y / 2, FIELD_SIZE_Y / 2 };
        double[] vz = { -FIELD_SIZE_Z / 2, FIELD_SIZE_Z / 2 };

        for (double x : vx) {
            for (double y : vy) {
                edge = new Box(1, 1, FIELD_SIZE_Z);
                edge.setTranslateX(x);
                edge.setTranslateY(y);
                edge.setMaterial(matBlack);
                item.getChildren().add(edge);
            }
        }
        for (double y : vy) {
            for (double z : vz) {
                edge = new Box(FIELD_SIZE_X, 1, 1);
                edge.setTranslateY(y);
                edge.setTranslateZ(z);
                edge.setMaterial(matBlack);
                item.getChildren().add(edge);
            }
        }
        for (double x : vx) {
            for (double z : vz) {
                edge = new Box(1, FIELD_SIZE_Y, 1);
                edge.setTranslateX(x);
                edge.setTranslateZ(z);
                edge.setMaterial(matBlack);
                item.getChildren().add(edge);
            }
        }

        return item;
    }

    private Point3D randomPoint(double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        double x = (maxX - minX) * Math.random() + minX;
        double y = (maxY - minY) * Math.random() + minY;
        double z = (maxZ - minZ) * Math.random() + minZ;

        return new Point3D(x, y, z);
    }

    private Boid createBoid(int index) {
        Boid boid = new Boid(index);
        boid.setPosition(randomPoint(-FIELD_SIZE_X / 2.0, FIELD_SIZE_X / 2.0, -FIELD_SIZE_Y / 2.0, FIELD_SIZE_Y / 2.0,
            -FIELD_SIZE_Z / 2.0, FIELD_SIZE_Z / 2.0));
        Point3D vec = new Point3D(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5).normalize();
        boid.setVelocity(vec.multiply((MIN_SPEED + MAX_SPEED) / 2.0));
        boid.draw();

        return boid;
    }

    private class Boid {
        private Group boid;
        private int index;
        private Color color = BOID_COLOR;
        final PhongMaterial boidMat = new PhongMaterial();
        final PhongMaterial tailMat = new PhongMaterial();
        private Point3D position = Point3D.ZERO;
        private Point3D velocity = Point3D.ZERO;

        public Boid(int index) {
            boid = new Group();
            this.index = index;

            boidMat.setDiffuseColor(color);
            boidMat.setSpecularColor(Color.GRAY);
            // tailMat.setDiffuseColor(Color.DARKGREEN);
            // tailMat.setSpecularColor(Color.GRAY);

            Sphere body = new Sphere(5);
            body.setScaleY(3);
            body.setMaterial(boidMat);
            Box wings = new Box(40, 10, 2);
            wings.setTranslateY(2.5);
            wings.setMaterial(boidMat);
            Box tail = new Box(2, 10, 10);
            tail.setTranslateY(-10);
            tail.setTranslateZ(5);
            tail.setMaterial(boidMat);

            Group figure = new Group(Arrays.asList(body, wings, tail));
            figure.setScaleX(BOID_SIZE);
            figure.setScaleY(BOID_SIZE);
            figure.setScaleZ(BOID_SIZE);
            boid.getChildren().addAll(figure);
        }

        public Node getNode() {
            return boid;
        }

        public int getIndex() {
            return index;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        public Point3D getPosition() {
            return position;

        }

        public void setPosition(double x, double y, double z) {
            position = new Point3D(x, y, z);
        }

        public void setPosition(Point3D position) {
            this.position = new Point3D(position.getX(), position.getY(), position.getZ());
        }

        public Point3D getVelocity() {
            return velocity;
        }

        public void setVelocity(double dx, double dy, double dz) {
            velocity = new Point3D(dx, dy, dz);
        }

        public void setVelocity(Point3D velocity) {
            this.velocity = new Point3D(velocity.getX(), velocity.getY(), velocity.getZ());
        }

        private Point3D towardCenter() {
            Point3D vec = position.multiply(-1.0);
            double size = Math.min(Math.min(FIELD_SIZE_X, FIELD_SIZE_Y), FIELD_SIZE_Z);
            if (vec.magnitude() > size) {
                return Point3D.ZERO;
            }
            return truncate(adjust(vec));
        }

        private Point3D avoidNearby() {
            List<Boid> nearbyBoids = getBoidsInRange(index, MAX_VIEW);
            if (nearbyBoids.size() == 0) {
                return Point3D.ZERO;
            }
            Point3D vec = Point3D.ZERO;
            for (Boid nearbyBoid : nearbyBoids) {
                vec = vec.add(adjust(vects[nearbyBoid.getIndex()][index]));
            }
            return truncate(vec);
        }

        private Point3D matchVelocity() {
            List<Boid> nearbyBoids = getBoidsInRange(index, MAX_VIEW);
            if (nearbyBoids.size() == 0) {
                return Point3D.ZERO;
            }
            Point3D vec = Point3D.ZERO;
            for (Boid nearbyBoid : nearbyBoids) {
                // TODO: Should this be weighted by the neighbor's proximity?
                vec = vec.add(nearbyBoid.getVelocity());
            }
            return truncate(vec);
        }

        private Point3D towardNearby() {
            List<Boid> nearbyBoids = getBoidsInRange(index, MAX_VIEW);
            if (nearbyBoids.size() == 0) {
                setColor(BOID_COLOR);
                return Point3D.ZERO;
            }
            // Use the range RED to BLUE (0.0 to 240.0)
            // Assume max of 5 nearby boids
            double hue = 240.0 * nearbyBoids.size() / 5.0;
            setColor(Color.hsb(hue, 0.5, 1.0));
            Point3D vec = Point3D.ZERO;
            for (Boid nearbyBoid : nearbyBoids) {
                vec = vec.add(adjust(vects[index][nearbyBoid.getIndex()]));
            }
            return truncate(vec);
        }

        // Objects further away should have less force
        // Adjust the vector by dividing it by the square of its length
        private Point3D adjust(Point3D vec) {
            return vec.multiply(1.0 / (Math.pow(vec.magnitude(), 2.0)));
        }

        // Truncate the vector to a max magnitude of 1.0
        private Point3D truncate(Point3D vec) {
            if (vec.magnitude() > 1.0) {
                return vec.normalize();
            }
            return vec;
        }

        private Point3D prioritize(List<Point3D> deltas) {
            Point3D vec = Point3D.ZERO;
            double totalMag = 0.0;
            for (Point3D delta : deltas) {
                double mag = delta.magnitude();
                if (totalMag + mag < MAX_DELTA) {
                    vec = vec.add(delta);
                    totalMag += mag;
                } else {
                    double scale = (MAX_DELTA - totalMag) / delta.magnitude();
                    vec = vec.add(delta.multiply(scale));
                    break;
                }
            }
            return vec;
        }

        public void update() {
            // Steer - Adjust velocity according to forces
            Point3D delta0 = towardCenter();
            Point3D delta1 = avoidNearby().multiply(PUSH_SCALE);
            Point3D delta2 = matchVelocity().multiply(MATCH_SCALE);
            Point3D delta3 = towardNearby().multiply(PULL_SCALE);
            Point3D delta = prioritize(Arrays.asList(delta1, delta0, delta2, delta3));

            // Add delta, but don't exceed maximum speed
            velocity = velocity.add(delta);
            if (velocity.magnitude() > MAX_SPEED) {
                velocity = velocity.normalize().multiply(MAX_SPEED);
            }

            // Update position
            Point3D newPos = position.add(velocity);
            position = wrapPosition(newPos);

            // Render the boid
            draw();
        }

        private Point3D wrapPosition(Point3D pos) {
            // Wrap around world
            double minX = -FIELD_SIZE_X / 2.0;
            double minY = -FIELD_SIZE_Y / 2.0;
            double minZ = -FIELD_SIZE_Z / 2.0;
            double maxX = FIELD_SIZE_X / 2.0;
            double maxY = FIELD_SIZE_Y / 2.0;
            double maxZ = FIELD_SIZE_Z / 2.0;
            double posX = pos.getX();
            double posY = pos.getY();
            double posZ = pos.getZ();
            double dirX = 1.0;
            double dirY = 1.0;
            double dirZ = 1.0;

            if (posX < minX) {
                posX = maxX - (minX - posX);
            } else if (posX > maxX) {
                posX = minX + (posX - maxX);
            }
            if (posY < minY) {
                posY = minY + (minY - posY);
                dirY = -dirY;
            } else if (posY > maxY) {
                posY = maxY - (posY - maxY);
                dirY = -dirY;
            }
            if (posZ < minZ) {
                posZ = maxZ - (minZ - posZ);
            } else if (posZ > maxZ) {
                posZ = minZ + (posZ - maxZ);
            }

            setVelocity(dirX * velocity.getX(), dirY * velocity.getY(), dirZ * velocity.getZ());
            return new Point3D(posX, posY, posZ);
        }

        public void draw() {
            boidMat.setDiffuseColor(color);
            boid.getTransforms().clear();

            // Move it to the correct position
            boid.getTransforms().add(new Translate(position.getX(), position.getY(), position.getZ()));

            // Rotate the boid to align with velocity
            double angle = Rotate.Y_AXIS.angle(velocity);
            Point3D norm = Rotate.Y_AXIS.crossProduct(velocity);
            boid.getTransforms().add(new Rotate(angle, norm));
            // Rotate the tail upwards
            if (up) {
                Point3D upward = boid.parentToLocal(position.getX(), position.getY() - 1000.0, position.getZ())
                    .normalize();
                Point3D vec = new Point3D(upward.getX(), 0.0, upward.getZ());
                angle = Rotate.Z_AXIS.angle(vec);
                if (vec.getX() < 0.0) {
                    angle = -angle;
                }
                boid.getTransforms().add(new Rotate(angle, Rotate.Y_AXIS));
            }
        }
    }

}