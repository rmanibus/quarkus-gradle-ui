package io.quarkiverse.gradleui.runtime;

import java.io.File;
import java.util.List;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class BuildConfigRecorder {

    public RuntimeValue<BuildConfig> create(String projectDir, List<Task> tasks) {
        return new RuntimeValue<>(new BuildConfig(new File(projectDir), tasks));
    }
}
