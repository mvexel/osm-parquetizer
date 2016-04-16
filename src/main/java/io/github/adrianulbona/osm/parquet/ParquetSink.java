package io.github.adrianulbona.osm.parquet;

import org.apache.parquet.hadoop.ParquetWriter;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static java.lang.String.format;


public class ParquetSink<T extends Entity> implements Sink {

    private final Path source;
    private final EntityType entityType;
    private final List<Predicate<T>> filters;

    private ParquetWriter<T> writer;

    public ParquetSink(Path source, EntityType entityType) {
        this.source = source;
        this.entityType = entityType;
        this.filters = new ArrayList<>();
    }

    @Override
    public void initialize(Map<String, Object> metaData) {
        final String pbfName = source.getFileName().toString();
        final String entityName = entityType.name().toLowerCase();
        final Path parentFolder = source.toAbsolutePath().getParent();
        final Path destination = parentFolder.resolve(format("%s.%s.parquet", pbfName, entityName));
        try {
            this.writer = ParquetWriterFactory.buildFor(destination.toAbsolutePath().toString(), entityType);
        } catch (IOException e) {
            throw new RuntimeException("Unable to build writers", e);
        }
    }

    @Override
    public void process(EntityContainer entityContainer) {
        try {
            if (this.entityType == entityContainer.getEntity().getType()) {
                final T entity = (T) entityContainer.getEntity();
                if (filters.stream().noneMatch(filter -> filter.test(entity))) {
                    writer.write(entity);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to write entity", e);
        }
    }

    @Override
    public void complete() {
        try {
            this.writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Unable to close writers", e);
        }
    }

    @Override
    public void release() {

    }

    public void addFilter(Predicate<T> predicate) {
        this.filters.add(predicate);
    }

    public void removeFilter(Predicate<T> predicate) {
        this.filters.remove(predicate);
    }
}