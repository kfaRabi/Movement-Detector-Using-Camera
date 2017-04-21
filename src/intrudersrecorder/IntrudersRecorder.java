/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package intrudersrecorder;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author myotherself
 */
public class IntrudersRecorder extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("MainFXMLDocument.fxml"));
        
        Scene scene = new Scene(root);
        scene.getStylesheets().add("/intrudersrecorder/style.css");
        
        stage.setTitle("Intruders Recorder");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void stop(){
        //System.out.println("clsd");
        System.exit(0);
    }
    
}
