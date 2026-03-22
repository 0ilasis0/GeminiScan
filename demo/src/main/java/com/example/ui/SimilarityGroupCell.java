package com.example.ui;

import com.example.data.MediaAsset;
import com.example.data.SimilarityGroup;
import com.example.engine.file.FileManagement;
import com.example.util.DesktopUtils;
import com.example.util.DialogUtils;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;

import java.util.LinkedHashMap;
import java.util.Map;

public class SimilarityGroupCell extends ListCell<SimilarityGroup> {

    // 重複利用的 UI 元件
    private final VBox container;
    private final Label titleLabel;
    private final ScrollPane imageScrollPane;
    private final HBox imageRow;
    private final FileManagement fileManager;

    // 全域共用的簡單圖片快取 (LRU Cache)
    private static final Map<String, Image> imageCache = new LinkedHashMap<>(
        (int) Math.ceil(Config.MAX_CACHE_SIZE / 0.75f) + 1,
        0.75f,
        true
    ) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
            return size() > Config.MAX_CACHE_SIZE;
        }
    };

    public SimilarityGroupCell(FileManagement fileManager) {
        super();
        this.fileManager = fileManager;

        container = new VBox(Config.CELL_SPACING);
        container.setPadding(new Insets(Config.CELL_PADDING));
        container.setStyle("-fx-border-color: #444; -fx-border-width: 0 0 1 0;");

        titleLabel = new Label();
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #aaa;");

        imageRow = new HBox(Config.IMAGE_ROW_SPACING);
        imageRow.setAlignment(Pos.CENTER_LEFT);
        imageRow.setMinHeight(Config.H_BOX_MIN_HEIGHT);

        imageScrollPane = new ScrollPane(imageRow);
        // 依賴原生滾動條：當圖片超過寬度時自動顯示，平時隱藏
        imageScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        imageScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        imageScrollPane.setFitToHeight(true);

        // 保持透明背景，讓 UI 看起來乾淨
        imageScrollPane.setStyle("-fx-background-color: transparent; -fx-viewport-fx-background-color: transparent; -fx-border-color: transparent;");

        // 將組裝好的 ScrollPane 放進 Cell 容器
        container.getChildren().addAll(titleLabel, imageScrollPane);
    }

    @Override
    protected void updateItem(SimilarityGroup group, boolean empty) {
        super.updateItem(group, empty);

        if (empty || group == null) {
            setGraphic(null);
            setText(null);
        } else {
            titleLabel.setText("📸 相似群組 (Hash: " + Long.toHexString(group.getGroupHash()) + ")");

            imageRow.getChildren().clear();
            imageScrollPane.setHvalue(0.0); // 重置滾動條到最左邊

            for (MediaAsset asset : group.getAssets()) {
                imageRow.getChildren().add(createImageItem(asset));
            }

            setGraphic(container);
        }
    }

    private VBox createImageItem(MediaAsset asset) {
        VBox card = new VBox(Config.CARD_SPACING);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(Config.CARD_WIDTH);

        String uriStr = asset.getFilePath().toFile().toURI().toString();
        String fileNameLower = asset.getFileName().toLowerCase();


        Image thumb = imageCache.computeIfAbsent(uriStr, k -> {
            // 如果是 webp 格式，透過 ImageIO 轉譯
            if (fileNameLower.endsWith(".webp")) {
                try {
                    // 呼叫你寫好的神級降採樣工具，只讀取縮圖大小，極度省 RAM
                    java.awt.image.BufferedImage bimg = com.example.engine.finger.image.ImageUtils.readSubsampledImage(asset.getFilePath(), Config.THUMBNAIL_SIZE);
                    if (bimg != null) {
                        // 將 AWT BufferedImage 無縫轉換為 JavaFX Image
                        return SwingFXUtils.toFXImage(bimg, null);
                    }
                } catch (Exception ex) {
                    com.example.Debug.error("無法載入 WebP 預覽圖: " + asset.getFilePath());
                }
            }

            // 如果是普通的 JPG/PNG，或是 WebP 讀取失敗，就退回使用 JavaFX 原生極速載入
            return new Image(k, Config.THUMBNAIL_SIZE, Config.THUMBNAIL_SIZE, true, true, true);
        });

        ImageView imageView = new ImageView(thumb);
        StackPane imgContainer = new StackPane(imageView);
        imgContainer.setStyle("-fx-border-color: #555; -fx-background-color: #333;");
        imgContainer.setPrefSize(155, 155);

        Label nameLabel = new Label(asset.getFileName());
        nameLabel.setStyle("-fx-font-size: 10px;");
        nameLabel.setWrapText(true);

        Label sizeLabel = new Label(String.format("%.2f MB", asset.getFileSize() / (1024.0 * 1024.0)));
        sizeLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #888;");

        CheckBox checkBox = new CheckBox("選取刪除");
        checkBox.setSelected(fileManager.isMarked(asset));
        checkBox.setOnAction(e -> {
            if (checkBox.isSelected()) {
                fileManager.markForDeletion(asset);
            } else {
                fileManager.unmarkForDeletion(asset);
            }
            com.example.Debug.log("目前待刪除數量: " + fileManager.getPendingDeleteCount());
        });

        card.getChildren().setAll(imgContainer, nameLabel, sizeLabel, checkBox);
        bindCardInteractions(card, asset);

        return card;
    }

    private void bindCardInteractions(VBox card, MediaAsset asset) {
        ContextMenu contextMenu = createContextMenu(asset);
        card.setOnContextMenuRequested(e -> {
            contextMenu.show(card, e.getScreenX(), e.getScreenY());
        });

        card.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                DialogUtils.showAssetDetails(asset);
            }
        });
    }

    private ContextMenu createContextMenu(MediaAsset asset) {
        ContextMenu menu = new ContextMenu();

        MenuItem detailsItem = new MenuItem("📄 顯示詳細資料");
        detailsItem.setOnAction(e -> DialogUtils.showAssetDetails(asset));

        MenuItem openImageItem = new MenuItem("📸 開啟原圖");
        openImageItem.setOnAction(e -> DesktopUtils.openImage(asset.getFilePath()));

        MenuItem openFolderItem = new MenuItem("📂 開啟檔案位置");
        openFolderItem.setOnAction(e -> DesktopUtils.openFileLocation(asset.getFilePath()));

        menu.getItems().addAll(detailsItem, openImageItem, openFolderItem);
        return menu;
    }
}
