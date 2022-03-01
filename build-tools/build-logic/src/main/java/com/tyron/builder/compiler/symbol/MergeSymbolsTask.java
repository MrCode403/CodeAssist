package com.tyron.builder.compiler.symbol;

import android.util.Log;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.compiler.resource.AAPT2Compiler;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.project.cache.CacheHolder;
import com.tyron.common.util.Cache;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Class that loads R.txt files generated by AAPT/AAPT2  and converts them
 * to R.java class files
 */
public class MergeSymbolsTask extends Task<AndroidModule> {

    public static final CacheHolder.CacheKey<Void, Void> CACHE_KEY =
            new CacheHolder.CacheKey<>("mergeSymbolsCache");

    private File mSymbolOutputDir;
    private File mFullResourceFile;

    public MergeSymbolsTask(AndroidModule project, ILogger logger) {
        super(project, project, logger);
    }

    @Override
    public String getName() {
        return "SymbolProcessor";
    }

    @Override
    public void prepare(BuildType type) throws IOException {
        mSymbolOutputDir = new File(getModule().getBuildDirectory(), "gen");
        mFullResourceFile = new File(getModule().getBuildDirectory(), "bin/res/R.txt");
    }

    @Override
    public void run() throws IOException, CompilationFailedException {
        Cache<Void, Void> cache = getModule().getCache(CACHE_KEY, new Cache<>());
        SymbolLoader fullSymbolValues = null;
        Multimap<String, SymbolLoader> libMap = ArrayListMultimap.create();

        List<File> RFiles = new ArrayList<>();
        for (File library : getModule().getLibraries()) {
            File parent = library.getParentFile();
            if (parent == null) {
                getLogger().error("Unable to access parent directory for " + library);
                continue;
            }

            String packageName = AAPT2Compiler.getPackageName(new File(parent, "AndroidManifest.xml"));
            if (packageName == null) {
                continue;
            }

            if (packageName.equals(getModule().getPackageName())) {
                // only generate libraries
                continue;
            }

            File rFile = new File(parent, "R.txt");
            if (!rFile.exists()) {
                continue;
            }

            RFiles.add(rFile);
        }

        for (Cache.Key<Void> key : new HashSet<>(cache.getKeys())) {
            if (!RFiles.contains(key.file.toFile())) {
                Log.d("MergeSymbolsTask", "Found deleted resource file, removing " + key.file.toFile().getName() + " on the cache.");
                cache.remove(key.file, (Void) null);
                FileUtils.delete(key.file.toFile());
            }
        }

        for (File rFile : RFiles) {

            if (!cache.needs(rFile.toPath(), null)) {
                continue;
            }

            File parent = rFile.getParentFile();
            if (parent == null) {
                getLogger().error("Unable to access parent directory for " + rFile);
                continue;
            }

            String packageName = AAPT2Compiler.getPackageName(new File(parent, "AndroidManifest.xml"));
            if (packageName == null) {
                continue;
            }

            if (fullSymbolValues == null) {
                fullSymbolValues = new SymbolLoader(mFullResourceFile, getLogger());
                fullSymbolValues.load();
            }
            SymbolLoader libSymbols = new SymbolLoader(rFile, getLogger());
            libSymbols.load();

            libMap.put(packageName, libSymbols);
        }

        // now loop on all the package name, merge all the symbols to write, and write them
        for (String packageName : libMap.keySet()) {
            Collection<SymbolLoader> symbols = libMap.get(packageName);

            SymbolWriter writer = new SymbolWriter(mSymbolOutputDir.getAbsolutePath(), packageName,
                    fullSymbolValues, getModule());
            for (SymbolLoader loader : symbols) {
                writer.addSymbolsToWrite(loader);
            }
            writer.write();
        }

        for (File file : RFiles) {
            cache.load(file.toPath(), null, null);
        }
    }
}
