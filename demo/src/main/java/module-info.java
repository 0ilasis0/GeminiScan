module demo {
    // JavaFX 模組
    requires javafx.controls;
    requires transitive javafx.graphics;
    requires javafx.fxml;
    requires javafx.swing;

    // SQLite 需要用到 SQL 模組
    requires java.sql;

    // TwelveMonkeys 影像庫通常會用到 java.desktop (AWT/ImageIO)
    requires java.desktop;

    // others
    requires java.prefs;
    requires static lombok;

    // 讓 JavaFX 能夠透過 Reflection 讀取你的 FXML 控制器
    opens com.example.ui to javafx.fxml;
    opens com.example.engine.finger.image to javafx.fxml;

    // 匯出你的套件，讓其他模組可以存取你的類別
    exports com.example;
    exports com.example.data;
    exports com.example.engine;
    exports com.example.project;
    exports com.example.engine.file;
    exports com.example.engine.finger;
}