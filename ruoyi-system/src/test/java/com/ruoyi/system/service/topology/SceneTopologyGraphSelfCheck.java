package com.ruoyi.system.service.topology;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.ruoyi.system.domain.SceneTopologyEdge;
import com.ruoyi.system.domain.SceneTopologyGraphVo;
import com.ruoyi.system.domain.SceneTopologyNode;

/**
 * Manual self-check for {@link SceneTopologyGraph} (no JUnit dependency).
 */
public final class SceneTopologyGraphSelfCheck
{
    private SceneTopologyGraphSelfCheck()
    {
    }

    public static void main(String[] args)
    {
        List<SceneTopologyNode> sample = sampleNodes();
        SceneTopologyGraphVo graph = SceneTopologyGraph.buildGraph(sample);
        assertEqual(7, graph.getNodes().size(), "sample node count");
        assertEqual(6, graph.getEdges().size(), "sample edge count");

        SceneTopologyEdge toA = findEdgeTo(graph, "dev-a-sw1");
        assertTrue(toA != null, "edge to dev-a-sw1 exists");
        assertEqual("dev-core-sw1", toA.getFromDeviceId(), "dev-a-sw1 parent");
        assertEqual("edge-dev-core-sw1-dev-a-sw1", toA.getId(), "edge id for a-sw1");

        SceneTopologyGraphVo sub = SceneTopologyGraph.focusSubgraph(graph, "dev-a-sw1");
        Set<String> ids = nodeIdSet(sub);
        assertEqual(setOf("dev-a-sw1", "dev-a-term1", "dev-core-sw1"), ids, "focus a-sw1 nodes");
        assertTrue(!ids.contains("dev-b-sw1"), "focus a-sw1 excludes b-sw1");
        for (SceneTopologyEdge e : sub.getEdges())
        {
            assertTrue(ids.contains(e.getFromDeviceId()) && ids.contains(e.getToDeviceId()),
                    "subgraph edge endpoints in node set");
        }

        SceneTopologyGraphVo subCore = SceneTopologyGraph.focusSubgraph(graph, "dev-core-sw1");
        assertEqual(7, subCore.getNodes().size(), "focus core node count");

        SceneTopologyGraphVo missing = SceneTopologyGraph.focusSubgraph(graph, "missing");
        assertEqual(0, missing.getNodes().size(), "missing focus nodes");
        assertEqual(0, missing.getEdges().size(), "missing focus edges");

        List<SceneTopologyNode> isolates = new ArrayList<>();
        isolates.add(node("orphan", "orphan", null));
        isolates.add(node("missing-parent", "mp", "no-such-parent"));
        isolates.add(node("self-parent", "sp", "self-parent"));
        SceneTopologyGraphVo isolateGraph = SceneTopologyGraph.buildGraph(isolates);
        assertEqual(3, isolateGraph.getNodes().size(), "isolate node count");
        assertEqual(0, isolateGraph.getEdges().size(), "isolate edge count");

        List<String> deviceIds = Arrays.asList("dev-core-sw1", "dev-a-sw1", "dev-a-term1", "dev-b-sw1");
        String[] parsed = SceneTopologyGraph.parseEdgeKey("edge-dev-core-sw1-dev-a-sw1", deviceIds);
        assertTrue(parsed != null && parsed.length == 2, "parseEdgeKey returns pair");
        assertEqual("dev-core-sw1", parsed[0], "parseEdgeKey from");
        assertEqual("dev-a-sw1", parsed[1], "parseEdgeKey to");
        assertTrue(SceneTopologyGraph.parseEdgeKey("not-an-edge", deviceIds) == null, "bad edge key null");
        assertTrue(SceneTopologyGraph.parseEdgeKey("edge-no-match", deviceIds) == null, "unmatched edge key null");

        System.out.println("SceneTopologyGraphSelfCheck OK");
    }

    private static List<SceneTopologyNode> sampleNodes()
    {
        List<SceneTopologyNode> list = new ArrayList<>();
        list.add(node("dev-core-sw1", "core", null));
        list.add(node("dev-core-router1", "r", "dev-core-sw1"));
        list.add(node("dev-a-sw1", "a", "dev-core-sw1"));
        list.add(node("dev-a-term1", "t", "dev-a-sw1"));
        list.add(node("dev-b-sw1", "b", "dev-core-sw1"));
        list.add(node("dev-c-term1", "c", "dev-core-sw1"));
        list.add(node("dev-d-router1", "d", "dev-core-router1"));
        return list;
    }

    private static SceneTopologyNode node(String id, String name, String parentDeviceId)
    {
        SceneTopologyNode n = new SceneTopologyNode();
        n.setId(id);
        n.setName(name);
        n.setIp("0");
        n.setType("switch");
        n.setBuildingId("bldg");
        n.setParentDeviceId(parentDeviceId);
        return n;
    }

    private static SceneTopologyEdge findEdgeTo(SceneTopologyGraphVo graph, String toDeviceId)
    {
        for (SceneTopologyEdge e : graph.getEdges())
        {
            if (toDeviceId.equals(e.getToDeviceId()))
            {
                return e;
            }
        }
        return null;
    }

    private static Set<String> nodeIdSet(SceneTopologyGraphVo graph)
    {
        Set<String> ids = new HashSet<>();
        for (SceneTopologyNode n : graph.getNodes())
        {
            ids.add(n.getId());
        }
        return ids;
    }

    private static Set<String> setOf(String... values)
    {
        return new HashSet<>(Arrays.asList(values));
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
