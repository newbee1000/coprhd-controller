/*
 * Copyright 2012-2015 iWave Software LLC
 * All Rights Reserved
 */
package com.emc.sa.asset.providers;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.emc.sa.asset.AssetOptionsContext;
import com.emc.sa.asset.BaseAssetOptionsProvider;
import com.emc.sa.asset.annotation.Asset;
import com.emc.sa.asset.annotation.AssetDependencies;
import com.emc.sa.asset.annotation.AssetNamespace;
import com.emc.storageos.db.client.model.BlockConsistencyGroup;
import com.emc.storageos.model.RelatedResourceRep;
import com.emc.storageos.model.block.BlockConsistencyGroupRestRep;
import com.emc.storageos.model.block.VolumeRestRep;
import com.emc.storageos.model.vpool.BlockVirtualPoolRestRep;
import com.emc.storageos.model.vpool.VirtualPoolChangeOperationEnum;
import com.emc.vipr.client.ViPRCoreClient;
import com.emc.vipr.client.core.filters.ConsistencyGroupFilter;
import com.emc.vipr.client.core.filters.DefaultResourceFilter;
import com.emc.vipr.model.catalog.AssetOption;

@Component
@AssetNamespace("vipr")
public class ConsistencyGroupProvider extends BaseAssetOptionsProvider {
	
	@Asset("consistencyGroupByProject")
	@AssetDependencies({ "project" })
    public List<AssetOption> getAllConsistencyGroups(final AssetOptionsContext ctx, URI projectId) {
		List<BlockConsistencyGroupRestRep> consistencyGroups = api(ctx).blockConsistencyGroups().search().byProject(projectId)
				.filter(new DefaultResourceFilter<BlockConsistencyGroupRestRep>() {
			@Override
			public boolean accept(BlockConsistencyGroupRestRep item) {
				return isSupportedConsistencyGroup(api(ctx), item);
			}
		}).run();
		return createBaseResourceOptions( consistencyGroups );
    }
	
	@Asset("consistencyGroupFullCopy")
    @AssetDependencies("consistencyGroupByProject")
    public List<AssetOption> getFullCopies(AssetOptionsContext ctx, URI consistencyGroupId) {
        return createNamedResourceOptions(api(ctx).blockConsistencyGroups().getFullCopies(consistencyGroupId));
    }
	
    @Asset("consistencyGroup")
    @AssetDependencies({ "project", "blockVirtualPool" })
    public List<AssetOption> getConsistencyGroups(AssetOptionsContext ctx, URI projectId, URI virtualPoolId) {
        BlockVirtualPoolRestRep vpool = api(ctx).blockVpools().get(virtualPoolId);

        // Only provide consistency groups if the selected VPool supports it
        if (isSupportedVPool(vpool)) {
            return createBaseResourceOptions(api(ctx).blockConsistencyGroups().search().byProject(projectId).run());
        } else {
            return Collections.emptyList();
        }
    }

    @Asset("consistencyGroupWithVirtualPoolChangeOperation")
    @AssetDependencies({ "project", "virtualPoolChangeOperation", "targetVirtualPool" })
    public List<AssetOption> getConsistencyGroupsChangeVPool(AssetOptionsContext ctx, URI projectId,
            String virtualPoolChangeOperation, URI virtualPoolId) {
        // Only support for RP Protection for now
        if (virtualPoolChangeOperation.equals(VirtualPoolChangeOperationEnum.RP_PROTECTED.name())) {
            BlockVirtualPoolRestRep vpool = api(ctx).blockVpools().get(virtualPoolId);
            // Only provide consistency groups if the selected VPool supports it
            if (isSupportedVPool(vpool)) {
                return createBaseResourceOptions(api(ctx).blockConsistencyGroups().search().byProject(projectId)
                        .filter(new ConsistencyGroupFilter(BlockConsistencyGroup.Types.RP.toString(), true)).run());
            }
        }
        return Collections.emptyList();
    }
    
    private boolean isSupportedConsistencyGroup(ViPRCoreClient client, BlockConsistencyGroupRestRep consistencyGroup) {
    	if(consistencyGroup.getVolumes() != null && consistencyGroup.getVolumes().isEmpty()){
    		return false;
    	}
    	Set<RelatedResourceRep> vpools = new HashSet<>();
    	client.blockVolumes().getByRefs( consistencyGroup.getVolumes() );
    	for(VolumeRestRep volume : client.blockVolumes().getByRefs( consistencyGroup.getVolumes() )){
    		vpools.add(volume.getVirtualPool());
    	}
    	for(BlockVirtualPoolRestRep vpool : client.blockVpools().getByRefs(vpools)){
    		if(!isSupportedVPool(vpool)){
        		return false;
        	}
    	}
    	return true;
    }
    
    private boolean isSupportedVPool(BlockVirtualPoolRestRep vpool) {
		return vpool != null && vpool.getMultiVolumeConsistent() != null && vpool.getMultiVolumeConsistent();
	}

}
