/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package intrudersrecorder;

import com.sun.deploy.net.DownloadEngine;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

/**
 *
 * @author myotherself
 */
public class MainFXMLDocumentController implements Initializable {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
//    FXML NODES
    @FXML
    private ImageView imageView;
    @FXML
    private ToggleButton cameraSwitch;
    @FXML
    private Color x2;
    @FXML
    private Font x1;
    @FXML
    private Insets x3;
    @FXML
    private Slider resWidthSlider;
    @FXML
    private Label resHeight;
    @FXML
    private Slider resHeightSlider;
    @FXML
    private Label pixDiff;
    @FXML
    private Slider pixDiffSlider;
    @FXML
    private Label objSize;
    @FXML
    private Slider objSizeSlider;
    @FXML
    private Label borderSize;
    @FXML
    private Label resWidth;
    
//    OCV VARIABLES
    private VideoCapture vidCap;
    private Mat prevMat, subsRes, singleFrame;
    
//    MY VARIABLES
    private ScheduledExecutorService SES;
    private Image inputImage;
    private static boolean applicationShouldClose = false;
    private boolean hasPrevious = false;
    private static int minPixelDiff = 20;
    private static int minPixelsOfAnObject = 1500;
    private int cnt;
    private int wi;
    private int hi;
    private int topLeftR;
    private int topLeftC;
    private int bottomRightR;
    private int bottomRightC;
    private ArrayList<Coordinates> col;
    private int resolutionWidth;
    private int resolutionHeight;
    private int borderSz = 2;
    private int borderClr = 0;
    @FXML
    private ToggleButton toggleColor;

    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        vidCap = new VideoCapture();
        
        prevMat = new Mat();
        subsRes = new Mat();
        col = new ArrayList<>();
        
        resolutionWidth = 100;
        resolutionHeight = 100;
        
        
        sliderInit();
        resSliderListener();
        pixDiffSliderListener();
        objSizeSliderListener();
        
        cameraSwitch.setStyle("-fx-background-color: red");
    }    

    @FXML
    private void StartMenuAction(ActionEvent event) {
        vidCap.open(0);
        vidCap.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, resolutionHeight);
        vidCap.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, resolutionWidth);
        SES = Executors.newSingleThreadScheduledExecutor();
        SES.scheduleAtFixedRate(() -> imageView.setImage(grabFrame()), 0, 10, TimeUnit.MILLISECONDS);
    }

    @FXML
    private void StopMenuAction(ActionEvent event) {
        SES.shutdown();
        if (vidCap.isOpened()) {
            vidCap.release();
        }
//        imageView.setFitHeight(450.0);
//        imageView.setFitWidth(600.0);
//        Mat singleFrame = new Mat((int)imageView.getFitWidth(), (int)imageView.getFitHeight(), CvType.CV_8UC1, new Scalar(0));
//        MatOfByte buff = new MatOfByte();
//        Imgcodecs.imencode(".jpg", singleFrame, buff);
//        blackScreen = new Image(new ByteArrayInputStream(buff.toArray()));
//        imageView.setImage(blackScreen);
    }

    private Image grabFrame() {
        inputImage = null;
        singleFrame = new Mat();
        if(vidCap.isOpened()){
            try {
                vidCap.read(singleFrame);
                Imgproc.cvtColor(singleFrame, singleFrame, Imgproc.COLOR_BGR2GRAY);
                if(!hasPrevious){
                    MatOfByte buff = new MatOfByte();
                    Imgcodecs.imencode(".jpg", singleFrame, buff);inputImage = new Image(new ByteArrayInputStream(buff.toArray()));
                }
            } catch (Exception e) {
                System.out.println("problem in reading frame");
            }
        }
        if(hasPrevious){
            Core.absdiff(singleFrame, prevMat, subsRes);
            hi = singleFrame.rows();
            wi = singleFrame.cols();
            col.clear();
            for(int r = 0; r < hi; r++){
                for(int c = 0; c < wi; c++){
//                    double v1[] = singleFrame.get(r, c);
//                    double v2[] = prevMat.get(r, c);
//                    double diff = Math.abs(v1[0] - v2[0]);
                    prevMat.put(r, c, singleFrame.get(r, c)[0]);
                    if(subsRes.get(r, c)[0] >= minPixelDiff){
                        subsRes.put(r, c, 255);
                        col.add(new Coordinates(r, c));
                    }
                    else{
                        subsRes.put(r, c, 0);
                    }
                }
            }
            
            //prevMat = singleFrame.clone();
            
            boolean ok = true;
            int sz = col.size();
            for(int i = 0; i < sz; i++){
                double v[] = subsRes.get(col.get(i).getR(), col.get(i).getC());
                if(v[0] == 255){
                    topLeftR = 2 * hi;
                    topLeftC = 2 * wi;
                    bottomRightR = -1;
                    bottomRightC = -1;
                    cnt = 1;
                    floodFill(col.get(i).getR(), col.get(i).getC());
                    if(cnt >= minPixelsOfAnObject){
//                            System.out.println("burglur........"+cnt);
                        //floodOne(r, c);
                        drawBorder();
                        //ok = false;
                    }
                }
            }

            
            // using non recursive flood fill
            
//            for(int i = 0; i < sz; i++){
//                topLeftR = 2 * hi;
//                topLeftC = 2 * wi;
//                bottomRightR = -1;
//                bottomRightC = -1;
//                cnt = 1;
//                Coordinates c = new Coordinates(col.get(i).getR(), col.get(i).getC());
//                if(subsRes.get(c.getR(), c.getC())[0] == 255){
//                    nonRecursiveFloodfill(c);
//                    if(cnt >= minPixelsOfAnObject){
//                        System.out.println("burglur........"+cnt);
//                        //floodOne(r, c);
//                        //drawBorder();
//                        //ok = false;
//                    }
//                }
//            }


// increase brightness of the changed pixels            
//            for(int r = 0; r < hi; r++){
//                for(int c = 0; c < wi; c++){
//                    double v[] = subsRes.get(r, c);
//                    double originalV[] = singleFrame.get(r, c);
//                    if(v[0] == 1){
////                        if(originalV[0] * 1.1 > 255){
////                            subsRes.put(r, c, 255);
////                        }
////                        else{
////                            subsRes.put(r, c, originalV[0] * 1.1);
////                        }
//                    }
//                    else{
//                        subsRes.put(r, c, originalV[0]);
//                    }
//                }
//            }
            
            
//            System.out.println("h: "+subsRes.rows() + " w "+subsRes.cols());
            MatOfByte buff = new MatOfByte();
            Imgcodecs.imencode(".jpg", singleFrame, buff);
            inputImage = new Image(new ByteArrayInputStream(buff.toArray()));
        }
        else{
            //System.out.println("first time");
            hasPrevious = true;
            prevMat = singleFrame.clone();
            //System.out.println("h: "+prevMat.rows() + " pppp w "+prevMat.cols());
            //System.out.println("h: "+singleFrame.size().height + " sss w "+singleFrame.size().width);
            //subsRes = new Mat(prevMat.rows(), prevMat.cols(), CvType.CV_8UC1);
            //System.out.println(singleFrame.dump());
            //System.out.println("done 1st");
        }
        return inputImage;
    }
    
    
    int dr[] = {-1, +1, +0, +0};
    int dc[] = {+0, +0, -1, +1};
    
    
