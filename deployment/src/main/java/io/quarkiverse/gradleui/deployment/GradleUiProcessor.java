package io.quarkiverse.gradleui.deployment;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.GradleTask;

import io.quarkiverse.gradleui.runtime.GradleConfig;
import io.quarkiverse.gradleui.runtime.GradleConfigRecorder;
import io.quarkiverse.gradleui.runtime.GradleJsonRPCService;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

class GradleUiProcessor {

    private static final String FEATURE = "gradle-ui";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.STATIC_INIT)
    public void gatherRateLimitCheck(BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            GradleConfigRecorder recorder) {

        File projectDir = highestKnownProjectDirectory(curateOutcomeBuildItem, outputTargetBuildItem);

        syntheticBeans.produce(
                SyntheticBeanBuildItem.configure(GradleConfig.class)
                        .scope(ApplicationScoped.class)
                        .unremovable()
                        .runtimeValue(recorder.create(projectDir.getAbsolutePath()))
                        .done());
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public JsonRPCProvidersBuildItem rpcProvider() {
        return new JsonRPCProvidersBuildItem(GradleJsonRPCService.class);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public CardPageBuildItem pages(
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem outputTargetBuildItem) {
        CardPageBuildItem pageBuildItem = new CardPageBuildItem();
        try (ProjectConnection con = GradleConnector.newConnector()
                .forProjectDirectory(highestKnownProjectDirectory(curateOutcomeBuildItem, outputTargetBuildItem))
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

            pageBuildItem
                    .addPage(Page.webComponentPageBuilder()
                            .icon("font-awesome-solid:clock")
                            .componentLink("qwc-gradle-tasks.js")
                            .staticLabel("test"));
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
