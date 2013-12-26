package com.newrelic.plugins.terracotta.utils;

public enum NewRelicMetricType
{
    Count( "count" ),
    CountSecond( "count/sec" ),
    Bytes( "bytes" ),
    QueriesSecond( "Queries/sec" ),
    Rate( "value/sec" ),
    BytesSecond( "bytes/sec" ),
    Percent( "percent" );
    
    final private String m_name;
    
    private NewRelicMetricType(final String name)
    {
        m_name = name;
    }
    
    public String getName()
    {
        return m_name;
    }
}
