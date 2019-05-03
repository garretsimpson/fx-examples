package com.gls;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.BoxBlur;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Spots extends Application {

    private static final int SIZE_X = 1024;
    private static final int SIZE_Y = 800;

    private static final int NUM_SPOTS = 25;
    private static final int SPOT_SIZE = 100;

    private Pane root;
    private List<Spot> spots = new ArrayList<>();
    private ScoreBox scoreBox;
    boolean winner = false;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setScene(new Scene(createContent()));
        stage.show();
    }

    public Parent createContent() {
        root = new Pane();
        root.setPrefSize(SIZE_X, SIZE_Y);

        Rectangle boarder = new Rectangle(SIZE_X, SIZE_Y, Color.WHITE);
        boarder.setStrokeType(StrokeType.INSIDE);
        boarder.setStrokeWidth(5);
        boarder.setStroke(Color.BLACK);
        root.getChildren().add(boarder);

        for (int i = 0; i < NUM_SPOTS; i++) {
            createSpot();
        }

        scoreBox = new ScoreBox();
        root.getChildren().add(scoreBox.getNode());

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                onUpdate();
            }
        };
        timer.start();

        return root;
    }

    private void createSpot() {
        Spot spot = new Spot();
        spot.setPosition(root.getPrefWidth() * Math.random(), root.getPrefWidth() * Math.random());
        spot.setVelocity(10 * Math.random() - 5, 10 * Math.random() - 5);
        spots.add(spot);
        root.getChildren().add(spot.getNode());
    }

    private void onUpdate() {
        spots.forEach(Spot::update);
    }

    private class Spot {
        private Circle spot;
        private double radius;
        private Color color = Color.BLACK;
        private Point2D velocity = new Point2D(0, 0);

        public Spot() {
            radius = SPOT_SIZE * Math.random() + 20;

            spot = new Circle(radius);
            spot.setFill(color);
            spot.setStrokeType(StrokeType.INSIDE);
            spot.setStroke(Color.GRAY);
            spot.setStrokeWidth(5);
            spot.setEffect(new BoxBlur(10, 10, 1));

            spot.setOnMouseClicked(e -> {
                if (color.equals(Color.BLACK)) {
                    scoreBox.incrementScore();
                    color = Color.hsb(360 * Math.random(), 1, 1, 0.5);
                    spot.setFill(color);
                }
            });
        }

        public void setPosition(double x, double y) {
            spot.setTranslateX(x);
            spot.setTranslateY(y);
        }

        public void setVelocity(double dx, double dy) {
            velocity = new Point2D(dx, dy);
        }

        public Node getNode() {
            return spot;
        }

        public void update() {
            double minX = radius;
            double minY = radius;
            double maxX = root.getPrefWidth() - radius;
            double maxY = root.getPrefHeight() - radius;
            double dx = velocity.getX();
            double dy = velocity.getY();

            double posX = spot.getTranslateX() + dx;
            double posY = spot.getTranslateY() + dy;

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
            spot.setTranslateX(posX);
            spot.setTranslateY(posY);

            if (winner) {
                dy += 1; // Gravity
            }
            setVelocity(dx, dy);
            // velocity = velocity.multiply(0.99); // Friction
            rotateColor();
        }

        private void rotateColor() {
            if (color.equals(Color.BLACK))
                return;
            double hue = color.getHue() + 1;
            if (hue > 360) {
                hue -= 360;
            }
            color = Color.hsb(hue, 1, 1, 0.5);
            spot.setFill(color);
        }
    }

    private class ScoreBox {
        Text text;
        int score = 0;

        public ScoreBox() {
            text = new Text(20, 50, "");
            text.setFont(new Font(40));
            text.setFill(Color.RED);
        }

        public void incrementScore() {
            score++;
            update();
        }

        public Node getNode() {
            return text;
        }

        public void update() {
            if (score < NUM_SPOTS) {
                text.setText(String.format("SCORE: %d", score));
            } else {
                text.setText("YOU WIN!");
                winner = true;
            }
        }
    }

}
