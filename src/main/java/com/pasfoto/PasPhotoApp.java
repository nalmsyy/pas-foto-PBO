package com.pasfoto;

import com.pasfoto.controller.PhotoController;
import com.pasfoto.gui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class PasPhotoApp extends Application {
    @Override
    public void start(Stage stage) {
        PhotoController controller = new PhotoController();
        MainView mainView = new MainView(controller, stage);

        Scene scene = new Scene(mainView.getRoot(), 1100, 700);
        stage.setTitle("Pas Foto Background Remover");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
