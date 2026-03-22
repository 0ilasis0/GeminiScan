package com.example.project;

public enum ProjectColumn {
    PROJECT_NAME("project_name"),
    ROOT_DIRECTORY("root_directory");

    public final String value;
    ProjectColumn(String value) { this.value = value; }
}
