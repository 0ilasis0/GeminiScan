package com.example;

public enum ViewPath {
    // src/main/resources/fxml/main_view.fxml
    FXML_MAIN("/fxml/main_view.fxml"),
    FXML_DASHBOARD("/fxml/dashboard_view.fxml");

    public final String path;
    ViewPath(String path) { this.path = path; }
}
