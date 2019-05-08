package com.gls;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

/**
 * @author Garret Simpson (gsimpson@gmail.com)
 */
public class Simple3D extends Application {

    private static final int WIDTH = 1024;
    private static final int HEIGHT = 800;

    Group world;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // primaryStage.setResizable(false);
        world = new Group();
        Scene scene = new Scene(world, WIDTH, HEIGHT, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.SILVER);

        // Create camera
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-400);
        camera.setNearClip(0.1); // Setting this to zero disables the Z buffer.
        camera.setFarClip(1000);
        scene.setCamera(camera);

        // Create lights
        PointLight light = new PointLight();
        light.setTranslateX(-100);
        light.setTranslateY(-100);
        light.setTranslateZ(-100);
        world.getChildren().add(light);

        // Create content
        Group item = createContent();
        world.getChildren().add(item);

        // Display scene
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()) {
            case UP:
                item.getTransforms().add(new Rotate(-10, Rotate.X_AXIS));
                break;
            case DOWN:
                item.getTransforms().add(new Rotate(10, Rotate.X_AXIS));
                break;
            case LEFT:
                item.getTransforms().add(new Rotate(10, Rotate.Y_AXIS));
                break;
            case RIGHT:
                item.getTransforms().add(new Rotate(-10, Rotate.Y_AXIS));
                break;
            case ESCAPE:
                Platform.exit();
                break;
            default:
                break;
            }
        });
    }

    public Group createContent() throws Exception {
        Group item = new Group();

        // Shiny red
        PhongMaterial mat1 = new PhongMaterial();
        mat1.setDiffuseColor(Color.RED);
        mat1.setSpecularColor(Color.ORANGE);

        // Shiny silver
        PhongMaterial mat2 = new PhongMaterial();
        mat2.setDiffuseColor(Color.SILVER);
        mat2.setSpecularColor(Color.WHITE);

        Cylinder item1 = new Cylinder(10, 80);
        item1.getTransforms().add(new Rotate(90, Rotate.Z_AXIS));
        item1.setMaterial(mat2);

        Box item2 = new Box(100, 50, 20);
        item2.setMaterial(new PhongMaterial(Color.GREEN));
        item2.setTranslateZ(30);
        item2.setMaterial(mat1);

        Sphere item3 = new Sphere(10);
        item3.setTranslateX(40);
        item3.setMaterial(mat2);

        Sphere item4 = new Sphere(10);
        item4.setTranslateX(-40);
        item4.setMaterial(mat2);

        item.getChildren().addAll(item1, item2, item3, item4);

        return item;
    }

}
