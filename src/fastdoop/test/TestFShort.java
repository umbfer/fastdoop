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

import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.util.*;

import fastdoop.FASTAlongInputFileFormat;
import fastdoop.FASTAshortInputFileFormat;
import fastdoop.Record;

import org.apache.hadoop.mapreduce.lib.input.*; 
import org.apache.hadoop.mapreduce.lib.output.*;

/**
 * A Simple Hadoop application useful to test {@code FASTAlongInputFileFormat}.
 * 
 * @author Gianluca Roscigno
 * 
 * @version 1.0
 * 
 * @see FASTAlongInputFileFormat
 */


public class TestFShort extends Configured implements Tool {

	
	public static void main(String args[]) {
		if (args.length<1){
			System.out.println("Usage: TestFShort input_file");
			System.exit(1);
		}

		try {
			ToolRunner.run(new TestFShort(), args);
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
		}
	}

	

	public int run(String[] args) throws Exception {
		String hdfsInputFile = args[0];
		String hdfsOutputFile = "dummy";
		Path hdfsInputPath = new Path(hdfsInputFile);
		Path hdfsOutputPath = new Path(hdfsOutputFile);
		Configuration conf = getConf();
		String jobname = "FASTAshortInputFileFormat Test";
		Job job = Job.getInstance(conf, jobname);
		FileSystem fs = FileSystem.get(conf);
		fs.delete(hdfsOutputPath, true);
		job.setNumReduceTasks(0);
		FileInputFormat.setInputPaths(job, hdfsInputPath);
		FileOutputFormat.setOutputPath(job, hdfsOutputPath);
		job.setJarByClass(this.getClass());
		job.setInputFormatClass(FASTAshortInputFileFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		job.setMapOutputKeyClass(NullWritable.class);
		job.setMapOutputValueClass(NullWritable.class);
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(NullWritable.class);
		job.setMapperClass(MyMapper.class);
		return job.waitForCompletion(true) ? 0 : 1;
	}

	public static class MyMapper extends Mapper<NullWritable, Record, NullWritable, NullWritable> {
		@Override
		public void map(NullWritable key, Record value, Context context) throws IOException, InterruptedException {
			String header = value.getKey();
			String sequence = value.getValue();
			
			System.out.println("Header: " + header);
			System.out.println("Sequence: " + sequence);
		}
	}
}