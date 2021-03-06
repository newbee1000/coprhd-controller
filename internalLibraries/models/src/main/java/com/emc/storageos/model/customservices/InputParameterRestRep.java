/*
 * Copyright 2017 Dell Inc. or its subsidiaries.
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
package com.emc.storageos.model.customservices;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class InputParameterRestRep {

    private String name;
    private boolean required;
    private List<String> defaultValue;
    private String fieldType;

    @XmlElement(name = "name")
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @XmlElement(name = "required")
    public boolean getRequired() {
        return required;
    }

    public void setRequired(final boolean required) {
        this.required = required;
    }

    @XmlElement(name = "default_value")
    public List<String> getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue( final List<String> defaultValue) {
        this.defaultValue = defaultValue;
    }

    @XmlElement(name = "field_type")
    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(final String fieldType) {
        this.fieldType = fieldType;
    }
}
