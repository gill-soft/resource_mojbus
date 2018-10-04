package com.gillsoft.client;

import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.gillsoft.cache.IOCacheException;
import com.gillsoft.cache.RedisMemoryCache;
import com.gillsoft.util.ContextProvider;

public class UpdateTripsTask implements Runnable, Serializable {

	private static final long serialVersionUID = -612450869121241871L;

	private URI key;

	public UpdateTripsTask() {

	}

	public UpdateTripsTask(URI key) {
		this.key = key;
	}

	@Override
	public void run() {
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, RestClient.TRIPS_CACHE_KEY + key.getQuery());
		params.put(RedisMemoryCache.UPDATE_TASK, this);
		params.put(RedisMemoryCache.UPDATE_DELAY, Config.getCacheTripUpdateDelay());
		TripPackage tripPackage = null;

		// получаем рейсы для создания кэша
		RestClient client = ContextProvider.getBean(RestClient.class);
		try {
			tripPackage = client.getTrips(key);
			params.put(RedisMemoryCache.TIME_TO_LIVE, getTimeToLive(tripPackage));
		} catch (ResponseError e) {
			// ошибку поиска тоже кладем в кэш но с другим временем жизни
			params.put(RedisMemoryCache.TIME_TO_LIVE, Config.getCacheErrorTimeToLive());
			params.put(RedisMemoryCache.UPDATE_DELAY, Config.getCacheErrorUpdateDelay());
			tripPackage = new TripPackage();
			tripPackage.setError(e);
		}
		try {
			client.getCache().write(tripPackage, params);
		} catch (IOCacheException e) {
			e.printStackTrace();
		}
	}

	// время жизни до момента самого позднего отправления
	private long getTimeToLive(TripPackage tripPackage) {
		if (Config.getCacheTripTimeToLive() != 0) {
			return Config.getCacheTripTimeToLive();
		}
		long max = 0;
		if (tripPackage.getCourseList() != null && !tripPackage.getCourseList().isEmpty()) {
			for (com.gillsoft.model.Course course : tripPackage.getCourseList()) {
				if (course.getFinishDateTime() != null && course.getFinishDateTime().getTime() > max) {
					max = course.getFinishDateTime().getTime();
				}
			}
			return max - System.currentTimeMillis();
		}
		return 0;
	}

}
