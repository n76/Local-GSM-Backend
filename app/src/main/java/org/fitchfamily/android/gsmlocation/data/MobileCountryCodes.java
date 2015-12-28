package org.fitchfamily.android.gsmlocation.data;

import android.content.Context;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MobileCountryCodes {
    private static final Object lock = new Object();

    private static MobileCountryCodes instance;

    private Map<String, Set<Integer>> areaToNumber = new HashMap<>();

    private Map<Integer, String> numberToArea = new HashMap<>();

    private MobileCountryCodes(InputStream inputStream) throws IOException {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

            String line;

            while (((line = reader.readLine())) != null) {
                if (TextUtils.isEmpty(line) || line.startsWith("#")) {
                    continue;
                }

                final String[] splitted = line.split(":");

                if (splitted.length != 2) {
                    throw new IOException("defect mcc.txt");
                }

                final String area = splitted[0].toLowerCase(Locale.ENGLISH);
                final Set<Integer> regions = new HashSet<>();

                for (String region : splitted[1].toLowerCase(Locale.ENGLISH).split(",")) {
                    final int regionNumber = Integer.parseInt(region);
                    regions.add(regionNumber);
                    numberToArea.put(regionNumber, area);
                }

                areaToNumber.put(area, Collections.unmodifiableSet(regions));
            }

            areaToNumber = Collections.unmodifiableMap(areaToNumber);
            numberToArea = Collections.unmodifiableMap(numberToArea);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    public static MobileCountryCodes with(Context context) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    try {
                        instance = new MobileCountryCodes(context.getAssets().open("mcc.txt"));
                    } catch (IOException ex) {
                        throw new AssertionError("the asset mcc.txt is invalid");
                    }
                }
            }
        }

        return instance;
    }

    public Regions getAreas(Set<Integer> numbers) {
        Set<String> regions = new HashSet<>();
        boolean unresolved = false;

        if (numbers != null) {
            for (int number : numbers) {
                String area = numberToArea.get(number);

                if (area != null) {
                    if (!regions.contains(area)) {
                        regions.add(area);
                    }
                } else {
                    unresolved = true;
                }
            }

            // remove all regions where not all numbers were given

            Iterator<String> area = regions.iterator();
            while (area.hasNext()) {
                for (int number : areaToNumber.get(area.next())) {
                    if (!numbers.contains(number)) {
                        area.remove();
                        unresolved = true;
                        break;
                    }
                }
            }
        }

        return new Regions(Collections.unmodifiableSet(regions), unresolved);
    }

    public Map<String, Set<Integer>> getAreas() {
        return areaToNumber;
    }

    public String getArea(int number) {
        return numberToArea.get(number);
    }

    public Set<Integer> getNumbers(String area) {
        return areaToNumber.get(area);
    }

    public static class Regions {
        private final Set<String> areas;

        private final boolean containsUnresolved;

        private Regions(Set<String> areas, boolean containsUnresolved) {
            this.areas = areas;
            this.containsUnresolved = containsUnresolved;
        }

        public Set<String> areas() {
            return areas;
        }

        public boolean containsUnresolved() {
            return containsUnresolved;
        }
    }
}
