package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.CollabDoc;

/**
 * Collaborative document data access layer.
 */
public interface CollabDocMapper
{
    public CollabDoc selectCollabDocById(Long docId);

    public List<CollabDoc> selectCollabDocList(CollabDoc collabDoc);

    public int insertCollabDoc(CollabDoc collabDoc);

    public int updateCollabDoc(CollabDoc collabDoc);
}