//    private void flood(int r, int c) {
//        if(r >= 0 && r < hi && c >= 0 && c < wi){
//            double v[] = subsRes.get(r, c);
//            if(v[0] == 255){
//                topLeftR = Math.min(r, topLeftR);
//                topLeftC = Math.min(c, topLeftC);
//                bottomRightR = Math.max(r, bottomRightR);
//                bottomRightC = Math.max(c, bottomRightC);
//                cnt++;
//                subsRes.put(r, c, 2);
//                for(int i = 0; i < 4; i++){
//                    flood(r + dr[i], c + dc[i]);
//                }
//            }
//            else{
//                return;
//            }
//        }
//        else{
//            return;
//        }
//    }

    private void floodFill(int r, int c) {
        topLeftR = Math.min(r, topLeftR);
        topLeftC = Math.min(c, topLeftC);
        bottomRightR = Math.max(r, bottomRightR);
        bottomRightC = Math.max(c, bottomRightC);
        cnt++;
        subsRes.put(r, c, 2);
        for(int i = 0; i < 4; i++){
            if((r + dr[i]) >= 0 && (r + dr[i]) < hi && (c + dc[i]) >= 0 && (c + dc[i]) < wi && subsRes.get(r + dr[i], c + dc[i])[0] == 255){
                floodFill(r + dr[i], c + dc[i]);
            }
        }
    }
    
