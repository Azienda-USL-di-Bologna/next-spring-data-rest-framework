package it.nextsw.common.repositories;

/**
 *
 * @author spritz
 */
public class StringOperation {

    public static enum Operators {
        startsWith,
        startsWithIgnoreCase,
        contains,
        containsIgnoreCase,
        equals,
        equalsIgnoreCase
    }

    private Operators operator;
    private String value;

    public StringOperation(Operators operator, String value) {
        this.operator = operator;
        this.value = value;
    }

    public Operators getOperator() {
        return operator;
    }

    public void setOperator(Operators operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
