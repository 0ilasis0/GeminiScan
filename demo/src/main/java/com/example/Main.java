package com.example;

import javafx.application.Platform;
import com.example.ui.DashboardController;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            // 載入大廳的 FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ViewPath.FXML_DASHBOARD.path));
            Parent root = loader.load();

            // 取得 DashboardController 的實體，使建立資源
            DashboardController controller = loader.getController();

            // 建立 Scene，並設定一個寬敞的初始大小
            Scene scene = new Scene(root, ConfigGlobal.SCENE_WIDTH, ConfigGlobal.SCENE_HEIGHT);

            // 設定視窗屬性
            primaryStage.setTitle(ConfigGlobal.APP_TITLE + " - 專案管理大廳");

            // 設定視窗最小寬度與高度，防止 UI 擠成一團
            primaryStage.setMinWidth(ConfigGlobal.SCENE_MIN_WIDTH);
            primaryStage.setMinHeight(ConfigGlobal.SCENE_MIN_HEIGHT);
            primaryStage.setMaximized(true);

            // 捕捉視窗右上角「X」的關閉事件
            primaryStage.setOnCloseRequest(event -> {
                Debug.log("收到關閉視窗請求，正在安全釋放資源...");
                // 在大廳直接關閉即可，不需要呼叫 shutdown
                Platform.exit();
            });

            // 將 Scene 放到 Stage 上，並展現出來！
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() throws Exception {
        Debug.log("資源釋放完畢，程式結束。");
        System.exit(0);
    }

    public static void main(String[] args) {
        Debug.log("系統啟動，正在載入 JavaFX 介面...");
        launch(args);
    }
}
