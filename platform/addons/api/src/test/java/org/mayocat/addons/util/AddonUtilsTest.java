package org.mayocat.addons.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * @version $Id$
 */
public class AddonUtilsTest
{
    @Test
    public void testIsListWithKeyAndDisplayValuesWhenNull()
    {
        Map<String, Object> properties = null;
        Assert.assertFalse(AddonUtils.isListWithKeyAndDisplayValues(properties));
    }

    @Test
    public void testIsListWithKeyAndDisplayValuesWhenEmpty()
    {
        Map<String, Object> properties = new HashMap<String, Object>();
        Assert.assertFalse(AddonUtils.isListWithKeyAndDisplayValues(properties));
    }

    @Test
    public void testIsListWithKeyAndDisplayValuesWhenListOfStrings()
    {

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("listValues", Arrays.asList("toto", "tata"));
        Assert.assertFalse(AddonUtils.isListWithKeyAndDisplayValues(properties));
    }

    @Test
    public void testIsListWithKeyAndDisplayValuesIsKeyValueMap()
    {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("listValues", Arrays.asList(
                new HashMap()
                {
                    {
                        put("key", "toto");
                        put("name", "Toto");
                    }
                },
                new HashMap()
                {
                    {
                        put("key", "tata");
                        put("name", "Tata");
                    }
                }
        ));

        Assert.assertTrue(AddonUtils.isListWithKeyAndDisplayValues(properties));
    }
}
