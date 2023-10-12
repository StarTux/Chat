package com.winthier.chat.sql;

import org.junit.Test;
import static com.winthier.sql.SQLDatabase.testTableCreation;

public final class SQLTest {
    @Test
    public void test() {
        for (var it : SQLDB.getDatabaseClasses()) {
            System.out.println(testTableCreation(it));
        }
    }
}
