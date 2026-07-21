// SPDX-License-Identifier: UEL-1.0

package dev.sebastiano.indexino.buildlogic;

import java.util.Map;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

@DisableCachingByDefault(because = "This verification task has no outputs")
public abstract class VerifyNativeDistributionConfiguration extends DefaultTask {
    @Input
    public abstract MapProperty<String, String> getExpectedValues();

    @Input
    public abstract MapProperty<String, String> getActualValues();

    @Input
    public abstract ListProperty<String> getRequiredModules();

    @Input
    public abstract ListProperty<String> getConfiguredModules();

    @TaskAction
    public void verifyConfiguration() {
        Map<String, String> expected = getExpectedValues().get();
        Map<String, String> actual = getActualValues().get();
        expected.forEach(
                (key, expectedValue) -> {
                    String actualValue = actual.get(key);
                    if (!expectedValue.equals(actualValue)) {
                        throw new GradleException(
                                "Unexpected native distribution "
                                        + key
                                        + ": expected "
                                        + expectedValue
                                        + ", got "
                                        + actualValue);
                    }
                });

        var missingModules =
                getRequiredModules().get().stream()
                        .filter(module -> !getConfiguredModules().get().contains(module))
                        .toList();
        if (!missingModules.isEmpty()) {
            throw new GradleException("Missing required jlink modules: " + missingModules);
        }
    }
}
