package com.ruoyi.web.controller.scene;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.SceneDevice;
import com.ruoyi.system.service.ISceneDeviceService;

/**
 * Scene device REST endpoints.
 */
@RestController
@RequestMapping("/scene/device")
public class SceneDeviceController extends BaseController
{
    @Autowired
    private ISceneDeviceService deviceService;

    /**
     * Query scene device list.
     */
    @PreAuthorize("@ss.hasPermi('scene:device:list')")
    @GetMapping("/list")
    public AjaxResult list(SceneDevice query)
    {
        List<SceneDevice> list = deviceService.selectSceneDeviceList(query);
        return success(list);
    }

    /**
     * Get scene device by id.
     */
    @PreAuthorize("@ss.hasPermi('scene:device:query')")
    @GetMapping(value = "/{deviceId}")
    public AjaxResult getInfo(@PathVariable String deviceId)
    {
        return success(deviceService.selectSceneDeviceById(deviceId));
    }

    /**
     * Add scene device.
     */
    @PreAuthorize("@ss.hasPermi('scene:device:add')")
    @Log(title = "Scene device", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SceneDevice device)
    {
        device.setCreateBy(getUsername());
        return toAjax(deviceService.insertSceneDevice(device));
    }

    /**
     * Update scene device.
     */
    @PreAuthorize("@ss.hasPermi('scene:device:edit')")
    @Log(title = "Scene device", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SceneDevice device)
    {
        device.setUpdateBy(getUsername());
        return toAjax(deviceService.updateSceneDevice(device));
    }

    /**
     * Delete scene devices by ids.
     */
    @PreAuthorize("@ss.hasPermi('scene:device:remove')")
    @Log(title = "Scene device", businessType = BusinessType.DELETE)
    @DeleteMapping("/{deviceIds}")
    public AjaxResult remove(@PathVariable String[] deviceIds)
    {
        return toAjax(deviceService.deleteSceneDeviceByIds(deviceIds));
    }
}
