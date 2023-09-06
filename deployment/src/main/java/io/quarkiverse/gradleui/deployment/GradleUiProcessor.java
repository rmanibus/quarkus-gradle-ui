package io.quarkiverse.gradleui.deployment;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.GradleProject;

import io.quarkiverse.gradleui.runtime.GradleConfig;
import io.quarkiverse.gradleui.runtime.GradleConfigRecorder;
import io.quarkiverse.gradleui.runtime.GradleJsonRPCService;
import io.quarkiverse.gradleui.runtime.Task;
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
import io.quarkus.devui.spi.page.FooterPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.WebComponentPageBuilder;

class GradleUiProcessor {

    private static final String FEATURE = "gradle-ui";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.STATIC_INIT)
    public void produceGradleConfig(BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            List<BuildTaskItem> buildTaskItems,
            GradleConfigRecorder recorder) {

        File projectDir = highestKnownProjectDirectory(curateOutcomeBuildItem, outputTargetBuildItem);

        syntheticBeans.produce(
                SyntheticBeanBuildItem.configure(GradleConfig.class)
                        .scope(ApplicationScoped.class)
                        .unremovable()
                        .runtimeValue(recorder.create(projectDir.getAbsolutePath(),
                                buildTaskItems.stream().map(BuildTaskItem::getTask)
                                        .collect(Collectors.toList())))
                        .done());
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public JsonRPCProvidersBuildItem rpcProvider() {
        return new JsonRPCProvidersBuildItem(GradleJsonRPCService.class);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public void gradleTasks(
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            BuildProducer<BuildTaskItem> buildTaskProducer) {
        try (ProjectConnection con = GradleConnector.newConnector()
                .forProjectDirectory(highestKnownProjectDirectory(curateOutcomeBuildItem, outputTargetBuildItem))
                .connect()) {

            List<Task> tasks = traverse(con.getModel(GradleProject.class));
            for (Task task : tasks) {
                buildTaskProducer.produce(new BuildTaskItem(task));
            }
        }
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public CardPageBuildItem pages(List<BuildTaskItem> buildTaskItems,
            BuildProducer<FooterPageBuildItem> footerPages) {
        CardPageBuildItem pageBuildItem = new CardPageBuildItem();

        pageBuildItem
                .addPage(Page.webComponentPageBuilder()
                        .icon("font-awesome-solid:list-check")
                        .componentLink("qwc-build-tasks.js")
                        .staticLabel(String.valueOf(buildTaskItems.size())));

        WebComponentPageBuilder logPageBuilder = Page.webComponentPageBuilder()
                .icon("font-awesome-solid:list-check")
                .title("Build UI")
                .componentLink("qwc-build-log.js");
        footerPages.produce(new FooterPageBuildItem(logPageBuilder));

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

}
