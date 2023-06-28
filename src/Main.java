import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.sql.*;
import java.util.*;
import org.json.simple.*;
import org.json.simple.parser.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Main {
    public static void main(String args[]) throws IOException {
        String url = "https://api.weather.gov/gridpoints/TOP/31,80/forecast";
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yy");
        String fileName = formatter.format(date) + "-Forecast.json";

        DownloadWebPage(url, fileName);              // Downloads webpage as .json
        String fcDescription = ParseDoc(fileName);   // Parses the document to get only forecast info
        forecastToDatabase(fcDescription);           // Sends forecast desc to MySQL
        outputDatabase();                            // Outputs database temps to console
    }

    public static void DownloadWebPage(String webpage, String pageName) {
        // Code used from: https://www.geeksforgeeks.org/download-web-page-using-java/
        try {
            URL url = new URL(webpage);
            BufferedReader readr = new BufferedReader(new InputStreamReader(url.openStream()));
            BufferedWriter writer = new BufferedWriter(new FileWriter(pageName));
            String line;
            while ((line = readr.readLine()) != null) {
                writer.write(line + "\n");
            }
            readr.close();
            writer.close();
        }
        catch (MalformedURLException mue) {
            System.out.println("Malformed URL Exception raised");
        }
        catch (IOException ie) {
            System.out.println("IOException raised");
        }
    }

    public static String ParseDoc(String fileName){
        JSONParser parser = new JSONParser();
        try {
            // Needs to step through each nested element of .json document
            Object obj = parser.parse(new FileReader(fileName));
            JSONObject jsonObject = (JSONObject)obj;
            JSONObject properties = (JSONObject)jsonObject.get("properties");
            JSONArray periods = (JSONArray)properties.get("periods");
            JSONObject desiredPeriod = (JSONObject)periods.get(0);
            Long temp = (Long)desiredPeriod.get("temperature");
            String tempString = temp.toString();
            return tempString;

        } catch(Exception e) {
            e.printStackTrace();
        }
        return "Failed to retrieve forecast.";
    }



    public static void forecastToDatabase(String fc) {

        if (Objects.equals(fc, "Failed to retrieve forecast.")) {
            System.out.println("No forecast detected, data not sent");
        }
        else {
            Connection connection = null;
            try {
                connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/weather-database", "root", "2awftQ&btm");
                Statement statement = connection.createStatement();
                String sqlString = "INSERT INTO forecasts(fcDate, fcTemp) VALUES (CURDATE(), '" + fc + "')";
                statement.executeUpdate(sqlString);

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static void outputDatabase() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/weather-database", "root", "2awftQ&btm");
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("select * from forecasts");

            while(resultSet.next()) {
                System.out.println(resultSet.getString("fcTemp"));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}