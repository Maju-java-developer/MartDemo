package raven.modal.demo.dao;

import raven.modal.demo.annotations.ColumnName;
import raven.modal.demo.annotations.DropdownField;
import raven.modal.demo.mysql.MySQLConnection;

import javax.swing.*;
import java.lang.reflect.Field;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class UtilsDao {
    public static int getCount(String tableName) {
        String sql = "SELECT COUNT(*) FROM "+ tableName;
        try (Connection conn = MySQLConnection.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static ResultSet callSP(Connection conn, String sql, Object... params) throws SQLException {
        CallableStatement cs = conn.prepareCall(sql);

        // Set parameters safely
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];

            if (param == null) {
                cs.setNull(i + 1, Types.NULL);
            } else {
                cs.setObject(i + 1, param);
            }
        }

        return cs.executeQuery();
    }

    private static <T> List<T> mapResultSet(ResultSet rs, Class<T> clazz) throws SQLException {
        List<T> list = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        try {
            while (rs.next()) {
                T instance = clazz.getDeclaredConstructor().newInstance();

                // Loop over class fields only ONCE per row for efficiency
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    String targetColName = getString(field);

                    for (int i = 1; i <= columnCount; i++) {
                        String rsColName = meta.getColumnLabel(i).toLowerCase();

                        if (targetColName.equals(rsColName)) {
                            Object value = rs.getObject(i);
                            Class<?> fieldType = field.getType();

                            // --- Value Mapping Logic (Optimized for clarity) ---
                            if (value == null) {
                                field.set(instance, null);
                            } else if (fieldType == Integer.class || fieldType == int.class) {
                                field.set(instance, ((Number) value).intValue());
                            } else if (fieldType == Long.class || fieldType == long.class) {
                                field.set(instance, ((Number) value).longValue());
                            } else if (fieldType == Double.class || fieldType == double.class) {
                                field.set(instance, ((Number) value).doubleValue());
                            } else if (fieldType == Boolean.class || fieldType == boolean.class) {
                                field.set(instance, value);
                            } else if (fieldType == String.class) {
                                field.set(instance, value.toString());
                            } else {
                                field.set(instance, value); // Fallback for other types (Date, Timestamp, etc.)
                            }
                            break;
                        }
                    }
                }
                list.add(instance);
            }
        } catch (Exception e) {
            throw new RuntimeException("Mapping error: " + e.getMessage(), e);
        }

        return list;
    }

    private static String getString(Field field) {
        String fieldNameLower = field.getName().toLowerCase();

        // Check for custom mapping annotation
        ColumnName annotation = field.getAnnotation(ColumnName.class);

        // Determine the target database column name (lower case)
        String targetColName;
        if (annotation != null) {
            // Use the name specified in the annotation
            targetColName = annotation.value().toLowerCase();
        } else {
            // Use the field name as the default column name
            targetColName = fieldNameLower;
        }
        return targetColName;
    }

    public static  <T> List<T> getSpListProcedure(
            String listType,
            int offset,
            int limit,
            String searchText,
            Class<T> clazz) {

        String sql = "{CALL SP_GetList(?, ?, ?, ?, ?, ?, ?)}";

        try (Connection conn = MySQLConnection.getInstance().getConnection()) {

            ResultSet rs = callSP(conn, sql,
                    0,
                    limit,
                    offset,
                    searchText,
                    listType,
                    0,
                    null
            );

            return mapResultSet(rs, clazz);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "DB Error: " + e.getMessage(),
                    "SQL Error",
                    JOptionPane.ERROR_MESSAGE);
            return new ArrayList<>();
        }
    }

    /**
     * 1. THE MAIN CORE METHOD (NO DEFAULT OPTION ADDED HERE)
     * Executes the SP_Get stored procedure with all parameters and returns the raw result set.
     * This method is the one that directly connects to the database.
     */
    public static <T> List<T> getSpGetDropdownProcedure(
            Integer id,
            Integer otherId,
            String search,
            String otherData,
            String searchType,
            Integer userId,
            Timestamp dateTime,
            Class<T> clazz) {

        String sql = "{CALL SP_Get(?, ?, ?, ?, ?, ?, ?)}";

        try (Connection conn = MySQLConnection.getInstance().getConnection()) {

            // Assuming callSP and mapResultSet are defined elsewhere
            ResultSet rs = callSP(conn, sql,
                    id,
                    otherId,
                    search,
                    otherData,
                    searchType,
                    userId,
                    dateTime
            );

            return mapResultSet(rs, clazz);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "DB Error: " + e.getMessage(),
                    "SQL Error",
                    JOptionPane.ERROR_MESSAGE);
            return new ArrayList<>();
        }
    }

    /**
     * 2. OVERLOAD WITH ID AND DYNAMIC DEFAULT OPTION
     * Fetches data by ID, then creates and inserts the default option item at the start.
     */
    public static <T> List<T> getSpGetDropdownProcedure(
            String defaultOptionText, // Used to create the default item
            int id,
            String searchType,
            Class<T> clazz) {

        // A. Fetch the raw data using the main method
        List<T> resultList = getSpGetDropdownProcedure(
                id, null, null, null, searchType, null, null, clazz // Pass 'id' and 'searchType'
        );

        // B. Create the default item using the helper (from previous step)
        T defaultItem = createDefaultDropdownItem(clazz, 0, defaultOptionText);

        // C. Add the default item to the start
        if (defaultItem != null) {
            resultList.add(0, defaultItem);
        }

        return resultList;
    }

    /**
     * 3. OVERLOAD WITHOUT ID AND DYNAMIC DEFAULT OPTION
     * Fetches all data (no ID), then creates and inserts the default option item at the start.
     */
    public static <T> List<T> getSpGetDropdownProcedure(
            String defaultOptionText, // Used to create the default item
            String searchType,
            Class<T> clazz) {

        // A. Fetch the raw data using the main method
        List<T> resultList = getSpGetDropdownProcedure(
                null, null, null, null, searchType, null, null, clazz // All nulls for parameters not used
        );

        // B. Create the default item using the helper (from previous step)
        T defaultItem = createDefaultDropdownItem(clazz, 0, defaultOptionText);

        // C. Add the default item to the start
        if (defaultItem != null) {
            resultList.add(0, defaultItem);
        }

        return resultList;
    }

    private static <T> T createDefaultDropdownItem(Class<T> clazz, int defaultId, String defaultText) {
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();

            // Loop through all declared fields in the class
            for (Field field : clazz.getDeclaredFields()) {
                // Check if the field has our custom annotation
                DropdownField annotation = field.getAnnotation(DropdownField.class);

                if (annotation != null) {
                    field.setAccessible(true); // Allow setting private fields

                    if (annotation.isId()) {
                        // Set the field marked as ID (e.g., companyId) to the default ID (0)
                        if (field.getType() == int.class || field.getType() == Integer.class) {
                            field.set(instance, defaultId);
                        } else {
                            System.err.println("Warning: @DropdownField(isId=true) applied to non-int/Integer field: " + field.getName());
                        }
                    }

                    if (annotation.isText()) {
                        // Set the field marked as TEXT (e.g., companyName) to the default text
                        if (field.getType() == String.class) {
                            field.set(instance, defaultText);
                        } else {
                            System.err.println("Warning: @DropdownField(isText=true) applied to non-String field: " + field.getName());
                        }
                    }
                }
            }

            return instance;

        } catch (Exception e) {
            System.err.println("Error creating default dropdown item for class " + clazz.getName() + ": " + e.getMessage());
            return null;
        }
    }
}
