package it.nextsw.common.persistence.entities;

public interface EntityConstants {

    public static final int VARCHAR_FIELD_SMALLEST=10;
    public static final int VARCHAR_FIELD_SMALL=20;
    public static final int VARCHAR_FIELD_MEDIUM=50;
    public static final int VARCHAR_FIELD_DOUBLE_MEDIUM=VARCHAR_FIELD_MEDIUM*2;
    public static final int VARCHAR_FIELD_LARGE=500;
    public static final int VARCHAR_FIELD_DOUBLE_LARGE=VARCHAR_FIELD_LARGE*2;
    public static final int MEDIUM_TEXT_MAX_LENGTH=16000000;

    public static final String TYPE_LONG_BLOB_FIELD="LONGBLOB";
    public static final String TYPE_MEDIUM_TEXT_FIELD="MEDIUMTEXT";
    public static final String TYPE_TINY_BLOB_FIELD="TINYBLOB";


    public static final String DEFAULT_ID_PROPERTY_NAME="id";
    public static final String DEFAULT_ID_COLUMN_NAME="ID";
    public static final String DEFAULT_DISCRIMINATOR_COLUMN_NAME="TYPE";
    public static final String DEFAULT_LOGICAL_DELETED_COLUMN_NAME="DELETED";
    public static final String DEFAULT_TIMED_LOGICAL_DELETED_COLUMN_NAME="TIME_DELETED";

    // formattazione date
    public static final String DEFAULT_TIMEZONE = "Europe/Rome";
    public static final String DEFAULT_DATE_FORMAT_PATTERN = "dd-MM-yyyy";
    public static final String DEFAULT_DATETIME_FORMAT_PATTERN = "dd-MM-yyyy HH:mm";

}
