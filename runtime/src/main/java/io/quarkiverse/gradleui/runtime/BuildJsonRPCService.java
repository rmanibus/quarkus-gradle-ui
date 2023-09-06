package io.quarkiverse.gradleui.runtime;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.gradle.tooling.*;
import org.gradle.tooling.events.ProgressListener;

import io.quarkus.arc.Unremovable;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
@Unremovable
public class BuildJsonRPCService {

    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy - HH:mm:ss Z");
    final BuildConfig config;

    final BroadcastProcessor<JsonObject> progress = BroadcastProcessor.create();
    final BroadcastProcessor<JsonObject> result = BroadcastProcessor.create();;

    @Inject
    BuildJsonRPCService(BuildConfig config) {
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

    public Multi<JsonObject> streamProgress() {
        return progress;
    }

    public Multi<JsonObject> streamResult() {
        return result;
    }

    public JsonObject executeTask(String task) {
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(config.getProjectDir())
                .connect()) {
            BuildLauncher build = connection.newBuild();
            build.addProgressListener((ProgressListener) event -> {
                progress.onNext(new JsonObject()
                        .put("task", task)
                        .put("timestamp",
                                formatter.format(Instant.ofEpochMilli(event.getEventTime()).atZone(ZoneId.systemDefault())))
                        .put("message", event.getDisplayName()));
            });
            build.forTasks(task);
            build.run(new ResultHandler<>() {

                @Override
                public void onComplete(Void r) {
                    result.onNext(new JsonObject()
                            .put("task", task)
                            .put("success", true));
                }

                @Override
                public void onFailure(GradleConnectionException failure) {
                    result.onNext(new JsonObject()
                            .put("task", task)
                            .put("success", false)
                            .put("message", failure.getMessage()));
                }
            });
            return new JsonObject()
                    .put("success", true);
        } catch (Exception e) {
            return new JsonObject()
                    .put("success", false)
                    .put("message", e.getMessage());
        }
    }
}
