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

package org.gradle.ide.visualstudio.fixtures

import org.gradle.nativeplatform.fixtures.AbstractInstalledToolChainIntegrationSpec

class AbstractVisualStudioIntegrationSpec extends AbstractInstalledToolChainIntegrationSpec {
    void useMsbuildTool() {
        executer.requireGradleDistribution().requireIsolatedDaemons()

        buildFile << '''
            gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS_FULL
            Properties gatherEnvironment() {
                Properties properties = new Properties()
                properties.JAVA_HOME = String.valueOf(System.getenv('JAVA_HOME'))
                properties.GRADLE_USER_HOME = String.valueOf(gradle.gradleUserHomeDir.absolutePath)
                properties.GRADLE_OPTS = String.valueOf(System.getenv('GRADLE_OPTS'))
                return properties
            }
            
            void assertEquals(key, expected, actual) {
                assert expected[key] == actual[key]
                if (expected[key] != actual[key]) {
                    throw new GradleException("""
Environment's $key did not match! 
Expected: ${expected[key]} 
Actual: ${actual[key]} 
""")
                }
            }
            
            def gradleEnvironment = file("gradle-environment")
            tasks.matching { it.name == 'visualStudio' }.all { ideTask ->
                ideTask.doLast {
                    def writer = gradleEnvironment.newOutputStream()
                    gatherEnvironment().store(writer, null)
                    writer.close()
                }
            }
            gradle.buildFinished {
                if (!gradleEnvironment.exists()) {
                    throw new GradleException("could not determine if xcodebuild is using the correct environment, did xcode task run?")
                } else {
                    def expectedEnvironment = new Properties()
                    expectedEnvironment.load(gradleEnvironment.newInputStream())
                    
                    def actualEnvironment = gatherEnvironment()
                    
                    assertEquals('JAVA_HOME', expectedEnvironment, actualEnvironment)
                    assertEquals('GRADLE_USER_HOME', expectedEnvironment, actualEnvironment)
                    assertEquals('GRADLE_OPTS', expectedEnvironment, actualEnvironment)
                }
            }
        '''
    }

    protected MSBuildExecuter getMsbuild() {
        // Gradle needs to be isolated so the msbuild does not leave behind daemons
        assert executer.isRequiresGradleDistribution()
        assert !executer.usesSharedDaemons()
        def executer = new MSBuildExecuter(testDirectory)
        executer.withArgument('/p:Platform=Win32')
        return executer
    }
}
