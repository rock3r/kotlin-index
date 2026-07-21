// SPDX-License-Identifier: UEL-1.0

package dev.sebastiano.indexino.buildlogic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.work.DisableCachingByDefault;

@DisableCachingByDefault(
        because = "Cache restoration is not proven to preserve the application JAR filesystem mtime")
public abstract class NormalizedJar extends Jar {
    @Input
    public abstract Property<Long> getNormalizedTimestampMillis();

    @Override
    @TaskAction
    protected void copy() {
        super.copy();
        var output = getArchiveFile().get().getAsFile().toPath();
        var timestamp = FileTime.fromMillis(getNormalizedTimestampMillis().get());
        try {
            Files.setLastModifiedTime(output, timestamp);
            var actualTimestamp = Files.getLastModifiedTime(output);
            if (!actualTimestamp.equals(timestamp)) {
                throw new GradleException(
                        "Could not set normalized JAR mtime: expected "
                                + timestamp
                                + ", got "
                                + actualTimestamp);
            }
        } catch (IOException failure) {
            throw new GradleException("Could not normalize application JAR mtime", failure);
        }
    }
}
