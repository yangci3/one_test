package com.ruoyi.system.service.collab;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manual self-check for {@link CollabSectionHelper} (no JUnit dependency).
 */
public final class CollabSectionHelperSelfCheck
{
    private CollabSectionHelperSelfCheck()
    {
    }

    public static void main(String[] args)
    {
        String html = "<div data-sec-id=\"sec-1\"><p>one</p></div>"
                + "<div data-sec-id=\"sec-2\"><p>two</p></div>";

        List<String> ids = CollabSectionHelper.listSectionIds(html);
        assertEqual(Arrays.asList("sec-1", "sec-2"), ids, "listSectionIds finds sec-1, sec-2");

        String extracted = CollabSectionHelper.extractSections(html, Collections.singletonList("sec-1"));
        assertTrue(extracted.contains("data-sec-id=\"sec-1\""), "extractSections includes sec-1 marker");
        assertTrue(extracted.contains("<p>one</p>"), "extractSections includes sec-1 content");
        assertTrue(!extracted.contains("sec-2"), "extractSections returns only sec-1 when requested");
        assertTrue(!extracted.contains("<p>two</p>"), "extractSections excludes sec-2 content");

        Map<String, String> updates = new HashMap<>();
        updates.put("sec-2", "<p>updated-two</p>");
        String merged = CollabSectionHelper.mergeSnapshots(html, updates);
        assertTrue(merged.contains("<p>one</p>"), "mergeSnapshots keeps sec-1");
        assertTrue(merged.contains("<p>updated-two</p>"), "mergeSnapshots replaces sec-2");
        assertTrue(!merged.contains("<p>two</p>"), "mergeSnapshots removes old sec-2 content");

        String wrapped = CollabSectionHelper.ensureDefaultSection("<p>hello</p>");
        assertEqual("<div data-sec-id=\"sec-1\"><p>hello</p></div>", wrapped,
                "ensureDefaultSection wraps bare HTML");

        System.out.println("CollabSectionHelperSelfCheck OK");
    }

    private static void assertEqual(Object expected, Object actual, String label)
    {
        if (expected == null ? actual != null : !expected.equals(actual))
        {
            throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertTrue(boolean cond, String label)
    {
        if (!cond)
        {
            throw new AssertionError(label);
        }
    }
}
