package it.nextsw.common.types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.usertype.ParameterizedType;

public class GenericArrayUserType<T extends Serializable> implements UserType, ParameterizedType {

    public static final String INTEGER_ELEMENT_TYPE = "integer";
    public static final String TEXT_ELEMENT_TYPE = "text";

    protected static final int[] SQL_TYPES = {Types.ARRAY};
    private Class<T> typeParameterClass;
    Properties parameters;

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return this.deepCopy(cached);
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (T) this.deepCopy(value);
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {

        if (x == null) {
            return y == null;
        }
        return x.equals(y);
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet resultSet, String[] names, SharedSessionContractImplementor sharedSessionContractImplementor, Object o) throws HibernateException, SQLException {
        if (resultSet.wasNull()) {
            return null;
        }
        //@SuppressWarnings("unchecked")
//        Object object = new Object();
//        T foo;

        if (resultSet.getArray(names[0]) == null) {
            Object res = null;
            switch (parameters.getProperty("elements-type")) {
                case INTEGER_ELEMENT_TYPE:
                    res = java.lang.reflect.Array.newInstance(Integer.class, 0);
                    break;
                case TEXT_ELEMENT_TYPE:
                    res = java.lang.reflect.Array.newInstance(String.class, 0);
                    break;
            }
            return res;
        }

        Array array = resultSet.getArray(names[0]);
        @SuppressWarnings("unchecked")
        T javaArray = (T) array.getArray();
//        List<T> asList = Arrays.asList(javaArray);
        return javaArray;
    }

    @Override
    public void nullSafeSet(PreparedStatement statement, Object value, int index, SharedSessionContractImplementor sharedSessionContractImplementor) throws HibernateException, SQLException {
        Connection connection = statement.getConnection();
        if (value == null) {
            statement.setNull(index, SQL_TYPES[0]);
        } else {
            @SuppressWarnings("unchecked")
            T castObject = (T) value;
            Array array = null;
            array = connection.createArrayOf(parameters.getProperty("elements-type"), (Object[]) castObject);

            statement.setArray(index, array);
        }
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }

    @Override
    public Class<T> returnedClass() {
        return typeParameterClass;
    }

    @Override
    public int[] sqlTypes() {
        return new int[]{Types.ARRAY};
    }

    @Override
    public void setParameterValues(Properties parameters) {
        this.parameters = parameters;
    }
}
