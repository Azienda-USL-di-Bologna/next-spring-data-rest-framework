package it.nextsw.common.spring.resolver;


import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Classe per gestire una paginazione basata solo su offset e limit
 */
public class OffsetLimitPageRequest implements Pageable {

    private static int LIMIT_DEAFULT_VALUE = Integer.MAX_VALUE;
    private static int OFFSET_DEAFULT_VALUE = 0;

    private int offset;
    private int limit;
    private Sort sort;


    public OffsetLimitPageRequest(Integer offset, Integer limit, Sort sort){
       // super(offset,limit);
        if (limit == null)
            limit = LIMIT_DEAFULT_VALUE;
        if (offset == null)
            offset = OFFSET_DEAFULT_VALUE;
        if (limit == 0){
            throw new IllegalArgumentException("Limit cannot be 0 (zero)");
        }
        this.offset = offset;
        this.limit = limit;
        this.sort = sort;
    }

    public OffsetLimitPageRequest(String offset, String limit, Sort sort){
        this(offset != null ? Integer.parseInt(offset) : null, limit != null ? Integer.parseInt(limit) : null, sort!= null ? sort : Sort.unsorted());
    }

    @Override
    public int getPageNumber() {
        // visto che non si tratta di una paginazione vera e propria si astrae il concetto di pagina
        return Math.round(offset/limit);
    }

    @Override
    public int getPageSize() {
        return this.limit;
    }

    @Override
    public long getOffset(){
        return this.offset;
    }

    @Override
    public Sort getSort() {
        return this.sort;
    }

    @Override
    public Pageable next() {
        return this;
    }

    @Override
    public Pageable previousOrFirst() {
        return this;
    }

    @Override
    public Pageable first() {
        return this;
    }

    @Override
    public boolean hasPrevious() {
        return false;
    }
}