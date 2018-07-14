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
package fastdoop;

import java.io.EOFException;
import java.io.IOException;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.RecordReader;

/**
 * 
 * @author Gianluca Roscigno
 * 
 * @version 1.0
 * 
 *          Date: Nov, 22 2016
 * 
 *          This class reads <key, value> pairs from an InputSplit.
 *          The input file is in FASTA format.
 *          A FASTA record has a header line that is the key, and data lines
 *          that are the value.
 *          >header...
 *          data
 *          ...
 * 
 * 
 *          Example:
 *          >Seq1
 *          TAATCCCAAATGATTATATCCTTCTCCGATCGCTAGCTATACCTTCCAGGCGATGAACTTAGACGGAATCCACTTTGCTA
 *          CAACGCGATGACTCAACCGCCATGGTGGTACTAGTCGCGGAAAAGAAAGAGTAAACGCCAACGGGCTAGACACACTAATC
 *          CTCCGTCCCCAACAGGTATGATACCGTTGGCTTCACTTCTA
 *          >Seq2
 *          CTACATTCGTAATCTCTTTGTCAGTCCTCCCGTACGTTGGCAAAGGTTCACTGGAAAAATTGCCGACGCACAGGTGCCGG
 *          GCCGTGAATAGGGCCAGATGAACAAGGAAATAATCACCACCGAGGTGTGACATGCCCTCTCGGGCAACCACTCTTCCTCA
 *          TACCCCCTCTGGGCTAACTCGGAGCAAAGAACTTGGTAA
 *          ...
 */

public class ShortReadsRecordReader extends RecordReader<NullWritable, Record> {

	private FSDataInputStream inputFile;

	private long startByte;

	private NullWritable currKey;

	private Record currValue;

	public static final int KV_BUFFER_SIZE = 2000; 

	/*
	 * Used to buffer the content of the input split
	 */
	private byte[] myInputSplitBuffer;

	/*
	 * Auxiliary buffer used to store the ending buffer of this input split and
	 * the initial bytes of the next split
	 */
	private byte[] borderBuffer;

	/*
	 * Marks the current position in the input split buffer
	 */
	private int posBuffer;

	/*
	 * Stores the size of the input split buffer
	 */
	private int sizeBuffer;

	/*
	 * True, if we processed the entire input split buffer. False, otherwise
	 */
	private boolean endMyInputSplit = false;

	public ShortReadsRecordReader() {
		super();
	}

	@Override
	public void initialize(InputSplit genericSplit, TaskAttemptContext context)
			throws IOException, InterruptedException {

		posBuffer = 0;
		Configuration job = context.getConfiguration();


		/*
		 * We open the file corresponding to the input split and
		 * start processing it
		 */
		FileSplit split = (FileSplit) genericSplit;
		Path path = split.getPath();
		startByte = split.getStart();
		inputFile = path.getFileSystem(job).open(path);
		inputFile.seek(startByte);

		currKey = NullWritable.get();
		currValue = new Record();

		/*
		 * We read the whole content of the split in memory using
		 * myInputSplitBuffer. Plus, we read in the memory the first
		 * KV_BUFFER_SIZE of the next split
		 */

		myInputSplitBuffer = new byte[(int) split.getLength()];
		currValue.buffer = myInputSplitBuffer;

		borderBuffer = new byte[KV_BUFFER_SIZE];

		sizeBuffer = inputFile.read(startByte, myInputSplitBuffer, 0, myInputSplitBuffer.length);
		inputFile.seek(startByte + sizeBuffer);

		if (sizeBuffer <= 0) {
			endMyInputSplit = true;
			return;
		}

		/*
		 * We move the starting pointer past the first occurrence of the '>'
		 * symbol as we assume these characters
		 * will be processed together with the previous split
		 */
		for (int i = 0; i < sizeBuffer; i++) {
			if (myInputSplitBuffer[i] == '>') {
				posBuffer = i + 1;
				break;
			}
		}

		if (posBuffer == 0) {
			endMyInputSplit = true;
		}

	}

