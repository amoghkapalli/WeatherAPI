import com.google.gson.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Amogh Kapalli
 * WeatherAPI.java
 * This program takes one argument as a city and then connects to three different APIs and displays some information about
 * the city. The first API produces information about the weather, the second about nearby cities in a 25 mile radius
 * with a population over 100000, and the third API produces information about the air quality and its impact.
 */
public class WeatherAPI{
    private static String weatherappID="07259dea2d24dee6a7c42e38826cba3a";
    private static String rapidAPIID="2c014d45a0mshcabe7740d205a80p1e07bajsn98bd6090d856";

    public static void main(String[] args) throws IOException, URISyntaxException {
        
        if(args.length<1){
            System.out.println("Please enter only a city");
            System.exit(1);
        }
        String city=args[0];

         

        //String city="hyderabad";
        String weatherURL="http://api.openweathermap.org/data/2.5/weather?q=" + city + "&units=imperial&appid=" + weatherappID;

        try{
            System.out.println("Connecting to first API...");
            JsonElement response=WeatherAPI.weatherAPIJSON(weatherURL);
            //if first api is not accessible, the program quits
            if(response==null){
                throw new IOException("The first API was not accessible.");
            }
            //parsing the first JsonObject to get the coordinates, temperature, feels like temp, and the wind speed
            JsonObject rootObject = response.getAsJsonObject();
            JsonObject child=rootObject.get("coord").getAsJsonObject();
            String longitude = child.get("lon").toString();
            System.out.println("Longitude: " + longitude);
            String latitude=child.get("lat").toString();
            System.out.println("Latitude: " + latitude);
            double kelvinCurrTemp = Double.parseDouble(rootObject.get("main").getAsJsonObject().get("temp").toString());
            System.out.println("Temperature: " + String.format("%.2f",kelvinCurrTemp) + " fahrenheit");

            double kelvinFeelsTemp = Double.parseDouble(rootObject.get("main").getAsJsonObject().get("feels_like").toString());
            System.out.println("Feels Like: " + String.format("%.2f",kelvinFeelsTemp) + " fahrenheit");

            String windSpeed=rootObject.get("wind").getAsJsonObject().get("speed").toString();
            System.out.println("Wind Speed: " + windSpeed + " miles/hour \n");


            //SECOND API CALLS
            System.out.println("Connecting to second API...");
            String secondURL="https://countries-cities.p.rapidapi.com/location/city/nearby?latitude=" + latitude + "&longitude=" + longitude + "&radius=25&min_population=100000&max_population=1000000000&per_page=10";
            String secondHost="countries-cities.p.rapidapi.com";
            JsonElement secondCall=makeApiCall(secondHost, secondURL);
            //if second or third apis are not accessible, program still continues.
            if(secondCall==null){
                System.out.println("The second API was not accessible.");
            }
            else {
                JsonObject jsonObject = secondCall.getAsJsonObject();
                JsonArray cities = jsonObject.getAsJsonArray("cities");
                String originalCity = "\"" + city.substring(0, 1).toUpperCase() + city.substring(1) + "\"";
                System.out.println("Nearby cities in a 25 mile radius with a population over 100000:");
                for (JsonElement nearbyCity : cities) {
                    JsonObject cityArr = nearbyCity.getAsJsonObject();
                    String childCity = cityArr.get("name").toString();
                    int population = Integer.parseInt(cityArr.get("population").toString());
                    if (!(childCity.equals(originalCity))) {
                        System.out.println(childCity + " population of: " + population);
                    }
                }
            }
            System.out.println();
            
            System.out.println("Connecting to third API...");
            String thirdURL="https://air-quality-by-api-ninjas.p.rapidapi.com/v1/airquality?lat=" + latitude + "&lon=" + longitude;
            String thirdHost="air-quality-by-api-ninjas.p.rapidapi.com";
            JsonElement thirdCall=makeApiCall(thirdHost, thirdURL);
            if(thirdCall==null){
                System.out.println("The third API was not accessible.");
            }
            else{
                //parsing third Json Response by printing out the AQI and its interpretation, the source is where I 
                // recieved information about what AQI is
                System.out.println("Carbon Monoxide Concentration: "+ thirdCall.getAsJsonObject().get("CO").getAsJsonObject().get("concentration").toString());
                int aqi=Integer.parseInt(thirdCall.getAsJsonObject().get("overall_aqi").toString());
                System.out.println("Air Quality Index: " + aqi);
                if(aqi<=50){
                    System.out.println("The range shows that the air quality is good and it poses no health threat.");
                } else if(aqi<=100){
                    System.out.println("This range is moderate and the quality is acceptable. Some people may experience discomfort.");
                }else if(aqi<=150){
                    System.out.println("The air quality in this range is unhealthy for sensitive groups. They experience breathing discomfort.");
                } else if(aqi<=200){
                    System.out.println("The range shows unhealthy air quality and people start to experience effects such as breathing difficulty.");
                }else{
                    System.out.println("This is the hazardous category of air quality and serious health impacts such as breathing discomfort, suffocation, airway irritation, etc. may be experienced by all.");
                }
                //source: https://www.pranaair.com/us/blog/what-is-air-quality-index-aqi-and-its-calculation/
            }

        } catch (IOException | InterruptedException e) {
            System.out.println(e.toString());
        }
    }

