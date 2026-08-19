package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.CollabDoc;

/**
 * Collaborative document service layer.
 */
public interface ICollabDocService
{
    /**
     * Query collaborative document list.
     *
     * @param collabDoc query criteria
     * @return document list
     */
    public List<CollabDoc> selectCollabDocList(CollabDoc collabDoc);

    /**
     * Query collaborative document by id.
     *
     * @param docId document id
     * @return document or null
     */
    public CollabDoc selectCollabDocById(Long docId);

    /**
     * Insert collaborative document.
     *
     * @param collabDoc document to insert
     * @return rows affected
     */
    public int insertCollabDoc(CollabDoc collabDoc);

    /**
     * Update collaborative document.
     *
     * @param collabDoc document to update
     * @return rows affected
     */
    public int updateCollabDoc(CollabDoc collabDoc);
}
