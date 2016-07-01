/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dbg.cache;
import dbg.cache.thrift.TDbgCacheUPool;
import dbg.cache.thrift.T_CardLockedSerialInfo;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.thrift.TException;

/**
 *
 * @author bangdq
 */
public class CardLockedInfoCacheClient
{
    private static final Lock CreateLock = new ReentrantLock();
    private static CardLockedInfoCacheClient Instance;    
    private TDbgCacheUPool _dbgCacheUpool;
    private String _source;
    private String _auth;  
    public static CardLockedInfoCacheClient getInstance(String host, String port, String source, String auth)
    {
        if (Instance == null)
        {
            CreateLock.lock();
            try
            {
                if (Instance == null)
                    Instance = new CardLockedInfoCacheClient(host, port, source, auth);
            } 
            finally
            {
                CreateLock.unlock();
            }
            
        }
        return Instance;
    }
     public CardLockedInfoCacheClient(String host, String port, String source, String auth)
    {
        String serviceName = String.format("%s:%s::%s", host, port, CardLockedInfoCacheClient.class.toString());
        String hostPort = String.format("%s:%s", host, port);
        _source = source;
        _auth = auth;
        _dbgCacheUpool = TDbgCacheUPool.getInstance(serviceName, hostPort, hostPort);
    }
    public  void increaseCardLockCounter(String key, int maxcounter, int diffinminute, int lockinminute) throws TException
    {
         _dbgCacheUpool.increaseCardLockCounter(_source, _auth, key, maxcounter, diffinminute, lockinminute);
    }    
    public boolean isCardLocked(String key, int maxcounter, int diffinminute, int lockinminute) throws TException
    {
        return _dbgCacheUpool.isCardLocked(_source, _auth, key, maxcounter, diffinminute, lockinminute);
    }
    public  T_CardLockedSerialInfo getCardLockedInfo( String key) throws TException
    {
        return _dbgCacheUpool.getCardLockedInfo(_source, _auth, key);
    }
}
