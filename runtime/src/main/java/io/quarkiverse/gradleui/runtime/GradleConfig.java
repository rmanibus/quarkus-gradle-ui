package io.quarkiverse.gradleui.runtime;

import java.io.File;

public class GradleConfig {
    GradleConfig(File projectDir) {
        this.projectDir = projectDir;
    }

    final File projectDir;

    public File getProjectDir() {
        return projectDir;
    }
}
