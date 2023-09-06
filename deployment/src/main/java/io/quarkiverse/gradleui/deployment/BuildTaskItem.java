package io.quarkiverse.gradleui.deployment;

import io.quarkiverse.gradleui.runtime.Task;
import io.quarkus.builder.item.MultiBuildItem;

public final class BuildTaskItem extends MultiBuildItem {

    final Task task;

    public BuildTaskItem(Task task) {
        this.task = task;
    }

    public Task getTask() {
        return this.task;
    }
}
