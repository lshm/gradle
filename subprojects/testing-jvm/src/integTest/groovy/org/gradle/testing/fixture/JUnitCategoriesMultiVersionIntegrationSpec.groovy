/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.testing.fixture

import org.gradle.integtests.fixtures.executer.ExecutionFailure
import org.gradle.integtests.fixtures.executer.ExecutionResult

import static org.gradle.test.fixtures.junitplatform.JUnitPlatformTestRewriter.replaceCategoriesWithTags
import static org.gradle.testing.fixture.JUnitCoverage.VINTAGE

abstract class JUnitCategoriesMultiVersionIntegrationSpec extends JUnitMultiVersionIntegrationSpec {
    @Override
    protected ExecutionResult succeeds(String... tasks) {
        if (version == VINTAGE) {
            replaceCategoriesWithTags(executer.workingDir)
        }
        super.succeeds(tasks)
    }

    @Override
    protected ExecutionFailure fails(String... tasks) {
        if (version == VINTAGE) {
            replaceCategoriesWithTags(executer.workingDir)
        }
        super.fails(tasks)
    }
}
