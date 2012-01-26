package com.netflix.exhibitor.imps;

public interface JDBCQueries
{
    public String       queryToValidateSchema();

    public String       queryToCreateSchema();

    public String       queryToInitializeSchema();

    public String       queryToReadSchema();

    public String       queryToWriteSchema();
}
