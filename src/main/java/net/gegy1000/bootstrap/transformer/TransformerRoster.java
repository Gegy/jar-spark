package net.gegy1000.bootstrap.transformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TransformerRoster {
    private final Map<String, Collection<IByteTransformer>> transformers = new HashMap<>();

    public void volunteer(IByteTransformer transformer, String... targets) {
        this.volunteer(transformer, Arrays.asList(targets));
    }

    public void volunteer(IByteTransformer transformer, Collection<String> targets) {
        for (String target : targets) {
            Collection<IByteTransformer> volunteers = this.transformers.computeIfAbsent(target, s -> new ArrayList<>());
            volunteers.add(transformer);
        }
    }

    public Collection<IByteTransformer> collectVolunteers(String target) {
        Collection<IByteTransformer> volunteers = this.transformers.get(target);
        if (volunteers == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(volunteers);
    }
}