    /**
     * This method utilizes proper retry logic with a exponential back-off to wait after very request where the response code is not 200.
     * This method returns null if the response code is a 400 or 500 code even after the maximum amount of retries
     * @param weatherURL: this contains the URL to access the first openweather API which contains the key and the city provided
     * @return JsonElement containing the information returned by the API call
     * @throws IOException
     */
    private static JsonElement weatherAPIJSON(String weatherURL) throws IOException {
        int backoffMillis = 1;
        int attempts = 0;
        int responseCode = 0;
        HttpURLConnection connection = null;
        while (attempts < 4) {
            try {
                URL apiEndpoint = new URL(weatherURL);
                connection = (HttpURLConnection) apiEndpoint.openConnection();
                connection.setRequestMethod("GET");

                responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    break;
                }
                else if(responseCode>=400 && responseCode<500){
                    TimeUnit.SECONDS.sleep(backoffMillis);
                    backoffMillis *= 2;
                    attempts++;
                }
                else if(responseCode>=500 && responseCode<600){
                    // Wait before retrying the request
                    TimeUnit.SECONDS.sleep(backoffMillis);
                    backoffMillis *= 2;
                    attempts++;
                }

            } catch (IOException | InterruptedException e) {
                System.out.println("Invalid URL");
                return null;
            }
        }
        //if statements to check if the maximum attempts was reached
        if(responseCode>=400 && responseCode<500 && attempts>=4){
            System.out.println("Maximum attempts reached and city inputted was not found");
            return null;
        }
        else if(responseCode>=500 && responseCode<600 && attempts>=4){
            System.out.println("Maximum attempts reached and Server ERROR");
            return null;
        }
        Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        return JsonParser.parseReader(reader);
    }

    /**
     * The purpose of this method is the same however it takes in two arguments instead of 1 as this is meant for the RapidAPIs
     * I used in this program.
     * @param method contains the method
     * @param url contains the base url for the API being utilized (air quality or nearby cities)
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static JsonElement makeApiCall(String method, String url) throws IOException, InterruptedException {
        int backoffMillis = 1;
        int attempts = 0;
        HttpURLConnection connection = null;
        int responseCode=0;
        while (attempts < 4) {
            try {
                URL apiEndpoint = new URL(url);
                connection = (HttpURLConnection) apiEndpoint.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("x-rapidapi-host", method);
                connection.setRequestProperty("x-rapidapi-key", rapidAPIID);

                responseCode=connection.getResponseCode();
                if (responseCode == 200) {
                    break;
                }
                else if(responseCode>=400 && responseCode<500){

                    TimeUnit.SECONDS.sleep(backoffMillis);
                    backoffMillis *= 2;
                    attempts++;
                }
                else if(responseCode>=500 && responseCode<600){
                    // Wait before retrying the request
                    TimeUnit.SECONDS.sleep(backoffMillis);
                    backoffMillis *= 2;
                    attempts++;
                }
            } catch (IOException e) {
                System.out.println("Invalid URL");
                return null;
            }
        }
        if(responseCode>=400 && responseCode<500 && attempts>=4){
            System.out.println("Maximum attempts reached and city inputted was not found");
            return null;
        }
        else if(responseCode>=500 && responseCode<600 && attempts>=4){
            System.out.println("Maximum attempts reached and Server ERROR");
            return null;
        }
        Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        return JsonParser.parseReader(reader);
    }

}
