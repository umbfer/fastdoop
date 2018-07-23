# FASTdoop
[![Analytics](https://ga-beacon.appspot.com/UA-122272492-1/fastdoop)](https://github.com/umbfer/fastdoop)

### Introduction

FASTdoop is a generic Hadoop library for the management of FASTA and FASTQ files. It includes
three input reader formats with associated record readers. These readers are optimized to
read data efficiently from FASTA/FASTQ files in a variety of settings. They are:

* _FASTAshortInputFileFormat_: optimized to read a collection of short sequences from a FASTA file.
* _FASTAlongInputFileFormat_: optimized to read a very large sequence (even gigabytes long) from a FASTA file.
* _FASTQInputFileFormat_: optimized to read a collection of short sequences from a FASTQ file.


### Using FASTdoop

As a preliminary step, in order to use FASTdoop in an Hadoop application, the FASTdoop jar
file must be included in the classpath of the virtual machines used to run that application. 
Then, it is possible to use one of the readers coming with FASTdoop by running the standard
setInputFormatClass method.

The HDFS file systems splits large files in smaller blocks of fixed size (default: 128M) called input splits. This may cause problems when parsing large FAST/FASTA/FASTQ files as a sequence may cross two or more blocks. By default, FASTdoop requires that the worker owning the input split containing the beginning of a sequence is in charge of retrieving that entire sequence. This could require that worker to ask the  worker owning the next input split for the bytes that are needed to complete that sequence, according to a user-defined look ahead buffer. (Notice that FASTdoop does not currently allow to read an entire sequence in one single record if this spans more than two blocks) Instead, if a worker owns a split containing the ending part of a sequence starting elsewhere, this part is ignored by the worker when looking for sequences to read. 

When dealing with very long sequences (e.g., assembled genomes) there may be need of having different workers process different blocks of the same input file. In such a case, it may be required for a worker to have along with its input splits, also a certain number of the bytes available in the initial part of the following input splits (e.g., when doing k-mers counting, if the last character of an input split belongs to a sequence, it has to be processed together with the first k-1 characters of the following input split).

It is possible to alter the behavior of FASTdoop in these cases by modifying the following configuration parameters using the _Configuration_ class available in Apache Hadoop. 
* _k_: determines how many bytes from the initial part of the next input split (if any) should be retrieved together with the bytes of the current input split (if any) when reading a sequence not ending before the end of the split. (This parameter is available for only the _LongReadsRecordReader_ class).
* _look_ahead_buffer_size_: is the number of bytes coming from the initial part of the next input split and used (eventually( to complete a sequence being read in the current split. It cannot be longer than the size of the input split. (This parameter is available for the _FASTQReadsRecordReader_ and _ShortReadsRecordReader_ classes).

This is an example where a file containing one long sequence encoded in FASTA format is loaded using the _FASTAlongInputFileFormat_ FASTdoop class:

```java
public class TestLongFastdoop {
	
	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		conf.setInt("k", 50);
		
		Job job = Job.getInstance(conf, "FASTdoop Test Long");
		
		String inputPath = "data/big.fasta";
		FileInputFormat.addInputPath(job, new Path(inputPath));
		FileOutputFormat.setOutputPath(job, new Path("output"));
		
		job.setJarByClass(TestLongFastdoop.class);
		
		job.setInputFormatClass(FASTAlongInputFileFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(PartialSequence.class);
		
		job.setMapperClass(MyMapper.class);
		
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
	
	public static class MyMapper extends Mapper<Text, PartialSequence, Text, Text> {
		@Override
		public void map(Text nullKey, PartialSequence sequence, Context context) throws IOException, InterruptedException {
			// the standard output is written as log in: 
			// $HADOOP_HOME/los/userlogs/application_<timestamp>_<id>/*/stdout
			System.out.println("ID: " + sequence.getKey());
			System.out.println("Sequence: " + sequence.getValue());
			
			context.write(new Text(sequence.getKey()), new Text(sequence.getValue()));
		}
	}
}
```

Instead, this is an example where a file containing one or more short sequences encoded in FASTA format are loaded using the _FASTAshortInputFileFormat_ FASTdoop class:

```java
public class TestShortFastdoop {
	
	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		conf.setInt("look_ahead_buffer_size", 4096);
		
		Job job = Job.getInstance(conf, "FASTdoop Test Short");
		
		String inputPath = "data/short.fasta";
		FileInputFormat.addInputPath(job, new Path(inputPath));
		FileOutputFormat.setOutputPath(job, new Path("output"));
		
		job.setJarByClass(TestShortFastdoop.class);
		
		job.setInputFormatClass(FASTAshortInputFileFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Record.class);
		
		job.setMapperClass(MyMapper.class);
		
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
	
	public static class MyMapper extends Mapper<Text, Record, Text, Text> {
		
		@Override
		public void map(Text nullKey, Record sequence, Context context) throws IOException, InterruptedException {
			// the standard output is written as log in: 
			// $HADOOP_HOME/los/userlogs/application_<timestamp>_<id>/*/stdout
			System.out.println("ID: " + sequence.getKey());
			System.out.println("Sequence: " + sequence.getValue());
		
			context.write(new Text(sequence.getKey()), new Text(sequence.getValue()));
		}
	}
}
```

Finally, this is an example where a file containing one or more sequences encoded in FASTQ format are loaded using the _FASTQInputFileFormat_ FASTdoop class:

```java
Configuration conf = new Configuration();
		conf.setInt("look_ahead_buffer_size", 2048);
		
		Job job = Job.getInstance(conf, "FASTdoop Test FASTQ");
		
		String inputPath = "data/small.fastq";
		FileInputFormat.addInputPath(job, new Path(inputPath));
		FileOutputFormat.setOutputPath(job, new Path("output"));
		
		job.setJarByClass(TestFASTQFastdoop.class);
		
		job.setInputFormatClass(FASTQInputFileFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(QRecord.class);
		
		job.setMapperClass(MyMapper.class);
		
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
	
	public static class MyMapper extends Mapper<Text, QRecord, Text, Text> {
		
		@Override
		public void map(Text nullKey, QRecord sequence, Context context) throws IOException, InterruptedException {
			// the standard output is written as log in: 
			// $HADOOP_HOME/los/userlogs/application_<timestamp>_<id>/*/stdout
			System.out.println("ID: " + sequence.getKey());
			System.out.println("Sequence: " + sequence.getValue());
		
			context.write(new Text(sequence.getKey()), new Text(sequence.getValue()));
		}
	}
```
	
A usage example of the three readers is provided in the directory src/fastoop/test.


### Building FASTdoop

(Using Maven)
As an alternative to using the provided jar, it is possible to build FASTdoop from scratch
starting from the source files. In this case, the building of FASTdoop would not require the installation of Hadoop,
as it can be managed using the provided Maven project. In this case, it is only required to clone the repository and load the project inside Eclipse or another IDE. Then, the FASTdoop jar could be created by issuing the Maven install procedure (w.g., clicking on the ```Run As > Maven install``` option if using Eclipse).

The Maven dependecies are:
* [Apache Hadoop Common 2.7.0](https://mvnrepository.com/artifact/org.apache.hadoop/hadoop-common/2.7.0)
* [Apache Hadoop MapReduce Core 2.7.0](https://mvnrepository.com/artifact/org.apache.hadoop/hadoop-mapreduce-client-core/2.7.0)

The building process can also be issued via terminal, by moving in the FASTdoop main directory and running the following command-line:

```console
mvn install
```
(Using Ant)
You can also build FASTdoop from scratch using the Hadoop libraries installed on your own computer. 
The compilation process uses the __ant__ software (see http://ant.apache.org). Be also sure to have
the ```$HADOOP_HOME``` environment variable set at the Hadoop installation path and, then,
run the following commands from the shell:

```console
cd FASTdoop-1.0/
ant build clean
ant build
ant build createjar
```

At the end of the compilation, the FASTdoop-1.0.jar file will be created in the current
directory.


### Usage Examples

FASTdoop comes with three test classes that can be used to parse the content of FASTA/FASTQ
files. The source code of these classes is available in the src directory. The following 
examples assume that the java classpath environment variable is set to include the jar files
of a standard Hadoop installation (>=2.7.0).

Example 1: Print on screen all the short sequences contained in FASTdoop-1.0/data/short.fasta

```console
java -cp FASTdoop-1.0.jar fastdoop.test.TestFShort data/short.fasta
```

Example 2: Print on screen the long sequence contained in fastdoop-1.0/data/long.fasta

```console
java -cp FASTdoop-1.0.jar fastdoop.test.TestFLong data/long.fasta
```

Example 3:  Print on screen all the short sequences contained in fastdoop-1.0/data/short.fastq

```console
java -cp FASTdoop-1.0.jar fastdoop.test.TestFQ data/short.fastq
```

## Datasets

The datasets used for our experiments can be downloaded from the following links: 

* [Datasets used for testing FASTAlongInputFileFormat (about 12 GB)](https://goo.gl/PBACD2)
* [Datasets used for testing FASTAshortInputFileFormat (about 8 GB)](https://goo.gl/34MYxI)
* [Datasets used for testing FASTQInputFileFormat (about 10 GB)](https://goo.gl/ZmJs7A)


## Citation

If you have used FASTdoop for research purposes, please cite the paper related to this software as reference in your publication. Use the following BibTex entry to do that.

```
@article{doi:10.1093/bioinformatics/btx010,
author = {Ferraro Petrillo, Umberto and Roscigno, Gianluca and Cattaneo, Giuseppe and Giancarlo, Raffaele},
title = {FASTdoop: a versatile and efficient library for the input of FASTA and FASTQ files for MapReduce Hadoop bioinformatics applications},
journal = {Bioinformatics},
volume = {33},
number = {10},
pages = {1575-1577},
year = {2017},
doi = {10.1093/bioinformatics/btx010},
}
```
