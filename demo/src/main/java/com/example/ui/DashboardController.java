package com.example.ui;

import com.example.ConfigGlobal;
import com.example.project.SQLiteDao;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class DashboardController {

    @FXML private FlowPane projectFlowPane; // FXML 裡的容器，用來放卡片
    @FXML private Button btnCreateNew;      // 專案按鈕

    private SQLiteDao dao;

    @FXML
    public void initialize() {
        // 初始化資料庫連線
        dao = new SQLiteDao(ConfigGlobal.getDataSubPath(ConfigGlobal.DB_BASE));

        // 載入並繪製歷史專案卡片
        loadProjectCards();

        // 綁定「新增專案」按鈕
        btnCreateNew.setOnAction(e -> handleCreateNewProject());
    }

    private void loadProjectCards() {
        projectFlowPane.getChildren().clear();
        Map<String, String> projects = dao.getAllProjects();

        if (projects.isEmpty()) {
            Label emptyLabel = new Label("目前沒有任何專案，請點擊上方按鈕建立新掃描。");
            emptyLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 14px;");
            projectFlowPane.getChildren().add(emptyLabel);
            return;
        }

        // 動態生成每一張專案卡片
        for (Map.Entry<String, String> entry : projects.entrySet()) {
            String projectName = entry.getKey();
            String rootPathStr = entry.getValue();
            Path rootPath = Paths.get(rootPathStr);
            boolean isOnline = Files.exists(rootPath); // 檢查路徑是否存活！

            VBox card = createCard(projectName, rootPath, isOnline);
            projectFlowPane.getChildren().add(card);
        }
    }

    private VBox createCard(String projectName, Path rootPath, boolean isOnline) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setPrefSize(250, 120);
        card.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label("📁 " + projectName);
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; " + (isOnline ? "-fx-text-fill: #fff;" : "-fx-text-fill: #666;"));

        Label pathLabel = new Label(rootPath.toString());
        pathLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #aaa;");
        pathLabel.setWrapText(true);

        Label statusLabel = new Label(isOnline ? "🟢 就緒" : "🔴 找不到路徑 (硬碟未連接?)");
        statusLabel.setStyle(isOnline ? "-fx-text-fill: #4CAF50;" : "-fx-text-fill: #F44336; -fx-font-weight: bold;");

        card.getChildren().addAll(nameLabel, pathLabel, statusLabel);

        // 卡片樣式與互動
        String baseStyle = "-fx-background-color: #2b2b2b; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #444; -fx-border-width: 2; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: #3b3b3b; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #66b3ff; -fx-border-width: 2; -fx-cursor: hand;";

        card.setStyle(baseStyle);
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));

        // 點擊事件
        card.setOnMouseClicked(e -> {
            if (isOnline) {
                // 如果存活，跳轉到主畫面並自動載入
                switchToMainView(rootPath.toFile());
            } else {
                // 如果失效，詢問是否從資料庫刪除這個專案的紀錄
                promptDeleteOfflineProject(projectName);
            }
        });

        return card;
    }

    private void handleCreateNewProject() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("選擇要掃描的專案資料夾");
        File dir = chooser.showDialog(btnCreateNew.getScene().getWindow());
        if (dir != null) {
            switchToMainView(dir);
        }
    }

    private void promptDeleteOfflineProject(String projectName) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "找不到此專案的資料夾。\n是否要將「" + projectName + "」從歷史清單與資料庫中徹底移除？", ButtonType.YES, ButtonType.NO);
        alert.setTitle("無效的專案路徑");
        alert.setHeaderText("移除失效專案");
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                dao.deleteProject(projectName);
                loadProjectCards(); // 重新整理大廳畫面
            }
        });
    }

    /**
     * 切換場景到掃描主畫面
     */
    private void switchToMainView(File selectedDirectory) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main_view.fxml"));
            Parent root = loader.load();

            MainController mainController = loader.getController();
            mainController.initFromDashboard(selectedDirectory);

            // 取得目前的 Scene
            Scene currentScene = btnCreateNew.getScene();

            currentScene.setRoot(root);

            // 重新綁定關閉事件
            Stage stage = (Stage) currentScene.getWindow();
            stage.setOnCloseRequest(event -> mainController.shutdown());

        } catch (Exception e) {
            com.example.Debug.error("切換主畫面失敗: " + e.getMessage());
            e.printStackTrace();
        }
    }
}