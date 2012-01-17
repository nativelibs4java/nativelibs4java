package com.nativelibs4java.velocity;

import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;
import org.apache.maven.plugin.logging.Log;

/**
 * Yet another logging translator, this time going 
 * from: org.apache.velocity.runtime.log.LogChute
 * to: org.apache.maven.plugin.logging.Log
 */
public class MavenLogChute implements LogChute {
	private final Log log;
	public MavenLogChute(Log logger) {
		this.log = logger;
	}

    public void init(RuntimeServices arg0) throws Exception {}

    public boolean isLevelEnabled(int lvl) {
        return
			(lvl == DEBUG_ID && log.isDebugEnabled()) || 
			(lvl == INFO_ID && log.isInfoEnabled()) || 
            (lvl == WARN_ID && log.isWarnEnabled()) ||
            (lvl == ERROR_ID && log.isErrorEnabled());
    }

    public void log(int lvl, String msg) {
        if (isLevelEnabled(lvl)) {
            switch (lvl) {
            case DEBUG_ID : 
                    log.debug(msg);
                    break;
            case INFO_ID :
                    log.info(msg);
                    break;
            case WARN_ID :
                    log.warn(msg);
                    break;
            case ERROR_ID :
                    log.error(msg);
                    break;
            default:
            }
	    }
	}

    public void log(int lvl, String msg, Throwable t) {
        if (isLevelEnabled(lvl)) {
            switch (lvl) {
            case DEBUG_ID : 
                    log.debug(msg, t);
                    break;
            case INFO_ID :
                    log.info(msg, t);
                    break;
            case WARN_ID :
                    log.warn(msg, t);
                    break;
            case ERROR_ID :
                    log.error(msg, t);
                    break;
            default:
            }
		}
	}
}