//    private void floodOne(int r, int c) {
//        if(r >= 0 && r < hi && c >= 0 && c < wi){
//            double v[] = subsRes.get(r, c);
//            if(v[0] == 2){
//                subsRes.put(r, c, 1);
//                for(int i = 0; i < 4; i++){
//                    flood(r + dr[i], c + dc[i]);
//                }
//            }
//            else{
//                return;
//            }
//        }
//        else{
//            return;
//        }
//    }

    private void nonRecursiveFloodfill(Coordinates cord){
        Queue<Coordinates> q = new LinkedList<>();
        q.add(cord);
        while(!q.isEmpty()){
            cnt++;
            cord = q.poll();
            int r = cord.getR(), c = cord.getC();
            subsRes.put(r, c, 2);
            for(int i = 0; i < 4; i++){
                if((r + dr[i]) >= 0 && (r + dr[i]) < hi && (c + dc[i]) >= 0 && (c + dc[i]) < wi && subsRes.get(r + dr[i], c + dc[i])[0] == 255){
                    q.add(new Coordinates(r + dr[i], c + dc[i]));
                }
            }
        }
    }
    
    private void drawBorder() {
        for(int r = topLeftR - borderSz; r <= bottomRightR + borderSz; r++){
            for(int x = -borderSz; x < 0; x++){
                if(r >= 0 && r < hi && (topLeftC + x) >= 0 && (topLeftC + x) < wi)
                    singleFrame.put(r, topLeftC + x, borderClr);
                if(r >= 0 && r < hi && (topLeftC - x) >= 0 && (topLeftC - x) < wi)
                    singleFrame.put(r, bottomRightC - x, borderClr);
            }
        }
        for(int c = topLeftC; c <= bottomRightC; c++){
            for(int x = -borderSz; x < 0; x++){
                if((topLeftR + x) >= 0 && (topLeftR + x) < hi && c >= 0 && c < wi)
                    singleFrame.put(topLeftR + x, c, borderClr);
                if((topLeftR - x) >= 0 && (topLeftR - x) < hi && c >= 0 && c < wi)
                    singleFrame.put(bottomRightR - x, c, borderClr);
            }
        }
    }

    @FXML
    private void cameraSwitchAction(ActionEvent event) {
        if(cameraSwitch.getText().equals("Turn On Camera")){
            vidCap.open(0);
            vidCap.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT, resolutionHeight);
            vidCap.set(Videoio.CV_CAP_PROP_FRAME_WIDTH, resolutionWidth);
            SES = Executors.newSingleThreadScheduledExecutor();
            SES.scheduleAtFixedRate(() -> imageView.setImage(grabFrame()), 0, 10, TimeUnit.MILLISECONDS);
            cameraSwitch.setText("Turn Off Camera");
            cameraSwitch.setStyle("-fx-background-color:green");
            
        }
        else{
            SES.shutdown();
            if (vidCap.isOpened()) {
                vidCap.release();
            }
            cameraSwitch.setStyle("-fx-background-color:red");
            cameraSwitch.setText("Turn On Camera");
            hasPrevious = false;
        }
    }

    private void resSliderListener() {
        resWidthSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                resolutionWidth = (int)newValue.doubleValue();
                resWidth.setText(String.valueOf(resolutionWidth));
            }
        });
        resHeightSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                //System.out.println("old="+oldValue.doubleValue()+" new="+newValue.doubleValue());
                resolutionHeight = (int)newValue.doubleValue();
                resHeight.setText(String.valueOf(resolutionHeight));
            }
        });
    }
    
    private void pixDiffSliderListener(){
        pixDiffSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                //System.out.println("old="+oldValue.doubleValue()+" new="+newValue.doubleValue());
                minPixelDiff = (int)newValue.doubleValue();
                pixDiff.setText(String.valueOf(minPixelDiff));
            }
        });        
    }

    private void objSizeSliderListener(){
        objSizeSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                //System.out.println("old="+oldValue.doubleValue()+" new="+newValue.doubleValue());
                minPixelsOfAnObject = (int)newValue.doubleValue();
                objSize.setText(String.valueOf(minPixelsOfAnObject));
            }
        });        
    }
    
    
    private void sliderInit(){
        resWidth.setText(String.valueOf(resolutionWidth));
        resWidthSlider.setMin(50);
        resWidthSlider.setMax(1024);
        resWidthSlider.setValue(resolutionWidth);
        resWidthSlider.setMajorTickUnit(100);
        resWidthSlider.setMinorTickCount(50);        
        
        resHeight.setText(String.valueOf(resolutionHeight));
        resHeightSlider.setMin(50);
        resHeightSlider.setMax(786);
        resHeightSlider.setValue(resolutionHeight);
        resHeightSlider.setMajorTickUnit(100);
        resHeightSlider.setMinorTickCount(50);     
        
        pixDiff.setText(String.valueOf(minPixelDiff));
        pixDiffSlider.setMin(1);
        pixDiffSlider.setMax(255);
        pixDiffSlider.setValue(minPixelDiff);
        pixDiffSlider.setMajorTickUnit(10);
        pixDiffSlider.setMinorTickCount(5);     
        
        objSize.setText(String.valueOf(minPixelsOfAnObject));
        objSizeSlider.setMin(1);
        objSizeSlider.setMax(100000);
        objSizeSlider.setValue(minPixelsOfAnObject);
        objSizeSlider.setMajorTickUnit(10000);
        objSizeSlider.setMinorTickCount(1000);
        
        borderSize.setText(String.valueOf(borderSz));
        toggleColor.setStyle("-fx-border-color:black black black black");
        //toggleColor.setStyle("-fx-background-color:black");
        //toggleColor.setStyle("-fx-text-fill:white");
    }
    
    
    @FXML
    private void reduceBorderSize(ActionEvent event) {
        if(borderSz - 1 > 0){
            borderSz--;
            borderSize.setText(String.valueOf(borderSz));
        }
    }

    @FXML
    private void increaseBorderSize(ActionEvent event) {
        if((borderSz + 1) <= (resolutionHeight + resolutionWidth)/4){
            borderSz++;
            borderSize.setText(String.valueOf(borderSz));
        }
    }

    @FXML
    private void changeBorderColor(ActionEvent event) {
        if(toggleColor.getText().equals("Set White")){
            borderClr = 255;
            toggleColor.setText("Set Black");
            toggleColor.setStyle("-fx-border-color:white white white white");
        }
        else{
            borderClr = 0;
            toggleColor.setText("Set White");
            toggleColor.setStyle("-fx-border-color:black black black black");
        }
    }
    
    
    
}
