package configurations

import common.Os
import common.applyDefaultSettings
import common.buildToolGradleParameters
import common.gradleWrapper
import jetbrains.buildServer.configs.kotlin.v2018_2.AbsoluteId
import jetbrains.buildServer.configs.kotlin.v2018_2.BuildStep
import jetbrains.buildServer.configs.kotlin.v2018_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2018_2.Dependencies
import jetbrains.buildServer.configs.kotlin.v2018_2.FailureAction
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2018_2.triggers.ScheduleTrigger
import jetbrains.buildServer.configs.kotlin.v2018_2.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.v2018_2.triggers.schedule
import jetbrains.buildServer.configs.kotlin.v2018_2.triggers.vcs
import model.CIBuildModel
import model.Stage
import model.StageNames
import model.Trigger
import projects.StageProject

class StagePasses(model: CIBuildModel, stage: Stage, prevStage: Stage?, stageProject: StageProject) : BaseGradleBuildType(model, init = {
    uuid = stageTriggerUuid(model, stage)
    id = stageTriggerId(model, stage)
    name = stage.stageName.stageName + " (Trigger)"

    applyDefaultSettings()
    artifactRules = "build/build-receipt.properties"

    val triggerExcludes = """
        -:.idea
        -:.github
        -:.teamcity
        -:.teamcityTest
        -:subprojects/docs/src/docs/release
    """.trimIndent()
    val masterReleaseFilter = model.masterAndReleaseBranches.joinToString(prefix = "+:", separator = "\n+:")

    features {
        publishBuildStatusToGithub(model)
    }

    if (stage.trigger == Trigger.eachCommit) {
        triggers.vcs {
            quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_CUSTOM
            quietPeriod = 90
            triggerRules = triggerExcludes
            branchFilter = masterReleaseFilter
        }
    } else if (stage.trigger != Trigger.never) {
        triggers.schedule {
            if (stage.trigger == Trigger.weekly) {
                schedulingPolicy = weekly {
                    dayOfWeek = ScheduleTrigger.DAY.Saturday
                    hour = 1
                }
            } else {
                schedulingPolicy = daily {
                    hour = 0
                    minute = 30
                }
            }
            triggerBuild = always()
            withPendingChangesOnly = true
            param("revisionRule", "lastFinished")
            param("branchFilter", masterReleaseFilter)
        }
    }

    params {
        param("env.JAVA_HOME", buildJavaHome())
    }

    val baseBuildType = this
    val buildScanTags = model.buildScanTags + stage.id

    val defaultGradleParameters = (
        buildToolGradleParameters() +
            baseBuildType.buildCache.gradleParameters(Os.linux) +
            buildScanTags.map(::buildScanTag)
        ).joinToString(" ")
    steps {
        gradleWrapper {
            name = "GRADLE_RUNNER"
            tasks = "createBuildReceipt" + if (stage.stageName == StageNames.READY_FOR_NIGHTLY) " updateBranchStatus" else ""
            gradleParams = defaultGradleParameters
        }
        script {
            name = "CHECK_CLEAN_M2"
            executionMode = BuildStep.ExecutionMode.ALWAYS
            scriptContent = m2CleanScriptUnixLike
        }
        if (model.tagBuilds) {
            gradleWrapper {
                name = "TAG_BUILD"
                executionMode = BuildStep.ExecutionMode.ALWAYS
                tasks = "tagBuild"
                gradleParams = "$defaultGradleParameters -PteamCityToken=%teamcity.user.bot-gradle.token% -PteamCityBuildId=%teamcity.build.id% -PgithubToken=%github.ci.oauth.token% ${buildScanTag("StagePasses")}"
            }
        }
    }

    dependencies {
        if (!stage.runsIndependent && prevStage != null) {
            dependency(stageTriggerId(model, prevStage)) {
                snapshot {
                    onDependencyFailure = FailureAction.ADD_PROBLEM
                }
            }
        }

        snapshotDependencies(stageProject.specificBuildTypes)
        snapshotDependencies(stageProject.performanceTests)
        snapshotDependencies(stageProject.functionalTests)
    }
})

fun stageTriggerUuid(model: CIBuildModel, stage: Stage) = "${model.projectPrefix}Stage_${stage.stageName.uuid}_Trigger"
fun stageTriggerId(model: CIBuildModel, stage: Stage) = AbsoluteId("${model.projectPrefix}Stage_${stage.stageName.id}_Trigger")

fun Dependencies.snapshotDependencies(buildTypes: Iterable<BuildType>) {
    buildTypes.forEach {
        dependency(it.id!!) {
            snapshot {}
        }
    }
}
