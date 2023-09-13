package it.nextsw.common.spring.resolver;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Map;

/**
 * Resolver per utilizzare la paginazione dinamica:
 *  - tramite limit e offset nel caso in cui fra i parametri sia presente {@link this#DEFAULT_OFFSET_PARAMETER}
 *      o {@link this#DEFAULT_LIMIT_PARAMETER}, usando {@link OffsetLimitPageRequest}
 *  - tramite paginazione classica in tutti gli altri casi, usando {@link PageRequest}
 * da registrare su {@link WebMvcConfigurer#addArgumentResolvers} del progetto
 */
public class DynamicOffsetLimitPageRequestOrPageRequestResolver extends PageableHandlerMethodArgumentResolver {

    protected static final SortHandlerMethodArgumentResolver DEFAULT_SORT_RESOLVER = new SortHandlerMethodArgumentResolver();
    protected static final String OFFSET_PARAMETER = "pagingOffset";
    protected static final String LIMIT_PARAMETER = "pagingLimit";
    protected static final String PAGE_PARAMETER = "page";
    protected static final String SIZE_PARAMETER = "size";
    protected static final String NO_COUNT_PARAMETER = "noCount";
    protected static final String UNPAGED_PARAMETER = "unpaged";

    protected SortArgumentResolver sortResolver;


    public DynamicOffsetLimitPageRequestOrPageRequestResolver() {
        this((SortArgumentResolver) null);
    }

    public DynamicOffsetLimitPageRequestOrPageRequestResolver(SortHandlerMethodArgumentResolver sortResolver) {
        this((SortArgumentResolver) sortResolver);
    }


    public DynamicOffsetLimitPageRequestOrPageRequestResolver(@Nullable SortArgumentResolver sortResolver) {
        this.sortResolver = sortResolver == null ? DEFAULT_SORT_RESOLVER : sortResolver;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter){
        return Pageable.class.equals(parameter.getParameterType());
    }

    @Override
    public Pageable resolveArgument(MethodParameter parameter, ModelAndViewContainer container, NativeWebRequest request, WebDataBinderFactory factory){
        Map<String,String[]> params = request.getParameterMap();
        Sort sort = sortResolver.resolveArgument(parameter, container, request, factory);
        if (params.get(OFFSET_PARAMETER) != null || params.get(LIMIT_PARAMETER) != null) {
            return new OffsetLimitPageRequest(
                    params.get(OFFSET_PARAMETER)[0], 
                    params.get(LIMIT_PARAMETER)[0], 
                    sort,
                    params.get(NO_COUNT_PARAMETER) != null ? params.get(NO_COUNT_PARAMETER)[0] : null
            );
        } else if (params.get(PAGE_PARAMETER) != null && params.get(SIZE_PARAMETER) != null) {
            return new NextSdrPageable(
                    params.get(PAGE_PARAMETER)[0], 
                    params.get(SIZE_PARAMETER)[0], 
                    sort, 
                    params.get(NO_COUNT_PARAMETER) != null ? params.get(NO_COUNT_PARAMETER)[0] : null
            );
        } else if (params.get(UNPAGED_PARAMETER) != null && Boolean.parseBoolean(params.get(UNPAGED_PARAMETER)[0])) {
            return Pageable.unpaged();
        }
        
        return super.resolveArgument(parameter, container, request, factory);
    }
}
