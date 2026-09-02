package dbrighthd.elytratrails.api;

import dbrighthd.elytratrails.config.pack.ResolvedSampleSettings;
import dbrighthd.elytratrails.config.pack.ResolvedTrailSettings;
import dbrighthd.elytratrails.config.pack.TrailOverrides;

public record ResolvedValues(ResolvedTrailSettings resolvedTrailSettings, ResolvedSampleSettings sampleSettings) {
    public ResolvedValues(TrailOverrides trailOverrides)
    {
        TrailOverrides newBase = TrailOverrides.getBase().with(trailOverrides);
        this(newBase.resolvedTrailSettings(true),newBase.resolvedSampleSettings());
    }
}
