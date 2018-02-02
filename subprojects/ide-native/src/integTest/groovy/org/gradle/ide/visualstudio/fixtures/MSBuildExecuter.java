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

package org.gradle.ide.visualstudio.fixtures;

import com.google.common.collect.Maps;
import org.gradle.api.Transformer;
import org.gradle.integtests.fixtures.executer.ExecutionFailure;
import org.gradle.integtests.fixtures.executer.ExecutionResult;
import org.gradle.integtests.fixtures.executer.OutputScrapingExecutionFailure;
import org.gradle.integtests.fixtures.executer.OutputScrapingExecutionResult;
import org.gradle.test.fixtures.file.ExecOutput;
import org.gradle.test.fixtures.file.TestFile;
import org.gradle.util.CollectionUtils;
import org.gradle.util.GUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.testng.Assert.assertTrue;

public class MSBuildExecuter {
    public enum MSBuildAction {
        BUILD;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    private final List<String> args = new ArrayList<String>();
    private final TestFile testDirectory;

    public MSBuildExecuter(TestFile testDirectory) {
        this.testDirectory = testDirectory;
    }

//    public MSBuildExecuter withProject(XcodeProjectPackage xcodeProject) {
//        TestFile projectDir = new TestFile(xcodeProject.getDir());
//        projectDir.assertIsDir();
//        return addArguments("-project", projectDir.getAbsolutePath());
//    }

    public MSBuildExecuter withSolution(SolutionFile visualStudioSolution) {
        TestFile solutionFile = new TestFile(visualStudioSolution.getFile());
        solutionFile.assertIsFile();
        return addArguments(solutionFile.getAbsolutePath());
    }

//    public MSBuildExecuter withScheme(String schemeName) {
//        return addArguments("-scheme", schemeName);
//    }

    public MSBuildExecuter withConfiguration(String configurationName) {
        return addArguments("/p:Configuration=" + configurationName);
    }

    public MSBuildExecuter withArgument(String arg) {
        this.args.add(arg);
        return this;
    }

    private MSBuildExecuter addArguments(String... args) {
        this.args.addAll(Arrays.asList(args));
        return this;
    }

    public ExecutionResult succeeds() {
        return succeeds(MSBuildAction.BUILD);
    }

    public ExecutionResult succeeds(MSBuildAction action) {
        withArgument("/t:" + action.toString());
        ExecOutput result = findMSBuild().execute(args, buildEnvironment());
        System.out.println(result.getOut());
        return new OutputScrapingExecutionResult(result.getOut(), result.getError());
    }

    public ExecutionFailure fails() {
        return fails(MSBuildAction.BUILD);
    }

    public ExecutionFailure fails(MSBuildAction action) {
        withArgument(action.toString());
        ExecOutput result = findMSBuild().execWithFailure(args, buildEnvironment());
        System.out.println(result.getOut());
        System.out.println(result.getError());
        return new OutputScrapingExecutionFailure(result.getOut(), result.getError());
    }

    private List<String> buildEnvironment() {
        Map<String, String> envvars = Maps.newHashMap();
        envvars.putAll(System.getenv());

        Properties props = GUtil.loadProperties(testDirectory.file("gradle-environment"));
        assert !props.isEmpty();

        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            if (entry.getKey().toString().equals("GRADLE_OPTS")) {
                // macOS adds Xdock properties in a funky way that makes us duplicate them on the command-line
                String value = entry.getValue().toString();
                int lastIndex = value.lastIndexOf("\"-Xdock:name=Gradle\"");
                if (lastIndex > 0) {
                    envvars.put(entry.getKey().toString(), value.substring(0, lastIndex-1));
                    continue;
                }
            }
            envvars.put(entry.getKey().toString(), entry.getValue().toString());
        }

        return CollectionUtils.toList(CollectionUtils.collect(envvars.entrySet(), new Transformer<String, Map.Entry<String, String>>() {
            @Override
            public String transform(Map.Entry<String, String> envvar) {
                return envvar.getKey() + "=" + envvar.getValue();
            }
        }));
    }

    private TestFile findMSBuild() {
        TestFile vswhere = new TestFile(System.getenv("ProgramFiles(x86)") + "/Microsoft Visual Studio/Installer/vswhere.exe");
        assertTrue(vswhere.exists(), "This test requires vswhere to be installed in '%ProgramFiles(x86)%/Microsoft Visual Studio/Installer/vswhere.exe'");

        TestFile installDir = new TestFile(vswhere.exec("-latest", "-products", "*", "-requires", "Microsoft.Component.MSBuild", "-property", "installationPath").getOut().trim());

        TestFile msbuild = installDir.file("MSBuild/15.0/Bin/MSBuild.exe");
        assertTrue(msbuild.exists(), "This test requires msbuild to be installed");
        return msbuild;
    }
}
