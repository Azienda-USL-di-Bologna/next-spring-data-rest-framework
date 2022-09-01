package it.nextsw.common.configs.spring;

import org.springframework.context.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.web.config.QuerydslWebConfiguration;

@Configuration
@ComponentScan(value = {"it.nextsw"}, excludeFilters=@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {QuerydslWebConfiguration.class}))
@Import({SpringWebMvcConfigurer.class})
public class SpringConfig {


    @Bean
    public SpelAwareProxyProjectionFactory projectionFactory() {
        return new SpelAwareProxyProjectionFactory();
    }


}
