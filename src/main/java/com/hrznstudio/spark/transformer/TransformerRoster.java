package com.hrznstudio.spark.transformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TransformerRoster {
    private final List<IByteTransformer> transformers = new ArrayList<>();

    public void volunteer(IByteTransformer transformer) {
        this.transformers.add(transformer);
    }

    public Collection<IByteTransformer> getTransformers() {
        return Collections.unmodifiableList(this.transformers);
    }
}
