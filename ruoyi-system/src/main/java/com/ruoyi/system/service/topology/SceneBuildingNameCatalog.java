package com.ruoyi.system.service.topology;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

/**
 * Classpath building id-to-name map for topology node enrichment.
 */
@Component
public class SceneBuildingNameCatalog
{
    private static final String RESOURCE = "/scene/buildings-names.json";

    private final Map<String, String> names;

    public SceneBuildingNameCatalog()
    {
        this.names = load();
    }

    public String nameOf(String buildingId)
    {
        if (buildingId == null || buildingId.trim().isEmpty())
        {
            return "";
        }
        String name = names.get(buildingId);
        return name != null ? name : buildingId;
    }

    private static Map<String, String> load()
    {
        try (InputStream in = SceneBuildingNameCatalog.class.getResourceAsStream(RESOURCE))
        {
            if (in == null)
            {
                return Collections.emptyMap();
            }
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            List<JSONObject> rows = JSON.parseArray(json, JSONObject.class);
            Map<String, String> map = new HashMap<>();
            if (rows != null)
            {
                for (JSONObject row : rows)
                {
                    if (row == null)
                    {
                        continue;
                    }
                    String id = row.getString("id");
                    String name = row.getString("name");
                    if (id != null && !id.isEmpty() && name != null)
                    {
                        map.put(id, name);
                    }
                }
            }
            return map;
        }
        catch (Exception e)
        {
            return Collections.emptyMap();
        }
    }
}
