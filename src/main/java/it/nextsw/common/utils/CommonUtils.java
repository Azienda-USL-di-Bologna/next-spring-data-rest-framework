package it.nextsw.common.utils;

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
@Component
public class CommonUtils {
    
    public String getHostname(HttpServletRequest request) {
        return StringUtils.hasText(request.getHeader("X-Forwarded-Host"))? request.getHeader("X-Forwarded-Host"): request.getServerName();
    }
}
