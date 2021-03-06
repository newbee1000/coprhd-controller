/*
 * Copyright (c) 2012-2015 iWave Software LLC
 * All Rights Reserved
 */
package com.emc.sa.asset.providers;

import java.util.List;

import org.springframework.stereotype.Component;

import com.emc.sa.asset.AssetOptionsContext;
import com.emc.sa.asset.BaseAssetOptionsProvider;
import com.emc.sa.asset.annotation.Asset;
import com.emc.sa.asset.annotation.AssetDependencies;
import com.emc.sa.asset.annotation.AssetNamespace;
import com.emc.vipr.model.catalog.AssetOption;
import com.google.common.collect.Lists;

@Component
@AssetNamespace("vipr")
public class ProjectProvider extends BaseAssetOptionsProvider {
    @Asset("project")
    public List<AssetOption> getProjects(AssetOptionsContext ctx) {
        return createBaseResourceOptions(api(ctx).projects().getByTenant(ctx.getTenant()));
    }

    @Asset("project")
    @AssetDependencies("mobilityGroupMethod")
    public List<AssetOption> getProjects(AssetOptionsContext ctx, String mobilityGroupMethod) {
        if (mobilityGroupMethod.equalsIgnoreCase("INGEST_AND_MIGRATE")) {
            return createBaseResourceOptions(api(ctx).projects().getByTenant(ctx.getTenant()));
        } else {
            return Lists.newArrayList();
        }
    }

    public List<AssetOption> getProjectFilters(AssetOptionsContext ctx) {
        List<AssetOption> options = createBaseResourceOptions(api(ctx).projects().getByTenant(ctx.getTenant()));
        options.add(0, new AssetOption("All", "All"));
        return options;
    }

    @Asset("objectBucketProject")
    public List<AssetOption> getObjectBucketProjects(AssetOptionsContext ctx) {
        List<AssetOption> options = createBaseResourceOptions(api(ctx).projects().getByTenant(ctx.getTenant()));
        options.add(newAssetOption("defaultProject", getMessage("object.project.default")));
        return options;
    }
}
