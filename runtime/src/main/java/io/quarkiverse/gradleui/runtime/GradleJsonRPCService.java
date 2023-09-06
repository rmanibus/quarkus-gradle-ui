package io.quarkiverse.gradleui.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.arc.Unremovable;
import io.smallrye.common.annotation.NonBlocking;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
@Unremovable
public class GradleJsonRPCService {

    final GradleConfig config;

    @Inject
    GradleJsonRPCService(GradleConfig config) {
        this.config = config;
    }

    @NonBlocking
    public JsonObject getTasks() {
        JsonObject ret = new JsonObject();
        JsonArray tasksJson = new JsonArray();
        ret.put("tasks", tasksJson);
        for (Task task : config.getTasks()) {
            JsonObject taskJson = new JsonObject();
            taskJson.put("name", task.name);
            taskJson.put("description", task.description);
            taskJson.put("project", task.project);
            tasksJson.add(taskJson);
        }
        return ret;
    }

}
