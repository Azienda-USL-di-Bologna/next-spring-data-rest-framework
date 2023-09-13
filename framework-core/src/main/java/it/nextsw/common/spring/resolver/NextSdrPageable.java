package it.nextsw.common.spring.resolver;


import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Classe come Pageable con in più il boolean per noCount
 */
public class NextSdrPageable extends PageRequest {
    private boolean noCount;
    
    public NextSdrPageable(int page, int size, Sort sort,  boolean noCount) {
        super(page, size, sort);
        this.noCount = noCount;
    }
    
    public NextSdrPageable(String page, String size, Sort sort, String noCount) {
        this(
                page != null ? Integer.parseInt(page) : null, 
                size != null ? Integer.parseInt(size) : null, 
                sort != null ? sort : Sort.unsorted(),
                Boolean.parseBoolean(noCount));
    }

    public boolean getNoCount() {
        return this.noCount;
    }
}