package com.github.thesilentpro.headdb.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

public class Utils {

    private static final Pattern SPACE_PATTERN = Pattern.compile(" ");
    private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);

    /**
     * Splits the given list into sublists of the given chunkSize.
     *
     * @param <T>       the type of elements in the list
     * @param list      the list to split
     * @param chunkSize the maximum size of each chunk
     * @return a list of sublists, each at most chunkSize elements long
     */
    public static <T> List<List<T>> chunk(List<T> list, int chunkSize) {
        List<List<T>> chunks = new ArrayList<>();
        int size = list.size();
        for (int i = 0; i < size; i += chunkSize) {
            int end = Math.min(size, i + chunkSize);
            // subList returns a view, if you want an independent copy use new ArrayList<>(...)
            chunks.add(new ArrayList<>(list.subList(i, end)));
        }
        return chunks;
    }

    public static boolean matches(@Nullable String text, @Nullable String query) {
        if (text == null || query == null) {
            return false;
        }
        if (text.equalsIgnoreCase(query)) {
            return true;
        }
        for (String part : SPACE_PATTERN.split(text)) {
            if (part.equalsIgnoreCase(query)) {
                return true;
            }
        }
        return false;
    }


    public static ExecutorService executorService(int nThreads, String namePrefix) {
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, namePrefix + " #" + POOL_NUMBER.getAndIncrement());
            t.setDaemon(true);
            return t;
        };
        return nThreads > 1 ? Executors.newFixedThreadPool(nThreads, factory) : Executors.newSingleThreadExecutor(factory);
    }


}
