package it.nextsw.common.utils;

import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
@Component
public class CommonUtils {

    private static final Logger log = LoggerFactory.getLogger(CommonUtils.class);

    @Value("${nextsdr.request.default.azienda-path:localhost}")
    private String defaultAziendaPath;

    public String getHostname(HttpServletRequest request) {

        String res;
        String header = request.getHeader("X-Forwarded-Host");
        // TODO: non è detto che vada bene tornare sempre il primo elemento, bisognerebbe controllare che il Path dell'azienda matchi con uno qualsiasi degli elementi
        if (StringUtils.hasText(header)) {
            String[] headerToken = header.split(",");
            res = headerToken[0];
        } else {
            res = request.getServerName();
        }

        if ("localhost".equals(res)) {
            res = defaultAziendaPath;
        }

        return res;
    }
}
