package io.quarkiverse.gradleui.runtime;

import org.gradle.tooling.model.GradleTask;

public class Task {

    public Task() {

    }

    public Task(GradleTask task) {
        this.project = task.getProject().getName();
        this.name = task.getName();
        this.description = task.getDescription();
    }

    public String project;
    public String name;
    public String description;
}
