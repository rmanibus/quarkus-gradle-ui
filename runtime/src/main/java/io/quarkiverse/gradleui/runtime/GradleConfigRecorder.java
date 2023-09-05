package io.quarkiverse.gradleui.runtime;

import java.io.File;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class GradleConfigRecorder {

    public RuntimeValue<GradleConfig> create(String projectDir) {
        return new RuntimeValue<>(new GradleConfig(new File(projectDir)));
    }
}
