package dbrighthd.elytratrails.api;

import dbrighthd.elytratrails.config.pack.ResolvedSampleSettings;
import dbrighthd.elytratrails.config.pack.ResolvedTrailSettings;
import dbrighthd.elytratrails.config.pack.TrailOverrides;

/**
 * Resolves the trail overrides as to not do the hard math every time a trail is created
 * @param resolvedTrailSettings trail settings from overrides
 * @param sampleSettings sample settings from overrides
 */
public record ResolvedValues(ResolvedTrailSettings resolvedTrailSettings, ResolvedSampleSettings sampleSettings) {
    public ResolvedValues(TrailOverrides trailOverrides)
    {
        TrailOverrides newBase = TrailOverrides.getBase().with(trailOverrides);
        this(newBase.resolvedTrailSettings(true),newBase.resolvedSampleSettings());
    }
}
