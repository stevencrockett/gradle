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

package org.gradle.api.internal.provider

import org.gradle.api.Task
import org.gradle.api.logging.configuration.WarningMode
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskState
import org.gradle.internal.Describables
import org.gradle.internal.featurelifecycle.DeprecatedFeatureUsage
import org.gradle.internal.featurelifecycle.DeprecatedUsageBuildOperationProgressBroadaster
import org.gradle.internal.featurelifecycle.UsageLocationReporter
import org.gradle.util.DeprecationLogger
import org.gradle.util.RedirectStdOutAndErr
import org.junit.Rule
import spock.lang.Specification

class TransformBackedProviderTest extends Specification {
    @Rule
    RedirectStdOutAndErr outputs = new RedirectStdOutAndErr()
    def broadcaster = Mock(DeprecatedUsageBuildOperationProgressBroadaster)

    def setup() {
        DeprecationLogger.reset()
        DeprecationLogger.init(Stub(UsageLocationReporter), WarningMode.All, broadcaster)
    }

    def teardown() {
        DeprecationLogger.reset()
    }

    def "warns when calling isPresent() before producer task has completed"() {
        given:
        def property = propertyWithProducer()
        def provider = property.map { Integer.parseInt(it) }

        when:
        provider.isPresent()

        then:
        1 * broadcaster.progress(_) >> { DeprecatedFeatureUsage usage ->
            assert usage.formattedMessage() == "Querying the mapped value of <prop> before <task> has completed has been deprecated. This will fail with an error in Gradle 7.0."
        }
        0 * broadcaster._
    }

    def "warns when calling get() before producer task has completed"() {
        given:
        def property = propertyWithProducer()
        def provider = property.map { Integer.parseInt(it) }

        when:
        provider.get()

        then:
        1 * broadcaster.progress(_) >> { DeprecatedFeatureUsage usage ->
            assert usage.formattedMessage() == "Querying the mapped value of <prop> before <task> has completed has been deprecated. This will fail with an error in Gradle 7.0."
        }
        0 * broadcaster._
    }

    def "does not warn when calling get() after producer task has completed"() {
        given:
        def property = propertyWithCompletedProducer()
        def provider = property.map { Integer.parseInt(it) }

        when:
        provider.get()

        then:
        0 * broadcaster._
    }

    def "warns when calling getOrNull() before producer task has completed"() {
        given:
        def property = propertyWithProducer()
        def provider = property.map { Integer.parseInt(it) }

        when:
        provider.getOrNull()

        then:
        1 * broadcaster.progress(_) >> { DeprecatedFeatureUsage usage ->
            assert usage.formattedMessage() == "Querying the mapped value of <prop> before <task> has completed has been deprecated. This will fail with an error in Gradle 7.0."
        }
        0 * broadcaster._
    }

    def "warns when calling getOrElse() before producer task has completed"() {
        given:
        def property = propertyWithProducer()
        def provider = property.map { Integer.parseInt(it) }

        when:
        provider.getOrElse(12)

        then:
        1 * broadcaster.progress(_) >> { DeprecatedFeatureUsage usage ->
            assert usage.formattedMessage() == "Querying the mapped value of <prop> before <task> has completed has been deprecated. This will fail with an error in Gradle 7.0."
        }
        0 * broadcaster._
    }

    def "warns when querying chained mapping before producer task has completed"() {
        given:
        def property = propertyWithProducer()
        def provider = property.map { Integer.parseInt(it) }.map { it + 2 }

        when:
        provider.get()

        then:
        1 * broadcaster.progress(_) >> { DeprecatedFeatureUsage usage ->
            assert usage.formattedMessage() == "Querying the mapped value of map(<prop>) before <task> has completed has been deprecated. This will fail with an error in Gradle 7.0."
        }
        1 * broadcaster.progress(_) >> { DeprecatedFeatureUsage usage ->
            assert usage.formattedMessage() == "Querying the mapped value of <prop> before <task> has completed has been deprecated. This will fail with an error in Gradle 7.0."
        }
        0 * broadcaster._
    }

    def "warns when querying orElse() mapping before producer task has completed"() {
        given:
        def property = propertyWithProducer()
        def provider = property.map { Integer.parseInt(it) }.orElse(12)

        when:
        provider.get()

        then:
        1 * broadcaster.progress(_) >> { DeprecatedFeatureUsage usage ->
            assert usage.formattedMessage() == "Querying the mapped value of <prop> before <task> has completed has been deprecated. This will fail with an error in Gradle 7.0."
        }
        0 * broadcaster._
    }

    Property<String> propertyWithProducer() {
        def task = Mock(Task)
        def state = Mock(TaskState)
        _ * task.toString() >> "<task>"
        _ * task.state >> state
        def property = new DefaultProperty(String)
        property.attachDisplayName(Describables.of("<prop>"))
        property.attachProducer(task)
        property.set("12")
        return property
    }

    Property<String> propertyWithCompletedProducer() {
        def task = Mock(Task)
        def state = Mock(TaskState)
        _ * task.toString() >> "<task>"
        _ * task.state >> state
        _ * state.executed >> true
        def property = new DefaultProperty(String)
        property.attachDisplayName(Describables.of("<prop>"))
        property.attachProducer(task)
        property.set("12")
        return property
    }
}
