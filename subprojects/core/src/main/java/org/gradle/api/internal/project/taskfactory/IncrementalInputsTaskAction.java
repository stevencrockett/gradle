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

package org.gradle.api.internal.project.taskfactory;

import org.gradle.api.Task;
import org.gradle.api.execution.incremental.InputChanges;
import org.gradle.internal.execution.history.changes.ExecutionStateChanges;
import org.gradle.internal.reflect.JavaMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public class IncrementalInputsTaskAction extends AbstractIncrementalTaskAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(IncrementalInputsTaskAction.class);

    public IncrementalInputsTaskAction(Class<? extends Task> type, Method method) {
        super(type, method);
    }

    protected void doExecute(final Task task, String methodName) {
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        ExecutionStateChanges changes = getContext().getExecutionStateChanges().get();
        InputChanges inputChanges = changes.getInputChanges();
        if (!inputChanges.isIncremental()) {
            LOGGER.info("All inputs are considered out-of-date for incremental {}.", task);
        }
        getContext().setTaskExecutedIncrementally(inputChanges.isIncremental());
        JavaMethod.of(task, Object.class, methodName, InputChanges.class).invoke(task, inputChanges);
    }
}