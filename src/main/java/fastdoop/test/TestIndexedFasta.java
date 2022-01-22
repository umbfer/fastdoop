/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fastdoop.test;

import java.io.IOException;

import fastdoop.IndexedFastaFormat;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.util.*;

import fastdoop.FASTAlongInputFileFormat;
import fastdoop.FASTAshortInputFileFormat;
import fastdoop.PartialSequence;

import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;

/**
 * A Simple Hadoop application useful to test {@code IndexedFastaFormat}.
 *
 * @author Johan Nyström-Persson
 *
 * @version 1.0
 *
 * @see FASTAshortInputFileFormat
 */


public class TestIndexedFasta extends Configured implements Tool {

    public static void main(String args[]) {
        if (args.length<1){
            System.out.println("Usage: TestIndexedFasta input_file");
            System.exit(1);
        }

        try {
            ToolRunner.run(new TestIndexedFasta(), args);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int run(String[] args) throws Exception {
        String hdfsInputFile = args[0];
        String hdfsOutputFile = "dummy8";
        Path hdfsInputPath = new Path(hdfsInputFile);
        Path hdfsOutputPath = new Path(hdfsOutputFile);
        Configuration conf = getConf();
        conf.set("k","100");
        String jobname = "IndexedFastaFormat Test";
        Job job = Job.getInstance(conf, jobname);
        job.setNumReduceTasks(0);
        FileInputFormat.setInputPaths(job, hdfsInputPath);
        FileOutputFormat.setOutputPath(job, hdfsOutputPath);
        job.setJarByClass(this.getClass());
        job.setInputFormatClass(IndexedFastaFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        job.setMapOutputKeyClass(NullWritable.class);
        job.setMapOutputValueClass(NullWritable.class);
        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(NullWritable.class);
        job.setMapperClass(MyMapper.class);
        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static class MyMapper extends Mapper<Text, PartialSequence, NullWritable, NullWritable> {
        @Override
        public void map(Text key, PartialSequence value, Context context) {
            String header = value.getKey();
            String sequence = value.getValue();
            long position = value.getSeqPosition();
            boolean complete = value.isComplete();

            System.out.println("Header: " + header);
            if (sequence.length() > 100) {
                System.out.println("Sequence (first 100, last 100): " + sequence.substring(0, 100) + " ... " +
                        sequence.substring(sequence.length() - 100, sequence.length()));
            } else {
                System.out.println("Sequence: " + sequence);
            }
            System.out.println("Position: " + position);
            System.out.println("Complete: " + complete);

        }
    }
}