package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.CollabDoc;
import com.ruoyi.system.mapper.CollabDocMapper;
import com.ruoyi.system.service.ICollabDocService;
import com.ruoyi.system.service.collab.CollabSectionHelper;

/**
 * Collaborative document service layer.
 */
@Service
public class CollabDocServiceImpl implements ICollabDocService
{
    @Autowired
    private CollabDocMapper collabDocMapper;

    @Override
    public List<CollabDoc> selectCollabDocList(CollabDoc collabDoc)
    {
        return collabDocMapper.selectCollabDocList(collabDoc);
    }

    @Override
    public CollabDoc selectCollabDocById(Long docId)
    {
        return collabDocMapper.selectCollabDocById(docId);
    }

    @Override
    public int insertCollabDoc(CollabDoc collabDoc)
    {
        validateDoc(collabDoc, true);
        collabDoc.setContentHtml(CollabSectionHelper.ensureDefaultSection(collabDoc.getContentHtml()));
        collabDoc.setCreateBy(SecurityUtils.getUsername());
        if (StringUtils.isBlank(collabDoc.getStatus()))
        {
            collabDoc.setStatus("0");
        }
        return collabDocMapper.insertCollabDoc(collabDoc);
    }

    @Override
    public int updateCollabDoc(CollabDoc collabDoc)
    {
        validateDoc(collabDoc, false);
        collabDoc.setContentHtml(CollabSectionHelper.ensureDefaultSection(collabDoc.getContentHtml()));
        collabDoc.setUpdateBy(SecurityUtils.getUsername());
        return collabDocMapper.updateCollabDoc(collabDoc);
    }

    private void validateDoc(CollabDoc collabDoc, boolean isInsert)
    {
        if (!isInsert && collabDoc.getDocId() == null)
        {
            throw new ServiceException("Document id is required");
        }
        if (StringUtils.isBlank(collabDoc.getTitle()))
        {
            throw new ServiceException("Document title is required");
        }
    }
}