	@Override
	public boolean nextKeyValue() throws IOException, InterruptedException {

		if (endMyInputSplit)
			return false;

		boolean nextsplitKey = false;
		boolean nextsplitValue = false;

		currValue.startKey = posBuffer;

		/*
		 * We look for the next short sequence my moving posBuffer until a
		 * newline character is found.
		 * End of split is implicitly managed through
		 * ArrayIndexOutOfBoundsException handling
		 */
		try {
			while (myInputSplitBuffer[posBuffer] != '\n') {
				posBuffer++;
			}
		} catch (ArrayIndexOutOfBoundsException e) {

			/*
			 * If we reached the end of the split while scanning a sequence, we
			 * use nextsplitKey to remember that more characters have to be
			 * fetched from the next split for retrieving the key
			 */
			endMyInputSplit = true;
			nextsplitKey = true;
		}

		currValue.endKey = (posBuffer - 1);

		if (!endMyInputSplit) {
			/*
			 * Assuming there are more characters from the current split to
			 * process, we move forward the pointer
			 * until the symbol '>' is found
			 */
			currValue.startValue = (posBuffer + 1);

			try {
				while (myInputSplitBuffer[posBuffer] != '>') {
					posBuffer++;
				}

				currValue.endValue = (posBuffer - 2);
				posBuffer++;

			} catch (ArrayIndexOutOfBoundsException e) {
				/*
				 * If we reached the end of the split while scanning a sequence,
				 * we use nextsplitValue to remember that more characters have
				 * to be fetched from the next split for retrieving the value
				 */
				endMyInputSplit = true;
				nextsplitValue = true;
				currValue.endValue = (posBuffer - 1);

			}

		}

		/*
		 * The end of the split has been reached
		 */
		if (endMyInputSplit) {

			/*
			 * First, we check if we reached the end of the HDFS file (not of
			 * the split)
			 */
			if (inputFile.available() == 0) {
				int c = 0;

				for (int i = posBuffer - 1; i >= 0; i--) {
					if (((char) myInputSplitBuffer[i]) != '\n')
						break;

					c++;
				}

				currValue.endValue = (posBuffer - 1) - c;

				return true;
			}

			/*
			 * If there is another split after this one and we still need to
			 * retrieve the
			 * key of the current record, we switch to borderbuffer to fetch all
			 * the remaining characters
			 */
			if (nextsplitKey) {

				currValue.buffer = (borderBuffer);

				int j = posBuffer - currValue.startKey;

				System.arraycopy(myInputSplitBuffer, currValue.startKey, borderBuffer, 0, j);

				posBuffer = j;

				currValue.startKey = 0;
				nextsplitValue = true;

				try {
					while (true) {

						byte b = (byte) inputFile.readByte();

						if (b == '\n') {
							break;
						}

						borderBuffer[j++] = b;

					}
				} catch (EOFException e) {

				}

				if (!nextsplitValue)
					return false;

				currValue.endKey = (j - 1);
			}

			/*
			 * If there is another split after this one and we still need to
			 * retrieve the value of the current record, we switch to
			 * borderbuffer to fetch all the remaining characters
			 */
			if (nextsplitValue) {

				if (!nextsplitKey) {

					currValue.buffer = borderBuffer;

					int j = currValue.endKey + 1 - currValue.startKey;
					System.arraycopy(myInputSplitBuffer, currValue.startKey, borderBuffer, 0, j);

					currValue.startKey = 0;
					currValue.endKey = (j - 1);

					int start = currValue.startValue;
					currValue.startValue = j;

					if ((currValue.endValue + 1 - start) > 0) // TODO VERIFICARE
						System.arraycopy(myInputSplitBuffer, start, borderBuffer, j, (currValue.endValue + 1 - start));
					posBuffer = j + currValue.endValue + 1 - start;

					currValue.endValue = posBuffer;

				} else {
					posBuffer = currValue.endKey + 1;
					currValue.startValue = posBuffer;
				}

				byte b = 'a';

				try {
					while (true) {

						b = (byte) inputFile.readByte();

						if (b == '>')
							break;

						borderBuffer[posBuffer++] = b;
					}
				} catch (EOFException e) {
				}

				if (b == '>')
					currValue.endValue = (posBuffer - 2);
				else {

					int c = 0;

					for (int i = posBuffer - 1; i >= 0; i--) {

						if (((char) borderBuffer[i]) != '\n')
							break;

						c++;
					}

					currValue.endValue = (posBuffer - 1) - c;

				}

			}
		}

		return true;

	}

	@Override
	public float getProgress() throws IOException, InterruptedException {
		return sizeBuffer > 0 ? posBuffer / sizeBuffer : 1;
	}

	@Override
	public void close() throws IOException {

		if (inputFile != null)
			inputFile.close();
	}

	@Override
	public NullWritable getCurrentKey() throws IOException, InterruptedException {
		return currKey;
	}

	@Override
	public Record getCurrentValue() throws IOException, InterruptedException {
		return currValue;
	}

}
