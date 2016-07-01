/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dbg.cache.client;

import dbg.cache.thrift.TDbgCacheUPool;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.thrift.TException;

/**
 *
 * @author hainpt
 */
public class DBGCacheClient {

    private static final Lock CreateLock = new ReentrantLock();
    private static DBGCacheClient Instance;
    private TDbgCacheUPool _dbgCacheUpool;
    private String _source;
    private String _auth;

    public static DBGCacheClient getInstance(String host, String port, String source, String auth) {
        if (Instance == null) {
            CreateLock.lock();
            try {
                if (Instance == null) {
                    Instance = new DBGCacheClient(host, port, source, auth);
                }
            } finally {
                CreateLock.unlock();
            }

        }
        return Instance;
    }

    private DBGCacheClient(String host, String port, String source, String auth) {
        String serviceName = String.format("%s:%s::%s", host, port, DBGCacheClient.class.toString());
        String hostPort = String.format("%s:%s", host, port);
        _source = source;
        _auth = auth;
        _dbgCacheUpool = TDbgCacheUPool.getInstance(serviceName, hostPort, hostPort);
    }

    public void set(String key, String value) throws TException {
        _dbgCacheUpool.setTrans(_source, _auth, key, value);
    }

    public String get(String key) throws TException {
        return _dbgCacheUpool.getTrans(_source, _auth, key);
    }

    public Integer getCounterValue(String key) throws TException{
        return _dbgCacheUpool.getUseCardCounter(_source, _auth, key);
    }
}
