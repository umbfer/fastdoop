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

package fastdoop

import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.{InputSplit, RecordReader, TaskAttemptContext}
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat

/**
 * Hadoop input format for FASTA files with an accompanying .fai index file.
 *
 * @see [[IndexedFastaReader]]
 *
 * @author Johan Nyström-Persson
 *
 * @version 1.0
 */
class IndexedFastaFormat extends FileInputFormat[Text, PartialSequence] {
  override def createRecordReader(split: InputSplit, context: TaskAttemptContext): RecordReader[Text, PartialSequence] =
    new IndexedFastaReader()
}
