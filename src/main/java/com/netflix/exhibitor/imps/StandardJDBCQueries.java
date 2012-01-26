package com.netflix.exhibitor.imps;

public class StandardJDBCQueries implements JDBCQueries
{
    private static final String     TABLE_NAME = "ExhibitorGlobalSharedConfig";

    @Override
    public String queryToValidateSchema()
    {
        return "SELECT * FROM " + TABLE_NAME;
    }

    @Override
    public String queryToCreateSchema()
    {
        return "CREATE TABLE " + TABLE_NAME + " (" + JDBCBasedGlobalSharedConfig.FIELD_NAME + " varchar(1000) )";
    }

    @Override
    public String queryToInitializeSchema()
    {
        return "INSERT INTO " + TABLE_NAME + " (" + JDBCBasedGlobalSharedConfig.FIELD_NAME + ") VALUES ('')";
    }

    @Override
    public String queryToReadSchema()
    {
        return queryToValidateSchema();
    }

    @Override
    public String queryToWriteSchema()
    {
        return "UPDATE " + TABLE_NAME + " SET " + JDBCBasedGlobalSharedConfig.FIELD_NAME + " = ?";
    }
}
