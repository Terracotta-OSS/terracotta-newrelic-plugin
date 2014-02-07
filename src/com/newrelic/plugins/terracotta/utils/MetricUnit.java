package com.newrelic.plugins.terracotta.utils;

public enum MetricUnit
{
    Count( "count" ),
    CountSecond( "count/sec" ),
    Bytes( "bytes" ),
    QueriesSecond( "queries/sec" ),
    Rate( "value/sec" ),
    BytesSecond( "bytes/sec" ),
    Percent( "percent" );
    
    final private String m_name;
    
    private MetricUnit(final String name)
    {
        m_name = name;
    }
    
    public String getName()
    {
        return m_name;
    }
}
