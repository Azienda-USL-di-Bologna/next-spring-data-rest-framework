package it.nextsw.common.controller;

import java.util.Map;
import java.util.Set;
import org.hibernate.EmptyInterceptor;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
public class HibernateEntityInterceptor extends EmptyInterceptor {
    
    public static ThreadLocal<Map<String, String>> rankQueryObj = new ThreadLocal();

    @Override
    public String onPrepareStatement(String sql) {
        
//        System.out.println("ciao gdm non ti arrabbiare");
//        System.out.println("sql: " + sql);
        
        Map<String, String> rankQueryMap = rankQueryObj.get();
        final String regex = "\\{[a-zA-Z]+\\.PLACEHOLDER_TS_RANK\\}";
        
        if (sql.matches(".*" + regex + ".*")) {
            if (rankQueryMap != null && !rankQueryMap.isEmpty()) {
                Set<String> keySet = rankQueryMap.keySet();
                for (String key : keySet) {
                    String placeHolder = "{" + key + ".PLACEHOLDER_TS_RANK}";
                    String queryString = "'" + rankQueryMap.get(key).trim().replace("'", "''").replaceAll(" +", "' & '") + "'";
                    sql = sql.replace(placeHolder, queryString);
                }
            }
            
            // Se sono rimasti dei placeholder li "cancello"
            sql = sql.replaceAll(regex, "");
//            System.out.println("sql cambiato!: " + sql);
        }
            
        return super.onPrepareStatement(sql); //To change body of generated methods, choose Tools | Templates.
    }

}
