# FASTdoop
[![Analytics](https://ga-beacon.appspot.com/UA-122272492-1/fastdoop)](https://github.com/umbfer/fastdoop)

### Introduction

FASTdoop is a generic Hadoop library for the management of FASTA and FASTQ files. It includes
three input reader formats with associated record readers. These readers are optimized to
read data efficiently from FASTA/FASTQ files in a variety of settings. They are:

* FASTAshortInputFileFormat: optimized to read a collection of short sequences from a FASTA file.
* FASTAlongInputFileFormat: optimized to read a very large sequence (even gigabytes long) from a FASTA file.
* FASTQInputFileFormat: optimized to read a collection of short sequences from a FASTQ file.


### Using FASTdoop

As a preliminary step, in order to use FASTdoop in an Hadoop application, the FASTdoop jar
file must be included in the classpath of the virtual machines used to run that application. 
Then, it is possible to use one of the readers coming with FASTdoop by running the standard
setInputFormatClass method.

This is an example where a file containing one long sequence encoded in FASTA format is loaded using the FASTdoop class _FASTAlongInputFileFormat_:

```java
public class TestLongFastdoop {
	
	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
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

Instead, this is an example where a file containing one short sequence encoded in FASTA format is loaded using the FASTdoop class _FASTAshortInputFileFormat_:

```java
public class TestShortFastdoop {
	
	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
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
		
Notice that ```FASTAshortInputFileFormat``` and ```FASTQInputFileFormat``` takes no parameters while 
```FASTAlongInputFileFormat``` allows the user to specify, when processing a split, how much
characters of the following input split should be analyzed as well. This option has been 
added to handle cases like k-mer counting where a sequence of characters may begin in a
split and end in the following one. A usage example of the three readers is provided in
the directory src/fastoop/test.


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
