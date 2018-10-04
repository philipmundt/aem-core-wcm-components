/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2018 Adobe Systems Incorporated
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.wcm.core.components.extension.contentfragment.internal;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.FragmentTemplate;
import com.adobe.cq.export.json.ComponentExporter;
import com.day.cq.wcm.api.policies.ContentPolicy;
import com.day.cq.wcm.api.policies.ContentPolicyManager;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.resourceresolver.MockValueMap;
import org.hamcrest.CoreMatchers;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.hamcrest.collection.IsMapContaining;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.adobe.cq.wcm.core.components.extension.contentfragment.internal.ContentFragmentUtils.PN_CFM_GRID_TYPE;
import static com.day.cq.commons.jcr.JcrConstants.JCR_CONTENT;
import static com.day.cq.commons.jcr.JcrConstants.JCR_TITLE;

public class ContentFragmentUtilsTest {

    @Test
    public void getTypeWhenContentFragmentIsNull() {
        // GIVEN
        // WHEN
        String type = ContentFragmentUtils.getType(null);

        // THEN
        Assert.assertThat(type, CoreMatchers.is(""));
    }

    @Test
    public void getTypeWhenResourceIsNull() {
        // GIVEN
        FragmentTemplate fragmentTemplate = Mockito.mock(FragmentTemplate.class);
        ContentFragment contentFragment = Mockito.mock(ContentFragment.class);
        Mockito.when(contentFragment.getTemplate()).thenReturn(fragmentTemplate);
        Mockito.when(contentFragment.getName()).thenReturn("foobar");

        // WHEN
        String type = ContentFragmentUtils.getType(contentFragment);

        // THEN
        Assert.assertThat(type, CoreMatchers.is("foobar"));
    }

    @Test
    public void getTypeWhenTemplateResourceIsNotNull() {
        // GIVEN
        Resource fragmentResource = Mockito.mock(Resource.class);
        Resource templateResource = Mockito.mock(Resource.class);
        FragmentTemplate fragmentTemplate = Mockito.mock(FragmentTemplate.class);
        ContentFragment contentFragment = Mockito.mock(ContentFragment.class);

        Mockito.when(contentFragment.getTemplate()).thenReturn(fragmentTemplate);
        Mockito.when(contentFragment.adaptTo(Mockito.eq(Resource.class))).thenReturn(fragmentResource);
        Mockito.when(fragmentTemplate.adaptTo(Mockito.eq(Resource.class))).thenReturn(templateResource);
        Mockito.when(templateResource.getPath()).thenReturn("/foo/bar/qux");

        // WHEN
        String type = ContentFragmentUtils.getType(contentFragment);

        // THEN
        Assert.assertThat(type, CoreMatchers.is("/foo/bar/qux"));
    }

    @Test
    public void getTypeWhenTemplateResourceIsNotNullButIsContentNodeFallbackToParent() {
        // GIVEN
        Resource fragmentResource = Mockito.mock(Resource.class);
        Resource templateResourceParent = Mockito.mock(Resource.class);
        Resource templateResource = Mockito.mock(Resource.class);
        FragmentTemplate fragmentTemplate = Mockito.mock(FragmentTemplate.class);
        ContentFragment contentFragment = Mockito.mock(ContentFragment.class);

        Mockito.when(contentFragment.getTemplate()).thenReturn(fragmentTemplate);
        Mockito.when(contentFragment.adaptTo(Mockito.eq(Resource.class))).thenReturn(fragmentResource);
        Mockito.when(fragmentTemplate.adaptTo(Mockito.eq(Resource.class))).thenReturn(templateResource);
        Mockito.when(templateResource.getName()).thenReturn(JCR_CONTENT);
        Mockito.when(templateResource.getParent()).thenReturn(templateResourceParent);
        Mockito.when(templateResourceParent.getPath()).thenReturn("/foo/bar");

        // WHEN
        String type = ContentFragmentUtils.getType(contentFragment);

        // THEN
        Assert.assertThat(type, CoreMatchers.is("/foo/bar"));
    }

