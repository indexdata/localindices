package com.indexdata.masterkey.localindices.dao;

import java.io.InputStream;
import com.indexdata.masterkey.localindices.entity.Storage;
import com.indexdata.masterkey.localindices.web.service.converter.StorageBrief;

public interface StorageDAO extends CommonDAO<Storage, StorageBrief> 
{
    // TODO refactor.. 
    public InputStream getLog(long id);
}
