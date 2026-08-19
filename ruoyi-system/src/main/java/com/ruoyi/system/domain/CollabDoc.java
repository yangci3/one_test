package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * Collaborative document (collab_doc).
 */
public class CollabDoc extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long docId;

    private String title;

    private String contentHtml;

    private String status;

    public Long getDocId()
    {
        return docId;
    }

    public void setDocId(Long docId)
    {
        this.docId = docId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getContentHtml()
    {
        return contentHtml;
    }

    public void setContentHtml(String contentHtml)
    {
        this.contentHtml = contentHtml;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("docId", getDocId())
            .append("title", getTitle())
            .append("contentHtml", getContentHtml())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
