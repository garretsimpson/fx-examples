package com.gls;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

/***
 * Hilbert curves
 * 
 * Order 1
 * 0 1
 * 2 3
 * curve: 0,  1,  3,  2
 * delta: 0,(+1, +2, -1)
 * 
 * Order 2
 *  0  1  2  3
 *  4  5  6  7
 *  8  9 10 11
 * 12 13 14 15
 * curve: 0,  4,  5,  1,  2,  3,  7,  6, 10, 11, 15, 14, 13,  9,  8, 12
 * delta: 0,(+4, +1, -4,)+1,(+1, +4, -1,)+4,(+1, +4, -1,)-1,(-4, -1, +4) 
 * 
 */

/**
 * @author Garret Simpson (gsimpson@gmail.com)
 */
public class Hilb extends Application {

    private static final int SIZE_X = 1024;
    private static final int SIZE_Y = 800;
    private static final int ORDER = 7;
    private static final int SCALE = 5;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setScene(new Scene(createContent(), SIZE_X, SIZE_Y));
        stage.show();

        stage.getScene().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                Platform.exit();
            }
        });
    }

    private Parent createContent() {
        Pane root = new Pane();
        Hilbert hcurv = new Hilbert(ORDER);

        Polyline pl = new Polyline();
        int[] values = hcurv.asPoints();
        List<Double> points = new ArrayList<>();
        for (int v : values) {
            points.add(1.0 * v);
        }
        pl.getPoints().addAll(points);
        pl.setStroke(Color.BLACK);
        pl.setStrokeWidth(0.5);

        pl.setTranslateX(SCALE);
        pl.setTranslateY(SCALE);
        pl.getTransforms().add(new Scale(SCALE, SCALE));
        root.getChildren().add(pl);

        return root;
    }

    private static class Hilbert {
        private int order = 0;
        private int size = 1;
        private int[] seq;
        private int pos = 0;
        private int val = 0;

        Hilbert(int order) {
            this.order = order;
            size = 1 << order;
            seq = new int[size * size];
            h1(order, 1, size);
        }

        public int[] asPoints() {
            int[] result = new int[2 * seq.length];
            int i = 0;
            for (int v : seq) {
                result[i++] = v % size;
                result[i++] = v / size;
            }
            return result;
        }

        private void h1(int o, int a, int b) {
            if (o == 0) {
                seq[pos++] = val;
                return;
            }
            o--;
            h1(o, b, a);
            val += a;
            h1(o, a, b);
            val += b;
            h1(o, a, b);
            val -= a;
            h1(o, -b, -a);
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("Order: " + order + "\n");
            sb.append("Seq: ");
            for (int v : seq) {
                sb.append(" " + v);
            }
            sb.append("\n");
            sb.append("Points: ");
            for (int v : asPoints()) {
                sb.append(" " + v);
            }
            sb.append("\n");
            return sb.toString();
        }

    }
}
