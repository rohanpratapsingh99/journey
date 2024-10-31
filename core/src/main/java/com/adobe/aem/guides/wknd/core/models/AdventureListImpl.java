package com.adobe.aem.guides.wknd.core.models;

import com.adobe.aem.guides.wknd.core.models.impl.BookNowModelImpl;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Model(
    adaptables = {SlingHttpServletRequest.class, Resource.class},
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class AdventureListImpl {
    private static final Logger LOG = LoggerFactory.getLogger(AdventureListImpl.class);

    @ValueMapValue
    private String parentPath;

    @ValueMapValue
    private String defaultImage;

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private QueryBuilder queryBuilder;


    private List<Item> items;

    @PostConstruct
    protected void init() throws RepositoryException {
        items = new ArrayList<>();
        ResourceResolver resolver = request.getResourceResolver();
        String searchText = request.getParameter("searchtext");

        if (searchText != null && !searchText.isEmpty()) {
            // Use QueryBuilder to perform a full-text search
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put("path", parentPath);
            queryMap.put("fulltext", searchText);
            queryMap.put("type", "cq:Page");
            queryMap.put("p.limit", "-1");

            Query query = queryBuilder.createQuery(PredicateGroup.create(queryMap), resolver.adaptTo(Session.class));
            SearchResult result = query.getResult();

            for (Hit hit : result.getHits()) {
                Resource resource = hit.getResource();
                addItemFromResource(resource);
            }
        } else {
            // Load all items
            Resource parentResource = resolver.getResource(parentPath);
            if (parentResource != null) {
                for (Resource child : parentResource.getChildren()) {
                    if (child.getName().equals("jcr:content")) continue;
                    Resource childJCR = child.getChild("jcr:content");
                    if (childJCR != null) addItemFromResource(child);
                }
            }
        }
    }

    private void addItemFromResource(Resource resource) {
        if(resource.getChild("jcr:content") != null) {
            ValueMap vm = resource.getChild("jcr:content").getValueMap();
            String title = vm.get("jcr:title", String.class);
            String description = vm.get("jcr:description", String.class);
            String imagePath = defaultImage;

            Resource featuredImage = resource.getChild("jcr:content").getChild("cq:featuredimage");
            if (featuredImage != null) {
                ValueMap vm1 = featuredImage.getValueMap();
                imagePath = vm1.get("fileReference", String.class);
            }

            String url = resource.getPath();
            items.add(new Item(url, title, description, imagePath));
        }
    }

    public List<Item> getItems() {
        LOG.info("SIZ --> {}",items.size());
        for (Item i: items){
            LOG.info("img {}", i.getImage());

        }
        return items;
    }

    public static class Item {
        private final String URL;
        private final String title;
        private final String description;
        private final String image;

        public Item(String URL, String title, String description, String image) {
            this.URL = URL;
            this.title = title;
            this.description = description;
            this.image = image;
        }

        public String getURL() {
           if(!StringUtils.isBlank(URL))
            return URL.concat(".html");
           return URL;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getImage() {
            return image;
        }
    }
}