    @Test
    public void getTypeOfStructuredContentFragment() {
        // GIVEN
        Resource fragmentResource = Mockito.mock(Resource.class);
        Resource fragmentDataResource = Mockito.mock(Resource.class);
        Resource templateResource = Mockito.mock(Resource.class);
        FragmentTemplate fragmentTemplate = Mockito.mock(FragmentTemplate.class);
        ContentFragment contentFragment = Mockito.mock(ContentFragment.class);
        ValueMap valueMap = new MockValueMap(fragmentDataResource);
        valueMap.put("cq:model", "foo.bar.QuxModel");

        Mockito.when(contentFragment.getTemplate()).thenReturn(fragmentTemplate);
        Mockito.when(contentFragment.adaptTo(Mockito.eq(Resource.class))).thenReturn(fragmentResource);
        Mockito.when(fragmentResource.getChild(Mockito.eq(JCR_CONTENT + "/data"))).thenReturn(fragmentDataResource);
        Mockito.when(fragmentDataResource.getValueMap()).thenReturn(valueMap);
        Mockito.when(fragmentTemplate.adaptTo(Mockito.eq(Resource.class))).thenReturn(templateResource);
        Mockito.when(templateResource.getPath()).thenReturn("/foo/bar/qux/quux/corge/grault/garply");
        Mockito.when(templateResource.getName()).thenReturn("waldo");

        // WHEN
        String type = ContentFragmentUtils.getType(contentFragment);

        // THEN
        Assert.assertThat(type, CoreMatchers.is("bar/models/waldo"));
    }

    @Test
    public void filterEmptyElementNamesReturnsOriginalList() {
        // GIVEN
        ContentFragment contentFragment = Mockito.mock(ContentFragment.class);
        Iterator<ContentElement> contentElementIterator = Mockito.mock(Iterator.class);

        Mockito.when(contentFragment.getElements()).thenReturn(contentElementIterator);

        // WHEN
        Iterator<ContentElement> elementIterator = ContentFragmentUtils.filterElements(contentFragment, null);

        // THEN
        Assert.assertThat(elementIterator, CoreMatchers.is(contentElementIterator));
    }

    @Test
    public void filterElementNamesReturnsAppropriateElementsOnly() {
        // GIVEN
        ContentElement foo = Mockito.mock(ContentElement.class);
        ContentElement qux = Mockito.mock(ContentElement.class);
        ContentFragment contentFragment = Mockito.mock(ContentFragment.class);
        Mockito.when(contentFragment.hasElement(Mockito.eq("foo"))).thenReturn(true);
        Mockito.when(contentFragment.hasElement(Mockito.eq("bar"))).thenReturn(false);
        Mockito.when(contentFragment.hasElement(Mockito.eq("qux"))).thenReturn(true);
        Mockito.when(contentFragment.getElement(Mockito.eq("foo"))).thenReturn(foo);
        Mockito.when(contentFragment.getElement(Mockito.eq("qux"))).thenReturn(qux);

        // WHEN
        Iterator<ContentElement> elementIterator = ContentFragmentUtils.filterElements(contentFragment,
            new String[]{"foo", "bar", "qux"});

        // THEN
        Assert.assertThat(() -> elementIterator, IsIterableContainingInOrder.contains(foo, qux));
    }

    @Test
    public void getEditorJsonOutputOfContentFragment() throws Exception {
        // GIVEN
        InputStream expectedJsonResourceAsStream = getClass().getResourceAsStream("expectedJson.json");
        String expectedJsonOutput = IOUtils.toString(expectedJsonResourceAsStream, StandardCharsets.UTF_8);

        Resource contentFragmentResource = Mockito.mock(Resource.class);
        ContentFragment contentFragment = Mockito.mock(ContentFragment.class);
        Iterator<Resource> associatedContentResourceIterator = Mockito.mock(Iterator.class);
        Resource firstAndOnlyAssociatedContent = Mockito.mock(Resource.class);
        ValueMap associatedContentValueMap = new MockValueMap(firstAndOnlyAssociatedContent);
        associatedContentValueMap.put(JCR_TITLE, "associatedContentTitle");

        Mockito.when(contentFragment.getTitle()).thenReturn("titleOfTheContentFragment");
        Mockito.when(contentFragment.getAssociatedContent()).thenReturn(associatedContentResourceIterator);
        Mockito.when(contentFragment.adaptTo(Mockito.eq(Resource.class))).thenReturn(contentFragmentResource);
        Mockito.when(contentFragmentResource.getPath()).thenReturn("/path/to/the/content/fragment");
        Mockito.when(associatedContentResourceIterator.hasNext()).thenReturn(true, true, false);
        Mockito.when(associatedContentResourceIterator.next()).thenReturn(firstAndOnlyAssociatedContent);
        Mockito.when(firstAndOnlyAssociatedContent.getPath()).thenReturn("/path/to/the/associated/content");
        Mockito.when(firstAndOnlyAssociatedContent.adaptTo(ValueMap.class)).thenReturn(associatedContentValueMap);

        // WHEN
        String json = ContentFragmentUtils.getEditorJSON(contentFragment, "slave",
            new String[]{"foo", "bar"});

        // THEN
        Assert.assertThat(expectedJsonOutput.replaceAll("[\n\t ]", ""), CoreMatchers.is(json));
    }

