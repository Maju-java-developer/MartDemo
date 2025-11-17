package raven.modal.demo.forms;

import raven.modal.demo.utils.AESEncryption;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;

public class BackupRestoreManager {

    private final String MYSQLDUMP_PATH = "C:/xampp/mysql/bin/mysqldump.exe";
    private final String MYSQL_PATH = "C:/xampp/mysql/bin/mysql.exe";

    private final String USERNAME = "root";
    private final String PASSWORD = "root";
    private final String PORT = "110";
    private final String DB_NAME = "martDB";

    // -------------------------------------------------------------------
    //  BACKUP → SQL → ENCRYPT → .enc
    // -------------------------------------------------------------------
    public boolean createEncryptedBackup(File encFile) {
        try {

            // 1. Create temporary SQL file
            File tempSql = File.createTempFile("db_backup", ".sql");

            ProcessBuilder pb = new ProcessBuilder(
                    MYSQLDUMP_PATH,
                    "-u", USERNAME,
                    "-p" + PASSWORD,
                    "--port=" + PORT,
                    DB_NAME,
                    "--result-file=" + tempSql.getAbsolutePath()
            );

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                return false;
            }

            // 2. Encrypt SQL → .enc
            AESEncryption.encryptFile(tempSql, encFile);

            // 3. Delete SQL
            tempSql.delete();

            return true;

        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    // -------------------------------------------------------------------
    //  RESTORE → DECRYPT .enc → SQL → restore
    // -------------------------------------------------------------------
    public boolean restoreEncryptedBackup(File encFile) {
        try {

            // 1. Create temporary SQL file
            File tempSql = File.createTempFile("restore", ".sql");

            // 2. Decrypt .enc → SQL
            AESEncryption.decryptFile(encFile, tempSql);

            // 3. Restore SQL back to MySQL
            ProcessBuilder pb = new ProcessBuilder(
                    MYSQL_PATH,
                    "-u", USERNAME,
                    "-p" + PASSWORD,
                    "--port=" + PORT,
                    DB_NAME
            );

            Process process = pb.start();

            OutputStream os = process.getOutputStream();
            Files.copy(tempSql.toPath(), os);
            os.close();

            int exitCode = process.waitFor();

            tempSql.delete();

            return exitCode == 0;

        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
