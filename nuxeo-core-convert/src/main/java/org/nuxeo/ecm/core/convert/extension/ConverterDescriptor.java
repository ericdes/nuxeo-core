/*
 * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.core.convert.extension;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * XMap descriptor for the contribution of a new {@link Converter}.
 *
 * @author tiry
 */
@XObject("converter")
public class ConverterDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String CUSTOM_CONVERTER_TYPE = "Custom";
    public static final String CHAINED_CONVERTER_TYPE = "Chain";

    protected Converter instance;

    @XNode("@name")
    protected String converterName;

    @XNodeList(value = "sourceMimeType", type = ArrayList.class, componentType = String.class)
    protected List<String> sourceMimeTypes = new ArrayList<String>();

    @XNode("destinationMimeType")
    protected String destinationMimeType;

    @XNode("@class")
    protected Class className;

    @XNode("@type")
    protected String converterType = CUSTOM_CONVERTER_TYPE;

    protected boolean wrappedTransformer = false;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> parameters = new HashMap<String, String>();

    @XNodeList(value = "conversionSteps/step", type = ArrayList.class, componentType = String.class)
    protected List<String> steps = new ArrayList<String>();

    @XNodeList(value = "conversionSteps/subconverter", type = ArrayList.class, componentType = String.class)
    protected List<String> subConverters = new ArrayList<String>();

    public String getConverterName() {
        return converterName;
    }

    public List<String> getSourceMimeTypes() {
        return sourceMimeTypes;
    }

    public List<String> getSteps() {
        return steps;
    }

    public String getDestinationMimeType() {
        return destinationMimeType;
    }

    public void initConverter() throws Exception {
        if (instance == null) {
            if (className == null
                    || converterType.equals(CHAINED_CONVERTER_TYPE)) {

                if (subConverters == null || subConverters.size() == 0) {
                    // create a Chained converter based on mimetypes
                    instance = new ChainedConverter();
                } else {
                    // create a Chained converter based on converter chain
                    instance = new ChainedConverter(subConverters);
                }
                converterType = CHAINED_CONVERTER_TYPE;
            } else {
                instance = (Converter) className.newInstance();
            }
            instance.init(this);
        }
    }

    public Converter getConverterInstance() {
        try {
            initConverter();
        } catch (Exception e) {
            // TODO: handle exception
        }
        return instance;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public ConverterDescriptor merge(ConverterDescriptor other) {

        if (!other.getConverterName().equals(converterName)) {
            throw new UnsupportedOperationException(
                    "Can not merge ConverterDesciptors with different names");
        }

        if (wrappedTransformer) {
            // converter completly override wrapped transformers
            return other;
        }

        if (other.parameters != null) {
            parameters.putAll(other.parameters);
        }
        if (other.className != null) {
            instance = null;
            className = other.className;
        }
        if (other.sourceMimeTypes != null) {
            for (String mt : other.sourceMimeTypes) {
                if (!sourceMimeTypes.contains(mt)) {
                    sourceMimeTypes.add(mt);
                }

            }
            // sourceMimeTypes.addAll(other.sourceMimeTypes);
        }
        if (other.destinationMimeType != null) {
            destinationMimeType = other.destinationMimeType;
        }
        if (other.converterType != null) {
            converterType = other.converterType;
        }
        if (other.steps != null && other.steps.size() > 0) {
            steps = other.steps;
        }

        return this;
    }

    public String getConverterType() {
        return converterType;
    }

}
