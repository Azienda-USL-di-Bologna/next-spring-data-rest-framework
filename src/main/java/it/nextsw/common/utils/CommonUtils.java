package it.nextsw.common.utils;

import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
@Component
public class CommonUtils {

    private static final Logger log = LoggerFactory.getLogger(CommonUtils.class);

    public String getHostname(HttpServletRequest request) {

        // TODO: non è detto che vada bene tornare sempre il primo elemento, bisognerebbe controllare che il Path dell'azienda matchi con uno qualsiasi degli elementi
        String header = request.getHeader("X-Forwarded-Host");
        if (StringUtils.hasText(header)) {
            String[] headerToken = header.split(",");
            return headerToken[0];
        } else {
            return request.getServerName();
        }

    }
}
