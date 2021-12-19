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

/**
 * This class is used to store fragments of a long input FASTA 
 * sequence as an array of bytes.
 * 
 * @author Gianluca Roscigno
 * 
 * @version 1.0  
 */
public class PartialSequence extends Record { 
	 
	private static final long serialVersionUID = 6820343484812253133L; 

	private String header;
	private int bytesToProcess;

	@Override
	public String getKey(){
		return header;
	}
	
	public String getValue2(){
		return new String(getBuffer(), getStartValue(), bytesToProcess);
	}
	
	public String toString2() {
		return getStartValue() > 0 ? ">" + getKey() + "\n" + getValue2() : getValue2();
	}

	@Override
	public String toString() {
		return "PartialSequence [header=" + header + 
				", bufferSize=" + getValueLength() +
				", startValue=" + getStartValue() + 
				", endValue=" + getEndValue() + 
				", bytesToProcess=" + bytesToProcess + "]";
	}

	public int getBytesToProcess() {
		return bytesToProcess;
	}

	public void setBytesToProcess(int bytesToProcess) {
		this.bytesToProcess = bytesToProcess;
	}

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}
}