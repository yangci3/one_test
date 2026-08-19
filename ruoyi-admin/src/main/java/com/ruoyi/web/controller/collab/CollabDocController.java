package com.ruoyi.web.controller.collab;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
import com.ruoyi.system.domain.CollabDoc;
import com.ruoyi.system.service.ICollabDocService;

/**
 * Collaborative document REST endpoints.
 */
@RestController
@RequestMapping("/collab/doc")
public class CollabDocController extends BaseController
{
    @Autowired
    private ICollabDocService collabDocService;

    /**
     * Query collaborative document list.
     */
    @PreAuthorize("@ss.hasPermi('collab:doc:list')")
    @GetMapping("/list")
    public AjaxResult list(CollabDoc query)
    {
        List<CollabDoc> list = collabDocService.selectCollabDocList(query);
        return success(list);
    }

    /**
     * Get collaborative document by id.
     */
    @PreAuthorize("@ss.hasPermi('collab:doc:query')")
    @GetMapping(value = "/{docId}")
    public AjaxResult getInfo(@PathVariable Long docId)
    {
        return success(collabDocService.selectCollabDocById(docId));
    }

    /**
     * Add collaborative document.
     */
    @PreAuthorize("@ss.hasPermi('collab:doc:add')")
    @Log(title = "Collab document", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CollabDoc collabDoc)
    {
        return toAjax(collabDocService.insertCollabDoc(collabDoc));
    }

    /**
     * Update collaborative document.
     */
    @PreAuthorize("@ss.hasPermi('collab:doc:edit')")
    @Log(title = "Collab document", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CollabDoc collabDoc)
    {
        return toAjax(collabDocService.updateCollabDoc(collabDoc));
    }
}
