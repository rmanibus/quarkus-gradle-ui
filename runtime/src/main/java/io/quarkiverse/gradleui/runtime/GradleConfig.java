package io.quarkiverse.gradleui.runtime;

import java.io.File;
import java.util.List;

public class GradleConfig {
    GradleConfig(File projectDir, List<Task> tasks) {
        this.projectDir = projectDir;
        this.tasks = tasks;
    }

    final File projectDir;
    final List<Task> tasks;

    public File getProjectDir() {
        return projectDir;
    }

    public List<Task> getTasks() {
        return this.tasks;
    }
}
