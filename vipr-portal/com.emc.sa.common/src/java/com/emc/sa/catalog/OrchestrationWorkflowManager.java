/*
 * Copyright 2016 Dell Inc. or its subsidiaries.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.emc.sa.catalog;

import java.net.URI;
import java.util.Iterator;
import java.util.List;

import com.emc.storageos.db.client.constraint.NamedElementQueryResultList.NamedElement;
import com.emc.storageos.db.client.model.uimodels.OrchestrationWorkflow;
import com.emc.storageos.db.client.model.uimodels.OrchestrationWorkflow.OrchestrationWorkflowStatus;

public interface OrchestrationWorkflowManager {

    public OrchestrationWorkflow getById(final URI id);
    public List<OrchestrationWorkflow> getByName(final String name);
    public List<NamedElement> list();
    public Iterator<OrchestrationWorkflow> getSummaries(final List<URI> ids);
    public void save(final OrchestrationWorkflow workflow);
    public void delete(final OrchestrationWorkflow workflow);
    public boolean hasCatalogServices(final String name);
    public List<NamedElement> listByStatus(final OrchestrationWorkflowStatus status);
    
    
}
