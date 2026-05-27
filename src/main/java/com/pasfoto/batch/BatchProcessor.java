package com.pasfoto.batch;

import com.pasfoto.controller.PhotoController;
import com.pasfoto.model.BackgroundColor;
import com.pasfoto.model.PhotoSize;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchProcessor {
    public List<Path> process(
            List<File> files,
            Path outputDir,
            PhotoController controller,
            PhotoSize size,
            BackgroundColor backgroundColor,
            boolean autoCenter,
            String removerMethod,
            String extension
    ) {
        AtomicInteger counter = new AtomicInteger(0);

        return files.parallelStream()
                .map(file -> {
                    try {
                        System.out.println("Memproses " + counter.incrementAndGet() + "/" + files.size() + ": " + file.getName());
                        return controller.processAndExport(
                                file,
                                outputDir,
                                size,
                                backgroundColor,
                                autoCenter,
                                removerMethod,
                                extension
                        );
                    } catch (Exception e) {
                        throw new RuntimeException("Gagal memproses file: " + file.getName(), e);
                    }
                })
                .toList();
    }
}
