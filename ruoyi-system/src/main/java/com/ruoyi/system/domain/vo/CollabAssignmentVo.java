package com.ruoyi.system.domain.vo;

/**
 * Assignment payload when creating a collaborative task.
 */
public class CollabAssignmentVo
{
    private Long userId;

    private String scopeType;

    private String scopeJson;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getScopeType()
    {
        return scopeType;
    }

    public void setScopeType(String scopeType)
    {
        this.scopeType = scopeType;
    }

    public String getScopeJson()
    {
        return scopeJson;
    }

    public void setScopeJson(String scopeJson)
    {
        this.scopeJson = scopeJson;
    }
}
