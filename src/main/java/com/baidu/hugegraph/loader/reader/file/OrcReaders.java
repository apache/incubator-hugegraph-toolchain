package com.baidu.hugegraph.loader.reader.file;

import com.baidu.hugegraph.loader.constant.Constants;
import com.baidu.hugegraph.loader.exception.LoadException;
import com.baidu.hugegraph.loader.reader.Line;
import com.baidu.hugegraph.loader.reader.Readable;
import com.baidu.hugegraph.loader.source.file.FileSource;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.util.Log;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.io.orc.OrcFile;
import org.apache.hadoop.hive.ql.io.orc.Reader;
import org.apache.hadoop.hive.ql.io.orc.RecordReader;
import org.apache.hadoop.hive.serde2.objectinspector.StructField;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class OrcReaders extends Readers {

    private static final Logger LOG = Log.logger(OrcReaders.class);

    private OrcReader orcReader;
    private Configuration conf;

    public OrcReaders(FileSource source, List<Readable> readables,
                      Configuration conf) {
        super(source, readables);
        this.conf = conf;
    }

    @Override
    public Line readNextLine() throws IOException {
        // Open the first file need to read
        if (this.orcReader == null &&
           (this.orcReader = this.openNext()) == null) {
            return null;
        }

        if (!this.orcReader.recordReader.hasNext()) {
            return null;
        }

        this.orcReader.row = this.orcReader.recordReader.next(this.orcReader.row);
        String[] names = parseHeader(this.orcReader.inspector);
        Object[] values = this.orcReader.inspector.getStructFieldsDataAsList(
                          this.orcReader.row).stream().map(Object::toString)
                                             .toArray();

        String rowLine = StringUtils.join(values, Constants.COMMA_STR);
        return new Line(rowLine, names, values);
    }

    @Override
    public void close() throws IOException {
        if (this.index < this.readables.size()) {
            Readable readable = this.readables.get(this.index);
            LOG.debug("Ready to close '{}'", readable);
        }
        this.orcReader.close();
    }

    @Override
    public String[] readHeader() {
        E.checkArgument(this.readables.size() > 0,
                        "Must contain at least one readable file");
        Reader reader = openReader(this.readables.get(0));
        StructObjectInspector inspector =
                (StructObjectInspector) reader.getObjectInspector();
        return parseHeader(inspector);
    }

    private String[] parseHeader(StructObjectInspector inspector) {
        List<? extends StructField> fields = inspector.getAllStructFieldRefs();
        return fields.stream().map(StructField::getFieldName)
                     .collect(Collectors.toList())
                     .toArray(new String[]{});
    }


    private Reader openReader(Readable readable) {
        Path filePath = new Path(source.path());
        try {
            return OrcFile.createReader(filePath, OrcFile.readerOptions(this.conf));
        } catch (IOException e) {
            throw new LoadException("Failed to open orcReader for '%s'",
                                    e, readable);
        }
    }

    private OrcReader openNext() {
        if (++this.index >= this.readables.size()) {
            return null;
        }

        Readable readable = this.readables.get(this.index);
        if (this.checkLoaded(readable)) {
            return this.openNext();
        }

        try {
            return new OrcReader(this.openReader(readable));
        } catch (IOException e) {
            throw new LoadException("Failed to create orc reader for '%s'",
                                    e, readable);
        }
    }

    private static class OrcReader {

        private Reader reader;
        private RecordReader recordReader;
        private StructObjectInspector inspector;
        private Object row;

        public OrcReader(Reader reader) throws IOException {
            this.reader = reader;
            this.recordReader = reader.rows();
            this.inspector = (StructObjectInspector) reader.getObjectInspector();
            this.row = null;
        }

        public void close() throws IOException {
            if (this.recordReader != null) {
                this.recordReader.close();
            }
        }
    }
}
