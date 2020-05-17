package com.gls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

/**
 * 3D Boids This JavaFX application implements the Boids algorithm.
 * 
 * References http://www.red3d.com/cwr/boids/
 * http://www.cs.toronto.edu/~dt/siggraph97-course/cwr87/
 * 
 * @author Garret Simpson (gsimpson@gmail.com)
 */
public class Boids3D extends Application {

    private static final int WIDTH = 1600;
    private static final int HEIGHT = 900;
    private static final double FIELD_SIZE_X = 2400.0;
    private static final double FIELD_SIZE_Y = 1350.0;
    private static final double FIELD_SIZE_Z = 2400.0;

    private static final int NUM_BOIDS = 400;
    private static final double MIN_BOID_SIZE = 0.1;
    private static final double MAX_BOID_SIZE = 5.0;
    private static final double INIT_BOID_SIZE = 1.2;
    private static final double MIN_SPEED = 0.0;
    private static final double MAX_SPEED = 3.0;
    private static final double MAX_DELTA = 1.0;
    private static final double MIN_VIEW = 0.0;
    private static final double MAX_VIEW = 400.0;
    private static final double INIT_VIEW = 200.0;
    private static final double MIN_PUSH_SCALE = 0.0;
    private static final double MAX_PUSH_SCALE = 1.0;
    private static final double INIT_PUSH_SCALE = 1.0;
    private static final double MIN_PULL_SCALE = 0.0;
    private static final double MAX_PULL_SCALE = 1.0;
    private static final double INIT_PULL_SCALE = 0.1;
    private static final double MATCH_SCALE = 0.1;
    private static final double CENTER_SCALE = 1.0;

    private static final int INIT_NUM_COLORS = NUM_BOIDS / 4;

    private static final Color FILL_COLOR = Color.LIGHTSKYBLUE;
    private static final Color BOID_COLOR = Color.LIGHTSLATEGRAY;

    boolean up = true;
    boolean focusDirty = true;

    Boid[] boids = new Boid[NUM_BOIDS];
    Point3D[][] vects = new Point3D[NUM_BOIDS][NUM_BOIDS];

    Metric breakCount = new Metric("Break");

    public static void main(String[] args) {
        launch(args);
    }

    SimpleBooleanProperty pause = new SimpleBooleanProperty(false);

    SimpleBooleanProperty isCenter = new SimpleBooleanProperty(true);

    SimpleIntegerProperty numColors = new SimpleIntegerProperty(INIT_NUM_COLORS);

    private final DoubleProperty pullScale = new SimpleDoubleProperty(INIT_PULL_SCALE);

    public double getPullScale() {
        return pullScale.get();
    }

    public void setPullScale(double value) {
        pullScale.set(value);
    }

    private final DoubleProperty pushScale = new SimpleDoubleProperty(INIT_PUSH_SCALE);

    public double getPushScale() {
        return pushScale.get();
    }

    public void setPushScale(double value) {
        pushScale.set(value);
    }

    private final DoubleProperty view = new SimpleDoubleProperty(INIT_VIEW);

    @Override
    public void start(Stage stage) throws Exception {
        // primaryStage.setResizable(false);
        Group root = new Group();
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.setFill(FILL_COLOR);

        Group world = new Group();
        SubScene graphics = new SubScene(world, WIDTH, HEIGHT, true, SceneAntialiasing.BALANCED);
        scene.setFill(FILL_COLOR);

        // Create camera
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-1.5 * FIELD_SIZE_Z);
        camera.setNearClip(0.1); // Setting this to zero disables the Z buffer.
        camera.setFarClip(5.0 * FIELD_SIZE_Z);
        graphics.setCamera(camera);

        // Create lights
        AmbientLight light1 = new AmbientLight(Color.GRAY);
        PointLight light2 = new PointLight(Color.WHITE);
        light2.setTranslateX(-FIELD_SIZE_X);
        light2.setTranslateY(-FIELD_SIZE_Y);
        light2.setTranslateZ(-FIELD_SIZE_Z);
        world.getChildren().addAll(light1, light2);

        // Create content
        Group content = new Group();

        // Create border
        Node border = createBorder();
        border.setVisible(true);
        content.getChildren().add(border);

