/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.performance.regression.android

import org.gradle.performance.AbstractCrossBuildPerformanceTest
import org.gradle.performance.categories.PerformanceExperiment
import org.gradle.performance.fixture.BuildExperimentSpec
import org.gradle.performance.fixture.GradleBuildExperimentSpec
import org.junit.experimental.categories.Category
import spock.lang.Unroll

import static org.gradle.performance.regression.android.IncrementalAndroidTestProject.SANTA_TRACKER_JAVA
import static org.gradle.performance.regression.android.IncrementalAndroidTestProject.SANTA_TRACKER_KOTLIN

@Category(PerformanceExperiment)
class FasterIncrementalAndroidBuildsPerformanceTest extends AbstractCrossBuildPerformanceTest {
    private static final String INSTANT_EXECUTION_PROPERTY = "-Dorg.gradle.unsafe.instant-execution"
    private static final String PARTIAL_VFS_INVALIDATION_PROPERTY = "-Dorg.gradle.unsafe.partial-vfs-invalidation"

    @Unroll
    def "faster incremental build on #testProject (build comparison)"() {
        given:
        runner.testGroup = "incremental android changes"
        runner.buildSpec {
            testProject.configureForNonAbiChange(it)
            displayName("non abi change")
        }
        runner.buildSpec {
            testProject.configureForAbiChange(it)
            displayName("abi change")
        }
        runner.buildSpec {
            testProject.configureForNonAbiChange(it)
            configureFastIncrementalBuild(it, testProject)
            displayName("faster non abi change")
        }
        runner.buildSpec {
            testProject.configureForAbiChange(it)
            configureFastIncrementalBuild(it, testProject)
            displayName("faster abi change")
        }

        when:
        def results = runner.run()
        then:
        results

        where:
        testProject << [SANTA_TRACKER_KOTLIN, SANTA_TRACKER_JAVA]
    }

    @Override
    protected void defaultSpec(BuildExperimentSpec.Builder builder) {
        if (builder instanceof GradleBuildExperimentSpec.GradleBuilder) {
            builder.invocation.args('-Dcom.android.build.gradle.overrideVersionCheck=true')
        }
    }

    def configureFastIncrementalBuild(GradleBuildExperimentSpec.GradleBuilder builder, IncrementalAndroidTestProject testProject) {
        if (testProject != SANTA_TRACKER_KOTLIN) {
            // Kotlin is not supported for instant execution
            builder.invocation.args(INSTANT_EXECUTION_PROPERTY)
        }
        builder.invocation.args(PARTIAL_VFS_INVALIDATION_PROPERTY)
    }
}
