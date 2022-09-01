package it.nextsw.common.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.mock.web.MockServletContext;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

@Component
public class TestUtils {

    @Autowired
    protected ObjectMapper objectMapper;
//    @Mock
//    private ServletRequestAttributes attrs;


    public Map<String, Object> serializeInMap(Object object) throws JsonProcessingException {
        return objectMapper.convertValue(object, Map.class);
    }

    public HttpServletRequest createHttpServletRequest(RequestMethod requestMethod, String servletPath) throws URISyntaxException {
      //  MockitoAnnotations.initMocks(this);
        ServletContext servletContext = new MockServletContext();
        HttpServletRequest request = MockMvcRequestBuilders.request(requestMethod.name(),new URI(servletPath)).servletPath(servletPath).buildRequest(servletContext);
        ServletRequestAttributes servletRequestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
        return request;
    }
}
