package io.quarkiverse.gradleui.gradle.ui.deployment;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.GradleTask;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

class GradleUiProcessor {

    private static final String FEATURE = "gradle-ui";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public CardPageBuildItem pages(
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem outputTargetBuildItem) {
        File rootFolder = highestKnownProjectDirectory(curateOutcomeBuildItem, outputTargetBuildItem);
        CardPageBuildItem pageBuildItem = new CardPageBuildItem();
        try (ProjectConnection con = GradleConnector.newConnector()
                .forProjectDirectory(rootFolder)
                .connect()) {

            GradleProject project = con.getModel(GradleProject.class);
            pageBuildItem
                    .addBuildTimeData("gradleTasks", traverse(project));
            pageBuildItem
                    .addPage(Page.tableDataPageBuilder("All Tasks")
                            .buildTimeDataKey("gradleTasks")
                            .icon("font-awesome-solid:table")
                            .showColumn("project")
                            .showColumn("name")
                            .showColumn("description"));
        }

        return pageBuildItem;
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

    private File highestKnownProjectDirectory(CurateOutcomeBuildItem curateOutcomeBuildItem,
                                              OutputTargetBuildItem outputTargetBuildItem) {
        ApplicationModel applicationModel = curateOutcomeBuildItem.getApplicationModel();
        WorkspaceModule workspaceModule = applicationModel.getAppArtifact().getWorkspaceModule();
        if (workspaceModule != null) {
            // in this case we know the precise project root
            return workspaceModule.getModuleDir();
        }
        // in this case we will simply use the build directory and let jgit go up the file system to determine the git directory - if any
        return outputTargetBuildItem.getOutputDirectory().toFile();
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
