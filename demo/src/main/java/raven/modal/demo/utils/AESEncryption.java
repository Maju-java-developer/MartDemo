package raven.modal.demo.utils;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.security.SecureRandom;

public class AESEncryption {

    private static final String KEY_FILE = "aes.key";

    // ------------------------------------------------------------------
    // Load AES Key (Auto-generate if missing)
    // ------------------------------------------------------------------
    public static SecretKey loadKey() throws Exception {

        File file = new File(KEY_FILE);

        // If key does NOT exist → generate automatically
        if (!file.exists()) {
            generateKey();
        }

        byte[] keyBytes = Files.readAllBytes(file.toPath());
        return new SecretKeySpec(keyBytes, "AES");
    }

    // ------------------------------------------------------------------
    // Auto-generate AES-256 Key
    // ------------------------------------------------------------------
    private static void generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);   // AES-256
        SecretKey secretKey = keyGen.generateKey();

        try (FileOutputStream fos = new FileOutputStream(KEY_FILE)) {
            fos.write(secretKey.getEncoded());
        }

        System.out.println("AES key auto-generated → aes.key");
    }

    // ------------------------------------------------------------------
    // Encrypt SQL file → .enc
    // ------------------------------------------------------------------
    public static void encryptFile(File inputFile, File outputFile) throws Exception {
        SecretKey key = loadKey();

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        // Generate random IV
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);

        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

        try (FileOutputStream fos = new FileOutputStream(outputFile);
             FileInputStream fis = new FileInputStream(inputFile)) {

            // Write IV at start of file
            fos.write(iv);

            CipherOutputStream cos = new CipherOutputStream(fos, cipher);

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                cos.write(buffer, 0, bytesRead);
            }

            cos.close();
        }
    }

    // ------------------------------------------------------------------
    // Decrypt .enc → SQL
    // ------------------------------------------------------------------
    public static void decryptFile(File inputFile, File outputFile) throws Exception {
        SecretKey key = loadKey();

        try (FileInputStream fis = new FileInputStream(inputFile)) {

            // Read IV
            byte[] iv = new byte[16];
            fis.read(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

            CipherInputStream cis = new CipherInputStream(fis, cipher);
            FileOutputStream fos = new FileOutputStream(outputFile);

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = cis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }

            fos.close();
            cis.close();
        }
    }
}
