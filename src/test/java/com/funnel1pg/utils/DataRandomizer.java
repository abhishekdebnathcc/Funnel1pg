package com.funnel1pg.utils;

import com.funnel1pg.config.ConfigReader;

import java.util.Random;

/**
 * DataRandomizer – generates unique, believable American test data for every run.
 *
 * State:    Randomly selected from all 50 US state codes (select by value on the dropdown).
 * Phone:    Plain 10-digit US number, no formatting.
 * Zip:      Exactly 5 digits, correlated with state.
 * Email:    Override via -Demail= or email= in config.properties.
 */
public class DataRandomizer {

    private static final Random RNG = new Random();

    private static final String FIRST_NAME;
    private static final String LAST_NAME;
    private static final String EMAIL;
    private static final String PHONE;
    private static final String ADDRESS;
    private static final String CITY;
    private static final String ZIP;
    

    // ── Name pools ────────────────────────────────────────────────────────────

    private static final String[] FIRST_NAMES = {
        "James", "John", "Robert", "Michael", "William", "David", "Richard", "Joseph",
        "Thomas", "Charles", "Christopher", "Daniel", "Matthew", "Anthony", "Mark",
        "Donald", "Steven", "Paul", "Andrew", "Joshua", "Kenneth", "Kevin", "Brian",
        "Mary", "Patricia", "Jennifer", "Linda", "Barbara", "Elizabeth", "Susan",
        "Jessica", "Sarah", "Karen", "Lisa", "Nancy", "Betty", "Margaret", "Sandra",
        "Ashley", "Dorothy", "Kimberly", "Emily", "Donna", "Michelle", "Carol",
        "Amanda", "Melissa", "Deborah", "Stephanie", "Rebecca", "Sharon", "Laura"
    };

    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
        "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson",
        "Thomas", "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson",
        "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson",
        "Walker", "Young", "Allen", "King", "Wright", "Scott", "Torres", "Nguyen",
        "Hill", "Flores", "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera",
        "Campbell", "Mitchell", "Carter", "Roberts"
    };

    private static final String[] STREET_NAMES = {
        "Oak", "Maple", "Cedar", "Pine", "Elm", "Walnut", "Birch", "Spruce",
        "Willow", "Chestnut", "Lincoln", "Washington", "Jefferson", "Madison",
        "Franklin", "Highland", "Sunset", "Lakewood", "Riverside", "Meadow",
        "Forest", "Valley", "Ridge", "Summit", "Hillside", "Orchard", "Park"
    };

    private static final String[] STREET_TYPES = {
        "Street", "Avenue", "Boulevard", "Drive", "Road", "Lane", "Way",
        "Court", "Place", "Circle", "Trail", "Terrace"
    };

    // City/zip pool for display only (country+state selected live from dropdown)
    private static final String[][] LOCATIONS = {
        {"New York",       "100"}, {"Los Angeles",    "900"}, {"Chicago",        "606"},
        {"Houston",        "770"}, {"Phoenix",        "850"}, {"Philadelphia",   "191"},
        {"San Antonio",    "782"}, {"San Diego",      "921"}, {"Dallas",         "752"},
        {"San Jose",       "951"}, {"Austin",         "787"}, {"Jacksonville",   "322"},
        {"Fort Worth",     "761"}, {"Columbus",       "432"}, {"Charlotte",      "282"},
        {"Indianapolis",   "462"}, {"San Francisco",  "941"}, {"Seattle",        "981"},
        {"Denver",         "802"}, {"Nashville",      "372"}, {"Oklahoma City",  "731"},
        {"Las Vegas",      "891"}, {"Portland",       "972"}, {"Memphis",        "381"},
        {"Boston",         "021"}, {"Louisville",     "402"}, {"Baltimore",      "212"},
        {"Milwaukee",      "532"}, {"Albuquerque",    "871"}, {"Tucson",         "857"},
        {"Fresno",         "937"}, {"Sacramento",     "958"}, {"Kansas City",    "641"},
        {"Atlanta",        "303"}, {"Miami",          "331"}, {"Minneapolis",    "554"},
        {"Tampa",          "336"}, {"New Orleans",    "701"}, {"Cleveland",      "441"},
        {"Honolulu",       "968"}
    };

    static {
        String firstName = pick(FIRST_NAMES);
        String lastName  = pick(LAST_NAMES);
        FIRST_NAME = firstName;
        LAST_NAME  = lastName;

        String emailOverride = ConfigReader.getEmailOverride();
        EMAIL = (emailOverride != null && !emailOverride.isEmpty())
                ? emailOverride
                : firstName.toLowerCase() + "." + lastName.toLowerCase()
                  + (1000 + RNG.nextInt(9000)) + "@mailinator.com";

        // Plain 10-digit phone, no formatting
        int areaCode   = 200 + RNG.nextInt(700);
        int exchange   = 200 + RNG.nextInt(700);
        int subscriber = 1000 + RNG.nextInt(9000);
        PHONE = String.format("%d%d%d", areaCode, exchange, subscriber);

        ADDRESS = (100 + RNG.nextInt(9900)) + " " + pick(STREET_NAMES) + " " + pick(STREET_TYPES);

        // Random city/zip for display (country+state selected live from form dropdown)
        String[] loc = LOCATIONS[RNG.nextInt(LOCATIONS.length)];
        CITY = loc[0];
        ZIP  = loc[1] + String.format("%02d", RNG.nextInt(100));
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public static String getCustomerField(String field) {
        switch (field.toLowerCase()) {
            case "firstname": return FIRST_NAME;
            case "lastname":  return LAST_NAME;
            case "email":     return EMAIL;
            case "phone":     return PHONE;
            case "address":   return ADDRESS;
            case "city":      return CITY;
            case "zipcode":   return ZIP;
            default:          return "";
        }
    }

    public static void printIdentity() {
        System.out.println("┌─────────────────────────────────────────────");
        System.out.println("│  Generated Test Identity");
        System.out.println("│  Name    : " + FIRST_NAME + " " + LAST_NAME);
        System.out.println("│  Email   : " + EMAIL);
        System.out.println("│  Phone   : " + PHONE);
        System.out.println("│  Address : " + ADDRESS);
        System.out.println("│  City    : " + CITY + " " + ZIP + " (country+state from live dropdown)");
        System.out.println("└─────────────────────────────────────────────");
    }

    private static String pick(String[] arr) {
        return arr[RNG.nextInt(arr.length)];
    }
}
