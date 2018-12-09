package com.hrznstudio.spark;

import com.hrznstudio.spark.patch.IBytePatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PatcherRoster implements IBytePatcher {
    public static final PatcherRoster INSTANCE = new PatcherRoster();

    private final List<IBytePatcher> patchers = new ArrayList<>();

    public void volunteer(IBytePatcher transformer) {
        this.patchers.add(transformer);
    }

    public Collection<IBytePatcher> getPatchers() {
        return Collections.unmodifiableList(this.patchers);
    }

    @Override
    public byte[] apply(String target, byte[] bytes) {
        byte[] result = bytes;
        for (IBytePatcher patcher : this.patchers) {
            result = patcher.apply(target, result);
        }
        return result;
    }
}
