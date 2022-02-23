package it.nextsw.common.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 * @author gdm
 */
@Component
public class CommonUtils {

    private static final Logger log = LoggerFactory.getLogger(CommonUtils.class);
//
//    @Value("${nextsdr.request.default.azienda-path:localhost}")
//    private String defaultAziendaPath;
//    
    @Autowired
    private Environment env;

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
        return res;
    }
    
    public String resolvePlaceHolder(String property) {
        String pattern = "(\\$\\{(.*)\\})";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(property);
        String value = null;
        if (m.find()) {
            try {
                value = m.group(2);
            } catch (Exception ex) {
            }
        }

        if (value != null) {
            return property.replaceAll(pattern, env.getProperty(value));
        } else {
            return property;
        }
    }

    public ArrayList getNewInstanceOfCollection(ArrayList coll){
        ArrayList newColl = new ArrayList();
        newColl.addAll(coll);
        return  newColl;
    }
}
