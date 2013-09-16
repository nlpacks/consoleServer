package org.integration.roger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class StreamPumper extends Thread {
	private BufferedReader din;
	private boolean endOfStream = false;
	private static final int SLEEP_TIME = 5;
	private PrintStream log;

	public StreamPumper(InputStream is,PrintStream log) {
		this.din = new BufferedReader(new InputStreamReader(is));
		if (log == null)
			this.log=System.out;
		else
			this.log=log;
		setName(log.getClass().getName()+String.valueOf(System.currentTimeMillis()));
		
	}

	public void pumpStream() throws IOException {
		if (!endOfStream) {
			String line = din.readLine();

			if (line != null) {
				this.log.println(line);
			} else {
				endOfStream = true;
			}
		}
	}

	public void run() {
		try {
			try {
				while (!endOfStream) {
					pumpStream();
					sleep(SLEEP_TIME);
				}
			} catch (InterruptedException ie) {
				// ignore
			}
			din.close();
		} catch (IOException ioe) {
			// ignore
		}
	}
}