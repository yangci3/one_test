package com.ruoyi.system.service.collab;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure HTML section helpers for collab documents (no Spring / no Jsoup).
 * Sections are {@code <div data-sec-id="sec-N">...</div>} blocks.
 */
public final class CollabSectionHelper
{
    private static final Pattern SEC_ID_ATTR = Pattern.compile(
            "data-sec-id\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    private static final Pattern SEC_DIV_OPEN = Pattern.compile(
            "<div\\b[^>]*\\bdata-sec-id\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DIV_OPEN = Pattern.compile("<div\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern DIV_CLOSE = Pattern.compile("</div\\s*>", Pattern.CASE_INSENSITIVE);

    private CollabSectionHelper()
    {
    }

    public static List<String> listSectionIds(String html)
    {
        List<String> ids = new ArrayList<>();
        if (html == null || html.isEmpty())
        {
            return ids;
        }
        Set<String> seen = new LinkedHashSet<>();
        Matcher matcher = SEC_ID_ATTR.matcher(html);
        while (matcher.find())
        {
            String id = matcher.group(1);
            if (seen.add(id))
            {
                ids.add(id);
            }
        }
        return ids;
    }

    public static String extractSections(String html, Collection<String> sectionIds)
    {
        if (html == null || html.isEmpty())
        {
            return html == null ? "" : html;
        }
        if (sectionIds == null || sectionIds.isEmpty())
        {
            return "";
        }
        Set<String> wanted = new LinkedHashSet<>(sectionIds);
        StringBuilder out = new StringBuilder();
        int index = 0;
        while (index < html.length())
        {
            SectionBlock block = findNextSectionBlock(html, index);
            if (block == null)
            {
                break;
            }
            if (wanted.contains(block.sectionId))
            {
                out.append(block.fullHtml);
            }
            index = block.end;
        }
        return out.toString();
    }

    public static String mergeSnapshots(String baseHtml, Map<String, String> sectionIdToHtml)
    {
        if (baseHtml == null || baseHtml.isEmpty())
        {
            return baseHtml == null ? "" : baseHtml;
        }
        if (sectionIdToHtml == null || sectionIdToHtml.isEmpty())
        {
            return baseHtml;
        }
        StringBuilder out = new StringBuilder();
        int index = 0;
        while (index < baseHtml.length())
        {
            SectionBlock block = findNextSectionBlock(baseHtml, index);
            if (block == null)
            {
                out.append(baseHtml.substring(index));
                break;
            }
            out.append(baseHtml, index, block.start);
            String replacement = sectionIdToHtml.get(block.sectionId);
            if (replacement != null)
            {
                out.append(block.openTag).append(replacement).append(block.closeTag);
            }
            else
            {
                out.append(block.fullHtml);
            }
            index = block.end;
        }
        return out.toString();
    }

    public static String ensureDefaultSection(String html)
    {
        String content = html == null ? "" : html;
        if (listSectionIds(content).isEmpty())
        {
            return "<div data-sec-id=\"sec-1\">" + content + "</div>";
        }
        return content;
    }

    private static SectionBlock findNextSectionBlock(String html, int from)
    {
        Matcher openMatcher = SEC_DIV_OPEN.matcher(html);
        if (!openMatcher.find(from))
        {
            return null;
        }
        int start = openMatcher.start();
        int openEnd = openMatcher.end();
        String sectionId = openMatcher.group(1);
        int closeEnd = findClosingDivEnd(html, openEnd);
        int closeStart = findClosingDivStart(html, openEnd, closeEnd);
        String openTag = html.substring(start, openEnd);
        String closeTag = html.substring(closeStart, closeEnd);
        String fullHtml = html.substring(start, closeEnd);
        return new SectionBlock(start, closeEnd, sectionId, openTag, closeTag, fullHtml);
    }

    private static int findClosingDivEnd(String html, int searchFrom)
    {
        int depth = 1;
        int pos = searchFrom;
        while (pos < html.length() && depth > 0)
        {
            Matcher openMatcher = DIV_OPEN.matcher(html);
            Matcher closeMatcher = DIV_CLOSE.matcher(html);
            openMatcher.region(pos, html.length());
            closeMatcher.region(pos, html.length());
            boolean hasOpen = openMatcher.find();
            boolean hasClose = closeMatcher.find();
            if (!hasClose)
            {
                return html.length();
            }
            if (hasOpen && openMatcher.start() < closeMatcher.start())
            {
                depth++;
                pos = openMatcher.end();
            }
            else
            {
                depth--;
                if (depth == 0)
                {
                    return closeMatcher.end();
                }
                pos = closeMatcher.end();
            }
        }
        return html.length();
    }

    private static int findClosingDivStart(String html, int innerStart, int closeEnd)
    {
        Matcher closeMatcher = DIV_CLOSE.matcher(html);
        closeMatcher.region(innerStart, closeEnd);
        int closeStart = innerStart;
        while (closeMatcher.find())
        {
            closeStart = closeMatcher.start();
        }
        return closeStart;
    }

    private static final class SectionBlock
    {
        private final int start;
        private final int end;
        private final String sectionId;
        private final String openTag;
        private final String closeTag;
        private final String fullHtml;

        private SectionBlock(int start, int end, String sectionId, String openTag, String closeTag,
                String fullHtml)
        {
            this.start = start;
            this.end = end;
            this.sectionId = sectionId;
            this.openTag = openTag;
            this.closeTag = closeTag;
            this.fullHtml = fullHtml;
        }
    }
}
