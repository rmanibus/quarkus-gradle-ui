package io.quarkiverse.gradleui.runtime;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.GradleTask;

import io.quarkus.arc.Unremovable;
import io.smallrye.common.annotation.NonBlocking;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
@Unremovable
public class GradleJsonRPCService {

    final List<Task> tasks;

    @Inject
    GradleJsonRPCService(GradleConfig config) {
        try (ProjectConnection con = GradleConnector.newConnector()
                .forProjectDirectory(config.getProjectDir())
                .connect()) {
            GradleProject project = con.getModel(GradleProject.class);
            tasks = traverse(project);
        }
    }

    private List<Task> traverse(GradleProject gradleProject) {
        List<Task> tasks = gradleProject.getTasks().stream()
                .map(Task::new)
                .collect(Collectors.toList());
        for (var child : gradleProject.getChildren().getAll()) {
            tasks.addAll(traverse(child));
        }
        return tasks;
    }

    @NonBlocking
    public JsonObject getTasks() {
        JsonObject ret = new JsonObject();
        JsonArray tasksJson = new JsonArray();
        ret.put("tasks", tasksJson);
        for (Task task : tasks) {
            JsonObject taskJson = new JsonObject();
            taskJson.put("name", task.name);
            taskJson.put("description", task.description);
            taskJson.put("project", task.project);
            tasksJson.add(taskJson);
        }
        return ret;
    }

    public static class Task {
        Task(GradleTask task) {
            this.project = task.getProject().getName();
            this.name = task.getName();
            this.description = task.getDescription();
        }

        public final String project;
        public final String name;
        public final String description;
    }
}