        // Create boids
        for (int i = 0; i < NUM_BOIDS; i++) {
            Boid boid = createBoid(i);
            boids[i] = boid;
            content.getChildren().add(boid.getNode());
        }

        world.getChildren().add(content);

        // Create UI
        Node ui = createUI();

        root.getChildren().addAll(graphics, ui);

        // Display scene
        stage.setScene(scene);
        stage.show();

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (focusDirty) {
                    world.requestFocus();
                }
                if (pause.get()) {
                    content.getTransforms().add(new Rotate(-0.3, Rotate.Y_AXIS));
                } else {
                    onUpdate();
                }
            }
        };
        timer.start();

        stage.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()) {
            case UP:
                content.getTransforms().add(new Rotate(10, content.parentToLocal(Rotate.X_AXIS)));
                break;
            case DOWN:
                content.getTransforms().add(new Rotate(-10, content.parentToLocal(Rotate.X_AXIS)));
                break;
            case LEFT:
                content.getTransforms().add(new Rotate(-10, Rotate.Y_AXIS));
                break;
            case RIGHT:
                content.getTransforms().add(new Rotate(10, Rotate.Y_AXIS));
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
                scramble();
                break;
            case DIGIT2:
                up = !up;
                break;
            case SPACE:
                pause.set(!pause.get());
                break;
            case ESCAPE:
                Platform.exit();
                break;
            default:
                break;
            }
        });
    }

    private Node createUI() {
        GridPane grid = new GridPane();
//        TitledPane main = new TitledPane("BOID World", grid);
//        Accordion ui = new Accordion(main);

        grid.setVgap(5);
        grid.setHgap(5);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setStyle("-fx-background-color: rgba(0, 0, 0, 0.1)");
        grid.setFocusTraversable(false);

        TextField title = new TextField("BOID World");
        title.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        title.setAlignment(Pos.CENTER);

        ButtonBar bbar = new ButtonBar();
        Button button0 = new Button("Exit");
        button0.addEventHandler(ActionEvent.ACTION, event -> {
            Platform.exit();
        });
        ButtonBar.setButtonData(button0, ButtonData.LEFT);

        Button button1 = new Button("Scramble");
        button1.addEventHandler(ActionEvent.ACTION, event -> {
            scramble();
        });
        ButtonBar.setButtonData(button1, ButtonData.RIGHT);

        bbar.getButtons().addAll(button0, button1);

        CheckBox check0 = new CheckBox("Center pull field");
        check0.setSelected(isCenter.get());
        isCenter.bind(check0.selectedProperty());

        Label name1 = new Label("Size");
        Slider slide1 = new Slider(MIN_BOID_SIZE, MAX_BOID_SIZE, INIT_BOID_SIZE);
        slide1.setMinWidth(200);
        for (int i = 0; i < NUM_BOIDS; i++) {
            boids[i].getFigure().scaleXProperty().bind(slide1.valueProperty());
            boids[i].getFigure().scaleYProperty().bind(slide1.valueProperty());
            boids[i].getFigure().scaleZProperty().bind(slide1.valueProperty());
        }

        Label name2 = new Label("Pull");
        Slider slide2 = new Slider(MIN_PULL_SCALE, MAX_PULL_SCALE, INIT_PULL_SCALE);
        slide2.setMajorTickUnit(0.25);
        slide2.setMinorTickCount(4);
        slide2.setShowTickMarks(true);
        slide2.setShowTickLabels(true);
        slide2.setSnapToTicks(true);
        pullScale.bind(slide2.valueProperty());
        Text value2 = new Text();
        value2.textProperty().bind(slide2.valueProperty().asString("%1.2f"));

        Label name3 = new Label("Push");
        Slider slide3 = new Slider(MIN_PUSH_SCALE, MAX_PUSH_SCALE, INIT_PUSH_SCALE);
        slide3.setMajorTickUnit(0.25);
        slide3.setMinorTickCount(4);
        slide3.setShowTickMarks(true);
        slide3.setShowTickLabels(true);
        slide3.setSnapToTicks(true);
        pushScale.bind(slide3.valueProperty());
        Text value3 = new Text();
        value3.textProperty().bind(slide3.valueProperty().asString("%1.2f"));

        Label name4 = new Label("View");
        Slider slide4 = new Slider(MIN_VIEW, MAX_VIEW, INIT_VIEW);
        slide4.setMajorTickUnit(100);
        slide4.setMinorTickCount(4);
        slide4.setShowTickMarks(true);
        slide4.setShowTickLabels(true);
        slide4.setSnapToTicks(false);
        view.bind(slide4.valueProperty());
        Text value4 = new Text();
        value4.textProperty().bind(slide4.valueProperty().asString("%5.0f"));

        int row = 0;
        grid.add(title, 0, row++, 3, 1);
        grid.add(bbar, 0, row++, 3, 1);
        row++;
        grid.add(name1, 0, row);
        grid.add(slide1, 1, row);
        row++;
        grid.add(name4, 0, row);
        grid.add(slide4, 1, row);
        grid.add(value4, 2, row);
        row++;
        grid.add(name2, 0, row);
        grid.add(slide2, 1, row);
        grid.add(value2, 2, row);
        row++;
        grid.add(name3, 0, row);
        grid.add(slide3, 1, row);
        grid.add(value3, 2, row);
        row++;
        grid.add(check0, 1, row++, 2, 1);

        return grid;
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
        breakCount.reset();
        computeVects();
        for (int i = 0; i < NUM_BOIDS; i++) {
            boids[i].update();
        }
        updateUI();
    }

    private void updateUI() {
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
                if (vec.getX() > FIELD_SIZE_X / 2.0) {
                    vec = vec.subtract(FIELD_SIZE_X, 0.0, 0.0);
                }
                // Wrap X and Z
                double posX = vec.getX();
                double maxX = FIELD_SIZE_X;
                if (posX > maxX / 2.0) {
                    vec = vec.subtract(maxX, 0.0, 0.0);
                } else if (posX < -maxX / 2.0) {
                    vec = vec.add(maxX, 0.0, 0.0);
                }
                double posZ = vec.getZ();
                double maxZ = FIELD_SIZE_Z;
                if (posZ > maxZ / 2.0) {
                    vec = vec.subtract(0.0, 0.0, maxZ);
                } else if (posZ < -maxZ / 2.0) {
                    vec = vec.add(0.0, 0.0, maxZ);
                }

                // Set vectors for both boids
                vects[i][j] = vec;
                vects[j][i] = vec.multiply(-1.0);
                if (vec.magnitude() < (boids[i].getSize() + boids[j].getSize() / 2.0)) {
                    System.out.println("BOOM!");
                }
            }
        }
    }

    // TODO: Use angle of vision in front of boid
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

    public Node createBorder() throws Exception {
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
        private Group figure, boid;
        private int index;
        private Color color = BOID_COLOR;
        private double size = INIT_BOID_SIZE;
        final PhongMaterial boidMat = new PhongMaterial();
//        final PhongMaterial tailMat = new PhongMaterial();
        private Point3D position = Point3D.ZERO;
        private Point3D velocity = Point3D.ZERO;
        private List<Boid> nearbyBoids;

        public Boid(int index) {
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

            figure = new Group(Arrays.asList(body, wings, tail));
            figure.setScaleX(size);
            figure.setScaleY(size);
            figure.setScaleZ(size);

            boid = new Group(figure);
        }

        public Node getNode() {
            return boid;
        }

        public Node getFigure() {
            return figure;
        }

        public int getIndex() {
            return index;
        }

        public double getSize() {
            return size;
        }

        public Point3D getPosition() {
            return position;
        }

        public void setPosition(double x, double y, double z) {
            position = new Point3D(x, y, z);
        }

        public void setPosition(Point3D position) {
            this.position = position;
        }

        public Point3D getVelocity() {
            return velocity;
        }

        public void setVelocity(double dx, double dy, double dz) {
            velocity = new Point3D(dx, dy, dz);
        }

        public void setVelocity(Point3D velocity) {
            this.velocity = velocity;
        }

        private Point3D towardCenter() {
            if (!isCenter.get()) {
                return Point3D.ZERO;
            }
            Point3D vec = position.multiply(-1.0);
            return truncate(adjust(vec)).multiply(CENTER_SCALE);
        }

        /*
         * The toward function according to the spec. The attraction vector is toward
         * the center of the nearby boids, with a force equal to the inverse square of
         * the distance.
         * 
         * This seems to have interesting "follow the leader" behaviors, but also has
         * some jerkiness.
         * 
         * Note: The subtle difference between toward and avoid is that toward is an
         * adjusted sum (average) of vectors, while avoid is a sum of adjusted vectors.
         */
        private Point3D towardNearby0() {
            int numNearby = nearbyBoids.size();
            if (numNearby == 0) {
                return Point3D.ZERO;
            }
            Point3D vec = Point3D.ZERO;
            for (Boid nearbyBoid : nearbyBoids) {
                vec = vec.add(vects[index][nearbyBoid.getIndex()]);
            }
            vec = adjust(vec.multiply(1.0 / numNearby));
            return truncate(vec).multiply(getPullScale());
        }

        /*
         * My original toward function. It computes the sum of the attraction vectors
         * (inverse square) to each nearby boid.
         * 
         * This seems to result in a much smoother behavior.
         * 
         * Note: I'm not sure this is correct. Seems to me that the toward vector and
         * the avoid vector are exact opposites. If their scales are the same, will they
         * cancel each other out? The only mitigation I see is the prioritize function,
         * where one factor can override another if it is strong enough.
         */
        private Point3D towardNearby1() {
            Point3D vec = Point3D.ZERO;
            for (Boid nearbyBoid : nearbyBoids) {
                vec = vec.add(adjust(vects[index][nearbyBoid.getIndex()]));
            }
            return truncate(vec).multiply(getPullScale());
        }

        /*
         * My modified toward function. Similar to the spec version (uses the average
         * position), but the attraction force is proportional to the distance, rather
         * than the inverse square.
         */
        private Point3D towardNearby2() {
            Point3D vec = Point3D.ZERO;
            for (Boid nearbyBoid : nearbyBoids) {
                vec = vec.add(vects[index][nearbyBoid.getIndex()]);
            }
            vec = vec.normalize();
            return truncate(vec).multiply(getPullScale());
        }

        private Point3D avoidNearby() {
            Point3D vec = Point3D.ZERO;
            for (Boid nearbyBoid : nearbyBoids) {
                vec = vec.add(adjust(vects[nearbyBoid.getIndex()][index]));
            }
            return truncate(vec).multiply(getPushScale());
        }

        private Point3D matchVelocity() {
            int numNearby = nearbyBoids.size();
            if (numNearby == 0) {
                return Point3D.ZERO;
            }
            Point3D vec = Point3D.ZERO;
            for (Boid nearbyBoid : nearbyBoids) {
                // TODO: Should this be weighted by the neighbor's proximity?
                vec = vec.add(nearbyBoid.getVelocity());
            }
            vec = vec.multiply(1.0 / numNearby);
            return truncate(vec).multiply(MATCH_SCALE);
        }

        // Objects further away should have less force
        // Adjust the vector by dividing it by the square of its length
        private Point3D adjust(Point3D vec) {
            return vec.multiply(Math.pow(vec.magnitude(), -2.0));
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
                    breakCount.increment();
                    break;
                }
            }
            return vec;
        }

        public void update() {
            // Update list of nearby boids
            nearbyBoids = getBoidsInRange(index, view.get());

            // Steer - Adjust velocity according to forces
            Point3D delta = prioritize(Arrays.asList(avoidNearby(), towardCenter(), matchVelocity(), towardNearby2()));

            // Add delta, but don't exceed maximum speed
            velocity = velocity.add(delta);
            if (velocity.magnitude() > MAX_SPEED) {
                velocity = velocity.normalize().multiply(MAX_SPEED);
            }

            // Update position
            Point3D newPos = position.add(velocity);
            position = wrapPosition(newPos);

            // Render the boid
            updateColor();
            draw();
        }

        private void updateColor() {
            if ((numColors.get() == 0) || (nearbyBoids.size() == 0)) {
                color = BOID_COLOR;
                return;
            }
            double hue = (360.0 / numColors.get()) * nearbyBoids.size();
            color = Color.hsb(hue, 0.6, 1.0);
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

    private class Metric {
        String name;
        int count = 0;

        Metric(String name) {
            this.name = name;
        }

        public void reset() {
            count = 0;
        }

        public void increment() {
            count++;
        }

        public String toString() {
            return name + ": " + count;
        }
    }

}