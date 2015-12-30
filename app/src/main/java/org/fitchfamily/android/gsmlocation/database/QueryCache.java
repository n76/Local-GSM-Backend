package org.fitchfamily.android.gsmlocation.database;

import android.location.Location;
import android.support.v4.util.LruCache;

public class QueryCache {
    private static final int SIZE = 10000;

    /**
     * DB negative query cache (not found in db).
     */
    private final LruCache<QueryArgs, Boolean> queryResultNegativeCache = new LruCache<>(SIZE);

    /**
     * DB positive query cache (found in the db).
     */
    private final LruCache<QueryArgs, Location> queryResultCache = new LruCache<>(SIZE);

    /**
     * Saves an result in the cache
     * @param args the QueryArgs
     * @param location If a location was resolved, the Location, otherwise null
     * @return this object
     */
    public QueryCache put(QueryArgs args, Location location) {
        if(location == null) {
            queryResultNegativeCache.put(args, true);
        } else {
            queryResultCache.put(args, location);
        }

        return this;
    }

    public QueryCache putUnresolved(QueryArgs queryArgs) {
        return put(queryArgs, null);
    }

    public boolean contains(QueryArgs args) {
        return queryResultNegativeCache.get(args) != null ||
                queryResultCache.get(args) != null;
    }

    /**
     * Queries the cache with the given QueryArgs
     * @param args
     * @return the Location, or null if not cached or not resolved
     */
    public Location get(QueryArgs args) {
        return queryResultCache.get(args);
    }

    public QueryCache clear() {
        queryResultCache.evictAll();
        queryResultNegativeCache.evictAll();

        return this;
    }
}
