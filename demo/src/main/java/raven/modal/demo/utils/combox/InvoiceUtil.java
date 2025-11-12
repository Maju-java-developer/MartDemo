package raven.modal.demo.utils.combox;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class InvoiceUtil {

    private static final Random random = new Random();

    public static String generateInvoiceNumber() {
        // Example format: INV-20251110-83472
        String datePart = new SimpleDateFormat("yyyyMMdd").format(new Date());
        int randomPart = 10000 + random.nextInt(90000); // 5-digit random number
        return "INV-" + datePart + "-" + randomPart;
    }

    public static void main(String[] args) {
        System.out.println(generateInvoiceNumber());
    }
}

