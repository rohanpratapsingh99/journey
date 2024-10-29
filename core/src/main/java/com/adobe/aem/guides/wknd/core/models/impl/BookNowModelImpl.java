package com.adobe.aem.guides.wknd.core.models.impl;

import com.adobe.aem.guides.wknd.core.models.BookNowModel;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.day.cq.dam.api.Asset;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.Page;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;

@Model(
    adaptables = {SlingHttpServletRequest.class, Resource.class},
    adapters = {BookNowModel.class},
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class BookNowModelImpl implements BookNowModel {

    private static final Logger LOG = LoggerFactory.getLogger(BookNowModelImpl.class);

    private static final String JCR_CONTENT_DATA_MASTER_SUFFIX = "/jcr:content/data/master";

    @ValueMapValue
    private String fragmentPath;

    @SlingObject
    private ResourceResolver resourceResolver;


    @Override
    public String getPrice() {
        return getCfProperty("price");
    }

    @Override
    public String getAdventureTitle() {
        return getCfProperty("title");
    }

    @Override
    public String getAdventureSlug() {
        return getCfProperty("slug");
    }

    private String getCfProperty(String propertyName) {
        // Build the CF path with /jcr:content/data/master
        if (StringUtils.isBlank(fragmentPath)) {
            LOG.warn("Content Fragment path is blank.");
            return null;
        }

        String cfDataPath = fragmentPath + JCR_CONTENT_DATA_MASTER_SUFFIX;
        Resource cfResource = resourceResolver.getResource(cfDataPath);

        if (cfResource == null) {
            LOG.warn("Content Fragment not found at path: {}", cfDataPath);
            return null;
        }

        // Fetch the property from the Content Fragment's data
        ValueMap properties = cfResource.getValueMap();
        return Optional.ofNullable(properties.get(propertyName, String.class)).orElse(null);
    }
}