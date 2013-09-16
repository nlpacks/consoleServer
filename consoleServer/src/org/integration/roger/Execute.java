package org.integration.roger;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

public class Execute {
	public int execute(String command, File log) throws IOException,
			InterruptedException {

		PrintStream fos = null;
		int exit = -1;
		try {
			if (log != null)
				fos = new PrintStream(log);

			Process proc = Runtime.getRuntime().exec(command);

			// copy input and error to the output stream
			StreamPumper inputPumper = new StreamPumper(proc.getInputStream(),
					fos);
			StreamPumper errorPumper = new StreamPumper(proc.getErrorStream(),
					fos);

			// starts pumping away the generated output/error
			inputPumper.start();
			errorPumper.start();

			// Wait for everything to finish
			proc.waitFor();
			inputPumper.join();
			errorPumper.join();
			proc.destroy();

			proc.waitFor();
			exit = proc.exitValue();
		} finally {
			if (fos != null)
				fos.close();
		}

		return exit;
	}
}