    @Test
    public void getDefaultGridResourceType() {
        // GIVEN
        ResourceResolver resourceResolver = Mockito.mock(ResourceResolver.class);
        Resource resource = Mockito.mock(Resource.class);

        // WHEN
        String defaultGridResourceType = ContentFragmentUtils.getGridResourceType(resourceResolver, resource);

        // THEN
        Assert.assertThat(defaultGridResourceType, CoreMatchers.is(ContentFragmentUtils.DEFAULT_GRID_TYPE));
    }

    @Test
    public void getGridTypeSetInFragmentPolicy() {
        // GIVEN
        ContentPolicyManager contentPolicyManager = Mockito.mock(ContentPolicyManager.class);
        ContentPolicy contentPolicy = Mockito.mock(ContentPolicy.class);
        ResourceResolver resourceResolver = Mockito.mock(ResourceResolver.class);
        Resource resource = Mockito.mock(Resource.class);
        ValueMap valueMap = new MockValueMap(resource);
        valueMap.put(PN_CFM_GRID_TYPE, "foobar");

        Mockito.when(resourceResolver.adaptTo(Mockito.eq(ContentPolicyManager.class))).thenReturn(contentPolicyManager);
        Mockito.when(contentPolicyManager.getPolicy(Mockito.eq(resource))).thenReturn(contentPolicy);
        Mockito.when(contentPolicy.getProperties()).thenReturn(valueMap);

        // WHEN
        String defaultGridResourceType = ContentFragmentUtils.getGridResourceType(resourceResolver, resource);

        // THEN
        Assert.assertThat(defaultGridResourceType, CoreMatchers.is("foobar"));
    }

    @Test
    public void getItemsOrderOfEmptyMap() {
        // GIVEN
        Map<String, Object> items = new HashMap<>();

        // WHEN
        String[] itemsOrder = ContentFragmentUtils.getItemsOrder(items);

        // THEN
        Assert.assertThat(itemsOrder, CoreMatchers.is(ArrayUtils.EMPTY_STRING_ARRAY));
    }

    @Test
    public void getItemsOrderOfMap() {
        // GIVEN
        Map<String, Object> items = new LinkedHashMap<>();
        items.put("first", "3");
        items.put("second", 1);
        items.put("third", 2L);

        // WHEN
        String[] itemsOrder = ContentFragmentUtils.getItemsOrder(items);

        // THEN
        Assert.assertThat(itemsOrder, CoreMatchers.is(new String[]{"first", "second", "third"}));
    }

    @Test
    public void getComponentExport() {
        // GIVEN
        SlingContext slingContext = new SlingContext();
        slingContext.load().json(getClass().getResourceAsStream("foo.json"), "/foo");
        MockSlingHttpServletRequest slingHttpServletRequest =
            new MockSlingHttpServletRequest(slingContext.bundleContext());

        ComponentExporter componentExporter = new TestComponentExporter();

        ModelFactory modelFactory = Mockito.mock(ModelFactory.class);
        Mockito.when(modelFactory.getModelFromWrappedRequest(
            Mockito.any(), Mockito.any(), Mockito.eq(ComponentExporter.class)
        )).thenReturn(componentExporter);

        // WHEN
        Map<String, ComponentExporter> exporterMap =
            ContentFragmentUtils.getComponentExporters(slingContext.resourceResolver()
                .getResource("/foo").listChildren(), modelFactory, slingHttpServletRequest);

        // THEN
        Assert.assertThat(exporterMap, IsMapContaining.hasEntry("bar", componentExporter));
        Assert.assertThat(exporterMap, IsMapContaining.hasEntry("qux", componentExporter));
    }

    /**
     * Dummy test {@link ComponentExporter component exporter}.
     */
    private static class TestComponentExporter implements ComponentExporter {
        @Nonnull
        @Override
        public String getExportedType() {
            return "test";
        }
    }
}