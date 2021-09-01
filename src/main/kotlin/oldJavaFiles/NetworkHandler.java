package oldJavaFiles;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Scanner;

public class NetworkHandler {
    private static final double stepSecRatioX = 1296000d / 13500d;
    private static final double stepSecRatioY = 1296000d / 13500d;
    public static String name = "";
    public static String typ = "";

    public static void main(String[] args) {
        System.out.println(getSteps()[0]);
    }

    public static int[] getSteps() {
        String msg;
        try {
            InputStream inputStream = new URL("http://localhost:8090/api/objects/info").openStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            msg = bufferedReader.readLine();
        } catch (Exception e) {
            return new int[]{};
        }
        String[] parts = msg.split("<br>");
        name = parts[0].split(">")[2].split("<")[0];
        typ = parts[0].split("<b>")[1].split("</b>")[0];
        msg = parts[3];
        Scanner scanner = new Scanner(msg).useDelimiter("[^0-9]+");
        int stepsX = (int) ((scanner.nextInt() * 3600 + scanner.nextInt() * 60 + scanner.nextInt()) / stepSecRatioX);
        scanner.nextInt();
        int stepsY = (int) ((scanner.nextInt() * 3600 + scanner.nextInt() * 60 + scanner.nextInt()) / stepSecRatioY);
        return new int[]{stepsX, stepsY};
    }
}
