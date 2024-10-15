package org.apache.hugegraph.loader.direct.outputformat;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.rocksdb.Options;
import org.rocksdb.RocksDBException;
import org.rocksdb.SstFileWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SSTFileOutputFormat extends FileOutputFormat<byte[], byte[]> {

    @Override
    public RecordWriter<byte[], byte[]> getRecordWriter(TaskAttemptContext job) throws IOException {
        Path outputPath = getDefaultWorkFile(job, ".sst");
        FileSystem fs = outputPath.getFileSystem(job.getConfiguration());
        FSDataOutputStream fileOut = fs.create(outputPath, false);
        return new RocksDBSSTFileRecordWriter(fileOut, outputPath, fs);
    }

    public static class RocksDBSSTFileRecordWriter extends RecordWriter<byte[], byte[]> {
        private final FSDataOutputStream out;
        private final SstFileWriter sstFileWriter;
        private final Path outputPath;
        private final FileSystem fs;
        private final File localSSTFile;
        private boolean hasData = false; // 标记是否有数据写入

        public RocksDBSSTFileRecordWriter(FSDataOutputStream out, Path outputPath, FileSystem fs) throws IOException {
            this.out = out;
            this.outputPath = outputPath;
            this.fs = fs;
            Options options = new Options();
            options.setCreateIfMissing(true);
            this.localSSTFile = File.createTempFile("sstfile", ".sst");
            this.sstFileWriter = new SstFileWriter(new org.rocksdb.EnvOptions(), options);
            try {
                this.sstFileWriter.open(localSSTFile.getAbsolutePath());
            } catch (RocksDBException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void write(byte[] key, byte[] value) throws IOException {
            try {
                sstFileWriter.put(key, value);
                if (!hasData) {
                    hasData = true; // 仅在第一次写入时设置
                }
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public void close(TaskAttemptContext context) throws IOException {
            try {
                if (hasData) { // 只有在有数据写入时才进行后续操作
                    sstFileWriter.finish();
                    try (InputStream in = new FileInputStream(localSSTFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                    out.close();
                } else {
                    // 没有数据写入时删除临时文件
                    localSSTFile.delete();
                    out.close();
                    fs.delete(outputPath, false);
                }
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }
}
