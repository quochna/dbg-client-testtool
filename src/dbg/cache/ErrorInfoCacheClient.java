/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dbg.cache;

import dbg.cache.thrift.TDbgCacheUPool;
import dbg.cache.thrift.T_ErrorInfo;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.thrift.TException;

/**
 *
 * @author bangdq
 */
public class ErrorInfoCacheClient 
{
    private static final Lock CreateLock = new ReentrantLock();
    private static ErrorInfoCacheClient Instance;
    
    private TDbgCacheUPool _dbgCacheUpool;
    private String _source;
    private String _auth;  
    
    public static ErrorInfoCacheClient getInstance(String host, String port, String source, String auth)
    {
        if (Instance == null)
        {
            CreateLock.lock();
            try
            {
                if (Instance == null)
                    Instance = new ErrorInfoCacheClient(host, port, source, auth);
            } 
            finally
            {
                CreateLock.unlock();
            }
            
        }
        return Instance;
    }
    
    public ErrorInfoCacheClient(String host, String port, String source, String auth)
    {
        String serviceName = String.format("%s:%s::%s", host, port, ErrorInfoCacheClient.class.toString());
        String hostPort = String.format("%s:%s", host, port);
        _source = source;
        _auth = auth;
        _dbgCacheUpool = TDbgCacheUPool.getInstance(serviceName, hostPort, hostPort);
    }
    
    public int CountErrorInfo(int minuteInterval, int returnCode )throws TException
    {
      return  _dbgCacheUpool.countErrorInfo(_source, _auth, minuteInterval, returnCode);
    }
    public List<T_ErrorInfo> getErrorInfos(int minuteInterval) throws TException 
    {
      return  _dbgCacheUpool.getErrorInfos(_source, _auth, minuteInterval);
    }
    public void addErrorInfo(int minuteInterval, T_ErrorInfo tei) throws TException
    {
        _dbgCacheUpool.addErrorInfo(_source, _auth, minuteInterval, tei);
    }
}
